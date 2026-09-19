package com.megafishing.validation;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.fish.FishDefinition;
import com.megafishing.fishing.FishingZone;
import com.megafishing.progression.IslandDefinition;
import com.megafishing.rod.RodDefinition;
import com.megafishing.skill.SkillDefinition;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class ConfigValidator {
    private final MegaFishingPlugin plugin;

    public ConfigValidator(MegaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public void validate() {
        Logger logger = plugin.getLogger();
        validateGlobalSettings(logger);
        validateFish(logger);
        validateSkillsAndRods(logger);
        validatePets(logger);
        validatePetProgression(logger);
        validatePhaseProfiles(logger);
        validateCombatProfiles(logger);
        validateBagUpgrades(logger);
        validateIslands(logger);
    }

    private void validateGlobalSettings(Logger logger) {
        if (plugin.getConfig().getDouble("fishing.bite.min-delay", 2.0D) <= 0.0D) {
            logger.warning("[MEGA-FISHING] fishing.bite.min-delay should be > 0.");
        }
        if (plugin.getConfig().getDouble("fishing.bite.max-delay", 8.0D) < plugin.getConfig().getDouble("fishing.bite.min-delay", 2.0D)) {
            logger.warning("[MEGA-FISHING] fishing.bite.max-delay is lower than min-delay.");
        }
        if (plugin.getConfig().getDouble("tension.break-point", 100.0D) <= 0.0D) {
            logger.warning("[MEGA-FISHING] tension.break-point should be > 0.");
        }
        String method = plugin.getConfig().getString("fishing.active-skill.method", "SWAP_HANDS");
        if (!"SWAP_HANDS".equalsIgnoreCase(method) && !"DROP".equalsIgnoreCase(method)) {
            logger.warning("[MEGA-FISHING] Unsupported fishing.active-skill.method '" + method + "'. Supported: SWAP_HANDS, DROP.");
        }
    }

    private void validateFish(Logger logger) {
        Set<String> ids = new HashSet<>();
        for (FishDefinition fish : plugin.fishManager().getRegistry().values()) {
            if (!ids.add(fish.getId())) {
                logger.warning("[MEGA-FISHING] Duplicate fish id detected: " + fish.getId());
            }
            if (fish.getMaxHealth() <= 0.0D) {
                logger.warning("[MEGA-FISHING] Fish '" + fish.getId() + "' has non-positive health.");
            }
            if (fish.getMaxWeight() < fish.getMinWeight()) {
                logger.warning("[MEGA-FISHING] Fish '" + fish.getId() + "' max-weight is below min-weight.");
            }
            if (fish.getPullPower() < 0.0D || fish.getSwimSpeed() < 0.0D) {
                logger.warning("[MEGA-FISHING] Fish '" + fish.getId() + "' has negative movement stats.");
            }
        }
    }

    private void validateSkillsAndRods(Logger logger) {
        Set<String> skillIds = new HashSet<>();
        for (SkillDefinition skill : plugin.skillManager().values()) {
            if (!skillIds.add(skill.getId())) {
                logger.warning("[MEGA-FISHING] Duplicate skill id detected: " + skill.getId());
            }
            if (skill.getCooldownSeconds() <= 0) {
                logger.warning("[MEGA-FISHING] Skill '" + skill.getId() + "' has invalid cooldown.");
            }
        }
        Set<String> rodIds = new HashSet<>();
        for (RodDefinition rod : plugin.rodManager().getRegistry().values()) {
            if (!rodIds.add(rod.getId())) {
                logger.warning("[MEGA-FISHING] Duplicate rod id detected: " + rod.getId());
            }
            if (rod.getPrice() < 0 || rod.getDps() <= 0.0D || rod.getCastRange() <= 0.0D) {
                logger.warning("[MEGA-FISHING] Rod '" + rod.getId() + "' has invalid balance values.");
            }
            if (plugin.skillManager().get(rod.getSkillId()) == null) {
                logger.warning("[MEGA-FISHING] Rod '" + rod.getId() + "' references missing skill '" + rod.getSkillId() + "'.");
            }
        }
    }

    private void validatePets(Logger logger) {
        Set<String> petIds = new HashSet<>();
        plugin.petManager().getRegistry().values().forEach(pet -> {
            if (!petIds.add(pet.getId())) {
                logger.warning("[MEGA-FISHING] Duplicate pet id detected: " + pet.getId());
            }
            if (pet.getSellMultiplier() < 1.0D) {
                logger.warning("[MEGA-FISHING] Pet '" + pet.getId() + "' has sell-multiplier below 1.0.");
            }
            if (pet.getPrice() < 0L) {
                logger.warning("[MEGA-FISHING] Pet '" + pet.getId() + "' has negative price.");
            }
        });
    }

    private void validatePetProgression(Logger logger) {
        int defaultSlots = plugin.getConfig().getInt("pets.default-slots", 2);
        if (defaultSlots <= 0) {
            logger.warning("[MEGA-FISHING] pets.default-slots should be greater than 0.");
        }
        var section = plugin.getConfig().getConfigurationSection("pets.slot-upgrade-costs");
        if (section == null) {
            logger.warning("[MEGA-FISHING] pets.slot-upgrade-costs section is missing.");
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                int size = Integer.parseInt(key);
                long cost = section.getLong(key);
                if (size <= defaultSlots || cost < 0L) {
                    logger.warning("[MEGA-FISHING] Invalid pet slot upgrade entry: slots=" + key + ", cost=" + cost);
                }
            } catch (NumberFormatException exception) {
                logger.warning("[MEGA-FISHING] Invalid pet slot upgrade key: " + key);
            }
        }
    }

    private void validatePhaseProfiles(Logger logger) {
        var root = plugin.getConfig().getConfigurationSection("fish-phases");
        if (root == null) {
            logger.warning("[MEGA-FISHING] fish-phases section is missing.");
            return;
        }
        for (String key : root.getKeys(false)) {
            var section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            if (section.getDouble("speed-multiplier", 1.0D) <= 0.0D
                    || section.getDouble("pull-multiplier", 1.0D) <= 0.0D
                    || section.getDouble("turn-rate-multiplier", 1.0D) <= 0.0D) {
                logger.warning("[MEGA-FISHING] Invalid fish phase multipliers for " + key + ".");
            }
        }
    }

    private void validateCombatProfiles(Logger logger) {
        var section = plugin.getConfig().getConfigurationSection("combat.state-modifiers");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            var child = section.getConfigurationSection(key);
            if (child == null) {
                continue;
            }
            if (child.getDouble("damage-multiplier", 1.0D) <= 0.0D) {
                logger.warning("[MEGA-FISHING] Invalid combat.state-modifiers." + key + ".damage-multiplier");
            }
        }
    }

    private void validateBagUpgrades(Logger logger) {
        var section = plugin.getConfig().getConfigurationSection("bag.upgrade-costs");
        if (section == null) {
            logger.warning("[MEGA-FISHING] bag.upgrade-costs section is missing.");
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                int size = Integer.parseInt(key);
                long cost = section.getLong(key);
                if (size <= 0 || cost < 0L) {
                    logger.warning("[MEGA-FISHING] Invalid bag upgrade entry: size=" + key + ", cost=" + cost);
                }
            } catch (NumberFormatException exception) {
                logger.warning("[MEGA-FISHING] Invalid bag upgrade size key: " + key);
            }
        }
    }

    private void validateIslands(Logger logger) {
        for (IslandDefinition island : plugin.islandManager().islands()) {
            if (island.getUnlockCost() < 0L) {
                logger.warning("[MEGA-FISHING] Island '" + island.getId() + "' has negative unlock cost.");
            }
            for (FishingZone zone : island.getZones()) {
                if (zone.getEligibleFish().isEmpty()) {
                    logger.warning("[MEGA-FISHING] Zone '" + zone.getId() + "' has no eligible fish.");
                }
                if (zone.getRadius() == 0.0D) {
                    logger.warning("[MEGA-FISHING] Zone '" + zone.getId() + "' has radius 0, which is probably invalid.");
                }
                if (zone.getDepth() <= 0.0D) {
                    logger.warning("[MEGA-FISHING] Zone '" + zone.getId() + "' has non-positive depth.");
                }
                zone.getEligibleFish().keySet().forEach(fishId -> {
                    if (plugin.fishManager().getRegistry().get(fishId) == null) {
                        logger.warning("[MEGA-FISHING] Zone '" + zone.getId() + "' references missing fish '" + fishId + "'.");
                    }
                });
            }
        }
    }
}
