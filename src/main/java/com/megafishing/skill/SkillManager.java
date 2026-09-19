package com.megafishing.skill;

import com.megafishing.util.Text;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class SkillManager {
    private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();

    public void load(YamlConfiguration yaml, Logger logger) {
        skills.clear();
        ConfigurationSection section = yaml.getConfigurationSection("skills");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection skill = section.getConfigurationSection(id);
            if (skill == null) {
                continue;
            }
            try {
                skills.put(id.toLowerCase(), new SkillDefinition(
                        id.toLowerCase(),
                        skill.getString("display-name", Text.plainEnum(id)),
                        Math.max(1, skill.getInt("cooldown", 15)),
                        Math.max(0.1D, skill.getDouble("damage-multiplier", 5.0D)),
                        Math.max(0.0D, skill.getDouble("tension-reduction", 15.0D))
                ));
            } catch (Exception exception) {
                logger.warning("[MEGA-FISHING] Failed to load skill '" + id + "': " + exception.getMessage());
            }
        }
    }

    public SkillDefinition get(String id) {
        return id == null ? null : skills.get(id.toLowerCase());
    }

    public Collection<SkillDefinition> values() {
        return Collections.unmodifiableCollection(skills.values());
    }
}
