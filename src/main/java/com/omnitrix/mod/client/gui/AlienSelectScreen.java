package com.omnitrix.mod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.omnitrix.mod.alien.AlienForm;
import com.omnitrix.mod.client.ClientOmnitrixHandler;
import com.omnitrix.mod.data.OmnitrixPlayerData;
import com.omnitrix.mod.network.ModNetwork;
import com.omnitrix.mod.network.payload.OmnitrixSelectPayload;
import com.omnitrix.mod.network.payload.OmnitrixTransformPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class AlienSelectScreen extends Screen {

    private float rotationAngle = 0.0f;
    private float targetRotationAngle = 0.0f;
    private int selectedIndex = 0;
    private final AlienForm[] aliens = AlienForm.PLAYABLE_ALIENS;

    public AlienSelectScreen() {
        super(Component.translatable("gui.omnitrix.selector_title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new AlienSelectScreen());
    }

    @Override
    protected void init() {
        super.init();
        OmnitrixPlayerData data = ClientOmnitrixHandler.getLocalPlayerData();
        for (int i = 0; i < aliens.length; i++) {
            if (aliens[i] == data.getSelectedForm()) {
                selectedIndex = i;
                rotationAngle = i * (360.0f / aliens.length);
                targetRotationAngle = rotationAngle;
                break;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Smooth rotational interpolation towards selected alien
        rotationAngle = Mth.lerp(0.25f, rotationAngle, targetRotationAngle);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Subtle dark holographic backdrop
        graphics.fillGradient(0, 0, this.width, this.height, 0x88001100, 0xAA002205);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        AlienForm selected = aliens[selectedIndex];

        // 1. Draw outer holographic ring
        int ringRadius = 90;
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.toRadians((i * 360.0 / segments) + rotationAngle);
            double angle2 = Math.toRadians(((i + 1) * 360.0 / segments) + rotationAngle);
            int x1 = (int) (centerX + Math.cos(angle1) * ringRadius);
            int y1 = (int) (centerY + Math.sin(angle1) * ringRadius);
            int x2 = (int) (centerX + Math.cos(angle2) * ringRadius);
            int y2 = (int) (centerY + Math.sin(angle2) * ringRadius);

            graphics.fill(x1, y1, x2 + 1, y2 + 1, 0xFF00FF66);
        }

        // 2. Draw inner dial silhouette icons & nodes
        float angleStep = 360.0f / aliens.length;
        for (int i = 0; i < aliens.length; i++) {
            double angle = Math.toRadians((i * angleStep) - rotationAngle - 90);
            int iconX = (int) (centerX + Math.cos(angle) * (ringRadius - 15));
            int iconY = (int) (centerY + Math.sin(angle) * (ringRadius - 15));

            boolean isCurrent = (i == selectedIndex);
            int nodeColor = isCurrent ? 0xFF55FF55 : 0xAA008833;
            int nodeSize = isCurrent ? 6 : 4;

            graphics.fill(iconX - nodeSize, iconY - nodeSize, iconX + nodeSize, iconY + nodeSize, nodeColor);

            // Alien initial letter
            String initial = aliens[i].getDisplayName().substring(0, 1);
            graphics.drawString(this.font, initial, iconX - 3, iconY - 4, isCurrent ? 0xFFFFFFFF : 0xCCAAAAAA, false);
        }

        // 3. Central Hologram Centerpiece
        graphics.fill(centerX - 35, centerY - 35, centerX + 35, centerY + 35, 0x5500FF44);
        graphics.renderOutline(centerX - 35, centerY - 35, 70, 70, 0xFF00FF66);

        // Core Omnitrix Hourglass Symbol
        drawOmnitrixSymbol(graphics, centerX, centerY, 0xFF00FF44);

        // 4. Selected Alien HUD details
        String name = selected.getDisplayName();
        graphics.drawCenteredString(this.font, "§l" + name.toUpperCase(), centerX, centerY - 55, selected.getThemeColor());

        graphics.drawCenteredString(this.font, "§7Health: §f" + (int)selected.getMaxHealth() + " HP", centerX, centerY + 45, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, "§7Attack: §f+" + (int)selected.getAttackDamage() + "  |  Speed: §f" + (int)(selected.getMovementSpeed()*100), centerX, centerY + 58, 0xFFFFFFFF);

        graphics.drawCenteredString(this.font, "§a[SCROLL / A-D] §7Select   §a[SPACE / ENTER] §7Transform", centerX, this.height - 30, 0xFF88FF88);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawOmnitrixSymbol(GuiGraphics graphics, int cx, int cy, int color) {
        // Draw Hourglass / Dial
        for (int i = -16; i <= 16; i++) {
            int span = Math.abs(i);
            graphics.hLine(cx - span, cx + span, cy + i, color);
        }
        // Center black diamond
        graphics.fill(cx - 4, cy - 4, cx + 4, cy + 4, 0xFF000000);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            selectNext();
            return true;
        } else if (scrollY < 0) {
            selectPrevious();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
            selectNext();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
            selectPrevious();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            confirmTransform();
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void selectNext() {
        selectedIndex = (selectedIndex + 1) % aliens.length;
        targetRotationAngle = selectedIndex * (360.0f / aliens.length);
        sendSelection();
    }

    private void selectPrevious() {
        selectedIndex = (selectedIndex - 1 + aliens.length) % aliens.length;
        targetRotationAngle = selectedIndex * (360.0f / aliens.length);
        sendSelection();
    }

    private void sendSelection() {
        AlienForm form = aliens[selectedIndex];
        ModNetwork.sendToServer(new OmnitrixSelectPayload(form.getId()));
    }

    private void confirmTransform() {
        ModNetwork.sendToServer(new OmnitrixTransformPayload(false));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
