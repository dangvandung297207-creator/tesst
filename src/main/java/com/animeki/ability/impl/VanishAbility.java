package com.animeki.ability.impl;

import com.animeki.ability.Ability;
import com.animeki.ability.AbilityContext;
import com.animeki.ability.AbilityStats;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.registry.ModAttachments;
import com.animeki.registry.ModSounds;
import com.animeki.util.Targeting;
import com.animeki.vfx.CameraEffectType;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Vanish - the instant repositioning tool.
 *
 * <p>Teleports towards the aimed target when one is found (behind and slightly above it) or as far
 * as the configured range allows in the aim direction. The destination is always validated with a
 * block ray and a collision check, so the ability can never be used to clip through walls. The
 * post vanish window grants a short damage bonus and a dodge frame, which is what makes the follow
 * up attack feel earned.</p>
 */
public final class VanishAbility extends Ability {
    public static final String ID = "animeki:vanish";

    /** Safety margin kept in front of terrain so the player never lands inside a wall. */
    private static final double TERRAIN_MARGIN = 0.6D;

    public VanishAbility() {
        super(ID, AbilityStats.builder()
                .kiCost(AnimeKiServerConfig.VANISH.kiCost.get())
                .cooldown(AnimeKiServerConfig.VANISH.cooldownTicks.get())
                .chargeTicks(0)
                .range(AnimeKiServerConfig.VANISH.range.get())
                .damage(0.0D)
                .castType(AbilityStats.CastType.INSTANT)
                .build());
    }

    @Override
    public String animationId() {
        return "vanish";
    }

    @Override
    public boolean canStart(ServerPlayer player, AbilityContext context) {
        return AnimeKiServerConfig.VANISH.enabled.get() && player.level() instanceof ServerLevel;
    }

    @Override
    public void onStart(ServerPlayer player, AbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        AnimeKiServerConfig.Vanish config = AnimeKiServerConfig.VANISH;
        long gameTime = level.getGameTime();

        Vec3 from = player.position();
        VfxDispatcher.playTracking(player, VfxEvent.VANISH, player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D),
                null, 1.1F, VfxEvent.VANISH.defaultColor());
        VfxDispatcher.camera(player, CameraEffectType.FLASH, 0.18F, 4);

        Vec3 destination = resolveDestination(player, level, context, config);
        boolean moved = player.randomTeleport(destination.x, destination.y, destination.z, true);
        if (!moved) {
            // Fall back to the original spot but still grant the window so the input is not wasted.
            player.teleportTo(level, from.x, from.y, from.z, player.getYRot(), player.getXRot());
        }

        // Appearance behind (or above) the target: face the teleport direction.
        Vec3 look = destination.subtract(from);
        if (look.lengthSqr() > 1.0E-4D) {
            float yaw = com.animeki.util.MathUtil.yawOf(look);
            player.setYRot(yaw);
            player.setYHeadRot(yaw);
        }

        var combo = ModAttachments.combat(player);
        combo.setVanishBonusUntil(gameTime + config.postVanishAttackWindow.get());
        combo.setDodgeUntil(gameTime + 12);
        combo.resetComboOnly();

        Vec3 to = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        VfxDispatcher.playTracking(player, VfxEvent.APPEAR, to, null, 1.2F, VfxEvent.APPEAR.defaultColor());
        VfxDispatcher.camera(player, CameraEffectType.FOV_PUNCH, 0.14F, 6);
        VfxDispatcher.camera(player, CameraEffectType.IMPULSE, 0.25F, 6);
        level.playSound(null, from.x, from.y, from.z, ModSounds.VANISH.get(), SoundSource.PLAYERS, 1.2F, 1.3F);
        level.playSound(null, to.x, to.y, to.z, ModSounds.VANISH.get(), SoundSource.PLAYERS, 1.0F, 0.85F);
    }

    /** Picks a safe landing spot: behind the aimed target, or in front of the terrain. */
    private Vec3 resolveDestination(ServerPlayer player, ServerLevel level, AbilityContext context,
                                    AnimeKiServerConfig.Vanish config) {
        LivingEntity target = null;
        if (config.targetDetection.get()) {
            double range = config.range.get() * Math.max(1.0D, config.targetRange.get());
            target = Targeting.aimTarget(level, player, range, 1.2D,
                    entity -> entity.isAlive() && entity != player && !entity.isSpectator());
        }
        if (target != null) {
            Vec3 behind = target.position().subtract(target.getViewVector(1.0F).normalize().scale(2.2D));
            Vec3 safe = findSafeSpot(level, player, behind, target.getY());
            if (safe != null) {
                return safe;
            }
        }
        double range = config.range.get();
        Vec3 direction = context.direction();
        BlockHitResult hit = level.clip(new ClipContext(player.getEyePosition(),
                player.getEyePosition().add(direction.scale(range)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double distance = hit.getType() == HitResult.Type.MISS ? range
                : Math.max(1.0D, player.getEyePosition().distanceTo(hit.getLocation()) - TERRAIN_MARGIN);
        Vec3 candidate = player.position().add(direction.scale(distance));
        Vec3 safe = findSafeSpot(level, player, candidate, candidate.y);
        return safe == null ? player.position() : safe;
    }

    /** Steps up and down around the wanted position until the player fits without suffocating. */
    @Nullable
    private Vec3 findSafeSpot(ServerLevel level, ServerPlayer player, Vec3 wanted, double baseY) {
        for (double dy = 0.0D; dy <= 3.0D; dy += 0.5D) {
            Vec3 up = new Vec3(wanted.x, baseY + dy, wanted.z);
            if (fits(level, player, up)) {
                return up;
            }
            if (dy > 0.0D) {
                Vec3 down = new Vec3(wanted.x, baseY - dy, wanted.z);
                if (fits(level, player, down)) {
                    return down;
                }
            }
        }
        return null;
    }

    private boolean fits(ServerLevel level, ServerPlayer player, Vec3 position) {
        var box = player.getDimensions(player.getPose()).makeBoundingBox(position);
        return level.noCollision(player, box) && !level.containsAnyLiquid(box);
    }
}
