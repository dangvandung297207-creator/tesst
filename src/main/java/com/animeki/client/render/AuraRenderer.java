package com.animeki.client.render;

import com.animeki.client.ClientState;
import com.animeki.config.AnimeKiClientConfig;
import com.animeki.transformation.AuraProfile;
import com.animeki.transformation.Transformations;
import com.animeki.util.MathUtil;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws the aura around every synced entity.
 *
 * <p>The look is assembled from data (see {@link AuraProfile}): outer shell, inner glow, energy
 * flames, rotating rings, a ground ring and optional procedural lightning. Because the profile is
 * data, a resource pack (or a future Blender exported mesh) can completely replace the silhouette
 * without touching this class - the renderer only decides <em>where</em> things go, never what they
 * look like.</p>
 *
 * <p>Cost control: the aura is a fixed number of quads per entity (no per-frame allocation of
 * vectors beyond a handful), only entities within 48 blocks are considered, and quality level 1
 * removes rings, flames and lightning entirely.</p>
 */
public final class AuraRenderer {
    private static final double MAX_DISTANCE_SQUARED = 48.0D * 48.0D;

    private AuraRenderer() {
    }

    public static void render(VertexConsumer consumer, Matrix4f matrix, float partialTick, Vec3 cameraPosition,
                              Vec3 viewRight, Vec3 viewUp) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !AnimeKiClientConfig.VFX.auraEnabled.get()) {
            return;
        }
        int quality = AnimeKiClientConfig.VFX.auraQuality.get();
        float globalIntensity = AnimeKiClientConfig.VFX.intensity.get().floatValue();
        if (globalIntensity <= 0.0F) {
            return;
        }
        float time = level.getGameTime() + partialTick;

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            ClientState.VisualState visual = ClientState.visual(entity.getId());
            boolean charging = entity == minecraft.player ? ClientState.charging : (visual != null && visual.charging());
            boolean transformed = visual != null && visual.transformed();
            if (!transformed && !charging) {
                continue;
            }
            if (living.isInvisible() || !living.isAlive()) {
                continue;
            }
            if (entity.position().distanceToSqr(cameraPosition) > MAX_DISTANCE_SQUARED) {
                continue;
            }

            AuraProfile profile = resolveProfile(visual == null ? "" : visual.transformationId());
            float intensity = (visual == null ? 0.0F : visual.auraIntensity()) * profile.intensity() * globalIntensity;
            if (charging) {
                intensity = Math.max(intensity, ClientState.charging && entity == minecraft.player ? 0.7F : 0.5F);
            }
            if (intensity <= 0.02F) {
                continue;
            }

            Vec3 center = living.getBoundingBox().getCenter();
            float scale = profile.scale() * (living.getBbHeight() / 1.8F);
            int core = RenderGeometry.withAlpha(profile.coreColor(), Math.min(1.6F, 0.35F + intensity * 0.4F));
            int glow = RenderGeometry.withAlpha(profile.glowColor(), Math.min(1.2F, 0.2F + intensity * 0.3F));

            // Inner glow: a tight shell hugging the body.
            RenderGeometry.orb(consumer, matrix, center, 0.75F * scale, viewRight, viewUp, glow);
            // Outer shell: a taller, softer silhouette.
            Vec3 shellCenter = center.add(0.0D, 0.15D * scale, 0.0D);
            double pulse = 1.0D + Math.sin(time * 0.25D) * 0.05D * intensity;
            RenderGeometry.orb(consumer, matrix, shellCenter, (float) (1.15D * scale * pulse), viewRight, viewUp,
                    RenderGeometry.withAlpha(profile.glowColor(), 0.12F + intensity * 0.18F));

            if (quality >= 2) {
                // Rising flames: a few billboards that travel upwards, staggered in height.
                int flames = Math.min(profile.flames(), 4);
                for (int i = 0; i < flames; i++) {
                    double offset = ((time * 0.08D) + i / (double) flames) % 1.0D;
                    Vec3 flame = living.position().add(0.0D, offset * (living.getBbHeight() + 0.6D), 0.0D);
                    float fade = (float) (1.0D - offset);
                    float width = (0.45F + 0.15F * i) * scale;
                    RenderGeometry.billboard(consumer, matrix, flame, viewRight, viewUp, width * fade, width * 1.4F,
                            RenderGeometry.withAlpha(profile.sparkColor(), 0.18F * intensity * fade));
                }
            }

            if (quality >= 2 && profile.rings() > 0) {
                // Rotating energy rings: flat annuli that spin around the wearer.
                for (int ring = 0; ring < profile.rings(); ring++) {
                    double angle = Math.toRadians(time * profile.ringSpeed() + ring * 60.0D);
                    Vec3 normal = MathUtil.rotateY(new Vec3(0.0D, 1.0D, 0.0D), Math.toDegrees(angle * 0.35D));
                    double height = 0.55D + ring * 0.45D;
                    Vec3 ringCenter = living.position().add(0.0D, height * scale, 0.0D);
                    float radius = (0.95F + ring * 0.22F) * scale;
                    RenderGeometry.ring(consumer, matrix, ringCenter, normal, radius * 0.82F, radius, 20,
                            RenderGeometry.withAlpha(profile.coreColor(), 0.22F * intensity));
                }
            }

            if (profile.groundAura()) {
                Vec3 ground = new Vec3(living.getX(), living.getY() + 0.02D, living.getZ());
                float groundRadius = (float) (1.35D * scale * (1.0D + 0.08D * Math.sin(time * 0.15D)));
                RenderGeometry.ring(consumer, matrix, ground, new Vec3(0.0D, 1.0D, 0.0D), groundRadius * 0.55F,
                        groundRadius, 24, RenderGeometry.withAlpha(profile.glowColor(), 0.3F * intensity));
            }

            if (quality >= 3 && profile.lightning()) {
                // Procedural lightning: a few jagged vertical lines around the body.
                int bolts = 3;
                for (int bolt = 0; bolt < bolts; bolt++) {
                    double seed = time * 0.35D + bolt * 2.1D;
                    double baseAngle = seed * 1.7D + bolt * 2.0D;
                    Vec3 previous = living.position().add(Math.cos(baseAngle) * scale,
                            living.getBbHeight() * 0.15D, Math.sin(baseAngle) * scale);
                    int segments = 4;
                    for (int i = 0; i < segments; i++) {
                        double step = (i + 1) / (double) segments;
                        double wobble = Math.sin(seed * 6.0D + i * 1.7D) * 0.18D * scale;
                        Vec3 next = living.position().add(Math.cos(baseAngle) * scale + wobble,
                                living.getBbHeight() * (0.15D + 0.8D * step),
                                Math.sin(baseAngle) * scale - wobble);
                        RenderGeometry.quad(consumer, matrix, previous, next, next.add(viewRight.scale(0.06F)),
                                previous.add(viewRight.scale(0.06F)),
                                RenderGeometry.withAlpha(profile.sparkColor(), 0.5F * intensity));
                        previous = next;
                    }
                }
            }
        }
    }

    /** Looks up the aura profile of a transformation id, falling back to a neutral white aura. */
    private static AuraProfile resolveProfile(String transformationId) {
        if (!transformationId.isEmpty()) {
            var transformation = Transformations.byId(transformationId);
            if (transformation != null) {
                return transformation.aura();
            }
        }
        return AuraProfile.builder().colors(0xFFFFFFFF, 0x66FFFFFF, 0xFFFFFFFF).build();
    }

    /** Used by the HUD to describe the current form for the local player. */
    public static AuraProfile localProfile() {
        return resolveProfile(ClientState.transformationId);
    }

    /** Convenience for other renderers that need the wearer of a synced aura. */
    public static boolean isTransformed(Entity entity) {
        ClientState.VisualState visual = ClientState.visual(entity.getId());
        if (visual != null && visual.transformed()) {
            return true;
        }
        return entity instanceof Player player && player == Minecraft.getInstance().player
                && !ClientState.transformationId.isEmpty() && ClientState.transformationPhase >= 3;
    }
}
