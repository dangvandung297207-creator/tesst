package com.megafishing.progression;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.fish.FishRarity;
import com.megafishing.fishing.FishingZone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IslandManager {
    private final MegaFishingPlugin plugin;
    private final Map<String, IslandDefinition> islands = new LinkedHashMap<>();
    private final Map<String, FishingZone> zones = new LinkedHashMap<>();

    public IslandManager(MegaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        islands.clear();
        zones.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "islands.yml"));
        ConfigurationSection root = yaml.getConfigurationSection("islands");
        if (root == null) {
            return;
        }
        for (String islandId : root.getKeys(false)) {
            ConfigurationSection islandSection = root.getConfigurationSection(islandId);
            if (islandSection == null) {
                continue;
            }
            List<FishingZone> islandZones = new ArrayList<>();
            ConfigurationSection zonesSection = islandSection.getConfigurationSection("zones");
            if (zonesSection != null) {
                for (String zoneId : zonesSection.getKeys(false)) {
                    ConfigurationSection zoneSection = zonesSection.getConfigurationSection(zoneId);
                    if (zoneSection == null) {
                        continue;
                    }
                    Set<String> worlds = new LinkedHashSet<>();
                    for (String worldName : zoneSection.getStringList("worlds")) {
                        worlds.add(worldName.toLowerCase());
                    }
                    String centerRaw = zoneSection.getString("center", "0,0,0");
                    Location center = parseCenter(centerRaw, worlds.stream().findFirst().orElse(null));
                    Map<String, Integer> fish = new LinkedHashMap<>();
                    ConfigurationSection fishSection = zoneSection.getConfigurationSection("fish");
                    if (fishSection != null) {
                        for (String fishId : fishSection.getKeys(false)) {
                            fish.put(fishId.toLowerCase(), fishSection.getInt(fishId, 1));
                        }
                    }
                    Map<FishRarity, Double> rarityOverrides = new LinkedHashMap<>();
                    ConfigurationSection raritySection = zoneSection.getConfigurationSection("rarity-overrides");
                    if (raritySection != null) {
                        for (String rarity : raritySection.getKeys(false)) {
                            try {
                                rarityOverrides.put(FishRarity.valueOf(rarity.toUpperCase()), raritySection.getDouble(rarity));
                            } catch (IllegalArgumentException ignored) {
                                plugin.getLogger().warning("[MEGA-FISHING] Invalid rarity override: " + rarity + " in zone " + zoneId);
                            }
                        }
                    }
                    FishingZone zone = new FishingZone(
                            zoneId.toLowerCase(),
                            islandId.toLowerCase(),
                            zoneSection.getString("display-name", zoneId),
                            worlds,
                            center,
                            zoneSection.getDouble("radius", -1.0D),
                            zoneSection.getDouble("depth", 256.0D),
                            fish,
                            rarityOverrides
                    );
                    islandZones.add(zone);
                    zones.put(zone.getId(), zone);
                }
            }
            islands.put(islandId.toLowerCase(), new IslandDefinition(
                    islandId.toLowerCase(),
                    islandSection.getString("display-name", islandId),
                    Math.max(0L, islandSection.getLong("unlock-cost", 0L)),
                    islandSection.getBoolean("default-unlocked", false),
                    islandZones
            ));
        }
    }

    private Location parseCenter(String raw, String worldHint) {
        try {
            String[] split = raw.split(",");
            World world = resolveWorld(worldHint);
            return new Location(world, Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
        } catch (Exception exception) {
            World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            return new Location(world, 0, 0, 0);
        }
    }

    private World resolveWorld(String name) {
        if (name == null) {
            return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        }
        World direct = Bukkit.getWorld(name);
        if (direct != null) {
            return direct;
        }
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().equalsIgnoreCase(name)) {
                return world;
            }
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    public FishingZone findZone(Location location) {
        FishingZone fallback = null;
        FishingZone bestSpecific = null;
        for (FishingZone zone : zones.values()) {
            if (!zone.matches(location)) {
                continue;
            }
            if (zone.getRadius() > 0.0D) {
                if (bestSpecific == null || zone.getRadius() < bestSpecific.getRadius()) {
                    bestSpecific = zone;
                }
            } else if (fallback == null) {
                fallback = zone;
            }
        }
        return bestSpecific != null ? bestSpecific : fallback;
    }

    public IslandDefinition get(String id) {
        return id == null ? null : islands.get(id.toLowerCase());
    }

    public Collection<IslandDefinition> islands() {
        return Collections.unmodifiableCollection(islands.values());
    }
}
