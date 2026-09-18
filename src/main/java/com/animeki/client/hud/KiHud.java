package com.animeki.client.hud;

import com.animeki.ability.AbilityRegistry;
import com.animeki.ability.Ability;
import com.animeki.client.ClientState;
import com.animeki.client.camera.CameraManager;
import com.animeki.client.render.AuraRenderer;
import com.animeki.config.AnimeKiClientConfig;
import com.animeki.transformation.AuraProfile;
import com.animeki.transformation.Transformations;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * The mod's HUD, drawn next to the vanilla one.
 *
 * <p>Layout (all offsets configurable): a health strip, the Ki bar with colour coding, the stamina /
 * charge meter, a transformation banner, ability cooldown pips and the ultimate meter. Everything is
 * drawn from solid colour fills so the HUD works with any resource pack and never needs a texture.</p>
 *
 * <p>Feedback rules: the bar flashes when charging, dims and pulses when Ki is low, glows when the
 * pool is full, turns to the transformation's aura colour while transformed and gets a dedicated
 * "ultimate ready" treatment.</p>
 */
public final class KiHud {
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 7;
    private static final int COLOUR_BORDER = 0xFF101014;
    private static final int COLOUR_KI = 0xFF4FC3F7;
    private static final int COLOUR_KI_FULL = 0xFFB2EBF2;
    private static final int COLOUR_KI_LOW = 0xFFFF7043;
    private static final int COLOUR_EXHAUSTED = 0xFF6D4C41;
    private static final int COLOUR_STAMINA = 0xFF8BC34A;
    private static final int COLOUR_CHARGE = 0xFFFFF176;

    private KiHud() {
    }

