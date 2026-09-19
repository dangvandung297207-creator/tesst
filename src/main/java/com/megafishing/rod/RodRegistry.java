package com.megafishing.rod;

import com.megafishing.util.Text;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class RodRegistry {
    private final Map<String, RodDefinition> rods = new LinkedHashMap<>();

    public void load(File file, Logger logger) {
        rods.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("rods");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection rod = section.getConfigurationSection(id);
            if (rod == null) {
                continue;
            }
            try {
                rods.put(id.toLowerCase(), new RodDefinition(
                        id.toLowerCase(),
                        rod.getString("display-name", Text.plainEnum(id)),
                        Math.max(0L, rod.getLong("price", 0L)),
                        Math.max(0.1D, rod.getDouble("dps", 1.0D)),
                        Math.max(0.0D, rod.getDouble("tension-resistance", 0.0D)),
                        Math.max(2.0D, rod.getDouble("cast-range", 10.0D)),
                        rod.getString("skill", "power_pull").toLowerCase(),
                        Math.max(1, rod.getInt("skill-cooldown", 15)),
                        Math.max(0.0D, rod.getDouble("bite-speed", 0.0D)),
                        Math.max(0.0D, rod.getDouble("rarity-luck", 0.0D)),
                        Math.max(0.0D, rod.getDouble("size-bonus", 0.0D)),
                        Math.max(0.0D, rod.getDouble("encounter-rate", 0.0D)),
                        Math.max(0.0D, rod.getDouble("zone-efficiency", 0.0D))
                ));
            } catch (Exception exception) {
                logger.warning("[MEGA-FISHING] Failed to load rod '" + id + "': " + exception.getMessage());
            }
        }
    }

    public RodDefinition get(String id) {
        RodDefinition fallback = rods.get("wooden");
        if (fallback == null && !rods.isEmpty()) {
            fallback = rods.values().iterator().next();
        }
        return id == null ? fallback : rods.getOrDefault(id.toLowerCase(), fallback);
    }

    public RodDefinition getExact(String id) {
        return id == null ? null : rods.get(id.toLowerCase());
    }

    public boolean isAtLeast(String currentId, String requiredId) {
        if (requiredId == null || requiredId.isBlank()) {
            return true;
        }
        int currentRank = rankOf(currentId);
        int requiredRank = rankOf(requiredId);
        return currentRank >= 0 && requiredRank >= 0 && currentRank >= requiredRank;
    }

    public int rankOf(String id) {
        if (id == null) {
            return -1;
        }
        List<String> ids = new ArrayList<>(rods.keySet());
        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i).equalsIgnoreCase(id)) {
                return i;
            }
        }
        return -1;
    }

    public Collection<RodDefinition> values() {
        return Collections.unmodifiableCollection(rods.values());
    }
}
