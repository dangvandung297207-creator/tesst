package com.omnitrix.mod.registry;

import com.omnitrix.mod.OmnitrixMod;
import com.omnitrix.mod.data.OmnitrixPlayerData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModRegistry {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OmnitrixMod.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OmnitrixMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, OmnitrixMod.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, OmnitrixMod.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, OmnitrixMod.MODID);

    // Items
    public static final DeferredItem<Item> OMNITRIX_ITEM = ITEMS.registerItem("omnitrix",
            properties -> new com.omnitrix.mod.item.OmnitrixItem(properties.stacksTo(1).fireResistant()));

    // Creative Tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> OMNITRIX_TAB = CREATIVE_MODE_TABS.register("omnitrix_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.omnitrix"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> OMNITRIX_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(OMNITRIX_ITEM.get());
                    }).build());

    // Sounds
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_OMNITRIX_SELECT = registerSound("omnitrix_select");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_OMNITRIX_POPUP = registerSound("omnitrix_popup");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_TRANSFORM_START = registerSound("transform_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_TRANSFORM_BURST = registerSound("transform_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_REVERT = registerSound("revert");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_TIMEOUT_WARNING = registerSound("timeout_warning");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_ABILITY_FIREBALL = registerSound("ability_fireball");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_ABILITY_FIRE_SLAM = registerSound("ability_fire_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_ABILITY_SPEED_DASH = registerSound("ability_speed_dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_ABILITY_GROUND_SLAM = registerSound("ability_ground_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_ABILITY_CRYSTAL_SHARD = registerSound("ability_crystal_shard");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_ABILITY_CRYSTAL_WALL = registerSound("ability_crystal_wall");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_ABILITY_ROLL_START = registerSound("ability_roll_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_ABILITY_ROLL_IMPACT = registerSound("ability_roll_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_ABILITY_TECH_BEAM = registerSound("ability_tech_beam");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_ABILITY_SLIME_SPIT = registerSound("ability_slime_spit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_ABILITY_SONAR_PULSE = registerSound("ability_sonar_pulse");

    private static DeferredHolder<SoundEvent, SoundEvent> registerSound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(OmnitrixMod.MODID, name)));
    }

    // Projectile Entities
    public static final DeferredHolder<EntityType<?>, EntityType<com.omnitrix.mod.entity.HeatblastFireballEntity>> HEATBLAST_FIREBALL =
            ENTITY_TYPES.register("heatblast_fireball", () ->
                    EntityType.Builder.<com.omnitrix.mod.entity.HeatblastFireballEntity>of(com.omnitrix.mod.entity.HeatblastFireballEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("heatblast_fireball"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.omnitrix.mod.entity.DiamondheadShardEntity>> DIAMONDHEAD_SHARD =
            ENTITY_TYPES.register("diamondhead_shard", () ->
                    EntityType.Builder.<com.omnitrix.mod.entity.DiamondheadShardEntity>of(com.omnitrix.mod.entity.DiamondheadShardEntity::new, MobCategory.MISC)
                            .sized(0.4f, 0.4f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("diamondhead_shard"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.omnitrix.mod.entity.StinkflySlimeEntity>> STINKFLY_SLIME =
            ENTITY_TYPES.register("stinkfly_slime", () ->
                    EntityType.Builder.<com.omnitrix.mod.entity.StinkflySlimeEntity>of(com.omnitrix.mod.entity.StinkflySlimeEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("stinkfly_slime"));

    // Player Data Attachment
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<OmnitrixPlayerData>> OMNITRIX_DATA =
            ATTACHMENT_TYPES.register("player_data", () ->
                    AttachmentType.builder(OmnitrixPlayerData::new)
                            .serialize(OmnitrixPlayerData.CODEC)
                            .copyOnDeath()
                            .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        SOUNDS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
