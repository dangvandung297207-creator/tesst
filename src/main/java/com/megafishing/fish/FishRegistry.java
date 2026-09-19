package com.megafishing.fish;

import com.megafishing.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class FishRegistry {
    private final Map<String, FishDefinition> fish = new LinkedHashMap<>();
    private final Map<FishRarity, Double> rarityDefaults = new LinkedHashMap<>();

    public void load(File file, Logger logger) {
        fish.clear();
        rarityDefaults.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection defaults = yaml.getConfigurationSection("rarity-defaults");
        if (defaults != null) {
            for (String key : defaults.getKeys(false)) {
                try {
                    rarityDefaults.put(FishRarity.valueOf(key.toUpperCase()), defaults.getDouble(key));
                } catch (IllegalArgumentException exception) {
                    logger.warning("[MEGA-FISHING] Invalid fish rarity default: " + key);
                }
            }
        }
        ConfigurationSection root = yaml.getConfigurationSection("fish");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            try {
                FishRarity rarity = FishRarity.valueOf(section.getString("rarity", "COMMON").toUpperCase());
                Material material = Material.matchMaterial(section.getString("display-material", "COD"));
                if (material == null) {
                    material = Material.COD;
                }
                FishDefinition definition = new FishDefinition(
                        id.toLowerCase(),
                        section.getString("display-name", Text.plainEnum(id)),
                        rarity,
                        material,
                        Math.max(0.01D, section.getDouble("min-weight", 1.0D)),
                        Math.max(section.getDouble("min-weight", 1.0D), section.getDouble("max-weight", 1.0D)),
                        Math.max(1.0D, section.getDouble("health", 1.0D)),
                        Math.max(0.05D, section.getDouble("pull-power", 1.0D)),
                        Math.max(0.01D, section.getDouble("swim-speed", 0.1D)),
                        Math.max(0.01D, section.getDouble("sell-multiplier", 1.0D)),
                        Math.max(0.01D, section.getDouble("base-value", 1.0D)),
                        Math.max(0.2D, section.getDouble("direction-change-min", 1.0D)),
                        Math.max(section.getDouble("direction-change-min", 1.0D), section.getDouble("direction-change-max", 2.0D)),
                        section.getBoolean("boss", false)
                );
                fish.put(definition.getId(), definition);
            } catch (Exception exception) {
                logger.warning("[MEGA-FISHING] Failed to load fish '" + id + "': " + exception.getMessage());
            }
        }
    }

    public FishDefinition get(String id) {
        return id == null ? null : fish.get(id.toLowerCase());
    }

    public Collection<FishDefinition> values() {
        return Collections.unmodifiableCollection(fish.values());
    }

    public Map<FishRarity, Double> getRarityDefaults() {
        return Collections.unmodifiableMap(rarityDefaults);
    }
}
