package com.megafishing.progression;

import com.megafishing.persistence.PlayerData;

public class ProgressionManager {
    public enum UnlockResult {
        SUCCESS,
        ALREADY_UNLOCKED,
        UNKNOWN_ISLAND,
        NOT_ENOUGH_COINS
    }

    private final IslandManager islandManager;

    public ProgressionManager(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    public void applyDefaultUnlocks(PlayerData data) {
        islandManager.islands().stream().filter(IslandDefinition::isDefaultUnlocked).forEach(island -> data.getUnlockedIslands().add(island.getId()));
    }

    public boolean canAccessIsland(PlayerData data, String islandId) {
        return data.getUnlockedIslands().contains(islandId.toLowerCase());
    }

    public UnlockResult unlockIsland(PlayerData data, String islandId) {
        IslandDefinition island = islandManager.get(islandId);
        if (island == null) {
            return UnlockResult.UNKNOWN_ISLAND;
        }
        if (data.getUnlockedIslands().contains(island.getId())) {
            return UnlockResult.ALREADY_UNLOCKED;
        }
        if (!data.removeCoins(island.getUnlockCost())) {
            return UnlockResult.NOT_ENOUGH_COINS;
        }
        data.getUnlockedIslands().add(island.getId());
        return UnlockResult.SUCCESS;
    }

    public IslandDefinition activeIslandFor(PlayerData data) {
        return islandManager.islands().stream().filter(island -> data.getUnlockedIslands().contains(island.getId())).findFirst().orElse(null);
    }
}
