package com.megafishing.rod;

import com.megafishing.util.Text;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class RodItemFactory {
    private final NamespacedKey rodIdKey;
    private final NamespacedKey versionKey;

    public RodItemFactory(JavaPlugin plugin) {
        this.rodIdKey = new NamespacedKey(plugin, "rod_id");
        this.versionKey = new NamespacedKey(plugin, "version");
    }

    public ItemStack create(RodDefinition definition) {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.component(definition.getDisplayName()));
        meta.lore(List.of(
                Text.component("&6Mega Fishing Rod"),
                Text.component("&7DPS: &f" + Text.number(definition.getDps())),
                Text.component("&7Tension Resistance: &a" + Text.percent(definition.getTensionResistance() * 100.0D)),
                Text.component("&7Cast Range: &f" + Text.number(definition.getCastRange())),
                Text.component("&bQ &7Power Pull")
        ));
        meta.getPersistentDataContainer().set(rodIdKey, PersistentDataType.STRING, definition.getId());
        meta.getPersistentDataContainer().set(versionKey, PersistentDataType.INTEGER, 1);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public String readRodId(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(rodIdKey, PersistentDataType.STRING);
    }
}
