package com.megafishing.fishing;

import com.megafishing.fish.FishRarity;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FishingWorldProfile {
    private final String id;
    private final String displayName;
    private final Set<String> worlds;
    private final double biteTimeMultiplier;
    private final double fishSizeMultiplier;
    private final double fishHpMultiplier;
    private final double fishValueMultiplier;
    private final double rarityMultiplier;
    private final double encounterMultiplier;
    private final double bossChanceMultiplier;
    private final double minBiteSeconds;
    private final double maxBiteSeconds;
    private final boolean primaryFishingWorld;
    private final Set<FishRarity> blockedRarities;
    private final List<String> entryMessages;
    private final String startTitle;
    private final String startSubtitle;

    public FishingWorldProfile(String id, String displayName, Set<String> worlds, double biteTimeMultiplier,
                               double fishSizeMultiplier, double fishHpMultiplier, double fishValueMultiplier,
                               double rarityMultiplier, double encounterMultiplier, double bossChanceMultiplier,
                               double minBiteSeconds, double maxBiteSeconds, boolean primaryFishingWorld,
                               Set<FishRarity> blockedRarities, List<String> entryMessages,
                               String startTitle, String startSubtitle) {
        this.id = id;
        this.displayName = displayName;
        this.worlds = worlds == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(worlds));
        this.biteTimeMultiplier = biteTimeMultiplier;
        this.fishSizeMultiplier = fishSizeMultiplier;
        this.fishHpMultiplier = fishHpMultiplier;
        this.fishValueMultiplier = fishValueMultiplier;
        this.rarityMultiplier = rarityMultiplier;
        this.encounterMultiplier = encounterMultiplier;
        this.bossChanceMultiplier = bossChanceMultiplier;
        this.minBiteSeconds = minBiteSeconds;
        this.maxBiteSeconds = Math.max(minBiteSeconds, maxBiteSeconds);
        this.primaryFishingWorld = primaryFishingWorld;
        this.blockedRarities = blockedRarities == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(blockedRarities));
        this.entryMessages = entryMessages == null ? List.of() : List.copyOf(entryMessages);
        this.startTitle = startTitle;
        this.startSubtitle = startSubtitle;
    }

    public boolean matchesWorld(String worldName) {
        return worldName != null && worlds.contains(worldName.toLowerCase());
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Set<String> getWorlds() { return worlds; }
    public double getBiteTimeMultiplier() { return biteTimeMultiplier; }
    public double getFishSizeMultiplier() { return fishSizeMultiplier; }
    public double getFishHpMultiplier() { return fishHpMultiplier; }
    public double getFishValueMultiplier() { return fishValueMultiplier; }
    public double getRarityMultiplier() { return rarityMultiplier; }
    public double getEncounterMultiplier() { return encounterMultiplier; }
    public double getBossChanceMultiplier() { return bossChanceMultiplier; }
    public double getMinBiteSeconds() { return minBiteSeconds; }
    public double getMaxBiteSeconds() { return maxBiteSeconds; }
    public boolean isPrimaryFishingWorld() { return primaryFishingWorld; }
    public Set<FishRarity> getBlockedRarities() { return blockedRarities; }
    public List<String> getEntryMessages() { return entryMessages; }
    public String getStartTitle() { return startTitle; }
    public String getStartSubtitle() { return startSubtitle; }
}
