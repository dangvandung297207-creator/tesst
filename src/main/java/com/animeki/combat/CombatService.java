package com.animeki.combat;

import com.animeki.ability.AbilityService;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.destruction.DestructionProfile;
import com.animeki.destruction.DestructionService;
import com.animeki.ki.KiMath;
import com.animeki.ki.KiService;
import com.animeki.network.AnimeKiNetwork;
import com.animeki.registry.ModAttachments;
import com.animeki.registry.ModDamageTypes;
import com.animeki.transformation.TransformationService;
import com.animeki.util.Targeting;
import com.animeki.vfx.CameraEffectType;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Server authoritative martial arts combat.
 *
 * <p>Handles the combo chain, hit detection (cone + line of sight), damage attribution, knockback,
 * air combat (knock up / knock down), the chase follow up and the impact presentation. Everything
 * the client sends is only "I clicked" - the server decides what that means.</p>
 */
public final class CombatService {
    private CombatService() {
    }

    // ------------------------------------------------------------------ input

    /**
     * Client asked for a strike.
     *
     * <p>{@code requested} is only a <em>hint</em>: the server validates the timing window, the Ki
     * cost and the cooldown in {@link #attack}, and any illegal request is simply refused.</p>
     */
    public static void handleInput(ServerPlayer player, AttackKind requested, boolean heavyModifier) {
        attack(player, requested, heavyModifier);
    }

    /** Runs one strike of the combo chain. */
    public static boolean attack(ServerPlayer player, boolean heavyModifier) {
        return attack(player, null, heavyModifier);
    }

    public static boolean attack(ServerPlayer player, @Nullable AttackKind explicit, boolean heavyModifier) {
        if (!AnimeKiServerConfig.COMBAT.replaceVanillaAttack.get() && explicit == null) {
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        long gameTime = level.getGameTime();
        ComboState combo = ModAttachments.combat(player);
        boolean airborne = !player.onGround() && AnimeKiServerConfig.COMBAT.airCombatEnabled.get();

        int window = airborne ? AnimeKiServerConfig.COMBAT.airComboWindowTicks.get()
                : AnimeKiServerConfig.COMBAT.comboWindowTicks.get();
        boolean chainAlive = gameTime - combo.lastAttackTick() <= window;

        AttackKind kind;
        if (explicit != null) {
            kind = explicit;
        } else if (heavyModifier) {
            kind = airborne ? AttackKind.AIR_SLAM : AttackKind.HEAVY;
        } else {
            kind = AttackKind.forStep(chainAlive ? combo.step() : 0, airborne);
        }

        // Cooldown gate (also protects against packet spam).
        int cooldown = kind.cooldownTicks();
        if (gameTime - combo.lastAttackTick() < cooldown && !heavyModifier) {
            return false;
        }
        if (kind.kiCost() > 0.0D && !KiService.tryConsume(player, kind.kiCost(), true)) {
            AbilityService.feedback(player, com.animeki.ability.FeedbackReason.NOT_ENOUGH_KI, "");
            return false;
        }

        swingArm(player);
        executeAttack(player, kind, combo, gameTime);

        combo.setStep(chainAlive ? combo.step() + 1 : 1);
        combo.setLastAttackTick(gameTime);
        if (combo.step() > 3) {
            combo.setStep(0);
        }
        AnimeKiNetwork.syncPlayerState(player);
        AnimeKiNetwork.syncVisualState(player);
        return true;
    }

    private static void executeAttack(ServerPlayer player, AttackKind kind, ComboState combo, long gameTime) {
        ServerLevel level = (ServerLevel) player.level();
        double range = AnimeKiServerConfig.COMBAT.meleeRange.get();
        double arc = AnimeKiServerConfig.COMBAT.meleeArcDegrees.get();
        Vec3 eyes = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();

        level.playSound(null, player.getX(), player.getY(), player.getZ(), kind.swingSound(), SoundSource.PLAYERS,
                1.0F, kind.heavy() ? 0.8F : 1.1F);

        List<LivingEntity> candidates = Targeting.entitiesInCone(level, eyes, look, range, arc, player,
                entity -> isValidTarget(player, entity));
        if (candidates.isEmpty()) {
            // Whiff: still show the swing so combat reads clearly.
            VfxDispatcher.playTracking(player, VfxEvent.SPARK, eyes.add(look.scale(1.6D)), look, 0.5F,
                    VfxEvent.SPARK.defaultColor());
            return;
        }

        candidates.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
        LivingEntity primary = candidates.get(0);
        if (!hasLineOfSight(level, player, primary)) {
            return;
        }

        float damage = (float) (kind.damage()
                * (1.0D + AnimeKiServerConfig.COMBAT.comboDamageBonusPerHit.get() * combo.step())
                * KiMath.meleeMultiplier(player)
                * TransformationService.offenseMultiplier(player)
                * vanishBonus(player, combo, gameTime));

        applyHit(player, primary, kind, damage, combo, gameTime);

        boolean splash = AnimeKiServerConfig.COMBAT.nearbyHeavyDamage.get() && kind.heavy();
        if (splash) {
            double radius = AnimeKiServerConfig.COMBAT.nearbyHeavyRadius.get();
            for (LivingEntity nearby : candidates) {
                if (nearby == primary) {
                    continue;
                }
                if (nearby.distanceTo(primary) <= radius) {
                    applyHit(player, nearby, kind, damage * 0.5F, combo, gameTime);
                }
            }
        }

        if (kind.heavy() && player.onGround() && AnimeKiServerConfig.DESTRUCTION.enabled.get()) {
            DestructionService.apply(level, primary.position(), DestructionProfile.heavyImpact(1.6D), player, primary);
        }
    }

    private static void applyHit(ServerPlayer player, LivingEntity target, AttackKind kind, float damage,
                                ComboState combo, long gameTime) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 direction = target.position().subtract(player.position());
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            flat = player.getViewVector(1.0F);
        }
        flat = flat.normalize();

