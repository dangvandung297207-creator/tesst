package com.animeki.client.render;

import com.animeki.AnimeKi;
import com.animeki.boss.BossPhase;
import com.animeki.boss.VoidTitanEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Renders the Void Titan and its phase aura.
 *
 * <p>The entity itself is a normal textured mob; the glowing shell, the rising flames while charging
 * and the phase colour are drawn as extra geometry on top, so the boss reads as a boss from a
 * distance without needing dozens of animation files.</p>
 */
public class VoidTitanRenderer extends MobRenderer<VoidTitanEntity, VoidTitanModel> {
    private static final ResourceLocation TEXTURE = AnimeKi.id("textures/entity/void_titan.png");
    private static final ResourceLocation RAGE_TEXTURE = AnimeKi.id("textures/entity/void_titan_rage.png");

    public VoidTitanRenderer(EntityRendererProvider.Context context) {
        super(context, new VoidTitanModel(context.bakeLayer(VoidTitanModel.LAYER)), 1.6F);
    }

    @Override
    public void render(VoidTitanEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        int color = auraColor(entity.phase(), entity.isRaging());
        float intensity = entity.isRaging() ? 1.4F : 1.0F;
        float phaseProgress = (entity.tickCount + partialTick) * 0.1F;
        VertexConsumer consumer = bufferSource.getBuffer(GlowRenderType.get());
        Matrix4f matrix = poseStack.last().pose();
        Vec3 axisX = new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 axisY = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 axisZ = new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 center = new Vec3(0.0D, 1.1D, 0.0D);

        // Base aura: always visible so the boss silhouette glows.
        RenderGeometry.orb(consumer, matrix, center, 1.5F * intensity, axisX, axisY,
                RenderGeometry.withAlpha(color, 0.18F * intensity));
        RenderGeometry.orb(consumer, matrix, center, 1.5F * intensity, axisZ, axisY,
                RenderGeometry.withAlpha(color, 0.15F * intensity));
        // Ground ring.
        RenderGeometry.ring(consumer, matrix, new Vec3(0.0D, 0.03D, 0.0D), axisY, 1.2F, 2.1F, 24,
                RenderGeometry.withAlpha(color, 0.28F * intensity));
        // Rings rotate faster as the fight escalates.
        int rings = entity.phase() == BossPhase.PHASE_3 ? 3 : 2;
        for (int i = 0; i < rings; i++) {
            double angle = Math.toRadians(phaseProgress * 40.0D + i * 70.0D);
            Vec3 normal = new Vec3(Math.sin(angle) * 0.35D, 1.0D, Math.cos(angle) * 0.35D);
            RenderGeometry.ring(consumer, matrix, center.add(0.0D, i * 0.55D - 0.3D, 0.0D), normal, 1.1F + i * 0.15F,
                    1.45F + i * 0.2F, 20, RenderGeometry.withAlpha(color, 0.3F * intensity));
        }
        if (entity.isCharging()) {
            // Energy gathering for an ultimate or a phase transition.
            RenderGeometry.pillar(consumer, matrix, new Vec3(0.0D, 0.0D, 0.0D), 3.4F, 1.2F, 10,
                    RenderGeometry.withAlpha(color, 0.35F), axisX);
            RenderGeometry.orb(consumer, matrix, center.add(0.0D, 1.2D, 0.0D), 1.1F, axisX, axisY,
                    RenderGeometry.withAlpha(0xFFFFFFFF, 0.4F));
        }
    }

    private static int auraColor(BossPhase phase, boolean raging) {
        if (raging) {
            return 0xFFFF4D4D;
        }
        return switch (phase) {
            case PHASE_2 -> 0xFF9B5CFF;
            case PHASE_3 -> 0xFFFFFFFF;
            default -> 0xFF6A3BB5;
        };
    }

    @Override
    public ResourceLocation getTextureLocation(VoidTitanEntity entity) {
        return entity.isRaging() ? RAGE_TEXTURE : TEXTURE;
    }
}
