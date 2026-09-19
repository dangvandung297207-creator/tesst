package com.megafishing.fish;

import com.megafishing.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class FishRegistry {
    private final Map<String, FishDefinition> fish = new LinkedHashMap<>();
    private final Map<FishRarity, Double> rarityDefaults = new LinkedHashMap<>();

    public void load(YamlConfiguration yaml, Logger logger) {
        fish.clear();
        rarityDefaults.clear();
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
                ConfigurationSection modelSection = section.getConfigurationSection("model");
                Map<String, String> animations = new LinkedHashMap<>();
                if (modelSection != null) {
                    ConfigurationSection animationSection = modelSection.getConfigurationSection("animations");
                    if (animationSection != null) {
                        for (String animationKey : animationSection.getKeys(false)) {
                            animations.put(animationKey.toLowerCase(), animationSection.getString(animationKey, animationKey));
                        }
                    }
                }
                ConfigurationSection availabilitySection = section.getConfigurationSection("availability");
                FishAvailability availability = new FishAvailability(
                        availabilitySection == null ? Set.of() : lowered(availabilitySection.getStringList("seasons")),
                        availabilitySection == null ? Set.of() : lowered(availabilitySection.getStringList("events")),
                        availabilitySection != null && availabilitySection.getBoolean("require-active-event", false)
                );
                FishModelDefinition model = new FishModelDefinition(
                        modelSection == null ? id.toLowerCase() : modelSection.getString("id", id.toLowerCase()),
                        modelSection == null ? 1.0D : Math.max(0.10D, modelSection.getDouble("scale-base", 1.0D)),
                        animations
                );
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
                        section.getBoolean("boss", false),
                        model,
                        availability
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

    private Set<String> lowered(Collection<String> values) {
        Set<String> lowered = new LinkedHashSet<>();
        if (values == null) {
            return lowered;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                lowered.add(value.toLowerCase());
            }
        }
        return lowered;
    }
}
