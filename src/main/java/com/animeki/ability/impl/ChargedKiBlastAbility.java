package com.animeki.ability.impl;

import com.animeki.ability.Ability;
import com.animeki.ability.AbilityContext;
import com.animeki.ability.AbilityStats;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.entity.KiBlastEntity;
import com.animeki.registry.ModSounds;
import com.animeki.util.MathUtil;
import com.animeki.vfx.CameraEffectType;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Charged Ki Blast - a charged volley.
 *
 * <p>Charge until the spheres lock in place, then release for a spread fan of blasts. Count, spread,
 * damage and speed all come from {@code abilities.chargedKiBlast}.</p>
 */
public final class ChargedKiBlastAbility extends Ability {
    public static final String ID = "animeki:charged_ki_blast";

    public ChargedKiBlastAbility() {
        super(ID, AbilityStats.builder()
                .kiCost(AnimeKiServerConfig.ABILITIES.chargedKiBlast.kiCost.get())
                .cooldownTicks(AnimeKiServerConfig.ABILITIES.chargedKiBlast.cooldownTicks.get())
                .chargeTicks(AnimeKiServerConfig.ABILITIES.chargedKiBlast.chargeTicks.get())
                .range(AnimeKiServerConfig.ABILITIES.kiBlast.range.get())
                .damage(AnimeKiServerConfig.ABILITIES.chargedKiBlast.damage.get())
                .castType(AbilityStats.CastType.CHARGE_RELEASE)
                .build());
    }

    @Override
    public String animationId() {
        return "charged_ki_blast";
    }

    @Override
    public void onStart(ServerPlayer player, AbilityContext context) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.KI_CHARGE.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public void onChargeTick(ServerPlayer player, AbilityContext context, int chargeTicks) {
        int required = Math.max(1, stats().chargeTicks());
        float progress = Math.min(1.0F, chargeTicks / (float) required);
        if (chargeTicks % 3 == 0) {
            // Orbs circle the raised hand while the volley gathers.
            double angle = Math.toRadians(chargeTicks * 24.0D);
            Vec3 offset = new Vec3(Math.cos(angle) * 0.6D, 0.35D + progress * 0.2D, Math.sin(angle) * 0.6D);
            VfxDispatcher.playOnPlayer(player, VfxEvent.KI_CHARGE_TICK, offset, 0.5F + progress);
        }
        if (progress >= 1.0F && chargeTicks % 2 == 0) {
            VfxDispatcher.playOnPlayer(player, VfxEvent.CHARGE_READY, Vec3.ZERO, 1.2F);
        }
    }

    @Override
    public void onRelease(ServerPlayer player, AbilityContext context, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        AnimeKiServerConfig.Abilities.ChargedKiBlast config = AnimeKiServerConfig.ABILITIES.chargedKiBlast;
        int count = Math.max(1, config.projectiles.get());
        double spread = Math.toRadians(config.spreadDegrees.get());
        boolean fullCharge = chargeTicks >= stats().chargeTicks();
        double damage = config.damage.get() * context.power() * (fullCharge ? 1.0D : 0.6D);
        float size = fullCharge ? 0.8F : 0.55F;

        for (int i = 0; i < count; i++) {
            double t = count == 1 ? 0.0D : (i / (double) (count - 1)) * 2.0D - 1.0D;
            Vec3 direction = MathUtil.rotateY(context.direction(), Math.toDegrees(spread * t));
            Vec3 origin = player.getEyePosition().add(direction.scale(0.9D));
            KiBlastEntity.fire(level, player, origin, direction, size, 0xFFCDEBFF, fullCharge, false, damage,
                    AnimeKiServerConfig.ABILITIES.kiBlast.knockback.get(), 1.6D, false, config.speed.get(),
                    (int) (AnimeKiServerConfig.ABILITIES.kiBlast.range.get() * 1.4D));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BEAM_FIRE.get(),
                SoundSource.PLAYERS, 1.3F, 1.25F);
        VfxDispatcher.camera(player, CameraEffectType.SHAKE, 0.5F, 8);
        VfxDispatcher.camera(player, CameraEffectType.FOV_PUNCH, 0.2F, 8);
    }
}
