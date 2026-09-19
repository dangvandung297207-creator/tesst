package com.megafishing.pet;

import com.megafishing.util.Text;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class PetRegistry {
    private final Map<String, PetDefinition> pets = new LinkedHashMap<>();

    public void load(File file, Logger logger) {
        pets.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("pets");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection pet = section.getConfigurationSection(id);
            if (pet == null) {
                continue;
            }
            try {
                pets.put(id.toLowerCase(), new PetDefinition(
                        id.toLowerCase(),
                        pet.getString("display-name", Text.plainEnum(id)),
                        Math.max(1.0D, pet.getDouble("sell-multiplier", 1.0D)),
                        Math.max(0L, pet.getLong("price", 0L)),
                        Math.max(0.0D, pet.getDouble("encounter-rate", 0.0D)),
                        Math.max(0.0D, pet.getDouble("bite-speed", 0.0D)),
                        Math.max(0.0D, pet.getDouble("rarity-luck", 0.0D)),
                        Math.max(0.0D, pet.getDouble("large-fish-chance", 0.0D))
                ));
            } catch (Exception exception) {
                logger.warning("[MEGA-FISHING] Failed to load pet '" + id + "': " + exception.getMessage());
            }
        }
    }

    public PetDefinition get(String id) {
        return id == null ? null : pets.get(id.toLowerCase());
    }

    public Collection<PetDefinition> values() {
        return Collections.unmodifiableCollection(pets.values());
    }
}
