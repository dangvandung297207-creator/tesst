package com.megafishing.economy;

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

public class SellManager implements Listener {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private final MegaFishingPlugin plugin;
    private final EconomyManager economyManager;
    private final PlayerDataManager playerDataManager;
    private final MessageManager messages;

    public SellManager(MegaFishingPlugin plugin, EconomyManager economyManager, PlayerDataManager playerDataManager, MessageManager messages) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.playerDataManager = playerDataManager;
        this.messages = messages;
    }

    public void sell(Player player) {
        PlayerData data = playerDataManager.get(player.getUniqueId());
        if (data == null) {
            messages.send(player, "profile-loading");
            return;
        }
        EconomyManager.SellResult result = economyManager.sellAll(data);
        if (result.soldCount() <= 0) {
            messages.send(player, "merchant-empty");
            return;
        }
        messages.sendRaw(player, messages.raw("merchant-sold")
                .replace("{count}", String.valueOf(result.soldCount()))
                .replace("{coins}", Text.number(result.totalCoins())));
        playerDataManager.saveAsync(data);
    }

    @EventHandler
    public void onMerchantClick(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        if (villager.customName() == null || !PLAIN.serialize(villager.customName()).contains("Fish Merchant")) {
            return;
        }
        event.setCancelled(true);
        sell(event.getPlayer());
    }
}
