package com.megafishing.fish;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.fishing.FishingZone;
import com.megafishing.util.RandomUtil;

import java.io.File;
import java.util.LinkedHashMap;
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

    public FishController rollAndSpawn(FishingZone zone, org.bukkit.Location location) {
        if (zone == null) {
            return null;
        }
        Map<FishDefinition, Double> weighted = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : zone.getEligibleFish().entrySet()) {
            FishDefinition definition = registry.get(entry.getKey());
            if (definition != null) {
                double rarityModifier = zone.getRarityOverrides().getOrDefault(definition.getRarity(), registry.getRarityDefaults().getOrDefault(definition.getRarity(), 1.0D));
                weighted.put(definition, entry.getValue() * Math.max(0.01D, rarityModifier));
            }
        }
        FishDefinition chosen = RandomUtil.weighted(random, weighted);
        if (chosen == null) {
            chosen = registry.values().stream().findFirst().orElse(null);
        }
        return chosen == null ? null : spawner.spawn(chosen, zone, location);
    }
}
