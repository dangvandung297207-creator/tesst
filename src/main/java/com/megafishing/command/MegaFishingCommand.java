package com.megafishing.command;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.pet.PetDefinition;
import com.megafishing.pet.PetManager;
import com.megafishing.persistence.PlayerData;
import com.megafishing.rod.RodDefinition;
import com.megafishing.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MegaFishingCommand implements CommandExecutor, TabCompleter {
    private final MegaFishingPlugin plugin;

    public MegaFishingCommand(MegaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.messages().rawList("help").forEach(line -> sender.sendMessage(Text.component(plugin.messages().raw("prefix") + line)));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (!sender.hasPermission("megafishing.admin")) {
                    sender.sendMessage(Text.component("&cNo permission."));
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(Text.component("&aMegaFishing reloaded."));
                return true;
            }
            case "give" -> {
                if (!sender.hasPermission("megafishing.admin")) {
                    sender.sendMessage(Text.component("&cNo permission."));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(Text.component("&cUsage: /megafishing give <player> <rod>"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(Text.component("&cPlayer not found."));
                    return true;
                }
                RodDefinition giveDefinition = plugin.rodManager().getRegistry().getExact(args[2]);
                ItemStack rod = giveDefinition == null ? null : plugin.rodManager().create(giveDefinition.getId());
                if (rod == null) {
                    sender.sendMessage(Text.component("&cUnknown rod."));
                    return true;
                }
                target.getInventory().addItem(rod);
                PlayerData data = plugin.playerDataManager().get(target.getUniqueId());
                if (data != null) {
                    data.setRodId(args[2].toLowerCase(Locale.ROOT));
                    plugin.playerDataManager().saveAsync(data);
                }
                sender.sendMessage(Text.component("&aGave &f" + args[2] + " &ato " + target.getName()));
                return true;
            }
            case "stats" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Player only.");
                    return true;
                }
                PlayerData data = plugin.playerDataManager().get(player.getUniqueId());
                if (data == null) {
                    plugin.messages().send(player, "profile-loading");
                    return true;
                }
                player.sendMessage(Text.component("&3&lMega Fishing Stats"));
                player.sendMessage(Text.component("&7Coins: &6" + Text.number(data.getCoins())));
                player.sendMessage(Text.component("&7Rod: &f" + Text.plainEnum(data.getRodId())));
                player.sendMessage(Text.component("&7Bag: &b" + data.getFishBag().size() + "/" + data.getFishBag().getCapacity()));
                player.sendMessage(Text.component("&7Caught: &a" + data.getStatistics().getTotalCatches()));
                player.sendMessage(Text.component("&7Escaped: &c" + data.getStatistics().getFishEscaped()));
                player.sendMessage(Text.component("&7Largest: &f" + data.getStatistics().getLargestFishId() + " &7(" + Text.number(data.getStatistics().getLargestFishWeight()) + "kg)"));
                player.sendMessage(Text.component("&7Unlocked Islands: &b" + data.getUnlockedIslands().size()));
                var currentProfile = plugin.fishingEnvironmentManager().profileFor(player.getWorld());
                var currentZone = plugin.fishingEnvironmentManager().findZone(player.getLocation());
                if (currentProfile != null) {
                    player.sendMessage(Text.component("&7Fishing Profile: &f" + currentProfile.getDisplayName()));
                }
                if (currentZone != null) {
                    player.sendMessage(Text.component("&7Fishing Zone: &b" + currentZone.getDisplayName()));
                }
                player.sendMessage(Text.component("&7Pet Slots: &f" + data.getEquippedPets().size() + "&7/&f" + data.getMaxPetSlots()));
                player.sendMessage(Text.component("&7Pet Slot Upgrade: &f" + plugin.petSlotUpgradeManager().nextUpgradePreview(data)));
                player.sendMessage(Text.component("&7Pet Sell Multiplier: &a" + Text.number(plugin.petManager().getSellMultiplier(data)) + "x"));
                String equippedPets = data.getEquippedPets().isEmpty()
                        ? "None"
                        : data.getEquippedPets().stream()
                        .map(id -> {
                            PetDefinition pet = plugin.petManager().getRegistry().get(id);
                            return pet == null ? Text.plainEnum(id) : pet.getDisplayName();
                        })
                        .collect(Collectors.joining("&7, "));
                player.sendMessage(Text.component("&7Equipped Pets: &f" + equippedPets));
                player.sendMessage(Text.component("&7Bag Upgrade: &f" + plugin.bagUpgradeManager().nextUpgradePreview(data)));
                return true;
            }
            case "bag", "fish" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Player only.");
                    return true;
                }
                PlayerData data = plugin.playerDataManager().get(player.getUniqueId());
                if (data == null) {
                    plugin.messages().send(player, "profile-loading");
                    return true;
                }
                plugin.storageManager().openBag(player, data, id -> plugin.fishManager().getRegistry().get(id));
                return true;
            }
            case "claimrod" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Player only.");
                    return true;
                }
                PlayerData data = plugin.playerDataManager().get(player.getUniqueId());
                if (data == null) {
                    plugin.messages().send(player, "profile-loading");
                    return true;
                }
                ItemStack selectedRod = plugin.rodManager().createSelected(data);
                if (selectedRod == null) {
                    plugin.messages().send(player, "claimrod-failed");
                    return true;
                }
                player.getInventory().addItem(selectedRod);
                RodDefinition currentRod = plugin.rodManager().getRegistry().get(data.getRodId());
                plugin.messages().sendRaw(player, plugin.messages().raw("claimrod-success").replace("{rod}", currentRod == null ? Text.plainEnum(data.getRodId()) : currentRod.getDisplayName()));
                return true;
            }
            case "sell" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Player only.");
                    return true;
                }
                plugin.sellManager().sell(player);
                return true;
            }
            case "shop" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Player only.");
                    return true;
                }
                plugin.rodShopManager().openShop(player);
                return true;
            }
            case "bagupgrade" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Player only.");
                    return true;
                }
                PlayerData data = plugin.playerDataManager().get(player.getUniqueId());
                if (data == null) {
                    plugin.messages().send(player, "profile-loading");
                    return true;
                }
                player.sendMessage(Text.component(plugin.bagUpgradeManager().nextUpgradePreview(data)));
                plugin.bagUpgradeManager().purchaseNext(player);
                return true;
            }
            case "petslots" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Player only.");
                    return true;
                }
                PlayerData data = plugin.playerDataManager().get(player.getUniqueId());
                if (data == null) {
                    plugin.messages().send(player, "profile-loading");
                    return true;
                }
                player.sendMessage(Text.component(plugin.petSlotUpgradeManager().nextUpgradePreview(data)));
                plugin.petSlotUpgradeManager().purchaseNext(player);
                return true;
            }
            case "tutorial" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Player only.");
                    return true;
                }
                PlayerData data = plugin.playerDataManager().get(player.getUniqueId());
                if (data == null) {
                    plugin.messages().send(player, "profile-loading");
                    return true;
                }
                plugin.onboardingManager().replay(player, data);
                return true;
            }
            case "islands" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Player only.");
                    return true;
                }
                if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
                    plugin.islandMenuManager().sendIslandList(player);
                    return true;
                }
                plugin.islandMenuManager().openMenu(player);
                return true;
            }
            case "island" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Player only.");
                    return true;
                }
                PlayerData data = plugin.playerDataManager().get(player.getUniqueId());
                if (data == null) {
                    plugin.messages().send(player, "profile-loading");
                    return true;
                }
                if (args.length >= 3 && args[1].equalsIgnoreCase("unlock")) {
                    var island = plugin.islandManager().get(args[2]);
                    var result = plugin.progressionManager().unlockIsland(data, args[2]);
                    switch (result) {
                        case SUCCESS -> {
                            plugin.playerDataManager().saveAsync(data);
                            plugin.messages().sendRaw(player, plugin.messages().raw("island-unlocked")
                                    .replace("{island}", island == null ? args[2] : island.getDisplayName())
                                    .replace("{coins}", Text.number(island == null ? 0L : island.getUnlockCost())));
                        }
                        case ALREADY_UNLOCKED -> plugin.messages().sendRaw(player, plugin.messages().raw("island-already-unlocked")
                                .replace("{island}", island == null ? args[2] : island.getDisplayName()));
                        case NOT_ENOUGH_COINS -> plugin.messages().sendRaw(player, plugin.messages().raw("island-cannot-afford")
                                .replace("{island}", island == null ? args[2] : island.getDisplayName())
                                .replace("{coins}", Text.number(island == null ? 0L : island.getUnlockCost())));
                        case UNKNOWN_ISLAND -> plugin.messages().send(player, "island-unknown");
                    }
                    return true;
                }
                sender.sendMessage(Text.component("&cUsage: /megafishing island unlock <id>"));
                return true;
            }
            case "buyrod" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Player only.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Text.component("&cUsage: /megafishing buyrod <rod>"));
                    return true;
                }
                PlayerData data = plugin.playerDataManager().get(player.getUniqueId());
                if (data == null) {
                    plugin.messages().send(player, "profile-loading");
                    return true;
                }
                RodDefinition rod = plugin.rodManager().getRegistry().getExact(args[1]);
                if (rod == null) {
                    player.sendMessage(Text.component("&cUnknown rod."));
                    return true;
                }
                if (data.getRodId().equalsIgnoreCase(rod.getId())) {
                    plugin.messages().send(player, "rod-owned");
                    return true;
                }
                if (!data.removeCoins(rod.getPrice())) {
                    plugin.messages().sendRaw(player, plugin.messages().raw("rod-cannot-afford").replace("{coins}", Text.number(rod.getPrice())));
                    return true;
                }
                ItemStack purchasedRod = plugin.rodManager().create(rod.getId());
                if (purchasedRod == null) {
                    player.sendMessage(Text.component("&cUnable to create that rod item right now."));
                    data.addCoins(rod.getPrice());
                    return true;
                }
                data.setRodId(rod.getId());
                player.getInventory().addItem(purchasedRod);
                plugin.playerDataManager().saveAsync(data);
                plugin.messages().sendRaw(player, plugin.messages().raw("rod-bought").replace("{rod}", rod.getDisplayName()).replace("{coins}", Text.number(rod.getPrice())));
                return true;
            }
            case "pet" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("give") && sender.hasPermission("megafishing.admin")) {
                    if (args.length < 4) {
                        sender.sendMessage(Text.component("&cUsage: /megafishing pet give <player> <pet>"));
                        return true;
                    }
                    Player target = Bukkit.getPlayerExact(args[2]);
                    if (target == null) {
                        sender.sendMessage(Text.component("&cPlayer not found."));
                        return true;
                    }
                    PlayerData data = plugin.playerDataManager().get(target.getUniqueId());
                    PetDefinition pet = plugin.petManager().getRegistry().get(args[3]);
                    if (data == null || pet == null) {
                        sender.sendMessage(Text.component("&cUnknown pet."));
                        return true;
                    }
                    if (!plugin.petManager().grantPet(data, args[3])) {
                        sender.sendMessage(Text.component("&eThat player already owns that pet."));
                        return true;
                    }
                    plugin.messages().sendRaw(target, plugin.messages().raw("pet-granted").replace("{pet}", pet.getDisplayName()).replace("{player}", target.getName()));
                    plugin.playerDataManager().saveAsync(data);
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Player only.");
                    return true;
                }
                if (args.length == 1) {
                    plugin.petMenuManager().openMenu(player);
                    return true;
                }
                if (args[1].equalsIgnoreCase("list")) {
                    plugin.petMenuManager().sendPetList(player);
                    return true;
                }
                if (args[1].equalsIgnoreCase("equip")) {
                    if (args.length < 3) {
                        player.sendMessage(Text.component("&cUsage: /megafishing pet equip <pet>"));
                        return true;
                    }
                    PlayerData data = plugin.playerDataManager().get(player.getUniqueId());
                    if (data == null) {
                        plugin.messages().send(player, "profile-loading");
                        return true;
                    }
                    PetDefinition pet = plugin.petManager().getRegistry().get(args[2]);
                    PetManager.EquipResult result = plugin.petManager().equip(data, args[2]);
                    switch (result) {
                        case SUCCESS, ALREADY_EQUIPPED -> {
                            plugin.messages().sendRaw(player, plugin.messages().raw("pet-equipped").replace("{pet}", pet == null ? args[2] : pet.getDisplayName()));
                            plugin.playerDataManager().saveAsync(data);
                        }
                        case MAX_SLOTS -> plugin.messages().sendRaw(player, plugin.messages().raw("pet-max-slots").replace("{slots}", String.valueOf(data.getMaxPetSlots())));
                        case NOT_OWNED -> plugin.messages().sendRaw(player, plugin.messages().raw("pet-not-owned").replace("{pet}", pet == null ? args[2] : pet.getDisplayName()));
                        case UNKNOWN_PET -> plugin.messages().send(player, "pet-unknown");
                    }
                    return true;
                }
                if (args[1].equalsIgnoreCase("unequip")) {
                    if (args.length < 3) {
                        player.sendMessage(Text.component("&cUsage: /megafishing pet unequip <pet>"));
                        return true;
                    }
                    PlayerData data = plugin.playerDataManager().get(player.getUniqueId());
                    if (data == null) {
                        plugin.messages().send(player, "profile-loading");
                        return true;
                    }
                    PetDefinition pet = plugin.petManager().getRegistry().get(args[2]);
                    if (!plugin.petManager().unequip(data, args[2])) {
                        plugin.messages().sendRaw(player, plugin.messages().raw("pet-not-equipped").replace("{pet}", pet == null ? args[2] : pet.getDisplayName()));
                        return true;
                    }
                    plugin.messages().sendRaw(player, plugin.messages().raw("pet-unequipped").replace("{pet}", pet == null ? args[2] : pet.getDisplayName()));
                    plugin.playerDataManager().saveAsync(data);
                    return true;
                }
                if (args[1].equalsIgnoreCase("buy")) {
                    if (args.length < 3) {
                        player.sendMessage(Text.component("&cUsage: /megafishing pet buy <pet>"));
                        return true;
                    }
                    PlayerData data = plugin.playerDataManager().get(player.getUniqueId());
                    if (data == null) {
                        plugin.messages().send(player, "profile-loading");
                        return true;
                    }
                    PetDefinition pet = plugin.petManager().getRegistry().get(args[2]);
                    PetManager.PurchaseResult result = plugin.petManager().purchase(data, args[2]);
                    switch (result) {
                        case SUCCESS -> {
                            plugin.messages().sendRaw(player, plugin.messages().raw("pet-purchased")
                                    .replace("{pet}", pet == null ? args[2] : pet.getDisplayName())
                                    .replace("{coins}", Text.number(pet == null ? 0L : pet.getPrice())));
                            plugin.playerDataManager().saveAsync(data);
                        }
                        case ALREADY_OWNED -> plugin.messages().sendRaw(player, plugin.messages().raw("pet-already-owned").replace("{pet}", pet == null ? args[2] : pet.getDisplayName()));
                        case NOT_ENOUGH_COINS -> plugin.messages().sendRaw(player, plugin.messages().raw("pet-cannot-afford").replace("{coins}", Text.number(pet == null ? 0L : pet.getPrice())));
                        case UNKNOWN_PET -> plugin.messages().send(player, "pet-unknown");
                    }
                    return true;
                }
                sender.sendMessage(Text.component("&cUsage: /megafishing pet [list|buy|equip|unequip] <pet>"));
                return true;
            }
            case "debug" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Player only.");
                    return true;
                }
                player.sendMessage(Text.component("&3&lMega Fishing Debug"));
                player.sendMessage(Text.component("&7Active Sessions: &f" + plugin.fishingManager().getActiveSessionCount()));
                player.sendMessage(Text.component("&7Active Fish: &f" + plugin.fishingManager().getActiveFishCount()));
                player.sendMessage(Text.component("&7Visual Entities: &f" + plugin.fishingManager().getVisualEntityCount()));
                player.sendMessage(Text.component("&7Loaded Profiles: &f" + plugin.playerDataManager().loadedCount()));
                player.sendMessage(Text.component("&7Database: &f" + plugin.databaseManager().status()));
                player.sendMessage(Text.component("&7Tick: &f" + plugin.fishingManager().getCurrentTick()));
                player.sendMessage(Text.component("&7Debug Mode: &f" + plugin.getConfig().getBoolean("debug", false)));
                var session = plugin.fishingManager().getSession(player.getUniqueId());
                if (session != null) {
                    player.sendMessage(Text.component("&7Your Session: &f" + session.getState()));
                    player.sendMessage(Text.component("&7Your Tension: &f" + Text.number(session.getTension())));
                    if (session.getFish() != null) {
                        player.sendMessage(Text.component("&7Your Fish: &f" + session.getFish().getDefinition().getId() + " &7HP: &f" + Text.number(session.getFish().getHealth())));
                    }
                } else {
                    player.sendMessage(Text.component("&7Your Session: &fNone"));
                }
                return true;
            }
            default -> {
                plugin.messages().rawList("help").forEach(line -> sender.sendMessage(Text.component(plugin.messages().raw("prefix") + line)));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("reload", "give", "stats", "bag", "fish", "claimrod", "sell", "shop", "bagupgrade", "petslots", "tutorial", "islands", "island", "buyrod", "pet", "debug"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filter(plugin.rodManager().getRegistry().values().stream().map(RodDefinition::getId).collect(Collectors.toList()), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("buyrod")) {
            return filter(plugin.rodManager().getRegistry().values().stream().map(RodDefinition::getId).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("islands")) {
            return filter(List.of("list"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("island")) {
            return filter(List.of("unlock"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("island") && args[1].equalsIgnoreCase("unlock")) {
            return filter(plugin.islandManager().islands().stream().map(island -> island.getId()).collect(Collectors.toList()), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("pet")) {
            return filter(List.of("list", "buy", "equip", "unequip", "give"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("pet") && (args[1].equalsIgnoreCase("equip") || args[1].equalsIgnoreCase("buy") || args[1].equalsIgnoreCase("unequip"))) {
            return filter(plugin.petManager().getRegistry().values().stream().map(PetDefinition::getId).collect(Collectors.toList()), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("pet") && args[1].equalsIgnoreCase("give")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("pet") && args[1].equalsIgnoreCase("give")) {
            return filter(plugin.petManager().getRegistry().values().stream().map(PetDefinition::getId).collect(Collectors.toList()), args[3]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        List<String> filtered = new ArrayList<>();
        String lowered = input.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                filtered.add(value);
            }
        }
        return filtered;
    }
}
