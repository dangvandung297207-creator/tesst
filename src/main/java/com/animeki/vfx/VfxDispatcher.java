package com.animeki.vfx;

import com.animeki.network.AnimeKiNetwork;
import com.animeki.network.Payloads;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Server side entry point for every visual effect.
 *
 * <p>Gameplay code never draws anything itself: it calls one of these methods and the effect is
 * forwarded to the clients that can actually see it. Camera reactions are only sent to the player
 * who should feel them.</p>
 */
public final class VfxDispatcher {
    /** Default broadcast radius of a combat effect. */
    public static final double DEFAULT_RADIUS = 48.0D;
    /** Larger radius used by cinematic events (transformation, ultimates, boss phases). */
    public static final double CINEMATIC_RADIUS = 96.0D;

    private VfxDispatcher() {
    }

    public static void play(ServerLevel level, VfxEvent event, Vec3 position, @Nullable Vec3 direction,
                            float scale, int color) {
        play(level, event, position, direction, scale, color, DEFAULT_RADIUS);
    }

    public static void play(ServerLevel level, VfxEvent event, Vec3 position, @Nullable Vec3 direction,
                            float scale, int color, double radius) {
        Vec3 dir = direction == null ? Vec3.ZERO : direction.normalize();
        Payloads.Vfx payload = new Payloads.Vfx((byte) event.ordinal(), position.x, position.y, position.z,
                (float) dir.x, (float) dir.y, (float) dir.z, scale, color,
                event.cameraShakeDuration(), -1);
        AnimeKiNetwork.sendToNear(level, position, radius, payload);
    }

    /**
     * Like {@link #play} but with an explicit client side lifetime.
     *
     * <p>Used by long running markers (a marked ultimate target area stays visible until the sphere
     * lands) where the default "camera shake duration" is not the right display time.</p>
     */
    public static void playTimed(ServerLevel level, VfxEvent event, Vec3 position, @Nullable Vec3 direction,
                                 float scale, int color, int displayTicks, double radius) {
        Vec3 dir = direction == null ? Vec3.ZERO : direction.normalize();
        Payloads.Vfx payload = new Payloads.Vfx((byte) event.ordinal(), position.x, position.y, position.z,
                (float) dir.x, (float) dir.y, (float) dir.z, scale, color, displayTicks, -1);
        AnimeKiNetwork.sendToNear(level, position, radius, payload);
    }

    /** Sends an effect to everyone tracking the entity (used for player attached effects). */
    public static void playTracking(Entity source, VfxEvent event, Vec3 position, @Nullable Vec3 direction,
                                    float scale, int color) {
        Vec3 dir = direction == null ? Vec3.ZERO : direction.normalize();
        Payloads.Vfx payload = new Payloads.Vfx((byte) event.ordinal(), position.x, position.y, position.z,
                (float) dir.x, (float) dir.y, (float) dir.z, scale, color, event.cameraShakeDuration(),
                source.getId());
        AnimeKiNetwork.sendToTrackingAndSelf(source, payload);
        if (source.level() instanceof ServerLevel serverLevel) {
            AnimeKiNetwork.sendToNear(serverLevel, position, DEFAULT_RADIUS, payload);
        }
    }

    /** Convenience for effects that originate at a player (charge ticks, vanish, transforms). */
    public static void playOnPlayer(ServerPlayer player, VfxEvent event, Vec3 offset, float scale) {
        Vec3 position = player.position().add(offset).add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        playTracking(player, event, position, null, scale, event.defaultColor());
    }

    public static void playAtEntity(Entity entity, VfxEvent event, float scale) {
        playTracking(entity, event, entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D), null, scale,
                event.defaultColor());
    }

    /** Camera reaction for one player. */
    public static void camera(ServerPlayer player, CameraEffectType type, float intensity, int durationTicks) {
        AnimeKiNetwork.sendToPlayer(player, new Payloads.CameraEffect((byte) type.ordinal(), intensity,
                durationTicks, player.getRandom().nextInt()));
    }

    /** Camera reaction for everyone near a position (used by cinematic impacts). */
    public static void cameraNear(ServerLevel level, Vec3 position, double radius, CameraEffectType type,
                                  float intensity, int durationTicks, int seed) {
        AnimeKiNetwork.sendToNear(level, position, radius, new Payloads.CameraEffect((byte) type.ordinal(),
                intensity, durationTicks, seed));
    }

    /** Standard impact package: world effect plus a camera shake for nearby players. */
    public static void impact(ServerLevel level, VfxEvent event, Vec3 position, @Nullable Vec3 direction, float scale) {
        play(level, event, position, direction, scale, event.defaultColor());
        if (event.cameraShake() > 0.0F) {
            cameraNear(level, position, 32.0D, CameraEffectType.SHAKE, event.cameraShake() * scale,
                    event.cameraShakeDuration(), position.hashCode());
        }
    }
}
