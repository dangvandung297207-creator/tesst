package com.megafishing.pet;

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

public class PetMenuManager implements Listener {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final MegaFishingPlugin plugin;
    private final PetManager petManager;
    private final PlayerDataManager playerDataManager;
    private final MessageManager messages;

    public PetMenuManager(MegaFishingPlugin plugin, PetManager petManager, PlayerDataManager playerDataManager, MessageManager messages) {
        this.plugin = plugin;
        this.petManager = petManager;
        this.playerDataManager = playerDataManager;
        this.messages = messages;
    }

    public void openMenu(Player player) {
        PlayerData data = playerDataManager.get(player.getUniqueId());
        if (data == null) {
            messages.send(player, "profile-loading");
            return;
        }
        List<PetDefinition> pets = new ArrayList<>(petManager.getRegistry().values());
        int rows = Math.max(1, Math.min(6, (int) Math.ceil(pets.size() / 9.0D)));
        PetMenuHolder holder = new PetMenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, rows * 9, Text.component(messages.raw("pet-menu-title")));
        holder.setInventory(inventory);
        for (int i = 0; i < pets.size(); i++) {
            inventory.setItem(i, createItem(data, pets.get(i)));
        }
        player.openInventory(inventory);
    }

    public void sendPetList(Player player) {
        PlayerData data = playerDataManager.get(player.getUniqueId());
        if (data == null) {
            messages.send(player, "profile-loading");
            return;
        }
        player.sendMessage(Text.component(messages.raw("prefix") + messages.raw("pet-list-header")));
        player.sendMessage(Text.component("&7Slots: &f" + data.getEquippedPets().size() + "&7/&f" + data.getMaxPetSlots()));
        player.sendMessage(Text.component("&7Next Slot Upgrade: &f" + plugin.petSlotUpgradeManager().nextUpgradePreview(data)));
        player.sendMessage(Text.component("&7Sell Multiplier: &a" + Text.number(petManager.getSellMultiplier(data)) + "x"));
        for (PetDefinition pet : petManager.getRegistry().values()) {
            boolean owned = data.getOwnedPets().contains(pet.getId());
            boolean equipped = data.getEquippedPets().contains(pet.getId());
            String status = equipped ? "&aEQUIPPED" : owned ? "&eOWNED" : "&cLOCKED";
            player.sendMessage(Text.component("&8- " + pet.getDisplayName() + " &8| &7Sell: &a" + Text.number(pet.getSellMultiplier()) + "x &8| &7Price: &6"
                    + Text.number(pet.getPrice()) + " &8| " + status));
        }
    }

    private ItemStack createItem(PlayerData data, PetDefinition pet) {
        boolean owned = data.getOwnedPets().contains(pet.getId());
        boolean equipped = data.getEquippedPets().contains(pet.getId());
        Material material = equipped ? Material.TROPICAL_FISH_BUCKET : owned ? Material.COD_BUCKET : Material.NAUTILUS_SHELL;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.component(pet.getDisplayName() + (equipped ? " &a(EQUIPPED)" : owned ? " &e(OWNED)" : " &c(LOCKED)")));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(Text.component("&7Sell Multiplier: &a" + Text.number(pet.getSellMultiplier()) + "x"));
        lore.add(Text.component("&7Price: &6" + Text.number(pet.getPrice())));
        lore.add(Text.component("&7Slots Used: &f" + data.getEquippedPets().size() + "&7/&f" + data.getMaxPetSlots()));
        if (equipped) {
            lore.add(Text.component("&eClick to unequip"));
        } else if (owned) {
            if (data.getEquippedPets().size() >= data.getMaxPetSlots()) {
                lore.add(Text.component("&cNo free pet slots"));
            } else {
                lore.add(Text.component("&eClick to equip"));
            }
        } else if (data.getCoins() >= pet.getPrice()) {
            lore.add(Text.component("&eClick to purchase"));
        } else {
            lore.add(Text.component("&cNot enough coins"));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private void handleClick(Player player, PetDefinition pet) {
        PlayerData data = playerDataManager.get(player.getUniqueId());
        if (data == null) {
            messages.send(player, "profile-loading");
            return;
        }
        boolean owned = data.getOwnedPets().contains(pet.getId());
        if (!owned) {
            PetManager.PurchaseResult result = petManager.purchase(data, pet.getId());
            switch (result) {
                case SUCCESS -> {
                    messages.sendRaw(player, messages.raw("pet-purchased")
                            .replace("{pet}", pet.getDisplayName())
                            .replace("{coins}", Text.number(pet.getPrice())));
                    playerDataManager.saveAsync(data);
                    openMenu(player);
                }
                case ALREADY_OWNED -> messages.sendRaw(player, messages.raw("pet-already-owned").replace("{pet}", pet.getDisplayName()));
                case NOT_ENOUGH_COINS -> messages.sendRaw(player, messages.raw("pet-cannot-afford").replace("{coins}", Text.number(pet.getPrice())));
                case UNKNOWN_PET -> messages.send(player, "pet-unknown");
            }
            return;
        }
        if (petManager.isEquipped(data, pet.getId())) {
            if (petManager.unequip(data, pet.getId())) {
                messages.sendRaw(player, messages.raw("pet-unequipped").replace("{pet}", pet.getDisplayName()));
                playerDataManager.saveAsync(data);
                openMenu(player);
            }
            return;
        }
        PetManager.EquipResult result = petManager.equip(data, pet.getId());
        switch (result) {
            case SUCCESS, ALREADY_EQUIPPED -> {
                messages.sendRaw(player, messages.raw("pet-equipped").replace("{pet}", pet.getDisplayName()));
                playerDataManager.saveAsync(data);
                openMenu(player);
            }
            case MAX_SLOTS -> messages.sendRaw(player, messages.raw("pet-max-slots").replace("{slots}", String.valueOf(data.getMaxPetSlots())));
            case NOT_OWNED -> messages.sendRaw(player, messages.raw("pet-not-owned").replace("{pet}", pet.getDisplayName()));
            case UNKNOWN_PET -> messages.send(player, "pet-unknown");
        }
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof PetMenuHolder holder)) {
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
        List<PetDefinition> pets = new ArrayList<>(petManager.getRegistry().values());
        if (slot >= pets.size()) {
            return;
        }
        handleClick(player, pets.get(slot));
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PetMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onKeeperInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        if (villager.customName() == null || !PLAIN.serialize(villager.customName()).contains("Pet Keeper")) {
            return;
        }
        event.setCancelled(true);
        openMenu(event.getPlayer());
    }

    public static final class PetMenuHolder implements InventoryHolder {
        private final UUID owner;
        private Inventory inventory;

        public PetMenuHolder(UUID owner) {
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
