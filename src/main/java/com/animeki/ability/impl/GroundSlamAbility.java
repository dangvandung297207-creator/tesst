package com.animeki.ability.impl;

import com.animeki.ability.Ability;
import com.animeki.ability.AbilityContext;
import com.animeki.ability.AbilityStats;
import com.animeki.ability.ActiveAbility;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.destruction.DestructionProfile;
import com.animeki.destruction.DestructionService;
import com.animeki.ki.KiMath;
import com.animeki.registry.ModDamageTypes;
import com.animeki.registry.ModSounds;
import com.animeki.vfx.CameraEffectType;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ground Slam.
 *
 * <p>Leap into the air and come down hard: everything in the configured radius takes damage, gets
 * launched, and the ground takes a crater through the budgeted destruction service. The slam always
 * plays the same way whether it starts grounded or airborne.</p>
 */
public final class GroundSlamAbility extends Ability {
    public static final String ID = "animeki:ground_slam";

    private static final int RISE_TICKS = 10;
    private static final int MAX_AIR_TICKS = 60;

    private int phase;

    public GroundSlamAbility() {
        super(ID, AbilityStats.builder()
                .kiCost(AnimeKiServerConfig.ABILITIES.groundSlam.kiCost.get())
                .cooldownTicks(AnimeKiServerConfig.ABILITIES.groundSlam.cooldownTicks.get())
                .chargeTicks(0)
                .range(AnimeKiServerConfig.ABILITIES.groundSlam.radius.get())
                .damage(AnimeKiServerConfig.ABILITIES.groundSlam.damage.get())
                .castType(AbilityStats.CastType.SCRIPTED)
                .build());
    }

    @Override
    public String animationId() {
        return "ground_slam";
    }

    @Override
    public void onStart(ServerPlayer player, AbilityContext context) {
        this.phase = 0;
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x * 0.4D, Math.max(0.75D, movement.y), movement.z * 0.4D);
        player.hurtMarked = true;
        VfxDispatcher.playOnPlayer(player, VfxEvent.AURA_BURST, Vec3.ZERO, 1.0F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.KI_CHARGE.get(),
                SoundSource.PLAYERS, 1.0F, 0.85F);
    }

    @Override
    public void onTick(ServerPlayer player, AbilityContext context, ActiveAbility active) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 movement = player.getDeltaMovement();
        if (phase == 0) {
            if (active.ticks() < RISE_TICKS) {
                player.setDeltaMovement(movement.x * 0.85D, 0.62D, movement.z * 0.85D);
            } else {
                phase = 1;
            }
            player.hurtMarked = true;
            return;
        }
        player.setDeltaMovement(movement.x * 0.9D, Math.min(movement.y, -1.45D), movement.z * 0.9D);
        player.hurtMarked = true;
        player.resetFallDistance();
        VfxDispatcher.playTracking(player, VfxEvent.FLIGHT_BOOST, player.position(), null, 1.2F,
                VfxEvent.FLIGHT_BOOST.defaultColor());

        if (player.onGround() || active.ticks() > MAX_AIR_TICKS) {
            impact(player, level);
            active.markFinished();
        }
    }

    private void impact(ServerPlayer player, ServerLevel level) {
        AnimeKiServerConfig.Abilities.GroundSlam config = AnimeKiServerConfig.ABILITIES.groundSlam;
        Vec3 center = player.position();
        double radius = config.radius.get();
        double damage = config.damage.get() * KiMath.meleeMultiplier(player);

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius))) {
            if (entity == player || !entity.isAlive() || entity.isSpectator()) {
                continue;
            }
            double distance = entity.position().distanceTo(center);
            if (distance > radius) {
                continue;
            }
            float scaled = (float) (damage * (1.0D - distance / (radius * 1.2D)));
            entity.invulnerableTime = 0;
            if (entity.hurt(ModDamageTypes.of(level, ModDamageTypes.MARTIAL, player), scaled)) {
                Vec3 push = entity.position().subtract(center);
                if (push.lengthSqr() < 1.0E-4D) {
                    push = new Vec3(0.0D, 1.0D, 0.0D);
                }
                push = push.normalize().scale(0.9D);
                entity.push(push.x, config.knockUp.get(), push.z);
                entity.hurtMarked = true;
            }
        }

        if (AnimeKiServerConfig.DESTRUCTION.enabled.get()) {
            DestructionService.apply(level, center, DestructionProfile.groundSlam(config.craterRadius.get()), player, null);
        }
        VfxDispatcher.impact(level, VfxEvent.GROUND_IMPACT, center, null, (float) radius * 0.35F);
        VfxDispatcher.cameraNear(level, center, 40.0D, CameraEffectType.SHAKE, 1.4F, 24, player.getId());
        VfxDispatcher.camera(player, CameraEffectType.FOV_PUNCH, 0.2F, 10);
        level.playSound(null, center.x, center.y, center.z, ModSounds.GROUND_SLAM.get(), SoundSource.PLAYERS, 1.6F,
                0.8F);
    }
}
