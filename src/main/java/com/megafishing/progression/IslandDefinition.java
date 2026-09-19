package com.megafishing.progression;

import com.megafishing.fishing.FishingZone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IslandDefinition {
    private final String id;
    private final String displayName;
    private final long unlockCost;
    private final boolean defaultUnlocked;
    private final List<FishingZone> zones;

    public IslandDefinition(String id, String displayName, long unlockCost, boolean defaultUnlocked, List<FishingZone> zones) {
        this.id = id;
        this.displayName = displayName;
        this.unlockCost = unlockCost;
        this.defaultUnlocked = defaultUnlocked;
        this.zones = zones == null ? new ArrayList<>() : new ArrayList<>(zones);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public long getUnlockCost() { return unlockCost; }
    public boolean isDefaultUnlocked() { return defaultUnlocked; }
    public List<FishingZone> getZones() { return Collections.unmodifiableList(zones); }
}
