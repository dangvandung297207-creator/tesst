package com.animeki.client.render;

import com.animeki.client.ClientState;
import com.animeki.config.AnimeKiClientConfig;
import com.animeki.util.MathUtil;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws the server synced beams.
 *
 * <p>The client receives a compact snapshot every other tick and interpolates the length in between,
 * so a beam looks continuous even though it costs two tiny packets per second instead of a stream of
 * entity updates.</p>
 *
 * <p>Each beam is a glowing tube plus a hot core, an impact disc where it lands and - during a power
 * clash - a sphere of sparks at the collision point with the two beams visibly straining against each
 * other.</p>
 */
public final class BeamRenderer {
    /** Sides of the beam tube; scaled down on low quality settings. */
    private static final int SIDES_HIGH = 8;
    private static final int SIDES_LOW = 4;

    private BeamRenderer() {
    }

    public static void render(VertexConsumer consumer, Matrix4f matrix, float partialTick, Vec3 cameraPosition,
                              Vec3 viewRight, Vec3 viewUp) {
        if (!AnimeKiClientConfig.VFX.beamsEnabled.get()) {
            return;
        }
        float intensity = AnimeKiClientConfig.VFX.intensity.get().floatValue();
        if (intensity <= 0.0F) {
            return;
        }
        int sides = AnimeKiClientConfig.VFX.auraQuality.get() >= 3 ? SIDES_HIGH : SIDES_LOW;
        double time = System.nanoTime() / 50_000_000.0D;

        for (ClientState.BeamView beam : ClientState.BEAMS) {
            Vec3 origin = new Vec3(beam.originX, beam.originY, beam.originZ);
            if (origin.distanceToSqr(cameraPosition) > 128.0D * 128.0D) {
                continue;
            }
            Vec3 direction = new Vec3(beam.dirX, beam.dirY, beam.dirZ);
            if (direction.lengthSqr() < 1.0E-6D) {
                continue;
            }
            direction = direction.normalize();
            float length = Math.max(0.5F, beam.renderLength(partialTick));
            float width = Math.max(0.08F, beam.width);
            float flicker = (float) (1.0D + Math.sin(time * 1.7D + beam.id) * 0.06D);

            // Outer glow, hot core, and a bright tip.
            RenderGeometry.tube(consumer, matrix, origin, direction, length, width * 1.6F * flicker, sides,
                    RenderGeometry.withAlpha(beam.glowColor, 0.35F * intensity), viewRight, viewUp);
            RenderGeometry.tube(consumer, matrix, origin, direction, length, width * 0.7F, sides,
                    RenderGeometry.withAlpha(beam.coreColor, 0.85F * intensity), viewRight, viewUp);

            Vec3 end = origin.add(direction.scale(length));
            float headRadius = width * (1.1F + 0.1F * flicker);
            RenderGeometry.orb(consumer, matrix, end, headRadius * 1.4F, viewRight, viewUp,
                    RenderGeometry.withAlpha(beam.coreColor, 0.7F * intensity));
            RenderGeometry.disc(consumer, matrix, end, direction.scale(-1.0D), headRadius * 1.8F, 16,
                    RenderGeometry.withAlpha(beam.glowColor, 0.5F * intensity));

            if (beam.clashPartner >= 0) {
                Vec3 clash = new Vec3(beam.clashX, beam.clashY, beam.clashZ);
                float pressure = (float) Math.abs(beam.clashRatio);
                float sphere = width * (2.0F + pressure * 5.0F);
                RenderGeometry.orb(consumer, matrix, clash, sphere, viewRight, viewUp,
                        RenderGeometry.withAlpha(0xFFFFFFFF, (0.35F + pressure * 0.6F) * intensity));
                // Sparks escaping the contact point.
                int sparks = 6 + (int) (pressure * 8.0F);
                for (int i = 0; i < sparks; i++) {
                    double angle = time * 0.9D + i * (Math.PI * 2.0D / sparks);
                    double radius = sphere * (1.2D + 0.6D * Math.sin(time * 0.7D + i));
                    Vec3 spark = clash.add(Math.cos(angle) * radius, Math.sin(angle * 1.7D) * radius * 0.6D,
                            Math.sin(angle) * radius);
                    RenderGeometry.billboard(consumer, matrix, spark, viewRight, viewUp, 0.09F, 0.09F,
                            RenderGeometry.withAlpha(0xFFFFFFFF, 0.6F * intensity));
                }
            }
        }
    }

    /** Screen space "kick" strength for the local player while a clash is happening nearby. */
    public static float localClashPressure(Vec3 cameraPosition) {
        float pressure = 0.0F;
        for (ClientState.BeamView beam : ClientState.BEAMS) {
            if (beam.clashPartner < 0) {
                continue;
            }
            double distance = cameraPosition.distanceTo(new Vec3(beam.clashX, beam.clashY, beam.clashZ));
            if (distance > 48.0D) {
                continue;
            }
            float falloff = (float) (1.0D - distance / 48.0D);
            pressure = Math.max(pressure, (float) Math.abs(beam.clashRatio) * falloff);
        }
        return Mth.clamp(pressure, 0.0F, 1.0F);
    }

    /** Helper used by the HUD to show how many beams are active. */
    public static int activeBeams() {
        return ClientState.BEAMS.size();
    }

    /** Utility for the debug overlay. */
    public static String describe() {
        if (ClientState.BEAMS.isEmpty()) {
            return "no beams";
        }
        ClientState.BeamView beam = ClientState.BEAMS.get(0);
        return "beams=" + ClientState.BEAMS.size() + " first=" + beam.typeId + " len="
                + MathUtil.clamp(beam.length, 0.0F, 9999.0F);
    }
}
