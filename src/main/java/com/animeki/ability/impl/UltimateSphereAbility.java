package com.animeki.ability.impl;

import com.animeki.ability.Ability;
import com.animeki.ability.AbilityContext;
import com.animeki.ability.AbilityStats;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.entity.KiBlastEntity;
import com.animeki.ki.KiMath;
import com.animeki.registry.ModSounds;
import com.animeki.vfx.CameraEffectType;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Ultimate - Energy Sphere.
 *
 * <p>Gather a huge sphere in front of the fighter, then send it off: slow, inevitable and very
 * loud. Everything about it (size, speed, damage, explosion radius, cooldown) is configuration.</p>
 */
public final class UltimateSphereAbility extends Ability {
    public static final String ID = "animeki:ultimate_sphere";

    public UltimateSphereAbility() {
        super(ID, AbilityStats.builder()
                .kiCost(AnimeKiServerConfig.ABILITIES.sphere.kiCost.get())
                .cooldownTicks(AnimeKiServerConfig.ABILITIES.sphere.cooldownTicks.get())
                .chargeTicks(AnimeKiServerConfig.ABILITIES.sphere.chargeTicks.get())
                .range(AnimeKiServerConfig.ABILITIES.sphere.explosionRadius.get())
                .damage(AnimeKiServerConfig.ABILITIES.sphere.damage.get())
                .castType(AbilityStats.CastType.CHARGE_RELEASE)
                .build());
    }

    @Override
    public String animationId() {
        return "ultimate_sphere";
    }

    @Override
    public void onStart(ServerPlayer player, AbilityContext context) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ULTIMATE_CHARGE.get(),
                SoundSource.PLAYERS, 1.8F, 1.0F);
        VfxDispatcher.camera(player, CameraEffectType.ZOOM, 0.18F,
                AnimeKiServerConfig.ABILITIES.sphere.chargeTicks.get());
    }

    @Override
    public void onChargeTick(ServerPlayer player, AbilityContext context, int chargeTicks) {
        int required = Math.max(1, stats().chargeTicks());
        float progress = Math.min(1.0F, chargeTicks / (float) required);
        Vec3 movement = player.getDeltaMovement();

        // Gathering this much energy roots the fighter in place.
        player.setDeltaMovement(movement.x * 0.25D, Math.min(movement.y, 0.05D), movement.z * 0.25D);
        player.hurtMarked = true;

        if (chargeTicks % 3 == 0) {
            VfxDispatcher.playOnPlayer(player, VfxEvent.ULTIMATE_CHARGE, new Vec3(0.0D, 0.1D, 0.0D),
                    0.8F + progress * 2.2F);
        }
        if (chargeTicks % 6 == 0) {
            VfxDispatcher.camera(player, CameraEffectType.SHAKE, 0.2F + progress * 0.5F, 5);
        }
        if (progress >= 1.0F && chargeTicks % 2 == 0) {
            VfxDispatcher.playOnPlayer(player, VfxEvent.CHARGE_READY, Vec3.ZERO, 1.6F);
        }
    }

    @Override
    public void onRelease(ServerPlayer player, AbilityContext context, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        AnimeKiServerConfig.Abilities.Sphere config = AnimeKiServerConfig.ABILITIES.sphere;
        boolean full = chargeTicks >= stats().chargeTicks();
        double scale = full ? 1.0D : 0.55D;

        Vec3 origin = player.getEyePosition().add(context.direction().scale(1.6D));
        KiBlastEntity.fire(level, player, origin, context.direction(),
                (float) (config.size.get() * scale), 0xFFFFF0C0, true, true,
                config.damage.get() * KiMath.outputMultiplier(player) * scale, 1.2D,
                config.explosionRadius.get() * scale, true, config.speed.get(),
                (int) (config.speed.get() > 0.0D ? 240.0D : 200.0D));

        VfxDispatcher.camera(player, CameraEffectType.FLASH, 0.4F, 8);
        VfxDispatcher.camera(player, CameraEffectType.FOV_PUNCH, 0.3F, 12);
        VfxDispatcher.cameraNear(level, player.position(), 48.0D, CameraEffectType.SHAKE, 1.0F, 16, player.getId());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BEAM_FIRE.get(),
                SoundSource.PLAYERS, 2.0F, 0.85F);
    }
}
