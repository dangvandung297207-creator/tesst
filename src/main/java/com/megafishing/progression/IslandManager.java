package com.megafishing.progression;

import com.megafishing.MegaFishingPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class IslandManager {
    private final MegaFishingPlugin plugin;
    private final Map<String, IslandDefinition> islands = new LinkedHashMap<>();

    public IslandManager(MegaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        islands.clear();
        YamlConfiguration yaml = plugin.loadBundledConfiguration("islands.yml");
        ConfigurationSection root = yaml.getConfigurationSection("islands");
        if (root == null) {
            return;
        }
        for (String islandId : root.getKeys(false)) {
            ConfigurationSection islandSection = root.getConfigurationSection(islandId);
            if (islandSection == null) {
                continue;
            }
            islands.put(islandId.toLowerCase(), new IslandDefinition(
                    islandId.toLowerCase(),
                    islandSection.getString("display-name", islandId),
                    Math.max(0L, islandSection.getLong("unlock-cost", 0L)),
                    islandSection.getBoolean("default-unlocked", false),
                    new ArrayList<>()
            ));
        }
    }

    public IslandDefinition get(String id) {
        return id == null ? null : islands.get(id.toLowerCase());
    }

    public Collection<IslandDefinition> islands() {
        return Collections.unmodifiableCollection(islands.values());
    }
}
