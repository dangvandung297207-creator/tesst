package com.animeki.transformation;

import com.animeki.AnimeKi;
import com.animeki.ability.AbilityService;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.destruction.DestructionProfile;
import com.animeki.destruction.DestructionService;
import com.animeki.flight.FlightService;
import com.animeki.ki.KiMath;
import com.animeki.ki.KiService;
import com.animeki.ki.PlayerKi;
import com.animeki.network.AnimeKiNetwork;
import com.animeki.registry.ModAttachments;
import com.animeki.registry.ModSounds;
import com.animeki.util.MathUtil;
import com.animeki.vfx.CameraEffectType;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Drives the cinematic transformation sequence.
 *
 * <pre>
 * CHARGING -> ENERGY_BUILDUP -> TRANSFORMATION -> SHOCKWAVE -> ACTIVE -> EXITING
 * </pre>
 *
 * <p>Responsibilities: Ki economy (activation cost + drain), movement restriction during the
 * cutscene phases, stat application while active, aura intensity for the client renderer and all
 * the presentation (sound, shake, flash, shockwave, terrain scar).</p>
 */
public final class TransformationService {
    private static final ResourceLocation SPEED_MODIFIER_ID = AnimeKi.id("transformation_speed");
    private static final ResourceLocation KNOCKBACK_MODIFIER_ID = AnimeKi.id("transformation_stability");

    private static final float DESTRUCTION_RADIUS = 6.0F;

    private TransformationService() {
    }

    public static PlayerTransformation get(net.minecraft.world.entity.player.Player player) {
        return ModAttachments.transformation(player);
    }

    // ------------------------------------------------------------------ control

    /** Transform key behaviour: cycle to the next form, or drop out of the current one. */
    public static void cycle(ServerPlayer player) {
        PlayerTransformation state = get(player);
        if (state.isActive()) {
            cancel(player, false);
            return;
        }
        // Walk up the tier list from whatever the player used last, so repeated presses cycle
        // through every unlocked form before wrapping around.
        Transformation target = Transformations.next(state.lastFormId());
        if (target == null) {
            return;
        }
        start(player, target.id());
    }

