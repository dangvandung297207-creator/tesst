package com.megafishing.ui;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.persistence.PlayerData;
import com.megafishing.util.Text;
import org.bukkit.entity.Player;

import java.util.List;

public class OnboardingManager {
    private final MegaFishingPlugin plugin;

    public OnboardingManager(MegaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleProfileLoaded(Player player, PlayerData data) {
        if (!plugin.getConfig().getBoolean("tutorial.enabled", true) || data.isTutorialCompleted()) {
            return;
        }
        sendTutorial(player, data, true);
        data.setTutorialCompleted(true);
        plugin.playerDataManager().saveAsync(data);
    }

    public void replay(Player player, PlayerData data) {
        if (!plugin.getConfig().getBoolean("tutorial.enabled", true)) {
            plugin.messages().send(player, "tutorial-disabled");
            return;
        }
        sendTutorial(player, data, false);
        plugin.messages().send(player, "tutorial-replayed");
    }

    private void sendTutorial(Player player, PlayerData data, boolean firstTime) {
        String islandName = plugin.progressionManager().activeIslandFor(data) == null
                ? "Starter Island"
                : plugin.progressionManager().activeIslandFor(data).getDisplayName();
        plugin.messages().title(player, plugin.messages().raw("tutorial-title"), plugin.messages().raw("tutorial-subtitle"));
        List<String> lines = plugin.messages().rawList(firstTime ? "tutorial-first-time" : "tutorial-lines");
        if (lines.isEmpty()) {
            lines = plugin.messages().rawList("tutorial-lines");
        }
        for (String line : lines) {
            plugin.messages().sendRaw(player, line
                    .replace("{rod}", Text.plainEnum(data.getRodId()))
                    .replace("{bag}", String.valueOf(data.getBagCapacity()))
                    .replace("{petslots}", String.valueOf(data.getMaxPetSlots()))
                    .replace("{island}", islandName));
        }
    }
}
