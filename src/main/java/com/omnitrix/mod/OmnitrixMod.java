package com.omnitrix.mod;

import com.mojang.logging.LogUtils;
import com.omnitrix.mod.network.ModNetwork;
import com.omnitrix.mod.registry.ModRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(OmnitrixMod.MODID)
public class OmnitrixMod {

    public static final String MODID = "omnitrix";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Configuration Spec
    public static class ModConfiguration {
        public static final ModConfigSpec SPEC;
        public static final ModConfigSpec.IntValue TRANSFORMATION_DURATION_SECONDS;
        public static final ModConfigSpec.IntValue RECHARGE_COOLDOWN_SECONDS;
        public static final ModConfigSpec.BooleanValue ENABLE_SCREEN_SHAKE;
        public static final ModConfigSpec.BooleanValue ENABLE_BLOCK_DAMAGE;

        static {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

            builder.push("general");
            TRANSFORMATION_DURATION_SECONDS = builder
                    .comment("Transformation duration in seconds")
                    .defineInRange("transformation_duration", 60, 10, 600);

            RECHARGE_COOLDOWN_SECONDS = builder
                    .comment("Recharge cooldown duration after timing out in seconds")
                    .defineInRange("recharge_cooldown", 20, 0, 300);

            ENABLE_SCREEN_SHAKE = builder
                    .comment("Enable camera shake and impact feedback")
                    .define("screen_shake", true);

            ENABLE_BLOCK_DAMAGE = builder
                    .comment("Allow explosions to damage blocks")
                    .define("allow_block_damage", false);

            builder.pop();
            SPEC = builder.build();
        }
    }

    public OmnitrixMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Omnitrix: Ultimate Alien Experience!");

        // Register Deferred Registers
        ModRegistry.register(modEventBus);

        // Register Mod Lifecycle Events
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModNetwork::register);

        // Register Config
        modContainer.registerConfig(ModConfig.Type.COMMON, ModConfiguration.SPEC);

        // Register Game Event Bus
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Omnitrix common setup completed successfully!");
    }
}
