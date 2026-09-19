package com.megafishing.progression;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.persistence.PlayerData;
import com.megafishing.persistence.PlayerDataManager;
import com.megafishing.ui.MessageManager;
import com.megafishing.util.Text;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IslandMenuManager implements Listener {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final MegaFishingPlugin plugin;
    private final IslandManager islandManager;
    private final ProgressionManager progressionManager;
    private final PlayerDataManager playerDataManager;
    private final MessageManager messages;

    public IslandMenuManager(MegaFishingPlugin plugin, IslandManager islandManager, ProgressionManager progressionManager,
                             PlayerDataManager playerDataManager, MessageManager messages) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.progressionManager = progressionManager;
        this.playerDataManager = playerDataManager;
        this.messages = messages;
    }

    public void openMenu(Player player) {
        PlayerData data = playerDataManager.get(player.getUniqueId());
        if (data == null) {
            messages.send(player, "profile-loading");
            return;
        }
        List<IslandDefinition> islands = new ArrayList<>(islandManager.islands());
        int rows = Math.max(1, Math.min(6, (int) Math.ceil(islands.size() / 9.0D)));
        IslandMenuHolder holder = new IslandMenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, rows * 9, Text.component(messages.raw("island-menu-title")));
        holder.setInventory(inventory);
        for (int i = 0; i < islands.size(); i++) {
            inventory.setItem(i, createItem(data, islands.get(i)));
        }
        player.openInventory(inventory);
    }

    public void sendIslandList(Player player) {
        PlayerData data = playerDataManager.get(player.getUniqueId());
        if (data == null) {
            messages.send(player, "profile-loading");
            return;
        }
        player.sendMessage(Text.component(messages.raw("prefix") + messages.raw("island-list-header")));
        islandManager.islands().forEach(island -> {
            boolean unlocked = data.getUnlockedIslands().contains(island.getId());
            String status = unlocked ? "&aUNLOCKED" : "&cLOCKED &7(" + Text.number(island.getUnlockCost()) + " coins)";
            player.sendMessage(Text.component("&8- " + island.getDisplayName() + " &7[" + island.getId() + "] &8| " + status));
        });
    }

    private ItemStack createItem(PlayerData data, IslandDefinition island) {
        boolean unlocked = data.getUnlockedIslands().contains(island.getId());
        ItemStack item = new ItemStack(unlocked ? Material.LIME_STAINED_GLASS_PANE : Material.PRISMARINE_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.component(island.getDisplayName() + (unlocked ? " &a(UNLOCKED)" : " &c(LOCKED)")));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(Text.component("&7ID: &f" + island.getId()));
        List<com.megafishing.fishing.FishingZone> islandZones = plugin.fishingEnvironmentManager().zonesForIsland(island.getId());
        lore.add(Text.component("&7Unlock Cost: &6" + Text.number(island.getUnlockCost())));
        lore.add(Text.component("&7Zones: &f" + islandZones.size()));
        if (!islandZones.isEmpty()) {
            lore.add(Text.component("&7Primary Zone: &b" + islandZones.get(0).getDisplayName()));
        }
        if (unlocked) {
            lore.add(Text.component("&aAlready unlocked"));
        } else if (data.getCoins() >= island.getUnlockCost()) {
            lore.add(Text.component("&eClick to unlock"));
        } else {
            lore.add(Text.component("&cNot enough coins"));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private void handleClick(Player player, IslandDefinition island) {
        PlayerData data = playerDataManager.get(player.getUniqueId());
        if (data == null) {
            messages.send(player, "profile-loading");
            return;
        }
        ProgressionManager.UnlockResult result = progressionManager.unlockIsland(data, island.getId());
        switch (result) {
            case SUCCESS -> {
                playerDataManager.saveAsync(data);
                messages.sendRaw(player, messages.raw("island-unlocked")
                        .replace("{island}", island.getDisplayName())
                        .replace("{coins}", Text.number(island.getUnlockCost())));
                openMenu(player);
            }
            case ALREADY_UNLOCKED -> messages.sendRaw(player, messages.raw("island-already-unlocked").replace("{island}", island.getDisplayName()));
            case NOT_ENOUGH_COINS -> messages.sendRaw(player, messages.raw("island-cannot-afford")
                    .replace("{island}", island.getDisplayName())
                    .replace("{coins}", Text.number(island.getUnlockCost())));
            case UNKNOWN_ISLAND -> messages.send(player, "island-unknown");
        }
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof IslandMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !holder.owner().equals(player.getUniqueId())) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        List<IslandDefinition> islands = new ArrayList<>(islandManager.islands());
        if (slot >= islands.size()) {
            return;
        }
        handleClick(player, islands.get(slot));
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof IslandMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBrokerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        if (villager.customName() == null || !PLAIN.serialize(villager.customName()).contains("Island Broker")) {
            return;
        }
        event.setCancelled(true);
        openMenu(event.getPlayer());
    }

    public static final class IslandMenuHolder implements InventoryHolder {
        private final UUID owner;
        private Inventory inventory;

        public IslandMenuHolder(UUID owner) {
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
