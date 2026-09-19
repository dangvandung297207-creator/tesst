package com.megafishing.fishing;

import com.megafishing.fish.FishRarity;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class FishingZone {
    private final String id;
    private final String islandId;
    private final String displayName;
    private final Set<String> worlds;
    private final Location center;
    private final double radius;
    private final double depth;
    private final Map<String, Integer> eligibleFish;
    private final Map<FishRarity, Double> rarityOverrides;

    public FishingZone(String id, String islandId, String displayName, Set<String> worlds, Location center, double radius, double depth,
                       Map<String, Integer> eligibleFish, Map<FishRarity, Double> rarityOverrides) {
        this.id = id;
        this.islandId = islandId;
        this.displayName = displayName;
        this.worlds = worlds == null ? Collections.emptySet() : worlds;
        this.center = center;
        this.radius = radius;
        this.depth = depth;
        this.eligibleFish = eligibleFish == null ? Collections.emptyMap() : eligibleFish;
        this.rarityOverrides = rarityOverrides == null ? Collections.emptyMap() : rarityOverrides;
    }

    public String getId() { return id; }
    public String getIslandId() { return islandId; }
    public String getDisplayName() { return displayName; }
    public Location getCenter() { return center == null ? null : center.clone(); }
    public double getRadius() { return radius; }
    public double getDepth() { return depth; }
    public Map<String, Integer> getEligibleFish() { return eligibleFish; }
    public Map<FishRarity, Double> getRarityOverrides() { return rarityOverrides; }

    public boolean matches(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        if (!worlds.isEmpty() && !worlds.contains(world.getName().toLowerCase())) {
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
}