    public static boolean start(ServerPlayer player, String formId) {
        Transformations.bootstrap();
        Transformation form = Transformations.byId(formId);
        if (form == null) {
            return false;
        }
        PlayerTransformation state = get(player);
        if (state.isActive()) {
            if (!AnimeKiServerConfig.TRANSFORMATIONS.allowStackingTransformations.get()) {
                return false;
            }
            endTransformation(player, false);
        }
        PlayerKi ki = ModAttachments.ki(player);
        long gameTime = player.level().getGameTime();
        if (ki.isExhausted(gameTime)) {
            return false;
        }
        if (ki.current() < form.requiredKi()) {
            return false;
        }
        if (form.activationKiCost() > 0.0D && !KiService.tryConsume(player, form.activationKiCost(), true)) {
            return false;
        }

        state.begin(form.id());
        state.setPhase(TransformationPhase.CHARGING);

        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TRANSFORM_BUILDUP.get(),
                SoundSource.PLAYERS, 1.4F, 0.85F);
        VfxDispatcher.playTracking(player, VfxEvent.AURA_BURST, player.position().add(0.0D, 1.0D, 0.0D),
                null, 0.8F, form.aura().coreColor());
        VfxDispatcher.camera(player, CameraEffectType.ZOOM, 0.14F, form.phaseDuration(TransformationPhase.CHARGING));
        AnimeKiNetwork.syncPlayerState(player);
        AnimeKiNetwork.syncVisualState(player);
        return true;
    }

    /** Cancels the current form (or the sequence in progress). */
    public static void cancel(ServerPlayer player, boolean exhausted) {
        endTransformation(player, exhausted);
    }

    private static void endTransformation(ServerPlayer player, boolean exhausted) {
        PlayerTransformation state = get(player);
        if (!state.isActive()) {
            return;
        }
        Transformation form = state.active();
        state.setPhase(TransformationPhase.EXITING);
        removeModifiers(player);
        if (form != null) {
            ServerLevel level = (ServerLevel) player.level();
            VfxDispatcher.playTracking(player, VfxEvent.AURA_BURST,
                    player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D), null, 1.0F,
                    form.aura().glowColor());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TRANSFORM_BURST.get(),
                    SoundSource.PLAYERS, 1.0F, exhausted ? 0.6F : 0.9F);
        }
        state.clear();
        ModAttachments.ki(player).clampToMax(player);
        AnimeKiNetwork.syncPlayerState(player);
        AnimeKiNetwork.syncVisualState(player);
    }

    // ------------------------------------------------------------------ tick

    public static void tick(ServerPlayer player) {
        PlayerTransformation state = get(player);
        if (!state.isActive()) {
            return;
        }
        Transformation form = state.active();
        if (form == null) {
            state.clear();
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        long gameTime = level.getGameTime();

        if (!player.isAlive()) {
            endTransformation(player, false);
            return;
        }

        if (state.phase().locksMovement()) {
            restrictMovement(player);
        }

        switch (state.phase()) {
            case CHARGING -> tickPhase(player, state, form, TransformationPhase.ENERGY_BUILDUP, 0.35F);
            case ENERGY_BUILDUP -> {
                state.incrementPhaseTick();
                float progress = form.buildupTicks() <= 0 ? 1.0F : state.phaseTick() / (float) form.buildupTicks();
                state.setAuraIntensity(MathUtil.lerp(0.35F, 1.0F, (float) MathUtil.easeOutCubic(progress)));
                emitChargeEffects(player, state, form, progress);
                if (state.phaseTick() >= form.buildupTicks()) {
                    state.setPhase(TransformationPhase.TRANSFORMATION);
                    onTransformationFlash(player, state, form);
                }
            }
            case TRANSFORMATION -> tickPhase(player, state, form, TransformationPhase.SHOCKWAVE, 1.35F);
            case SHOCKWAVE -> {
                state.incrementPhaseTick();
                if (state.phaseTick() == 1) {
                    onShockwave(player, state, form);
                }
                if (state.phaseTick() >= form.shockwaveTicks()) {
                    state.setPhase(TransformationPhase.ACTIVE);
                    applyModifiers(player, form);
                    state.setAuraIntensity(form.aura().intensity());
                    AnimeKiNetwork.syncVisualState(player);
                }
            }
            case ACTIVE -> {
                state.incrementActiveTicks();
                state.setAuraIntensity(form.aura().intensity() * (1.0F + 0.06F * (float) Math.sin(gameTime / 6.0D)));
                drainKi(player, form, gameTime);
                if (form.durationTicks() > 0 && state.activeTicks() >= form.durationTicks()) {
                    endTransformation(player, false);
                    return;
                }
                if (state.activeTicks() % 10 == 0) {
                    AnimeKiNetwork.syncVisualState(player);
                }
            }
            case EXITING -> {
                state.incrementPhaseTick();
                state.setAuraIntensity(Math.max(0.0F, state.auraIntensity() - 0.08F));
                if (state.phaseTick() >= 10) {
                    endTransformation(player, false);
                }
            }
            default -> {
            }
        }
    }

    private static void tickPhase(ServerPlayer player, PlayerTransformation state, Transformation form,
                                  TransformationPhase next, float auraIntensity) {
        state.incrementPhaseTick();
        state.setAuraIntensity(auraIntensity);
        if (state.phaseTick() == 1) {
            AnimeKiNetwork.syncVisualState(player);
        }
        if (state.phaseTick() >= form.phaseDuration(state.phase())) {
            state.setPhase(next);
            AnimeKiNetwork.syncVisualState(player);
        }
    }

    private static void emitChargeEffects(ServerPlayer player, PlayerTransformation state, Transformation form, float progress) {
        if (state.phaseTick() % 4 != 0) {
            return;
        }
        float scale = 0.5F + progress;
        VfxDispatcher.playOnPlayer(player, VfxEvent.KI_CHARGE_TICK, Vec3.ZERO, scale);
        VfxDispatcher.cameraNear((ServerLevel) player.level(), player.position(), 24.0D, CameraEffectType.SHAKE,
                0.12F * progress, 3, state.phaseTick());
    }

    private static void onTransformationFlash(ServerPlayer player, PlayerTransformation state, Transformation form) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TRANSFORM_BURST.get(),
                SoundSource.PLAYERS, 1.6F, 1.15F);
        VfxDispatcher.playTracking(player, VfxEvent.TRANSFORMATION_SHOCKWAVE, center, null, 1.2F,
                form.aura().glowColor());
        VfxDispatcher.camera(player, CameraEffectType.FLASH, 0.55F, 8);
        VfxDispatcher.camera(player, CameraEffectType.SHAKE, 0.9F, 22);
        VfxDispatcher.camera(player, CameraEffectType.FOV_PUNCH, 0.22F, 12);
    }

    private static void onShockwave(ServerPlayer player, PlayerTransformation state, Transformation form) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = player.position();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GROUND_SLAM.get(),
                SoundSource.PLAYERS, 1.2F, 1.4F);
        VfxDispatcher.impact(level, VfxEvent.TRANSFORMATION_SHOCKWAVE, center, null, 1.8F);
        VfxDispatcher.cameraNear(level, center, 40.0D, CameraEffectType.SHAKE, 1.1F, 26, player.getId());
        if (form.destructiveActivation() && AnimeKiServerConfig.DESTRUCTION.enabled.get()) {
            DestructionService.apply(level, center, DestructionProfile.transformationBlast(DESTRUCTION_RADIUS),
                    player, null);
        }
    }

    private static void drainKi(ServerPlayer player, Transformation form, long gameTime) {
        if (form.drainPerTick() <= 0.0D) {
            return;
        }
        PlayerKi ki = ModAttachments.ki(player);
        double drain = form.drainPerTick() * (player.isSprinting() ? 1.4D : 1.0D);
        if (!KiService.drain(player, drain)) {
            level(player).playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.KI_BLAST_IMPACT.get(),
                    SoundSource.PLAYERS, 0.8F, 0.6F);
            endTransformation(player, true);
        }
    }

    private static ServerLevel level(ServerPlayer player) {
        return (ServerLevel) player.level();
    }

    // ------------------------------------------------------------------ stats

    private static void restrictMovement(ServerPlayer player) {
        Vec3 movement = player.getDeltaMovement();
        double damping = 0.15D;
        player.setDeltaMovement(movement.x * damping, Math.min(movement.y, 0.08D), movement.z * damping);
        player.hurtMarked = true;
        FlightService.setActive(player, false, false);
        AbilityService.active(player);
    }

    private static void applyModifiers(ServerPlayer player, Transformation form) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_MODIFIER_ID);
            speed.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID, form.speedMultiplier() - 1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        AttributeInstance knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.removeModifier(KNOCKBACK_MODIFIER_ID);
            knockback.addTransientModifier(new AttributeModifier(KNOCKBACK_MODIFIER_ID,
                    Math.min(0.6D, (form.defenseMultiplier() - 1.0D) * 0.5D),
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void removeModifiers(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }
        AttributeInstance knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.removeModifier(KNOCKBACK_MODIFIER_ID);
        }
    }

    /** Damage taken while transforming is heavily reduced so the cutscene is not a free kill. */
    public static float transformDamageMultiplier(ServerPlayer player) {
        PlayerTransformation state = get(player);
        if (!state.isActive()) {
            return 1.0F;
        }
        if (state.phase().locksMovement()) {
            return 0.35F;
        }
        Transformation form = state.active();
        if (form == null) {
            return 1.0F;
        }
        return (float) (1.0D / Math.max(0.2D, form.defenseMultiplier()));
    }

    /** Extra melee damage while transformed, used by the combat service. */
    public static double offenseMultiplier(ServerPlayer player) {
        Transformation form = get(player).active();
        return form == null ? 1.0D : form.damageMultiplier();
    }

    public static double kiOutputOf(ServerPlayer player) {
        return KiMath.outputMultiplier(player);
    }

    @Nullable
    public static Transformation currentForm(ServerPlayer player) {
        return get(player).active();
    }
}
