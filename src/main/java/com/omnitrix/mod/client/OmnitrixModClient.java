package com.omnitrix.mod.client;

import com.omnitrix.mod.OmnitrixMod;
import com.omnitrix.mod.client.gui.OmnitrixHudOverlay;
import com.omnitrix.mod.client.model.AlienModels;
import com.omnitrix.mod.entity.DiamondheadShardEntity;
import com.omnitrix.mod.entity.HeatblastFireballEntity;
import com.omnitrix.mod.entity.StinkflySlimeEntity;
import com.omnitrix.mod.registry.ModRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@Mod(value = OmnitrixMod.MODID, dist = Dist.CLIENT)
public class OmnitrixModClient {

    public OmnitrixModClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::registerGuiLayers);
        modEventBus.addListener(this::registerKeyMappings);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        OmnitrixMod.LOGGER.info("Omnitrix client initialized with 8 alien 3D models and VFX engine!");
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register projectile renderers using ThrownItemRenderer
        event.registerEntityRenderer(ModRegistry.HEATBLAST_FIREBALL.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModRegistry.DIAMONDHEAD_SHARD.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModRegistry.STINKFLY_SLIME.get(), ThrownItemRenderer::new);
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(OmnitrixMod.MODID, "hud_overlay"),
                new OmnitrixHudOverlay());
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        ModKeybinds.register(event);
    }
}
