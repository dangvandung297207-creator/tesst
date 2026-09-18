package com.animeki.ability;

import com.animeki.AnimeKi;
import com.animeki.combat.CombatService;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.flight.DashService;
import com.animeki.flight.FlightService;
import com.animeki.ki.KiService;
import com.animeki.network.AnimeKiNetwork;
import com.animeki.network.Payloads;
import com.animeki.registry.ModAttachments;
import com.animeki.transformation.TransformationService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Server side driver of the ability framework.
 *
 * <p>Owns exactly one running ability per player, translates input actions into ability calls,
 * enforces cooldowns and Ki costs and forwards everything else to the ability itself.</p>
 */
public final class AbilityService {
    /** Safety net so a misbehaving ability can never lock a player's skill slot forever. */
    private static final int MAX_ABILITY_TICKS = 20 * 30;

    private static final Map<UUID, ActiveAbility> ACTIVE = new HashMap<>();

    private AbilityService() {
    }

    // ------------------------------------------------------------------ input

    public static void handleInput(ServerPlayer player, AbilityAction action, Vec3 input) {
        float forward = (float) input.x;
        float strafe = (float) input.z;
        boolean sprint = player.isSprinting();
        boolean sneak = player.isShiftKeyDown();
        boolean jump = false;
        switch (action) {
            case KI_CHARGE_START -> {
                if (!KiService.startCharging(player)) {
                    feedback(player, FeedbackReason.REFUSED, "");
                }
            }
            case KI_CHARGE_STOP -> KiService.stopCharging(player);
            case KI_BLAST_START -> start(player, AbilityRegistry.byId(AnimeKi.MOD_ID + ":ki_blast"), forward, strafe, sprint, sneak, jump);
            case PRIMARY_START -> start(player, primaryAbility(), forward, strafe, sprint, sneak, jump);
            case SECONDARY_START -> start(player, secondaryAbility(), forward, strafe, sprint, sneak, jump);
            case ULTIMATE_START -> start(player, ultimateAbility(), forward, strafe, sprint, sneak, jump);
            case KI_BLAST_RELEASE, PRIMARY_RELEASE, SECONDARY_RELEASE, ULTIMATE_RELEASE -> release(player);
            case DASH -> DashService.tryDash(player, forward, strafe, sprint);
            case TRANSFORM -> TransformationService.cycle(player);
            case FLIGHT_TOGGLE -> FlightService.toggle(player);
            case CANCEL -> cancel(player, AbilityStopReason.CANCELLED);
        }
    }

    @Nullable
    public static Ability primaryAbility() {
        return AbilityRegistry.byId(AnimeKiServerConfig.ABILITIES.primarySkill.get());
    }

    @Nullable
    public static Ability secondaryAbility() {
        return AbilityRegistry.byId(AnimeKiServerConfig.ABILITIES.secondarySkill.get());
    }

    @Nullable
    public static Ability ultimateAbility() {
        return AbilityRegistry.byId(AnimeKiServerConfig.ABILITIES.ultimateSkill.get());
    }

    // ------------------------------------------------------------- lifecycle

    public static boolean start(ServerPlayer player, @Nullable Ability ability, float forward, float strafe,
                                boolean sprint, boolean sneak, boolean jump) {
        if (ability == null) {
            return false;
        }
        if (active(player) != null) {
            feedback(player, FeedbackReason.BUSY, ability.id());
            return false;
        }
        int remaining = cooldownRemaining(player, ability.id());
        if (remaining > 0) {
            feedback(player, FeedbackReason.ON_COOLDOWN, ability.id());
            return false;
        }
        AbilityContext context = AbilityContext.of(player, forward, strafe, sprint, sneak, jump);
        if (!ability.canStart(player, context)) {
            feedback(player, FeedbackReason.REFUSED, ability.id());
            return false;
        }
        if (ability.stats().kiCost() > 0.0D && !KiService.tryConsume(player, ability.stats().kiCost(), true)) {
            feedback(player, FeedbackReason.NOT_ENOUGH_KI, ability.id());
            return false;
        }

        long gameTime = player.level().getGameTime();
        ActiveAbility active = new ActiveAbility(ability, context, gameTime);
        ACTIVE.put(player.getUUID(), active);
        ModAttachments.abilities(player).setActiveAbilityId(ability.id());
        if (ability.stats().cooldownTicks() > 0) {
            setCooldown(player, ability.id(), ability.stats().cooldownTicks());
        }
        ability.onStart(player, context);

        if (ability.stats().castType() == AbilityStats.CastType.INSTANT) {
            finish(player, active, AbilityStopReason.COMPLETED);
        }
        AnimeKiNetwork.syncPlayerState(player);
        return true;
    }

    public static void tick(ServerPlayer player) {
        ActiveAbility active = ACTIVE.get(player.getUUID());
        if (active == null) {
            return;
        }
        Ability ability = active.ability();
        AbilityContext context = active.context();

        if (!player.isAlive()) {
            finish(player, active, AbilityStopReason.DEATH);
            return;
        }
        active.tick();
        if (active.ticks() > MAX_ABILITY_TICKS) {
            finish(player, active, AbilityStopReason.INTERRUPTED);
            return;
        }
        if (ability.cancelOnMovement() && moved(player, active)) {
            finish(player, active, AbilityStopReason.INTERRUPTED);
            return;
        }

        AbilityStats stats = ability.stats();
        if (stats.castType() == AbilityStats.CastType.SCRIPTED) {
            ability.onTick(player, context, active);
        } else if (!active.charged()) {
            active.addChargeTick();
            ability.onChargeTick(player, context, active.chargeTicks());
            if (active.chargeTicks() >= stats.chargeTicks()) {
                active.markCharged();
                active.markReleased();
                ability.onRelease(player, context, active.chargeTicks());
                if (stats.castType() == AbilityStats.CastType.CHARGE_RELEASE
                        && !ability.continuesAfterRelease()) {
                    finish(player, active, AbilityStopReason.COMPLETED);
                    return;
                }
            }
        } else {
            ability.onTick(player, context, active);
        }

        if (active.finished()) {
            finish(player, active, AbilityStopReason.COMPLETED);
        }
    }

