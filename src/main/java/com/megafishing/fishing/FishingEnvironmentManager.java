package com.megafishing.fishing;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.fish.FishRarity;
import com.megafishing.persistence.PlayerData;
import com.megafishing.pet.PetDefinition;
import com.megafishing.rod.RodDefinition;
import com.megafishing.util.MathUtil;
import com.megafishing.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FishingEnvironmentManager {
    private final MegaFishingPlugin plugin;
    private final Map<String, FishingWorldProfile> profiles = new LinkedHashMap<>();
    private final Map<String, FishingZone> zones = new LinkedHashMap<>();
    private boolean enabled = true;
    private String primaryWorldName = "fishing_world";
    private String defaultProfileId = "normal";

    public FishingEnvironmentManager(MegaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        profiles.clear();
        zones.clear();
        loadProfiles();
        loadZones();
    }

    public FishingWorldProfile profileFor(World world) {
        return world == null ? fallbackProfile() : profileFor(world.getName());
    }

    public FishingWorldProfile profileFor(String worldName) {
        if (worldName == null) {
            return fallbackProfile();
        }
        for (FishingWorldProfile profile : profiles.values()) {
            if (profile.matchesWorld(worldName)) {
                return profile;
            }
        }
        return fallbackProfile();
    }

    public FishingZone findZone(Location location) {
        FishingZone best = null;
        for (FishingZone zone : zones.values()) {
            if (!zone.matches(location)) {
                continue;
            }
            if (best == null
                    || zone.getPriority() > best.getPriority()
                    || (zone.getPriority() == best.getPriority() && zone.specificityScore() > best.specificityScore())
                    || (zone.getPriority() == best.getPriority() && zone.specificityScore() == best.specificityScore()
                    && zone.getRadius() > 0.0D && (best.getRadius() <= 0.0D || zone.getRadius() < best.getRadius()))) {
                best = zone;
            }
        }
        return best;
    }

    public FishingEnvironment resolve(Location castLocation, RodDefinition rod, PlayerData data) {
        if (castLocation == null || castLocation.getWorld() == null) {
            return null;
        }
        FishingWorldProfile profile = profileFor(castLocation.getWorld());
        FishingZone zone = findZone(castLocation);
        if (profile == null || zone == null) {
            return null;
        }

        double rodBiteSpeed = rod == null ? 0.0D : rod.getBiteSpeed();
        double rodRarityLuck = rod == null ? 0.0D : rod.getRarityLuck();
        double rodSizeBonus = rod == null ? 0.0D : rod.getSizeBonus();
        double rodEncounterRate = rod == null ? 0.0D : rod.getEncounterRate();
        double rodZoneEfficiency = profile.isPrimaryFishingWorld() && rod != null ? rod.getZoneEfficiency() : 0.0D;

        double petBiteSpeed = 0.0D;
        double petRarityLuck = 0.0D;
        double petEncounterRate = 0.0D;
        double petLargeFishChance = 0.0D;
        if (data != null) {
            for (String petId : data.getEquippedPets()) {
                PetDefinition pet = plugin.petManager().getRegistry().get(petId);
                if (pet == null) {
                    continue;
                }
                petBiteSpeed += pet.getBiteSpeed();
                petRarityLuck += pet.getRarityLuck();
                petEncounterRate += pet.getEncounterRate();
                petLargeFishChance += pet.getLargeFishChance();
            }
        }

        double biteTimeMultiplier = profile.getBiteTimeMultiplier() * zone.getBiteTimeMultiplier();
        biteTimeMultiplier *= Math.max(0.15D, 1.0D - rodBiteSpeed - petBiteSpeed - (rodZoneEfficiency * 0.35D));

        double encounterMultiplier = profile.getEncounterMultiplier() * zone.getEncounterMultiplier();
        encounterMultiplier *= Math.max(0.05D, 1.0D + rodEncounterRate + petEncounterRate + rodZoneEfficiency);

        double fishSizeMultiplier = profile.getFishSizeMultiplier() * zone.getFishSizeMultiplier();
        fishSizeMultiplier *= Math.max(0.10D, 1.0D + rodSizeBonus + (petLargeFishChance * 0.60D) + (rodZoneEfficiency * 0.35D));

        double fishHpMultiplier = profile.getFishHpMultiplier() * zone.getFishHpMultiplier();
        fishHpMultiplier *= Math.max(0.10D, 1.0D + (rodZoneEfficiency * 0.15D));

        double fishValueMultiplier = profile.getFishValueMultiplier() * zone.getFishValueMultiplier();
        fishValueMultiplier *= Math.max(0.10D, 1.0D + (rodZoneEfficiency * 0.20D));

        double rarityMultiplier = profile.getRarityMultiplier() * zone.getRarityMultiplier();
        rarityMultiplier *= Math.max(0.05D, 1.0D + rodRarityLuck + petRarityLuck + (rodZoneEfficiency * 0.40D));

        double bossChanceMultiplier = profile.getBossChanceMultiplier() * zone.getBossChanceMultiplier();
        bossChanceMultiplier *= Math.max(0.05D, 1.0D + (rodRarityLuck * 0.50D) + (petRarityLuck * 0.50D) + (rodZoneEfficiency * 0.75D));

        double largeFishMultiplier = Math.max(0.10D, fishSizeMultiplier * (1.0D + petLargeFishChance + (rodSizeBonus * 0.50D) + (rodZoneEfficiency * 0.25D)));

        Set<FishRarity> blocked = new LinkedHashSet<>(profile.getBlockedRarities());
        blocked.addAll(zone.getBlockedRarities());

        return new FishingEnvironment(
                castLocation.getWorld().getName(),
                profile,
                zone,
                MathUtil.clamp(biteTimeMultiplier, 0.10D, 10.0D),
                MathUtil.clamp(fishSizeMultiplier, 0.10D, 20.0D),
                MathUtil.clamp(fishHpMultiplier, 0.10D, 20.0D),
                MathUtil.clamp(fishValueMultiplier, 0.10D, 20.0D),
                MathUtil.clamp(rarityMultiplier, 0.01D, 20.0D),
                MathUtil.clamp(encounterMultiplier, 0.05D, 20.0D),
                MathUtil.clamp(bossChanceMultiplier, 0.01D, 20.0D),
                MathUtil.clamp(largeFishMultiplier, 0.10D, 20.0D),
                blocked
        );
    }

    public Collection<FishingWorldProfile> profiles() {
        return Collections.unmodifiableCollection(profiles.values());
    }

    public Collection<FishingZone> zones() {
        return Collections.unmodifiableCollection(zones.values());
    }

    public List<FishingZone> zonesForIsland(String islandId) {
        List<FishingZone> matches = new ArrayList<>();
        for (FishingZone zone : zones.values()) {
            if (zone.getIslandId().equalsIgnoreCase(islandId)) {
                matches.add(zone);
            }
        }
        matches.sort((left, right) -> Integer.compare(right.getPriority(), left.getPriority()));
        return matches;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getPrimaryWorldName() {
        return primaryWorldName;
    }

    public String getDefaultProfileId() {
        return defaultProfileId;
    }

    private void loadProfiles() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "fishing-world.yml"));
        enabled = yaml.getBoolean("enabled", true);
        primaryWorldName = yaml.getString("world-name", "fishing_world");
        defaultProfileId = yaml.getString("default-profile", "normal").toLowerCase();
        ConfigurationSection root = yaml.getConfigurationSection("fishing-worlds");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            Set<String> worlds = new LinkedHashSet<>();
            worlds.addAll(lowered(section.getStringList("worlds")));
            String explicitWorld = section.getString("world");
            if (explicitWorld != null && !explicitWorld.isBlank()) {
                worlds.add(explicitWorld.toLowerCase());
            }
            Set<FishRarity> blockedRarities = parseRaritySet(section.getStringList("blocked-rarities"));
            profiles.put(id.toLowerCase(), new FishingWorldProfile(
                    id.toLowerCase(),
                    section.getString("display-name", Text.plainEnum(id)),
                    worlds,
                    Math.max(0.05D, section.getDouble("bite-time-multiplier", 1.0D)),
                    Math.max(0.05D, section.getDouble("fish-size-multiplier", 1.0D)),
                    Math.max(0.05D, section.getDouble("fish-hp-multiplier", 1.0D)),
                    Math.max(0.05D, section.getDouble("fish-value-multiplier", 1.0D)),
                    Math.max(0.01D, section.getDouble("rarity-multiplier", 1.0D)),
                    Math.max(0.01D, section.getDouble("encounter-multiplier", 1.0D)),
                    Math.max(0.01D, section.getDouble("boss-chance-multiplier", 1.0D)),
                    Math.max(0.10D, section.getDouble("min-bite-seconds", 1.5D)),
                    Math.max(0.10D, section.getDouble("max-bite-seconds", 6.0D)),
                    section.getBoolean("primary-fishing-world", id.equalsIgnoreCase(primaryWorldName)),
                    blockedRarities,
                    section.getStringList("entry-messages"),
                    section.getString("start-title", section.getString("display-name", Text.plainEnum(id))),
                    section.getString("start-subtitle", "&7Waiting for a bite...")
            ));
        }
    }

    private void loadZones() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "zones.yml"));
        ConfigurationSection root = yaml.getConfigurationSection("zones");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            Set<String> worlds = new LinkedHashSet<>();
            worlds.addAll(lowered(section.getStringList("worlds")));
            String explicitWorld = section.getString("world");
            if (explicitWorld != null && !explicitWorld.isBlank()) {
                worlds.add(explicitWorld.toLowerCase());
            }
            Location center = parseCenter(section.getString("center"), worlds.stream().findFirst().orElse(null));
            Map<String, Integer> fish = parseFishWeights(section);
            Map<FishRarity, Double> rarityOverrides = new EnumMap<>(FishRarity.class);
            ConfigurationSection raritySection = section.getConfigurationSection("rarity-overrides");
            if (raritySection != null) {
                for (String rarity : raritySection.getKeys(false)) {
                    try {
                        rarityOverrides.put(FishRarity.valueOf(rarity.toUpperCase()), raritySection.getDouble(rarity));
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger().warning("[MEGA-FISHING] Invalid rarity override '" + rarity + "' in zone " + id);
                    }
                }
            }
            zones.put(id.toLowerCase(), new FishingZone(
                    id.toLowerCase(),
                    section.getString("island-id", "starter").toLowerCase(),
                    section.getString("display-name", Text.plainEnum(id)),
                    section.getString("subtitle", "&7Fishing Grounds"),
                    worlds,
                    center,
                    section.getDouble("radius", -1.0D),
                    section.getDouble("depth", 256.0D),
                    getOptionalDouble(section, "min-x"),
                    getOptionalDouble(section, "max-x"),
                    getOptionalDouble(section, "min-y"),
                    getOptionalDouble(section, "max-y"),
                    getOptionalDouble(section, "min-z"),
                    getOptionalDouble(section, "max-z"),
                    section.getInt("priority", 0),
                    section.getString("region-id", ""),
                    normalize(section.getString("minimum-rod", section.getString("min-rod", ""))),
                    Math.max(0.05D, section.getDouble("bite-time-multiplier", 1.0D)),
                    Math.max(0.05D, section.getDouble("fish-size-multiplier", 1.0D)),
                    Math.max(0.05D, section.getDouble("fish-hp-multiplier", 1.0D)),
                    Math.max(0.05D, section.getDouble("fish-value-multiplier", 1.0D)),
                    Math.max(0.01D, section.getDouble("rarity-multiplier", 1.0D)),
                    Math.max(0.01D, section.getDouble("encounter-multiplier", 1.0D)),
                    Math.max(0.01D, section.getDouble("boss-chance-multiplier", 1.0D)),
                    fish,
                    rarityOverrides,
                    parseRaritySet(section.getStringList("blocked-rarities"))
            ));
        }
    }

    private FishingWorldProfile fallbackProfile() {
        FishingWorldProfile profile = profiles.get(defaultProfileId);
        return profile != null ? profile : profiles.values().stream().findFirst().orElse(null);
    }

    private Set<String> lowered(List<String> input) {
        Set<String> lowered = new LinkedHashSet<>();
        for (String value : input) {
            if (value != null && !value.isBlank()) {
                lowered.add(value.toLowerCase());
            }
        }
        return lowered;
    }

    private Set<FishRarity> parseRaritySet(List<String> values) {
        Set<FishRarity> rarities = new LinkedHashSet<>();
        for (String value : values) {
            try {
                rarities.add(FishRarity.valueOf(value.toUpperCase()));
            } catch (Exception ignored) {
            }
        }
        return rarities;
    }

    private Map<String, Integer> parseFishWeights(ConfigurationSection section) {
        Map<String, Integer> fish = new LinkedHashMap<>();
        ConfigurationSection fishSection = section.getConfigurationSection("fish");
        if (fishSection != null) {
            for (String fishId : fishSection.getKeys(false)) {
                fish.put(fishId.toLowerCase(), Math.max(1, fishSection.getInt(fishId, 1)));
            }
            return fish;
        }
        for (String fishId : section.getStringList("fish")) {
            if (fishId != null && !fishId.isBlank()) {
                fish.put(fishId.toLowerCase(), 1);
            }
        }
        return fish;
    }

    private Double getOptionalDouble(ConfigurationSection section, String path) {
        return section.contains(path) ? section.getDouble(path) : null;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "" : value.toLowerCase();
    }

    private Location parseCenter(String raw, String worldHint) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            String[] split = raw.split(",");
            World world = resolveWorld(worldHint);
            return new Location(world, Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
        } catch (Exception exception) {
            return null;
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
}
