package com.megafishing.fishing;

import com.megafishing.fish.FishRarity;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FishingZone {
    private final String id;
    private final String islandId;
    private final String displayName;
    private final String subtitle;
    private final Set<String> worlds;
    private final Location center;
    private final double radius;
    private final double depth;
    private final Double minX;
    private final Double maxX;
    private final Double minY;
    private final Double maxY;
    private final Double minZ;
    private final Double maxZ;
    private final int priority;
    private final String regionId;
    private final String minimumRodId;
    private final double biteTimeMultiplier;
    private final double fishSizeMultiplier;
    private final double fishHpMultiplier;
    private final double fishValueMultiplier;
    private final double rarityMultiplier;
    private final double encounterMultiplier;
    private final double bossChanceMultiplier;
    private final Map<String, Integer> eligibleFish;
    private final Map<FishRarity, Double> rarityOverrides;
    private final Set<FishRarity> blockedRarities;

    public FishingZone(String id, String islandId, String displayName, String subtitle, Set<String> worlds,
                       Location center, double radius, double depth,
                       Double minX, Double maxX, Double minY, Double maxY, Double minZ, Double maxZ,
                       int priority, String regionId, String minimumRodId,
                       double biteTimeMultiplier, double fishSizeMultiplier, double fishHpMultiplier,
                       double fishValueMultiplier, double rarityMultiplier, double encounterMultiplier,
                       double bossChanceMultiplier, Map<String, Integer> eligibleFish,
                       Map<FishRarity, Double> rarityOverrides, Set<FishRarity> blockedRarities) {
        this.id = id;
        this.islandId = islandId;
        this.displayName = displayName;
        this.subtitle = subtitle;
        this.worlds = worlds == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(worlds));
        this.center = center;
        this.radius = radius;
        this.depth = depth;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.priority = priority;
        this.regionId = regionId;
        this.minimumRodId = minimumRodId;
        this.biteTimeMultiplier = biteTimeMultiplier;
        this.fishSizeMultiplier = fishSizeMultiplier;
        this.fishHpMultiplier = fishHpMultiplier;
        this.fishValueMultiplier = fishValueMultiplier;
        this.rarityMultiplier = rarityMultiplier;
        this.encounterMultiplier = encounterMultiplier;
        this.bossChanceMultiplier = bossChanceMultiplier;
        this.eligibleFish = eligibleFish == null ? Collections.emptyMap() : Collections.unmodifiableMap(eligibleFish);
        this.rarityOverrides = rarityOverrides == null ? Collections.emptyMap() : Collections.unmodifiableMap(rarityOverrides);
        this.blockedRarities = blockedRarities == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(blockedRarities));
    }

    public boolean matches(Location location) {
        if (location == null) {
            return false;
        }
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        if (!worlds.isEmpty() && !worlds.contains(world.getName().toLowerCase())) {
            return false;
        }
        if (minX != null && location.getX() < minX) {
            return false;
        }
        if (maxX != null && location.getX() > maxX) {
            return false;
        }
        if (minY != null && location.getY() < minY) {
            return false;
        }
        if (maxY != null && location.getY() > maxY) {
            return false;
        }
        if (minZ != null && location.getZ() < minZ) {
            return false;
        }
        if (maxZ != null && location.getZ() > maxZ) {
            return false;
        }
        if (center != null) {
            if (center.getWorld() != null && !center.getWorld().equals(world)) {
                return false;
            }
            if (radius > 0.0D && center.distanceSquared(location) > (radius * radius)) {
                return false;
            }
            if (depth > 0.0D && Math.abs(location.getY() - center.getY()) > depth) {
                return false;
            }
        }
        return true;
    }

    public int specificityScore() {
        int score = 0;
        if (minX != null || maxX != null) {
            score += 2;
        }
        if (minY != null || maxY != null) {
            score += 1;
        }
        if (minZ != null || maxZ != null) {
            score += 2;
        }
        if (radius > 0.0D) {
            score += 2;
        }
        if (regionId != null && !regionId.isBlank()) {
            score += 3;
        }
        return score;
    }

    public String getId() { return id; }
    public String getIslandId() { return islandId; }
    public String getDisplayName() { return displayName; }
    public String getSubtitle() { return subtitle; }
    public Set<String> getWorlds() { return worlds; }
    public Location getCenter() { return center == null ? null : center.clone(); }
    public double getRadius() { return radius; }
    public double getDepth() { return depth; }
    public Double getMinX() { return minX; }
    public Double getMaxX() { return maxX; }
    public Double getMinY() { return minY; }
    public Double getMaxY() { return maxY; }
    public Double getMinZ() { return minZ; }
    public Double getMaxZ() { return maxZ; }
    public int getPriority() { return priority; }
    public String getRegionId() { return regionId; }
    public String getMinimumRodId() { return minimumRodId; }
    public double getBiteTimeMultiplier() { return biteTimeMultiplier; }
    public double getFishSizeMultiplier() { return fishSizeMultiplier; }
    public double getFishHpMultiplier() { return fishHpMultiplier; }
    public double getFishValueMultiplier() { return fishValueMultiplier; }
    public double getRarityMultiplier() { return rarityMultiplier; }
    public double getEncounterMultiplier() { return encounterMultiplier; }
    public double getBossChanceMultiplier() { return bossChanceMultiplier; }
    public Map<String, Integer> getEligibleFish() { return eligibleFish; }
    public Map<FishRarity, Double> getRarityOverrides() { return rarityOverrides; }
    public Set<FishRarity> getBlockedRarities() { return blockedRarities; }
}
