package com.animeki.ability.impl;

import com.animeki.ability.Ability;
import com.animeki.ability.AbilityContext;
import com.animeki.ability.AbilityStats;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.entity.KiBlastEntity;
import com.animeki.registry.ModSounds;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Ki Blast.
 *
 * <p>Hold the key to gather a sphere in the raised hand, release to throw it. A quick tap fires the
 * cheap fast version, a full charge fires the heavy one - both are pure configuration so servers can
 * make the blast instant, spammy or nuke-like without code changes.</p>
 */
public final class KiBlastAbility extends Ability {
    public static final String ID = "animeki:ki_blast";

    public KiBlastAbility() {
        super(ID, AbilityStats.builder()
                .kiCost(AnimeKiServerConfig.ABILITIES.kiBlast.kiCost.get())
                .cooldown(AnimeKiServerConfig.ABILITIES.kiBlast.cooldownTicks.get())
                .chargeTicks(AnimeKiServerConfig.ABILITIES.kiBlast.chargedChargeTicks.get())
                .range(AnimeKiServerConfig.ABILITIES.kiBlast.range.get())
                .damage(AnimeKiServerConfig.ABILITIES.kiBlast.damage.get())
                .castType(AbilityStats.CastType.CHARGE_RELEASE)
                .build());
    }

    @Override
    public String animationId() {
        return "ki_blast";
    }

    @Override
    public boolean cancelWhenReleasedEarly() {
        return false;
    }

    @Override
    public void onStart(ServerPlayer player, AbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.KI_CHARGE.get(),
                SoundSource.PLAYERS, 0.9F, 1.2F);
        VfxDispatcher.playOnPlayer(player, VfxEvent.KI_CHARGE_TICK, handOffset(player), 0.5F);
    }

    @Override
    public void onChargeTick(ServerPlayer player, AbilityContext context, int chargeTicks) {
        int required = Math.max(1, stats().chargeTicks());
        float progress = Math.min(1.0F, chargeTicks / (float) required);
        if (chargeTicks % 3 == 0) {
            VfxDispatcher.playOnPlayer(player, VfxEvent.KI_CHARGE_TICK, handOffset(player), 0.4F + progress);
        }
        if (progress >= 1.0F && chargeTicks % 2 == 0) {
            VfxDispatcher.playOnPlayer(player, VfxEvent.CHARGE_READY, handOffset(player), 1.0F);
        }
    }

    @Override
    public void onRelease(ServerPlayer player, AbilityContext context, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        AnimeKiServerConfig.Abilities.KiBlast config = AnimeKiServerConfig.ABILITIES.kiBlast;
        boolean charged = chargeTicks >= stats().chargeTicks();

        double damage = config.damage.get() * context.power();
        double radius = config.explosionRadius.get();
        if (charged) {
            damage *= config.chargedDamageMultiplier.get();
            radius *= 1.35D;
        }
        Vec3 origin = player.getEyePosition().add(context.direction().scale(0.9D));
        KiBlastEntity.fire(level, player, origin, context.direction(), charged ? 1.25F : 0.6F,
                charged ? 0xFFFFF0B0 : 0xFF9FD8FF, charged, false, damage, config.knockback.get(), radius,
                player.getRandom().nextDouble() < 0.35D, config.speed.get(), (int) (config.range.get() * 1.6D));

        VfxDispatcher.camera(player, com.animeki.vfx.CameraEffectType.SHAKE, charged ? 0.45F : 0.2F, 6);
        VfxDispatcher.camera(player, com.animeki.vfx.CameraEffectType.FOV_PUNCH, charged ? 0.16F : 0.07F, 6);
        if (charged) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BEAM_FIRE.get(),
                    SoundSource.PLAYERS, 1.2F, 1.3F);
        }
    }

    /** Slightly in front of and above the main hand, where the sphere is visually gathered. */
    private static Vec3 handOffset(ServerPlayer player) {
        Vec3 look = player.getViewVector(1.0F);
        Vec3 right = new Vec3(-look.z, 0.0D, look.x).normalize();
        return look.scale(0.55D).add(right.scale(0.32D)).add(0.0D, player.getBbHeight() * 0.25D, 0.0D);
    }

    @Override
    public void onStop(ServerPlayer player, AbilityContext context, com.animeki.ability.AbilityStopReason reason) {
        if (reason == com.animeki.ability.AbilityStopReason.CANCELLED
                || reason == com.animeki.ability.AbilityStopReason.INTERRUPTED) {
            VfxDispatcher.playOnPlayer(player, VfxEvent.KI_BURNOUT, Vec3.ZERO, 0.6F);
        }
    }
}
