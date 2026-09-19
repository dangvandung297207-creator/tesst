package com.megafishing.ui;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.util.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MessageManager {
    private final MegaFishingPlugin plugin;
    private YamlConfiguration messages;

    public MessageManager(MegaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        this.messages = plugin.loadBundledConfiguration("messages.yml");
    }

    public String raw(String path) {
        return messages.getString(path, path);
    }

    public List<String> rawList(String path) {
        return new ArrayList<>(messages.getStringList(path));
    }

    public void send(Player player, String path) {
        sendRaw(player, raw(path));
    }

    public void sendRaw(Player player, String line) {
        player.sendMessage(Text.component(raw("prefix") + line));
    }

    public void title(Player player, String title, String subtitle) {
        player.showTitle(Title.title(Text.component(title), Text.component(subtitle), Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(2), Duration.ofMillis(400))));
    }
}
