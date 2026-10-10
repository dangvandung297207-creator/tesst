package com.omnitrix.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.omnitrix.mod.OmnitrixMod;
import com.omnitrix.mod.alien.AlienForm;
import com.omnitrix.mod.client.model.AlienModels;
import com.omnitrix.mod.data.OmnitrixPlayerData;
import com.omnitrix.mod.registry.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = OmnitrixMod.MODID, value = Dist.CLIENT)
public class ClientOmnitrixHandler {

    // Store latest synced data for all known players by entityId
    private static final Map<Integer, OmnitrixPlayerData> playerDataMap = new HashMap<>();

    public static void handleSync(int entityId, OmnitrixPlayerData data) {
        playerDataMap.put(entityId, data);
    }

    public static OmnitrixPlayerData getPlayerData(Player player) {
        if (player == null) return new OmnitrixPlayerData();
        OmnitrixPlayerData data = playerDataMap.get(player.getId());
        if (data == null) {
            data = player.getData(ModRegistry.OMNITRIX_DATA);
            playerDataMap.put(player.getId(), data);
        }
        return data;
    }

    public static OmnitrixPlayerData getLocalPlayerData() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            return getPlayerData(mc.player);
        }
        return new OmnitrixPlayerData();
    }

    /**
     * Intercept 3rd person player rendering to replace humanoid model with custom Alien model!
     */
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        OmnitrixPlayerData data = getPlayerData(player);

        if (data.isTransformed()) {
            // Cancel vanilla player body rendering
            event.setCanceled(true);

            // Render custom 3D alien model with animations
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource buffer = event.getMultiBufferSource();
            int packedLight = event.getPackedLight();
            float partialTick = event.getPartialTick();

            poseStack.pushPose();

            // Apply alien scale & offset
            double scale = data.getCurrentForm().getModelScale();
            poseStack.scale((float) scale, (float) scale, (float) scale);

            // Special rotation for Cannonbolt rolling
            if (data.getCurrentForm() == AlienForm.CANNONBOLT && data.getCannonboltRolling() > 0) {
                float rollAngle = (player.tickCount + partialTick) * 35.0f;
                poseStack.mulPose(Axis.XP.rotationDegrees(rollAngle));
            }

            AlienModels.renderAlien(data.getCurrentForm(), player, poseStack, buffer, packedLight, partialTick);

            poseStack.popPose();
        }
    }

    /**
     * First person arm rendering for transformed aliens
     */
    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        OmnitrixPlayerData data = getPlayerData(player);

        if (data.isTransformed()) {
            // We can cancel vanilla arm and draw the alien arm
            event.setCanceled(true);
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource buffer = event.getMultiBufferSource();
            int packedLight = event.getPackedLight();
            HumanoidArm arm = event.getArm();

            AlienModels.renderFirstPersonArm(data.getCurrentForm(), arm, poseStack, buffer, packedLight);
        }
    }
}
