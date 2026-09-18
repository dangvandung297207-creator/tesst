package com.animeki;

import com.animeki.command.KiCommand;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.gameplay.CommonGameplayEvents;
import com.animeki.registry.ModAttachments;
import com.animeki.registry.ModDamageTypes;
import com.animeki.registry.ModEntities;
import com.animeki.registry.ModItems;
import com.animeki.registry.ModSounds;
import com.animeki.ability.AbilityRegistry;
import com.animeki.network.AnimeKiNetwork;
import com.animeki.transformation.Transformations;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Anime Ki Combat - an original anime-inspired Ki energy martial arts combat framework.
 *
 * <p>Everything gameplay critical is server authoritative: the client only sends intent
 * (button presses, aim and direction hints) and receives state + visual events. All playable systems live
 * behind small services so new fighters, skills, transformations, bosses and Blender-authored
 * effects can be added without touching the core.</p>
 */
@Mod(AnimeKi.MOD_ID)
public final class AnimeKi {
    public static final String MOD_ID = "animeki";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AnimeKi(IEventBus modBus, ModContainer container) {
        ModAttachments.register(modBus);
        ModEntities.register(modBus);
        ModItems.register(modBus);
        ModSounds.register(modBus);
        AnimeKiNetwork.register(modBus);

        container.registerConfig(ModConfig.Type.SERVER, AnimeKiServerConfig.SPEC);

        modBus.addListener(this::onCommonSetup);

        // Gameplay hooks (server authoritative systems, ticking, damage interception, commands).
        NeoForge.EVENT_BUS.register(CommonGameplayEvents.class);
        NeoForge.EVENT_BUS.register(KiCommand.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModDamageTypes.bootstrap();
            Transformations.bootstrap();
            AbilityRegistry.bootstrap();
        });
    }

    /** Builds a namespaced id inside the mod namespace. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
