package com.megafishing.rod;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.persistence.PlayerData;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.io.File;

public class RodManager {
    private final MegaFishingPlugin plugin;
    private final RodRegistry registry = new RodRegistry();
    private final RodItemFactory itemFactory;

    public RodManager(MegaFishingPlugin plugin) {
        this.plugin = plugin;
        this.itemFactory = new RodItemFactory(plugin);
    }

    public void reload() {
        registry.load(new File(plugin.getDataFolder(), "rods.yml"), plugin.getLogger());
    }

    public boolean isRod(ItemStack item) {
        return item != null && item.getType() == Material.FISHING_ROD;
    }

    public RodDefinition resolve(ItemStack item) {
        if (!isRod(item)) {
            return null;
        }
        return registry.get(itemFactory.readRodId(item));
    }

    public ItemStack create(String rodId) {
        RodDefinition definition = registry.get(rodId);
        return definition == null ? null : itemFactory.create(definition);
    }

    public ItemStack createSelected(PlayerData data) {
        if (data == null) {
            return null;
        }
        return create(data.getRodId());
    }

    public RodRegistry getRegistry() {
        return registry;
    }

    public RodItemFactory getItemFactory() {
        return itemFactory;
    }

    public void applyCustomDurability(ItemStack rod, boolean escaped) {
        if (rod == null || !plugin.getConfig().getBoolean("rod.durability.enabled", false)) {
            return;
        }
        if (!(rod.getItemMeta() instanceof Damageable damageable)) {
            return;
        }
        int damage = escaped
                ? plugin.getConfig().getInt("rod.durability.damage-on-escape", 0)
                : plugin.getConfig().getInt("rod.durability.damage-on-catch", 1);
        if (damage <= 0) {
            return;
        }
        int unbreaking = rod.getEnchantmentLevel(Enchantment.UNBREAKING);
        if (unbreaking > 0 && Math.random() < (1.0D - (1.0D / (unbreaking + 1.0D)))) {
            return;
        }
        damageable.setDamage(damageable.getDamage() + damage);
        rod.setItemMeta(damageable);
    }
}
