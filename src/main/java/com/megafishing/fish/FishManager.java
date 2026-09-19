package com.megafishing.fish;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.fishing.FishingEnvironment;
import com.megafishing.fishing.FishingZone;
import com.megafishing.util.RandomUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FishManager {
    private final MegaFishingPlugin plugin;
    private final FishRegistry registry = new FishRegistry();
    private final FishSpawner spawner;
    private final Random random = new Random();

    public FishManager(MegaFishingPlugin plugin) {
        this.plugin = plugin;
        this.spawner = new FishSpawner(plugin);
    }

    public void reload() {
        registry.load(new File(plugin.getDataFolder(), "fish.yml"), plugin.getLogger());
    }

    public FishRegistry getRegistry() {
        return registry;
    }

    public FishController rollAndSpawn(FishingEnvironment environment, org.bukkit.Location location) {
        if (environment == null || environment.getZone() == null) {
            return null;
        }
        FishingZone zone = environment.getZone();
        List<FishDefinition> eligible = new ArrayList<>();
        for (String fishId : zone.getEligibleFish().keySet()) {
            FishDefinition definition = registry.get(fishId);
            if (definition == null || environment.blocks(definition.getRarity())) {
                continue;
            }
            eligible.add(definition);
        }
        if (eligible.isEmpty()) {
            return null;
        }

        FishRarity rarity = rollRarity(environment, zone, eligible);
        FishDefinition chosen = rollFish(environment, zone, eligible, rarity);
        if (chosen == null) {
            chosen = eligible.getFirst();
        }
        return spawner.spawn(chosen, environment, location);
    }

    private FishRarity rollRarity(FishingEnvironment environment, FishingZone zone, List<FishDefinition> eligible) {
        Map<FishRarity, Double> weighted = new EnumMap<>(FishRarity.class);
        for (FishRarity rarity : FishRarity.values()) {
            if (environment.blocks(rarity)) {
                continue;
            }
            boolean present = eligible.stream().anyMatch(fish -> fish.getRarity() == rarity);
            if (!present) {
                continue;
            }
            double base = registry.getRarityDefaults().getOrDefault(rarity, 0.01D);
            double weight = base * zone.getRarityOverrides().getOrDefault(rarity, 1.0D);
            weight *= Math.pow(Math.max(0.01D, environment.getRarityMultiplier()), rarity.ordinal());
            weighted.put(rarity, weight);
        }
        FishRarity chosen = RandomUtil.weighted(random, weighted);
        return chosen == null ? FishRarity.COMMON : chosen;
    }

    private FishDefinition rollFish(FishingEnvironment environment, FishingZone zone, List<FishDefinition> eligible, FishRarity rarity) {
        Map<FishDefinition, Double> weighted = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : zone.getEligibleFish().entrySet()) {
            FishDefinition definition = registry.get(entry.getKey());
            if (definition == null || definition.getRarity() != rarity || environment.blocks(definition.getRarity())) {
                continue;
            }
            double weight = Math.max(1.0D, entry.getValue());
            if (definition.isBoss()) {
                weight *= environment.getBossChanceMultiplier();
            }
            weighted.put(definition, weight);
        }
        FishDefinition chosen = RandomUtil.weighted(random, weighted);
        if (chosen != null) {
            return chosen;
        }
        return eligible.stream().filter(definition -> definition.getRarity() == rarity).findFirst().orElse(null);
    }
}
