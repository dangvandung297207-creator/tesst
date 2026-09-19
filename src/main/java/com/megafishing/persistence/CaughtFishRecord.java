package com.megafishing.persistence;

import com.megafishing.fish.FishRarity;

public class CaughtFishRecord {
    private final String fishId;
    private final double weight;
    private final FishRarity rarity;
    private final long value;
    private final long timestamp;

    public CaughtFishRecord(String fishId, double weight, FishRarity rarity, long value, long timestamp) {
        this.fishId = fishId;
        this.weight = weight;
        this.rarity = rarity;
        this.value = value;
        this.timestamp = timestamp;
    }

    public String getFishId() {
        return fishId;
    }

    public double getWeight() {
        return weight;
    }

    public FishRarity getRarity() {
        return rarity;
    }

    public long getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
