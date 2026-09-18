package com.animeki.ability.impl;

import com.animeki.ability.Ability;
import com.animeki.ability.AbilityContext;
import com.animeki.ability.AbilityStats;
import com.animeki.ability.AbilityStopReason;
import com.animeki.ability.ActiveAbility;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.destruction.DestructionProfile;
import com.animeki.destruction.DestructionService;
import com.animeki.ki.KiMath;
import com.animeki.ki.KiService;
import com.animeki.registry.ModDamageTypes;
import com.animeki.registry.ModSounds;
import com.animeki.vfx.CameraEffectType;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ultimate - Meteor Strike.
 *
 * <p>The cinematic ultimate: hold the key to charge (rapid Ki drain plus a rising aura), then the
 * fighter launches, a sphere forms overhead, the target area is marked and the sphere descends into
 * a shockwave that scars the terrain. The sequence is scripted, server driven and reproduces
 * identically for every player in the area. Releasing early cancels it; damage taken during the
 * charge phase interrupts it.</p>
 */
public final class MeteorStrikeAbility extends Ability {
    public static final String ID = "animeki:meteor_strike";

    private static final int PHASE_GROUND = 0;
    private static final int PHASE_RISE = 1;
    private static final int PHASE_HOLD = 2;
    private static final int PHASE_FALL = 3;
    private static final int PHASE_DONE = 4;

    private int phase = PHASE_GROUND;
    private int phaseTicks;
    private Vec3 target = Vec3.ZERO;
    private boolean sphereSpawned;

    public MeteorStrikeAbility() {
        super(ID, AbilityStats.builder()
                .kiCost(AnimeKiServerConfig.ABILITIES.meteor.kiCost.get())
                .cooldown(AnimeKiServerConfig.ABILITIES.meteor.cooldownTicks.get())
                .chargeTicks(AnimeKiServerConfig.ABILITIES.meteor.chargeTicks.get())
                .range(AnimeKiServerConfig.ABILITIES.meteor.radius.get())
                .damage(AnimeKiServerConfig.ABILITIES.meteor.damage.get())
                .castType(AbilityStats.CastType.CHARGE_RELEASE)
                .build());
    }

    @Override
    public String animationId() {
        return "meteor_strike";
    }

    @Override
    public boolean continuesAfterRelease() {
        return true;
    }

    @Override
    public boolean cancelOnMovement() {
        return true;
    }

    @Override
    public boolean interruptibleByDamage() {
        // Only the charge phase can be interrupted; once airborne the sequence is committed.
        return phase == PHASE_GROUND;
    }

