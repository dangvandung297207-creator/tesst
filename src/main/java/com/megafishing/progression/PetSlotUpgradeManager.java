package com.megafishing.progression;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.persistence.PlayerData;
import com.megafishing.persistence.PlayerDataManager;
import com.megafishing.ui.MessageManager;
import com.megafishing.util.Text;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class PetSlotUpgradeManager implements Listener {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final MegaFishingPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final MessageManager messages;

    public PetSlotUpgradeManager(MegaFishingPlugin plugin, PlayerDataManager playerDataManager, MessageManager messages) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.messages = messages;
    }

    public Integer getNextSlots(PlayerData data) {
        TreeMap<Integer, Long> upgrades = getUpgradeMap();
        return upgrades.keySet().stream().filter(size -> size > data.getMaxPetSlots()).min(Comparator.naturalOrder()).orElse(null);
    }

    public Long getNextCost(PlayerData data) {
        Integer next = getNextSlots(data);
        return next == null ? null : getUpgradeMap().get(next);
    }

    public boolean purchaseNext(Player player) {
        PlayerData data = playerDataManager.get(player.getUniqueId());
        if (data == null) {
            messages.send(player, "profile-loading");
            return false;
        }
        Integer nextSlots = getNextSlots(data);
        if (nextSlots == null) {
            messages.send(player, "pet-slot-upgrade-maxed");
            return false;
        }
        long cost = getUpgradeMap().getOrDefault(nextSlots, Long.MAX_VALUE);
        if (!data.removeCoins(cost)) {
            messages.sendRaw(player, messages.raw("pet-slot-upgrade-cannot-afford")
                    .replace("{coins}", Text.number(cost))
                    .replace("{slots}", String.valueOf(nextSlots)));
            return false;
        }
        int oldSlots = data.getMaxPetSlots();
        data.setMaxPetSlots(nextSlots);
        playerDataManager.saveAsync(data);
        messages.sendRaw(player, messages.raw("pet-slot-upgrade-success")
                .replace("{old}", String.valueOf(oldSlots))
                .replace("{slots}", String.valueOf(nextSlots))
                .replace("{coins}", Text.number(cost)));
        return true;
    }

    public String nextUpgradePreview(PlayerData data) {
        Integer next = getNextSlots(data);
        if (next == null) {
            return Text.color(messages.raw("pet-slot-upgrade-preview-maxed"));
        }
        long cost = getUpgradeMap().getOrDefault(next, 0L);
        return Text.color(messages.raw("pet-slot-upgrade-preview")
                .replace("{slots}", String.valueOf(next))
                .replace("{coins}", Text.number(cost)));
    }

    @EventHandler
    public void onMerchantInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        if (villager.customName() == null || !PLAIN.serialize(villager.customName()).contains("Pet Trainer")) {
            return;
        }
        event.setCancelled(true);
        PlayerData data = playerDataManager.get(event.getPlayer().getUniqueId());
        if (data == null) {
            messages.send(event.getPlayer(), "profile-loading");
            return;
        }
        messages.sendRaw(event.getPlayer(), nextUpgradePreview(data));
        purchaseNext(event.getPlayer());
    }

    private TreeMap<Integer, Long> getUpgradeMap() {
        TreeMap<Integer, Long> values = new TreeMap<>();
        var section = plugin.getConfig().getConfigurationSection("pets.slot-upgrade-costs");
        if (section == null) {
            return values;
        }
        for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
            try {
                values.put(Integer.parseInt(entry.getKey()), Long.parseLong(String.valueOf(entry.getValue())));
            } catch (Exception ignored) {
            }
        }
        return values;
    }
}
