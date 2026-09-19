package com.megafishing.storage;

import com.megafishing.fish.FishDefinition;
import com.megafishing.persistence.CaughtFishRecord;
import com.megafishing.persistence.PlayerData;
import com.megafishing.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class StorageManager implements Listener {
    public boolean addFish(PlayerData data, CaughtFishRecord record) {
        return data.getFishBag().add(record);
    }

    public List<CaughtFishRecord> contents(PlayerData data) {
        return data.getFishBag().snapshot();
    }

    public Inventory createBagInventory(PlayerData data, Function<String, FishDefinition> resolver) {
        int rows = Math.max(1, Math.min(6, (int) Math.ceil(Math.max(data.getFishBag().getCapacity(), data.getFishBag().size()) / 9.0D)));
        FishBagHolder holder = new FishBagHolder(data.getUuid());
        Inventory inventory = Bukkit.createInventory(holder, rows * 9, Text.component("&1Fish Bag &7(" + data.getFishBag().size() + "/" + data.getFishBag().getCapacity() + ")"));
        holder.setInventory(inventory);
        int slot = 0;
        for (CaughtFishRecord record : data.getFishBag().contents()) {
            FishDefinition definition = resolver.apply(record.getFishId());
            Material material = definition == null ? Material.COD : definition.getDisplayMaterial();
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.component((definition == null ? "&f" + record.getFishId() : definition.getDisplayName()) + " &7- &f" + Text.number(record.getWeight()) + "kg"));
            meta.lore(List.of(
                    Text.component("&7Rarity: &f" + record.getRarity().name()),
                    Text.component("&7Value: &6" + Text.number(record.getValue())),
                    Text.component("&7Caught: &f" + record.getTimestamp())
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
            inventory.setItem(slot++, item);
            if (slot >= inventory.getSize()) {
                break;
            }
        }
        return inventory;
    }

    public void openBag(Player player, PlayerData data, Function<String, FishDefinition> resolver) {
        player.openInventory(createBagInventory(data, resolver));
    }

    @EventHandler
    public void onBagClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof FishBagHolder holder) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player && !holder.owner().equals(player.getUniqueId())) {
                return;
            }
        }
    }

    @EventHandler
    public void onBagDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof FishBagHolder) {
            event.setCancelled(true);
        }
    }

    public static final class FishBagHolder implements InventoryHolder {
        private final UUID owner;
        private Inventory inventory;

        public FishBagHolder(UUID owner) {
            this.owner = owner;
        }

        public UUID owner() {
            return owner;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