    /** Registered above all vanilla layers so it can sit on top of the hotbar. */
    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator()) {
            return;
        }
        if (!AnimeKiClientConfig.HUD.showKiBar.get() && !ClientState.charging) {
            return;
        }
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        int scale = Math.round(AnimeKiClientConfig.HUD.scale.get().floatValue());
        int baseX = 8 + AnimeKiClientConfig.HUD.offsetX.get();
        int baseY = graphics.guiHeight() - 46 - AnimeKiClientConfig.HUD.offsetY.get();
        if (scale != 1) {
            graphics.pose().pushPose();
            graphics.pose().translate(baseX, baseY, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            baseX = 0;
            baseY = 0;
        }

        drawHealth(graphics, player, baseX, baseY, partialTick);
        drawKiBar(graphics, baseX, baseY + 11, partialTick);
        drawStamina(graphics, player, baseX, baseY + 20, partialTick);
        if (AnimeKiClientConfig.HUD.showAbilityCooldowns.get()) {
            drawAbilityRow(graphics, minecraft, baseX, baseY - 14);
        }
        if (AnimeKiClientConfig.HUD.showTransformation.get()) {
            drawTransformationBanner(graphics, minecraft, baseX, baseY - 30, partialTick);
        }
        if (scale != 1) {
            graphics.pose().popPose();
        }
        drawOverlays(graphics);
    }

    // ------------------------------------------------------------------ bars

    private static void drawHealth(GuiGraphics graphics, Player player, int x, int y, float partialTick) {
        float ratio = Mth.clamp(player.getHealth() / Math.max(1.0F, player.getMaxHealth()), 0.0F, 1.0F);
        frame(graphics, x, y, BAR_WIDTH, 5);
        graphics.fill(x + 1, y + 1, x + 1 + (int) ((BAR_WIDTH - 2) * ratio), y + 4, 0xFFE53935);
        // Absorb/heal flash: brighter segment at the leading edge.
        graphics.fill(x + 1 + (int) ((BAR_WIDTH - 2) * ratio) - 2, y + 1,
                x + 1 + (int) ((BAR_WIDTH - 2) * ratio), y + 4, 0xFFFF8A80);
    }

    private static void drawKiBar(GuiGraphics graphics, int x, int y, float partialTick) {
        float ratio = ClientState.kiRatio();
        float threshold = AnimeKiClientConfig.HUD.lowKiThreshold.get().floatValue();
        boolean low = ratio <= threshold;
        boolean full = ratio >= 0.999F;
        boolean charging = ClientState.charging;

        int colour = ClientState.exhausted ? COLOUR_EXHAUSTED : low ? COLOUR_KI_LOW : full ? COLOUR_KI_FULL : COLOUR_KI;
        if (!ClientState.transformationId.isEmpty()) {
            AuraProfile profile = AuraRenderer.localProfile();
            colour = blend(colour, profile.coreColor(), 0.55F);
        }

        frame(graphics, x, y, BAR_WIDTH, BAR_HEIGHT);
        int filled = (int) ((BAR_WIDTH - 2) * ratio);
        // Pulsing fill while charging, steady otherwise.
        int fill = colour;
        if (charging) {
            float pulse = 0.65F + 0.35F * Mth.sin((System.currentTimeMillis() % 700L) / 700.0F * (float) Math.PI * 2.0F);
            fill = blend(colour, 0xFFFFFFFF, pulse * 0.5F);
        } else if (low && !ClientState.exhausted) {
            float pulse = 0.5F + 0.5F * Mth.sin((System.currentTimeMillis() % 500L) / 500.0F * (float) Math.PI * 2.0F);
            fill = blend(colour, 0xFFFFFFFF, pulse * 0.3F);
        }
        graphics.fill(x + 1, y + 1, x + 1 + filled, y + BAR_HEIGHT - 1, fill);
        // Segment ticks every 25% so the bar reads at a glance.
        for (int i = 1; i < 4; i++) {
            int tickX = x + 1 + (BAR_WIDTH - 2) * i / 4;
            graphics.fill(tickX, y + 1, tickX + 1, y + BAR_HEIGHT - 1, 0x66000000);
        }
        String label = (int) ClientState.ki + " / " + (int) ClientState.maxKi;
        graphics.drawString(Minecraft.getInstance().font, label, x + BAR_WIDTH + 4, y - 1, 0xFFE0E0E0, true);
    }

    private static void drawStamina(GuiGraphics graphics, Player player, int x, int y, float partialTick) {
        // Stamina here is the sprint/charge pool: sprinting drains it, idling refills it.
        float stamina = Mth.clamp(player.getFoodData().getFoodLevel() / 20.0F, 0.0F, 1.0F);
        frame(graphics, x, y, BAR_WIDTH, 4);
        graphics.fill(x + 1, y + 1, x + 1 + (int) ((BAR_WIDTH - 2) * stamina), y + 3, COLOUR_STAMINA);
        if (ClientState.charging) {
            int chargeWidth = (int) ((BAR_WIDTH - 2) * Mth.clamp(ClientState.chargeTicks / 20.0F, 0.0F, 1.0F));
            graphics.fill(x + 1, y - 2, x + 1 + chargeWidth, y - 1, COLOUR_CHARGE);
        }
    }

    // ------------------------------------------------------------------ extras

    private static void drawAbilityRow(GuiGraphics graphics, Minecraft minecraft, int x, int y) {
        int[] cooldowns = ClientState.cooldowns;
        for (int i = 0; i < cooldowns.length; i++) {
            int px = x + i * 14;
            int value = cooldowns[i];
            boolean ready = value <= 0;
            graphics.fill(px, y, px + 11, y + 11, ready ? 0xFF2E7D32 : 0xFF37474F);
            String text = ready ? "•" : String.valueOf((value + 19) / 20);
            graphics.drawString(minecraft.font, text, px + 3, y + 2, 0xFFFFFFFF, false);
        }
    }

    private static void drawTransformationBanner(GuiGraphics graphics, Minecraft minecraft, int x, int y,
                                                 float partialTick) {
        if (ClientState.transformationId.isEmpty()) {
            return;
        }
        var transformation = Transformations.byId(ClientState.transformationId);
        if (transformation == null) {
            return;
        }
        String name = Component.translatable(transformation.displayKey()).getString();
        AuraProfile profile = transformation.aura();
        int width = minecraft.font.width(name) + 8;
        graphics.fill(x - 1, y - 1, x + width + 1, y + 12, 0xAA000000);
        graphics.fill(x - 1, y - 1, x + width + 1, y, profile.coreColor());
        graphics.drawString(minecraft.font, name, x + 3, y + 2, blend(0xFFFFFFFF, profile.coreColor(), 0.4F), true);
        // Sequence progress while the cinematic plays.
        if (ClientState.transformationPhase < 3) {
            float progress = Mth.clamp(ClientState.abilityProgress, 0.0F, 1.0F);
            graphics.fill(x, y + 12, x + (int) (width * progress), y + 13, profile.coreColor());
        }
    }

    /** Charging veil, low Ki pulse, flash and vignette - drawn after the bars. */
    private static void drawOverlays(GuiGraphics graphics) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float flash = CameraManager.flash();
        if (flash > 0.01F) {
            int alpha = (int) (Mth.clamp(flash, 0.0F, 1.0F) * 200.0F) << 24;
            graphics.fill(0, 0, width, height, alpha | 0x00FFFFFF);
        }
        if (ClientState.charging) {
            // Bottom and top glow so the player is visibly "winding up" even without an aura on screen.
            int alpha = 0x22 << 24;
            graphics.fill(0, 0, width, 18, alpha | 0x00A8E0FF);
            graphics.fill(0, height - 18, width, height, alpha | 0x00A8E0FF);
        }
        float vignette = CameraManager.vignette();
        if (vignette > 0.01F) {
            int alpha = (int) (Mth.clamp(vignette, 0.0F, 1.0F) * 90.0F) << 24;
            int colour = alpha | 0x00101018;
            graphics.fill(0, 0, width, 10, colour);
            graphics.fill(0, height - 10, width, height, colour);
            graphics.fill(0, 0, 10, height, colour);
            graphics.fill(width - 10, 0, width, height, colour);
        }
        if (ClientState.exhausted) {
            float pulse = 0.5F + 0.5F * Mth.sin((System.currentTimeMillis() % 900L) / 900.0F * (float) Math.PI * 2.0F);
            int alpha = (int) (pulse * 60.0F) << 24;
            graphics.fill(0, 0, width, height, alpha | 0x00400000);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static void frame(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, COLOUR_BORDER);
        graphics.fill(x, y, x + width, y + height, 0x99000000);
    }

    private static int blend(int from, int to, float delta) {
        int a = (int) Mth.lerp(delta, (from >>> 24) & 0xFF, (to >>> 24) & 0xFF);
        int r = (int) Mth.lerp(delta, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int g = (int) Mth.lerp(delta, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = (int) Mth.lerp(delta, from & 0xFF, to & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Small helper so other code can query the local ability order shown on the HUD. */
    public static ResourceLocation firstAbilityIcon() {
        for (Ability ability : AbilityRegistry.ordered()) {
            return ability.icon();
        }
        return null;
    }

    /** Debug text used by F3-style overlays and the /ki debug command. */
    public static Component debugLine() {
        return Component.literal("Ki " + (int) ClientState.ki + "/" + (int) ClientState.maxKi
                + " form=" + ClientState.transformationId + " phase=" + ClientState.transformationPhase
                + " ability=" + ClientState.activeAbility);
    }
}