        double knockback = kind.knockback();
        double vertical = kind.launch() ? AnimeKiServerConfig.COMBAT.knockUpPower.get() : 0.12D;
        if (kind == AttackKind.AIR_SLAM) {
            vertical = -AnimeKiServerConfig.COMBAT.knockDownPower.get();
            knockback *= 0.35D;
        }
        if (target.onGround() && kind.launch()) {
            vertical = AnimeKiServerConfig.COMBAT.knockUpPower.get();
        }

        target.invulnerableTime = 0;
        boolean hurt = target.hurt(ModDamageTypes.of(level, ModDamageTypes.MARTIAL, player), damage);
        if (hurt) {
            target.push(flat.x * knockback, vertical, flat.z * knockback);
            target.hurtMarked = true;
            combo.setLastTarget(target.getUUID());
            combo.setLastHitTick(gameTime);
            combo.setHits(combo.hits() + 1);
        }

        Vec3 impact = target.getBoundingBox().getCenter();
        VfxDispatcher.impact(level, kind.impactEvent(), impact, flat, kind.heavy() ? 1.2F : 0.8F);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), kind.hitSound(), SoundSource.PLAYERS,
                1.2F, kind.heavy() ? 0.7F : 1.2F);

        if (kind.heavy()) {
            VfxDispatcher.camera(player, CameraEffectType.SHAKE, 0.55F, 8);
            VfxDispatcher.camera(player, CameraEffectType.FOV_PUNCH, 0.12F, 6);
        } else {
            VfxDispatcher.camera(player, CameraEffectType.SHAKE, 0.18F, 4);
        }
    }

    private static double vanishBonus(ServerPlayer player, ComboState combo, long gameTime) {
        if (gameTime >= combo.vanishBonusUntil()) {
            return 1.0D;
        }
        return 1.0D + AnimeKiServerConfig.VANISH.postVanishDamageBonus.get();
    }

    private static boolean hasLineOfSight(ServerLevel level, ServerPlayer player, LivingEntity target) {
        Vec3 from = player.getEyePosition();
        Vec3 to = target.getBoundingBox().getCenter();
        return level.clip(new net.minecraft.world.level.ClipContext(from, to,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player)).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    public static boolean isValidTarget(ServerPlayer player, LivingEntity target) {
        if (target == player || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        if (target instanceof Player) {
            if (!AnimeKiServerConfig.COMBAT.attackPlayers.get()) {
                return false;
            }
            if (!player.canHarmPlayer((Player) target)) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ upkeep

    public static void tick(ServerPlayer player) {
        ComboState combo = ModAttachments.combat(player);
        long gameTime = player.level().getGameTime();
        if (combo.step() > 0) {
            int window = player.onGround() ? AnimeKiServerConfig.COMBAT.comboWindowTicks.get()
                    : AnimeKiServerConfig.COMBAT.airComboWindowTicks.get();
            if (gameTime - combo.lastAttackTick() > window) {
                combo.reset();
                AnimeKiNetwork.syncPlayerState(player);
            }
        }
    }

    public static void swingArm(ServerPlayer player) {
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    }

    public static void sync(ServerPlayer player) {
        AnimeKiNetwork.syncPlayerState(player);
    }

    /** Applies Ki charge / transformation multipliers to damage that the mod itself deals. */
    public static float modifyOutgoingDamage(ServerPlayer attacker, float amount) {
        return (float) (amount * KiMath.meleeMultiplier(attacker));
    }

    /** Called from the damage hook: reduces damage while a transformation cutscene plays. */
    public static float modifyIncomingDamage(ServerPlayer player, float amount) {
        return amount * TransformationService.transformDamageMultiplier(player);
    }

    /** The entity a chase attack should follow. */
    @Nullable
    public static LivingEntity chaseTarget(ServerPlayer player) {
        if (!AnimeKiServerConfig.COMBAT.chaseAttackEnabled.get()) {
            return null;
        }
        ComboState combo = ModAttachments.combat(player);
        UUID targetId = combo.lastTarget();
        if (targetId == null) {
            return null;
        }
        Entity entity = ((ServerLevel) player.level()).getEntity(targetId);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return null;
        }
        if (living.distanceTo(player) > AnimeKiServerConfig.COMBAT.chaseAttackRange.get()) {
            return null;
        }
        return living;
    }

    /** Executes the strike at the end of a chase. */
    public static void finishChase(ServerPlayer player, LivingEntity target) {
        if (!KiService.tryConsume(player, AnimeKiServerConfig.COMBAT.chaseAttackKiCost.get(), true)) {
            return;
        }
        ComboState combo = ModAttachments.combat(player);
        long gameTime = player.level().getGameTime();
        float damage = (float) (AttackKind.CHASE_STRIKE.damage() * KiMath.meleeMultiplier(player)
                * TransformationService.offenseMultiplier(player));
        swingArm(player);
        applyHit(player, target, AttackKind.CHASE_STRIKE, damage, combo, gameTime);
        combo.setLastAttackTick(gameTime);
        combo.setStep(0);
        AnimeKiNetwork.syncPlayerState(player);
    }

    public static void onDeath(ServerPlayer player) {
        ModAttachments.combat(player).reset();
        ModAttachments.combat(player).setVanishBonusUntil(Long.MIN_VALUE);
        AnimeKiNetwork.syncPlayerState(player);
    }
}
