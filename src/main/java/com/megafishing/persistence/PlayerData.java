package com.megafishing.persistence;

import com.megafishing.storage.FishBag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private long coins;
    private String rodId;
    private int maxPetSlots;
    private final FishBag fishBag;
    private final Set<String> ownedPets;
    private final List<String> equippedPets;
    private final Set<String> unlockedIslands;
    private final PlayerStatistics statistics;
    private boolean tutorialCompleted;

    public PlayerData(UUID uuid, long coins, String rodId, int bagCapacity, int maxPetSlots,
                      Set<String> ownedPets, List<String> equippedPets, Set<String> unlockedIslands,
                      List<CaughtFishRecord> fishRecords, PlayerStatistics statistics, boolean tutorialCompleted) {
        this.uuid = uuid;
        this.coins = coins;
        this.rodId = rodId;
        this.maxPetSlots = maxPetSlots;
        this.fishBag = new FishBag(bagCapacity);
        this.fishBag.replaceAll(fishRecords);
        this.ownedPets = ownedPets == null ? new LinkedHashSet<>() : new LinkedHashSet<>(ownedPets);
        this.equippedPets = equippedPets == null ? new ArrayList<>() : new ArrayList<>(equippedPets);
        this.unlockedIslands = unlockedIslands == null ? new LinkedHashSet<>() : new LinkedHashSet<>(unlockedIslands);
        this.statistics = statistics == null ? new PlayerStatistics() : statistics;
        this.tutorialCompleted = tutorialCompleted;
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getCoins() {
        return coins;
    }

    public void addCoins(long value) {
        coins += value;
    }

    public boolean removeCoins(long value) {
        if (coins < value) {
            return false;
        }
        coins -= value;
        return true;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

    public String getRodId() {
        return rodId;
    }

    public void setRodId(String rodId) {
        this.rodId = rodId;
    }

    public int getBagCapacity() {
        return fishBag.getCapacity();
    }

    public void setBagCapacity(int capacity) {
        fishBag.setCapacity(capacity);
    }

    public int getMaxPetSlots() {
        return maxPetSlots;
    }

    public void setMaxPetSlots(int maxPetSlots) {
        this.maxPetSlots = maxPetSlots;
    }

    public FishBag getFishBag() {
        return fishBag;
    }

    public Set<String> getOwnedPets() {
        return ownedPets;
    }

    public List<String> getEquippedPets() {
        return equippedPets;
    }

    public Set<String> getUnlockedIslands() {
        return unlockedIslands;
    }

    public PlayerStatistics getStatistics() {
        return statistics;
    }

    public boolean isTutorialCompleted() {
        return tutorialCompleted;
    }

    public void setTutorialCompleted(boolean tutorialCompleted) {
        this.tutorialCompleted = tutorialCompleted;
    }
}
