package com.animeki.client.render;

import com.animeki.client.ClientState;
import com.animeki.config.AnimeKiClientConfig;
import com.animeki.util.MathUtil;
import com.animeki.vfx.VfxEvent;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws every queued impact effect.
 *
 * <p>The events are grouped by shape rather than by ability: an expanding ground ring for slams, a
 * shockwave dome for transformations and ultimates, a directional burst for melee and projectile
 * hits, and a rising pillar for energy discharges. That is what lets a new ability get a full
 * cinematic impact by emitting a single event.</p>
 */
public final class ImpactRenderer {
    private ImpactRenderer() {
    }

    public static void render(VertexConsumer consumer, Matrix4f matrix, float partialTick, Vec3 cameraPosition,
                              Vec3 viewRight, Vec3 viewUp) {
        float intensity = AnimeKiClientConfig.VFX.intensity.get().floatValue();
        if (intensity <= 0.0F) {
            return;
        }
        for (ClientState.ActiveVfx vfx : ClientState.ACTIVE_VFX) {
            Vec3 position = new Vec3(vfx.x, vfx.y, vfx.z);
            if (position.distanceToSqr(cameraPosition) > 128.0D * 128.0D) {
                continue;
            }
            float progress = MathUtil.clamp(vfx.progress() + partialTick / Math.max(1, vfx.duration), 0.0F, 1.0F);
            float scale = Math.max(0.2F, vfx.scale);
            int color = vfx.color;
            switch (vfx.event) {
                case GROUND_IMPACT, DASH_IMPACT, SONIC_BOOM -> groundBurst(consumer, matrix, position, progress, scale,
                        color, intensity, viewUp);
                case TRANSFORMATION_SHOCKWAVE, ULTIMATE_IMPACT, CLASH_BURST, BOSS_PHASE -> shockwave(consumer, matrix,
                        position, progress, scale, color, intensity, viewUp, viewRight);
                case HEAVY_HIT, COMBO_HIT, LIGHT_HIT, KI_BLAST_IMPACT, BEAM_IMPACT, AURA_BURST -> directionalBurst(
                        consumer, matrix, vfx, position, progress, scale, color, intensity, viewRight, viewUp);
                case VANISH, APPEAR -> directionalBurst(consumer, matrix, vfx, position, progress, scale, color,
                        intensity, viewRight, viewUp);
                case ULTIMATE_CHARGE, KI_BLAST_FIRE, BEAM_FIRE, BOSS_ROAR -> pillar(consumer, matrix, position,
                        progress, scale, color, intensity, viewRight);
                case TARGET_MARKER -> targetMarker(consumer, matrix, position, scale, color, intensity, progress);
                default -> {
                }
            }
        }
    }

    /** Expanding ring on the ground plus a puff of dust height. */
    private static void groundBurst(VertexConsumer consumer, Matrix4f matrix, Vec3 position, float progress,
                                    float scale, int color, float intensity, Vec3 viewUp) {
        float radius = (0.6F + progress * 5.0F) * scale;
        float fade = 1.0F - progress;
        RenderGeometry.ring(consumer, matrix, position, new Vec3(0.0D, 1.0D, 0.0D), radius * 0.75F, radius, 28,
                RenderGeometry.withAlpha(color, 0.7F * fade * intensity));
        RenderGeometry.disc(consumer, matrix, position, new Vec3(0.0D, 1.0D, 0.0D), radius * 0.6F, 20,
                RenderGeometry.withAlpha(color, 0.18F * fade * intensity));
        RenderGeometry.pillar(consumer, matrix, position, (1.5F + progress * 2.5F) * scale, radius * 0.45F, 8,
                RenderGeometry.withAlpha(color, 0.25F * fade * intensity), viewUp);
    }

    /** Dome + double ring for cinematic impacts. */
    private static void shockwave(VertexConsumer consumer, Matrix4f matrix, Vec3 position, float progress,
                                  float scale, int color, float intensity, Vec3 viewUp, Vec3 viewRight) {
        float radius = (1.0F + progress * 12.0F) * scale;
        float fade = (1.0F - progress) * (1.0F - progress);
        RenderGeometry.ring(consumer, matrix, position, new Vec3(0.0D, 1.0D, 0.0D), radius * 0.8F, radius, 32,
                RenderGeometry.withAlpha(color, 0.8F * fade * intensity));
        RenderGeometry.ring(consumer, matrix, position.add(0.0D, 0.6D + progress * 3.0D, 0.0D),
                new Vec3(0.0D, 1.0D, 0.0D), radius * 0.5F, radius * 0.62F, 28,
                RenderGeometry.withAlpha(color, 0.5F * fade * intensity));
        // Expanding shell approximated by three orthogonal orbs.
        RenderGeometry.orb(consumer, matrix, position.add(0.0D, progress * 2.0D, 0.0D), radius * 0.55F, viewRight,
                viewUp, RenderGeometry.withAlpha(color, 0.22F * fade * intensity));
        RenderGeometry.pillar(consumer, matrix, position, (2.5F + progress * 6.0F) * scale, radius * 0.35F, 10,
                RenderGeometry.withAlpha(color, 0.3F * fade * intensity), viewUp);
    }

