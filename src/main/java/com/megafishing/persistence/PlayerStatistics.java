package com.megafishing.persistence;

public class PlayerStatistics {
    private long totalCatches;
    private double totalWeight;
    private double largestFishWeight;
    private String largestFishId = "";
    private long highestValue;
    private long totalCoinsEarned;
    private long fishEscaped;

    public static PlayerStatistics of(long totalCatches, double totalWeight, double largestFishWeight, String largestFishId,
                                      long highestValue, long totalCoinsEarned, long fishEscaped) {
        PlayerStatistics statistics = new PlayerStatistics();
        statistics.totalCatches = Math.max(0L, totalCatches);
        statistics.totalWeight = Math.max(0.0D, totalWeight);
        statistics.largestFishWeight = Math.max(0.0D, largestFishWeight);
        statistics.largestFishId = largestFishId == null ? "" : largestFishId;
        statistics.highestValue = Math.max(0L, highestValue);
        statistics.totalCoinsEarned = Math.max(0L, totalCoinsEarned);
        statistics.fishEscaped = Math.max(0L, fishEscaped);
        return statistics;
    }

    public long getTotalCatches() {
        return totalCatches;
    }

    public void incrementCatch() {
        totalCatches++;
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    public void addWeight(double weight) {
        totalWeight += weight;
    }

    public double getLargestFishWeight() {
        return largestFishWeight;
    }

    public String getLargestFishId() {
        return largestFishId;
    }

    public void updateLargest(String fishId, double weight) {
        if (weight > largestFishWeight) {
            largestFishWeight = weight;
            largestFishId = fishId;
        }
    }

    public long getHighestValue() {
        return highestValue;
    }

    public void updateHighestValue(long value) {
        if (value > highestValue) {
            highestValue = value;
        }
    }

    public long getTotalCoinsEarned() {
        return totalCoinsEarned;
    }

    public void addCoinsEarned(long coins) {
        totalCoinsEarned += coins;
    }

    public long getFishEscaped() {
        return fishEscaped;
    }

    public void incrementEscaped() {
        fishEscaped++;
    }
}
