package com.animeki.registry;

import com.animeki.AnimeKi;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Items.
 *
 * <p>The mod is a combat framework, so the only shipped item is the Void Titan spawn egg used to
 * summon the test boss. Everything else is driven by keybinds and commands.</p>
 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, AnimeKi.MOD_ID);

    public static final DeferredItem<Item> VOID_TITAN_SPAWN_EGG = ITEMS.register("void_titan_spawn_egg",
            () -> new SpawnEggItem(ModEntities.VOID_TITAN.get(), 0x140A28, 0x9A6BFF, new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(ModItems::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(new ItemStack(VOID_TITAN_SPAWN_EGG.get()), net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
