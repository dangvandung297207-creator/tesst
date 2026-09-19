package com.megafishing.economy;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.persistence.PlayerData;
import com.megafishing.persistence.PlayerDataManager;
import com.megafishing.rod.RodDefinition;
import com.megafishing.rod.RodManager;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class RodShopManager implements Listener {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final MegaFishingPlugin plugin;
    private final RodManager rodManager;
    private final PlayerDataManager playerDataManager;
    private final MessageManager messages;

    public RodShopManager(MegaFishingPlugin plugin, RodManager rodManager, PlayerDataManager playerDataManager, MessageManager messages) {
        this.plugin = plugin;
        this.rodManager = rodManager;
        this.playerDataManager = playerDataManager;
        this.messages = messages;
    }

    public void openShop(Player player) {
        PlayerData data = playerDataManager.get(player.getUniqueId());
        if (data == null) {
            messages.send(player, "profile-loading");
            return;
        }
        List<RodDefinition> rods = new ArrayList<>(rodManager.getRegistry().values());
        rods.sort(Comparator.comparingLong(RodDefinition::getPrice));
        int rows = Math.max(1, Math.min(6, (int) Math.ceil(rods.size() / 9.0D)));
        RodShopHolder holder = new RodShopHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, rows * 9, Text.component(messages.raw("shop-title")));
        holder.setInventory(inventory);
        int slot = 0;
        for (RodDefinition rod : rods) {
            inventory.setItem(slot++, createDisplayItem(data, rod));
        }
        player.openInventory(inventory);
    }

    private ItemStack createDisplayItem(PlayerData data, RodDefinition rod) {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.component(rod.getDisplayName()));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(Text.component("&7Price: &6" + Text.number(rod.getPrice())));
        lore.add(Text.component("&7DPS: &f" + Text.number(rod.getDps())));
        lore.add(Text.component("&7Tension Resist: &a" + Text.percent(rod.getTensionResistance() * 100.0D)));
        lore.add(Text.component("&7Range: &f" + Text.number(rod.getCastRange())));
        lore.add(Text.component("&7Bite Speed: &b-" + Text.percent(rod.getBiteSpeed() * 100.0D)));
        lore.add(Text.component("&7Rarity Luck: &d+" + Text.percent(rod.getRarityLuck() * 100.0D)));
        lore.add(Text.component("&7Size Bonus: &6+" + Text.percent(rod.getSizeBonus() * 100.0D)));
        lore.add(Text.component("&7Encounter Rate: &a+" + Text.percent(rod.getEncounterRate() * 100.0D)));
        lore.add(Text.component("&7Zone Efficiency: &3+" + Text.percent(rod.getZoneEfficiency() * 100.0D)));
        lore.add(Text.component("&7Skill: &b" + Text.plainEnum(rod.getSkillId())));
        if (data.getRodId().equalsIgnoreCase(rod.getId())) {
            lore.add(Text.component("&aCurrently equipped"));
            lore.add(Text.component("&7Click to claim another copy"));
        } else if (data.getCoins() >= rod.getPrice()) {
            lore.add(Text.component("&eClick to purchase and equip"));
        } else {
            lore.add(Text.component("&cNot enough coins"));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private void handlePurchase(Player player, RodDefinition rod) {
        PlayerData data = playerDataManager.get(player.getUniqueId());
        if (data == null) {
            messages.send(player, "profile-loading");
            return;
        }
        ItemStack rodItem = rodManager.create(rod.getId());
        if (rodItem == null) {
            messages.sendRaw(player, "&cUnable to create that rod item right now.");
            return;
        }
        if (data.getRodId().equalsIgnoreCase(rod.getId())) {
            player.getInventory().addItem(rodItem);
            messages.sendRaw(player, messages.raw("rod-equipped").replace("{rod}", rod.getDisplayName()));
            return;
        }
        if (!data.removeCoins(rod.getPrice())) {
            messages.sendRaw(player, messages.raw("rod-cannot-afford").replace("{coins}", Text.number(rod.getPrice())));
            return;
        }
        data.setRodId(rod.getId());
        player.getInventory().addItem(rodItem);
        messages.sendRaw(player, messages.raw("rod-bought").replace("{rod}", rod.getDisplayName()).replace("{coins}", Text.number(rod.getPrice())));
        playerDataManager.saveAsync(data);
        openShop(player);
    }

    @EventHandler
    public void onShopClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof RodShopHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!holder.owner().equals(player.getUniqueId())) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        List<RodDefinition> rods = new ArrayList<>(rodManager.getRegistry().values());
        rods.sort(Comparator.comparingLong(RodDefinition::getPrice));
        if (slot >= rods.size()) {
            return;
        }
        handlePurchase(player, rods.get(slot));
    }

    @EventHandler
    public void onShopDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof RodShopHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMerchantInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        if (villager.customName() == null || !PLAIN.serialize(villager.customName()).contains("Rod Merchant")) {
            return;
        }
        event.setCancelled(true);
        openShop(event.getPlayer());
    }

    public static final class RodShopHolder implements InventoryHolder {
        private final UUID owner;
        private Inventory inventory;

        public RodShopHolder(UUID owner) {
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
