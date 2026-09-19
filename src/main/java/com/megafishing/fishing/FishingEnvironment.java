package com.megafishing.fishing;

import com.megafishing.fish.FishRarity;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class FishingEnvironment {
    private final String worldName;
    private final FishingWorldProfile profile;
    private final FishingZone zone;
    private final double biteTimeMultiplier;
    private final double fishSizeMultiplier;
    private final double fishHpMultiplier;
    private final double fishValueMultiplier;
    private final double rarityMultiplier;
    private final double encounterMultiplier;
    private final double bossChanceMultiplier;
    private final double largeFishMultiplier;
    private final Set<FishRarity> blockedRarities;
    private final String seasonId;
    private final Set<String> activeEventIds;

    public FishingEnvironment(String worldName, FishingWorldProfile profile, FishingZone zone,
                              double biteTimeMultiplier, double fishSizeMultiplier, double fishHpMultiplier,
                              double fishValueMultiplier, double rarityMultiplier, double encounterMultiplier,
                              double bossChanceMultiplier, double largeFishMultiplier, Set<FishRarity> blockedRarities,
                              String seasonId, Set<String> activeEventIds) {
        this.worldName = worldName;
        this.profile = profile;
        this.zone = zone;
        this.biteTimeMultiplier = biteTimeMultiplier;
        this.fishSizeMultiplier = fishSizeMultiplier;
        this.fishHpMultiplier = fishHpMultiplier;
        this.fishValueMultiplier = fishValueMultiplier;
        this.rarityMultiplier = rarityMultiplier;
        this.encounterMultiplier = encounterMultiplier;
        this.bossChanceMultiplier = bossChanceMultiplier;
        this.largeFishMultiplier = largeFishMultiplier;
        this.blockedRarities = blockedRarities == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(blockedRarities));
        this.seasonId = seasonId == null || seasonId.isBlank() ? null : seasonId.toLowerCase();
        this.activeEventIds = activeEventIds == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(activeEventIds));
    }

    public boolean blocks(FishRarity rarity) {
        return rarity != null && blockedRarities.contains(rarity);
    }

    public String getWorldName() { return worldName; }
    public FishingWorldProfile getProfile() { return profile; }
    public FishingZone getZone() { return zone; }
    public double getBiteTimeMultiplier() { return biteTimeMultiplier; }
    public double getFishSizeMultiplier() { return fishSizeMultiplier; }
    public double getFishHpMultiplier() { return fishHpMultiplier; }
    public double getFishValueMultiplier() { return fishValueMultiplier; }
    public double getRarityMultiplier() { return rarityMultiplier; }
    public double getEncounterMultiplier() { return encounterMultiplier; }
    public double getBossChanceMultiplier() { return bossChanceMultiplier; }
    public double getLargeFishMultiplier() { return largeFishMultiplier; }
    public Set<FishRarity> getBlockedRarities() { return blockedRarities; }
    public String getSeasonId() { return seasonId; }
    public Set<String> getActiveEventIds() { return activeEventIds; }
}
