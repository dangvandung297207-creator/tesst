package com.animeki.client;

import com.animeki.AnimeKi;
import com.animeki.client.camera.CameraManager;
import com.animeki.client.hud.KiHud;
import com.animeki.client.input.ClientInput;
import com.animeki.client.render.KiBlastRenderer;
import com.animeki.client.render.VoidTitanModel;
import com.animeki.client.render.VoidTitanRenderer;
import com.animeki.client.render.WorldVfxRenderer;
import com.animeki.client.vfx.ClientVfx;
import com.animeki.config.AnimeKiClientConfig;
import com.animeki.registry.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client entry point.
 *
 * <p>Loaded only on the physical client (the main mod class calls it from inside a dist check), so no
 * rendering class is ever referenced on a dedicated server. Everything registered here is either a
 * mod bus listener (keybinds, renderers, HUD layers) or a game bus listener (frame hooks, input,
 * ticks).</p>
 */
public final class AnimeKiClient {
    private AnimeKiClient() {
    }

    public static void init(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, AnimeKiClientConfig.SPEC);

        // Mod bus: registries that must be filled before the game starts.
        modBus.addListener(AnimeKiClient::registerKeyMappings);
        modBus.addListener(AnimeKiClient::registerRenderers);
        modBus.addListener(AnimeKiClient::registerLayerDefinitions);
        modBus.addListener(AnimeKiClient::registerGuiLayers);

        // Game bus: per frame / per tick feedback.
        NeoForge.EVENT_BUS.addListener(AnimeKiClient::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(AnimeKiClient::onComputeCameraAngles);
        NeoForge.EVENT_BUS.addListener(AnimeKiClient::onComputeFov);
        NeoForge.EVENT_BUS.addListener(AnimeKiClient::onKey);
        NeoForge.EVENT_BUS.addListener(AnimeKiClient::onMouseButton);
        NeoForge.EVENT_BUS.addListener(AnimeKiClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(AnimeKiClient::onLoggingOut);
    }

    // ------------------------------------------------------------------ registries

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping mapping : KeyBindings.all()) {
            event.register(mapping);
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.KI_BLAST.get(), KiBlastRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_TITAN.get(), VoidTitanRenderer::new);
    }

    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(VoidTitanModel.LAYER, VoidTitanModel::createBodyLayer);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        // Drawn above every vanilla layer: the Ki HUD owns a strip of its own above the hotbar.
        event.registerAboveAll(AnimeKi.id("ki_hud"), KiHud::render);
    }

    // ------------------------------------------------------------------ frame hooks

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        WorldVfxRenderer.onRenderLevelStage(event);
    }

    private static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        CameraManager.applyAngles(event);
    }

    private static void onComputeFov(ViewportEvent.ComputeFov event) {
        CameraManager.applyFov(event);
    }

    private static void onKey(InputEvent.Key event) {
        ClientInput.onKey(event);
    }

    private static void onMouseButton(InputEvent.MouseButton.Post event) {
        ClientInput.onMouseButton(event);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        ClientVfx.tick();
        CameraManager.tick();
        ClientInput.onClientTick();
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.clear();
        CameraManager.clear();
    }

    /** Convenience for the debug overlay. */
    public static ResourceLocation hudLayerId() {
        return AnimeKi.id("ki_hud");
    }
}
