package com.animeki.client.render;

import com.animeki.entity.KiBlastEntity;
import com.animeki.util.MathUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws a Ki blast as a layered energy sphere with a trail.
 *
 * <p>The entity itself is only a hitbox: everything visible here is generated geometry, so a blast
 * costs no texture and no model file. Bigger (charged) blasts get an extra outer shell and a
 * pulsing core.</p>
 */
public class KiBlastRenderer extends EntityRenderer<KiBlastEntity> {
    private static final ResourceLocation FALLBACK = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/particle/particles.png");

    public KiBlastRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(KiBlastEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float size = Math.max(0.05F, entity.size());
        int color = entity.color();
        boolean charged = entity.charged();
        float time = entity.tickCount + partialTick;
        float pulse = 1.0F + Mth.sin(time * 0.6F) * 0.06F;

        VertexConsumer consumer = bufferSource.getBuffer(GlowRenderType.get());
        Matrix4f matrix = poseStack.last().pose();
        Vec3 center = new Vec3(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        // Three world-aligned planes are enough for a symmetric energy ball.
        Vec3 axisX = new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 axisY = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 axisZ = new Vec3(0.0D, 0.0D, 1.0D);

        // Outer shell (only worth drawing when the blast is chunky enough to show it).
        if (charged || size > 0.5F) {
            RenderGeometry.orb(consumer, matrix, center, size * 2.1F * pulse, axisX, axisY,
                    RenderGeometry.withAlpha(color, 0.16F));
            RenderGeometry.orb(consumer, matrix, center, size * 2.1F * pulse, axisZ, axisY,
                    RenderGeometry.withAlpha(color, 0.14F));
        }
        RenderGeometry.orb(consumer, matrix, center, size * 1.25F * pulse, axisX, axisY,
                RenderGeometry.withAlpha(color, 0.55F));
        RenderGeometry.orb(consumer, matrix, center, size * 1.25F * pulse, axisZ, axisY,
                RenderGeometry.withAlpha(color, 0.5F));
        RenderGeometry.orb(consumer, matrix, center, size * 0.65F, axisX, axisY,
                RenderGeometry.withAlpha(0xFFFFFFFF, 0.85F));
        RenderGeometry.orb(consumer, matrix, center, size * 0.65F, axisZ, axisY,
                RenderGeometry.withAlpha(0xFFFFFFFF, 0.8F));

        // Trail: a few fading billboards behind the blast.
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() > 1.0E-5D) {
            Vec3 back = MathUtil.horizontal(velocity).normalize().scale(-1.0D);
            Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
            Vec3 right = back.cross(up).normalize();
            for (int i = 1; i <= 3; i++) {
                double distance = size * 1.4D * i;
                Vec3 trail = center.add(back.scale(distance));
                float fade = 1.0F - i / 4.0F;
                RenderGeometry.billboard(consumer, matrix, trail, right, up, size * 0.9F * fade, size * 0.9F * fade,
                        RenderGeometry.withAlpha(color, 0.28F * fade));
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(KiBlastEntity entity) {
        return FALLBACK;
    }
}
