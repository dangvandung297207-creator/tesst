package com.omnitrix.mod.client.gui;

import com.omnitrix.mod.alien.AlienForm;
import com.omnitrix.mod.client.ClientOmnitrixHandler;
import com.omnitrix.mod.data.OmnitrixPlayerData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

public class OmnitrixHudOverlay implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        OmnitrixPlayerData data = ClientOmnitrixHandler.getLocalPlayerData();
        if (!data.hasOmnitrix()) return;

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        // Top-left futuristic Omnitrix status widget
        int hudX = 10;
        int hudY = 10;

        // Background dark tech slate
        graphics.fill(hudX, hudY, hudX + 130, hudY + 44, 0x88001100);
        graphics.renderOutline(hudX, hudY, 130, 44, 0xFF00FF66);

        // Omnitrix Status Indicator
        boolean transformed = data.isTransformed();
        boolean coolingDown = data.isCoolingDown();

        int statusColor = coolingDown ? 0xFFFF2222 : (transformed ? 0xFF00FF66 : 0xFFFFAA00);
        String statusText = coolingDown ? "RECHARGING" : (transformed ? data.getCurrentForm().getDisplayName().toUpperCase() : "STANDBY");

        // Mini Dial Icon
        graphics.fill(hudX + 6, hudY + 6, hudX + 22, hudY + 22, statusColor);
        graphics.fill(hudX + 9, hudY + 9, hudX + 19, hudY + 19, 0xFF000000);

        graphics.drawString(mc.font, statusText, hudX + 28, hudY + 8, statusColor, false);

        if (transformed) {
            // Transformation countdown bar
            int timeLeft = data.getTransformTicksLeft();
            int maxTime = 1200;
            float timeRatio = (float) timeLeft / maxTime;
            int barWidth = (int) (96 * timeRatio);

            graphics.drawString(mc.font, "DNA: " + (timeLeft / 20) + "s", hudX + 28, hudY + 20, 0xFFAAAAAA, false);
            graphics.fill(hudX + 28, hudY + 30, hudX + 28 + 96, hudY + 35, 0xFF222222);
            graphics.fill(hudX + 28, hudY + 30, hudX + 28 + barWidth, hudY + 35, 0xFF00FF66);

            // Bottom-right Ability cooldowns & Ultimate Gauge
            renderAbilityHotbar(graphics, mc, data, screenWidth, screenHeight);
        } else if (coolingDown) {
            int rechargeLeft = data.getRechargeCooldown();
            graphics.drawString(mc.font, "Cool: " + (rechargeLeft / 20) + "s", hudX + 28, hudY + 22, 0xFFFF4444, false);
        } else {
            graphics.drawString(mc.font, "Selected: " + data.getSelectedForm().getDisplayName(), hudX + 28, hudY + 22, 0xFF88FF88, false);
        }
    }

    private void renderAbilityHotbar(GuiGraphics graphics, Minecraft mc, OmnitrixPlayerData data, int width, int height) {
        int barX = width / 2 - 70;
        int barY = height - 48;

        // Container panel
        graphics.fill(barX - 4, barY - 4, barX + 144, barY + 26, 0x88001100);
        graphics.renderOutline(barX - 4, barY - 4, 148, 30, 0xFF00FF66);

        // 3 Ability Slots + 1 Ultimate Slot
        for (int i = 0; i < 4; i++) {
            int slotX = barX + i * 36;
            int slotY = barY;

            boolean isUlt = (i == 3);
            int boxColor = isUlt ? 0xAA331100 : 0xAA003311;
            int border = isUlt ? 0xFFFF8800 : 0xFF00FF88;

            graphics.fill(slotX, slotY, slotX + 30, slotY + 20, boxColor);
            graphics.renderOutline(slotX, slotY, 30, 20, border);

            String keyLabel = switch (i) {
                case 0 -> "Z";
                case 1 -> "X";
                case 2 -> "C";
                case 3 -> "V";
                default -> "";
            };

            int cd = switch (i) {
                case 0 -> data.getAbility1Cooldown();
                case 1 -> data.getAbility2Cooldown();
                case 2 -> data.getAbility3Cooldown();
                case 3 -> (data.getUltimateCharge() < 100) ? (100 - data.getUltimateCharge()) : 0;
                default -> 0;
            };

            if (isUlt) {
                if (data.getUltimateCharge() >= 100) {
                    graphics.drawString(mc.font, "READY", slotX + 2, slotY + 6, 0xFFFFCC00, false);
                } else {
                    graphics.drawString(mc.font, data.getUltimateCharge() + "%", slotX + 5, slotY + 6, 0xFFAAAAAA, false);
                }
            } else {
                if (cd > 0) {
                    graphics.drawString(mc.font, (cd / 20 + 1) + "s", slotX + 8, slotY + 6, 0xFFFF4444, false);
                } else {
                    graphics.drawString(mc.font, keyLabel, slotX + 11, slotY + 6, 0xFFFFFFFF, false);
                }
            }
        }
    }
}
