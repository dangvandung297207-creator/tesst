package com.animeki.client.render;

import com.animeki.config.AnimeKiClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Single world rendering entry point for every custom effect.
 *
 * <p>Called once per frame at the {@code AFTER_PARTICLES} stage, it emits auras, beams and impacts
 * into one additive render type and flushes it. One buffer, one flush: adding a new effect type means
 * adding a renderer class, not another pass over the world.</p>
 */
public final class WorldVfxRenderer {
    private WorldVfxRenderer() {
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (AnimeKiClientConfig.VFX.intensity.get() <= 0.0D) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        if (poseStack == null) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = poseStack.last().pose();

        // Camera aligned basis used by the billboards and tubes.
        Vec3 look = event.getCamera().getLookVector();
        Vec3 up = event.getCamera().getUpVector();
        Vec3 right = look.cross(up).normalize();
        Vec3 realUp = right.cross(look).normalize();

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType type = GlowRenderType.get();
        VertexConsumer consumer = buffers.getBuffer(type);

        // Additive effects must not write depth, but they do test against it so they stay behind walls.
        RenderSystem.depthMask(false);
        try {
            ImpactRenderer.render(consumer, matrix, partialTick, camera, right, realUp);
            BeamRenderer.render(consumer, matrix, partialTick, camera, right, realUp);
            AuraRenderer.render(consumer, matrix, partialTick, camera, right, realUp);
        } finally {
            buffers.endBatch(type);
            RenderSystem.depthMask(true);
            poseStack.popPose();
        }
    }
}