    @Override
    public void onStart(ServerPlayer player, AbilityContext context) {
        this.phase = PHASE_GROUND;
        this.phaseTicks = 0;
        this.sphereSpawned = false;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ULTIMATE_CHARGE.get(),
                SoundSource.PLAYERS, 2.0F, 0.9F);
        VfxDispatcher.camera(player, CameraEffectType.ZOOM, 0.2F, AnimeKiServerConfig.ABILITIES.meteor.chargeTicks.get());
    }

    @Override
    public void onChargeTick(ServerPlayer player, AbilityContext context, int chargeTicks) {
        AnimeKiServerConfig.Abilities.Meteor config = AnimeKiServerConfig.ABILITIES.meteor;
        int required = Math.max(1, stats().chargeTicks());
        float progress = Math.min(1.0F, chargeTicks / (float) required);

        // Rapid drain: charging the ultimate is itself a commitment.
        if (chargeTicks % 5 == 0 && !KiService.drain(player, config.kiCost.get() / (required * 0.35D))) {
            VfxDispatcher.playOnPlayer(player, VfxEvent.KI_BURNOUT, Vec3.ZERO, 1.4F);
            context.stop(AbilityStopReason.KI_DEPLETED);
            return;
        }
        // Movement restriction while the energy builds.
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x * 0.2D, Math.min(movement.y, 0.08D), movement.z * 0.2D);
        player.hurtMarked = true;

        if (chargeTicks % 3 == 0) {
            VfxDispatcher.playOnPlayer(player, VfxEvent.ULTIMATE_CHARGE, Vec3.ZERO, 0.6F + progress * 2.0F);
            VfxDispatcher.playOnPlayer(player, VfxEvent.KI_CHARGE_TICK, Vec3.ZERO, 1.0F + progress);
        }
        if (chargeTicks % 6 == 0) {
            VfxDispatcher.camera(player, CameraEffectType.SHAKE, 0.3F + progress * 0.6F, 6);
        }
    }

    @Override
    public void onRelease(ServerPlayer player, AbilityContext context, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        AnimeKiServerConfig.Abilities.Meteor config = AnimeKiServerConfig.ABILITIES.meteor;
        this.target = resolveTarget(player, level, config);
        this.phase = PHASE_RISE;
        this.phaseTicks = 0;

        // Launch up.
        player.setDeltaMovement(0.0D, Math.max(0.6D, config.riseHeight.get() / Math.max(1, config.riseTicks.get())), 0.0D);
        player.hurtMarked = true;
        player.resetFallDistance();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BEAM_FIRE.get(),
                SoundSource.PLAYERS, 2.0F, 0.8F);
        VfxDispatcher.play(level, VfxEvent.AURA_BURST, player.position(), null, 2.4F, 0xFFFFE9B0);
        // The marker stays up for the whole descent so everyone can see where it will land.
        VfxDispatcher.playTimed(level, VfxEvent.TARGET_MARKER, target, null, (float) config.radius.get(), 0xFFFFD166,
                200, VfxDispatcher.CINEMATIC_RADIUS);
        VfxDispatcher.cameraNear(level, player.position(), 64.0D, CameraEffectType.SHAKE, 1.2F, 20, player.getId());
    }

    @Override
    public void onTick(ServerPlayer player, AbilityContext context, ActiveAbility active) {
        ServerLevel level = (ServerLevel) player.level();
        AnimeKiServerConfig.Abilities.Meteor config = AnimeKiServerConfig.ABILITIES.meteor;
        phaseTicks++;
        switch (phase) {
            case PHASE_RISE -> {
                player.setDeltaMovement(0.0D,
                        Math.max(0.35D, config.riseHeight.get() / Math.max(1, config.riseTicks.get())), 0.0D);
                player.hurtMarked = true;
                player.resetFallDistance();
                if (phaseTicks % 4 == 0) {
                    VfxDispatcher.play(level, VfxEvent.KI_CHARGE_TICK, player.position(), null, 2.0F, 0xFFFFE9B0);
                }
                if (phaseTicks >= config.riseTicks.get()) {
                    phase = PHASE_HOLD;
                    phaseTicks = 0;
                }
            }
            case PHASE_HOLD -> {
                player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                player.hurtMarked = true;
                player.resetFallDistance();
                if (!sphereSpawned) {
                    sphereSpawned = true;
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ULTIMATE_CHARGE.get(),
                            SoundSource.PLAYERS, 2.4F, 0.7F);
                }
                if (phaseTicks % 3 == 0) {
                    VfxDispatcher.play(level, VfxEvent.ULTIMATE_CHARGE,
                            player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D), null,
                            2.0F + phaseTicks * 0.08F, 0xFFFFE9B0);
                    VfxDispatcher.cameraNear(level, player.position(), 64.0D, CameraEffectType.SHAKE,
                            0.4F + phaseTicks * 0.02F, 4, phaseTicks);
                }
                int holdTicks = Math.max(10, config.riseTicks.get() / 2);
                if (phaseTicks >= holdTicks) {
                    phase = PHASE_FALL;
                    phaseTicks = 0;
                }
            }
            case PHASE_FALL -> {
                Vec3 direction = target.subtract(player.position());
                double distance = direction.length();
                if (distance > 2.0D && phaseTicks < config.fallTicks.get() * 2) {
                    double speed = Math.max(config.radius.get() * 0.35D, distance / Math.max(1, config.fallTicks.get()));
                    player.setDeltaMovement(direction.normalize().scale(speed));
                    player.hurtMarked = true;
                    player.resetFallDistance();
                    VfxDispatcher.play(level, VfxEvent.ULTIMATE_CHARGE, player.position(), direction, 2.4F, 0xFFFFE9B0);
                } else {
                    impact(player, level);
                    active.markFinished();
                }
            }
            default -> active.markFinished();
        }
    }

    private void impact(ServerPlayer player, ServerLevel level) {
        AnimeKiServerConfig.Abilities.Meteor config = AnimeKiServerConfig.ABILITIES.meteor;
        Vec3 center = target.equals(Vec3.ZERO) ? player.position() : target;
        double radius = config.radius.get();
        double damage = config.damage.get() * KiMath.outputMultiplier(player);

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius))) {
            if (entity == player || !entity.isAlive() || entity.isSpectator()) {
                continue;
            }
            double distance = entity.position().distanceTo(center);
            if (distance > radius) {
                continue;
            }
            float scaled = (float) (damage * (1.0D - distance / (radius * 1.25D)));
            entity.invulnerableTime = 0;
            if (entity.hurt(ModDamageTypes.of(level, ModDamageTypes.ULTIMATE, player), scaled)) {
                Vec3 push = entity.position().subtract(center).normalize().scale(1.4D);
                entity.push(push.x, 0.6D, push.z);
                entity.hurtMarked = true;
            }
        }
        if (AnimeKiServerConfig.DESTRUCTION.enabled.get()) {
            DestructionService.apply(level, center, DestructionProfile.meteorImpact(config.craterRadius.get()), player, null);
        }
        VfxDispatcher.play(level, VfxEvent.ULTIMATE_IMPACT, center, null, 3.4F, 0xFFFFE9B0);
        VfxDispatcher.play(level, VfxEvent.GROUND_IMPACT, center, null, 3.0F, 0xFFFFCD66);
        VfxDispatcher.play(level, VfxEvent.DEBRIS, center, null, (float) config.craterRadius.get(), 0x8C8C8C);
        VfxDispatcher.cameraNear(level, center, 96.0D, CameraEffectType.SHAKE, 2.6F, 50, player.getId());
        VfxDispatcher.cameraNear(level, center, 96.0D, CameraEffectType.FLASH, 0.9F, 16, player.getId());
        VfxDispatcher.cameraNear(level, center, 96.0D, CameraEffectType.IMPULSE, 0.8F, 20, player.getId());
        level.playSound(null, center.x, center.y, center.z, ModSounds.ULTIMATE_IMPACT.get(), SoundSource.PLAYERS, 3.0F,
                0.9F);
        phase = PHASE_DONE;
    }

    /** Marks the spot the meteor will land on: the first solid ground in front of the fighter. */
    private Vec3 resolveTarget(ServerPlayer player, ServerLevel level, AnimeKiServerConfig.Abilities.Meteor config) {
        Vec3 eyes = player.getEyePosition();
        Vec3 direction = player.getViewVector(1.0F).normalize();
        Vec3 far = eyes.add(direction.scale(Math.max(12.0D, config.radius.get() * 1.5D)));
        BlockHitResult hit = level.clip(new ClipContext(eyes, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                player));
        if (hit.getType() != HitResult.Type.MISS) {
            return hit.getLocation();
        }
        // No wall in front: slam straight down under the fighter.
        Vec3 below = player.position().add(direction.x * 4.0D, 0.0D, direction.z * 4.0D);
        BlockHitResult ground = level.clip(new ClipContext(below.add(0.0D, 8.0D, 0.0D), below.add(0.0D, -24.0D, 0.0D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return ground.getType() == HitResult.Type.MISS ? below : ground.getLocation();
    }

    @Override
    public void onStop(ServerPlayer player, AbilityContext context, AbilityStopReason reason) {
        if (phase != PHASE_DONE && reason != AbilityStopReason.COMPLETED) {
            // Cancelled before the impact: the sphere fizzles instead of exploding.
            VfxDispatcher.playOnPlayer(player, VfxEvent.KI_BURNOUT, Vec3.ZERO, 1.6F);
        }
        phase = PHASE_DONE;
    }
}