    private static boolean moved(ServerPlayer player, ActiveAbility active) {
        Vec3 movement = player.position().subtract(active.context().origin());
        return movement.lengthSqr() > 4.0D;
    }

    public static void release(ServerPlayer player) {
        ActiveAbility active = ACTIVE.get(player.getUUID());
        if (active == null) {
            return;
        }
        AbilityStats.CastType type = active.ability().stats().castType();
        if (active.charged() && active.ability().continuesAfterRelease()) {
            // Cinematic ultimates play themselves out; releasing the key does not abort them.
            return;
        }
        switch (type) {
            case CHANNEL -> {
                if (!active.charged()) {
                    if (active.ability().cancelWhenReleasedEarly()) {
                        finish(player, active, AbilityStopReason.CANCELLED);
                        return;
                    }
                    active.markCharged();
                    active.markReleased();
                    active.ability().onRelease(player, active.context(), active.chargeTicks());
                }
                finish(player, active, AbilityStopReason.RELEASED);
            }
            case SCRIPTED -> {
                active.markReleased();
                finish(player, active, AbilityStopReason.RELEASED);
            }
            case CHARGE_RELEASE -> {
                if (!active.charged()) {
                    active.markCharged();
                    active.markReleased();
                    active.ability().onRelease(player, active.context(), active.chargeTicks());
                }
                finish(player, active, AbilityStopReason.RELEASED);
            }
            default -> {
                // Instant abilities have already finished.
            }
        }
    }

    public static void cancel(ServerPlayer player, AbilityStopReason reason) {
        ActiveAbility active = ACTIVE.get(player.getUUID());
        if (active != null) {
            finish(player, active, reason);
        }
    }

    private static void finish(ServerPlayer player, ActiveAbility active, AbilityStopReason reason) {
        ACTIVE.remove(player.getUUID());
        ModAttachments.abilities(player).setActiveAbilityId("");
        active.markFinished();
        active.ability().onStop(player, active.context(), reason);
    }

    /** Called when the player is hurt: damage interrupts most casts. */
    public static void onPlayerHurt(ServerPlayer player, float amount) {
        ActiveAbility active = ACTIVE.get(player.getUUID());
        if (active != null && active.ability().interruptibleByDamage()) {
            finish(player, active, AbilityStopReason.INTERRUPTED);
        }
    }

    public static void onPlayerRemoved(ServerPlayer player) {
        ActiveAbility active = ACTIVE.get(player.getUUID());
        if (active != null) {
            finish(player, active, AbilityStopReason.LOGOUT);
        }
    }

    @Nullable
    public static ActiveAbility active(ServerPlayer player) {
        return ACTIVE.get(player.getUUID());
    }

    public static boolean isBusy(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    // ------------------------------------------------------------- cooldowns

    public static int cooldownRemaining(ServerPlayer player, String abilityId) {
        long gameTime = player.level().getGameTime();
        return ModAttachments.abilities(player).remaining(abilityId, gameTime);
    }

    public static void setCooldown(ServerPlayer player, String abilityId, int ticks) {
        if (ticks <= 0) {
            return;
        }
        long gameTime = player.level().getGameTime();
        ModAttachments.abilities(player).setCooldown(abilityId, gameTime + ticks);
    }

    public static void resetCooldowns(ServerPlayer player) {
        ModAttachments.abilities(player).clear();
    }

    /** Snapshot used by the HUD: remaining cooldown in ticks for every registered ability. */
    public static int[] cooldownSnapshot(ServerPlayer player) {
        var abilities = AbilityRegistry.all();
        int[] snapshot = new int[abilities.size()];
        int index = 0;
        for (Ability ability : abilities) {
            snapshot[index++] = cooldownRemaining(player, ability.id());
        }
        return snapshot;
    }

    public static String activeAbilityId(ServerPlayer player) {
        return ModAttachments.abilities(player).activeAbilityId();
    }

    public static float activeAbilityProgress(ServerPlayer player) {
        ActiveAbility active = ACTIVE.get(player.getUUID());
        return active == null ? 0.0F : active.progress();
    }

    public static void feedback(ServerPlayer player, FeedbackReason reason, String abilityId) {
        if (reason == FeedbackReason.NONE) {
            return;
        }
        ModAttachments.abilities(player).setLastFeedback(reason);
        ModAttachments.abilities(player).setFeedbackAbilityId(abilityId);
        AnimeKiNetwork.sendToPlayer(player, new Payloads.AbilityFeedback((byte) reason.ordinal(),
                abilityId == null ? "" : abilityId, cooldownRemaining(player, abilityId == null ? "" : abilityId), 0.0D));
    }

    /** Convenience used by abilities that need to stop themselves. */
    public static void stopSelf(@Nullable ServerPlayer player, AbilityStopReason reason) {
        if (player != null) {
            cancel(player, reason);
        }
    }

    /** Swing helper used by melee style abilities. */
    public static void swing(ServerPlayer player) {
        CombatService.swingArm(player);
    }
}