    /** Directional burst used by melee and projectile impacts. */
    private static void directionalBurst(VertexConsumer consumer, Matrix4f matrix, ClientState.ActiveVfx vfx,
                                         Vec3 position, float progress, float scale, int color, float intensity,
                                         Vec3 viewRight, Vec3 viewUp) {
        Vec3 direction = new Vec3(vfx.dx, vfx.dy, vfx.dz);
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = new Vec3(0.0D, 1.0D, 0.0D);
        }
        direction = direction.normalize();
        float radius = (0.25F + progress * 1.8F) * scale;
        float fade = 1.0F - progress;
        RenderGeometry.disc(consumer, matrix, position, direction, radius, 14,
                RenderGeometry.withAlpha(color, 0.55F * fade * intensity));
        RenderGeometry.ring(consumer, matrix, position, direction, radius * 1.05F, radius * 1.35F, 16,
                RenderGeometry.withAlpha(color, 0.4F * fade * intensity));
        RenderGeometry.orb(consumer, matrix, position, radius * 0.7F, viewRight, viewUp,
                RenderGeometry.withAlpha(color, 0.35F * fade * intensity));
    }

    /** Rising energy column for charges and roars. */
    private static void pillar(VertexConsumer consumer, Matrix4f matrix, Vec3 position, float progress, float scale,
                               int color, float intensity, Vec3 viewRight) {
        float height = (1.5F + progress * 6.0F) * scale;
        float fade = (1.0F - progress) * (1.0F - progress);
        RenderGeometry.pillar(consumer, matrix, position, height, (0.35F + progress * 0.6F) * scale, 10,
                RenderGeometry.withAlpha(color, 0.35F * fade * intensity), viewRight);
        RenderGeometry.orb(consumer, matrix, position.add(0.0D, height * 0.6D, 0.0D), scale * 0.8F, viewRight,
                new Vec3(0.0D, 1.0D, 0.0D), RenderGeometry.withAlpha(color, 0.3F * fade * intensity));
    }

    /**
     * The marked landing zone of an ultimate: a rotating outer ring, a fixed inner ring and four
     * crosshair spokes, all on the ground plane.
     */
    private static void targetMarker(VertexConsumer consumer, Matrix4f matrix, Vec3 position, float radius,
                                     int color, float intensity, float progress) {
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        float pulse = 0.6F + 0.4F * (float) Math.sin(progress * Math.PI * 2.0D);
        RenderGeometry.ring(consumer, matrix, position.add(0.0D, 0.05D, 0.0D), up, radius * 0.94F, radius,
                (int) radius * 8 + 16, RenderGeometry.withAlpha(color, 0.6F * pulse * intensity));
        RenderGeometry.ring(consumer, matrix, position.add(0.0D, 0.05D, 0.0D), up, radius * 0.25F, radius * 0.32F,
                20, RenderGeometry.withAlpha(color, 0.4F * pulse * intensity));
        for (int i = 0; i < 4; i++) {
            double angle = Math.toRadians(90.0D * i + progress * 90.0D);
            Vec3 direction = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            Vec3 inner = position.add(direction.scale(radius * 0.35F)).add(0.0D, 0.05D, 0.0D);
            Vec3 outer = position.add(direction.scale(radius * 0.9F)).add(0.0D, 0.05D, 0.0D);
            RenderGeometry.quad(consumer, matrix, inner, outer, outer.add(0.0D, 0.0D, 0.12D),
                    inner.add(0.0D, 0.0D, 0.12D), RenderGeometry.withAlpha(color, 0.5F * pulse * intensity));
        }
    }

    /** Debug helper listing the queued events. */
    public static String describe() {
        StringBuilder builder = new StringBuilder();
        for (ClientState.ActiveVfx vfx : ClientState.ACTIVE_VFX) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(vfx.event.id()).append('@').append(vfx.age);
        }
        return builder.length() == 0 ? "no vfx" : builder.toString();
    }

    /** Fired when the local player charges; small local-only pillar so charging always reads. */
    public static VfxEvent chargeEvent() {
        return VfxEvent.KI_CHARGE_TICK;
    }
}
