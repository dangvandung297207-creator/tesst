package com.megafishing.fish;

public enum FishSizeTier {
    SMALL(0.0D, 0.25D, 50.0D, 0.80D, 0.85D),
    MEDIUM(0.25D, 0.55D, 30.0D, 1.00D, 1.00D),
    LARGE(0.55D, 0.75D, 15.0D, 1.50D, 1.15D),
    HUGE(0.75D, 0.90D, 4.0D, 2.50D, 1.35D),
    COLOSSAL(0.90D, 0.99D, 0.9D, 4.00D, 1.60D),
    TITANIC(0.99D, 1.00D, 0.1D, 6.00D, 1.90D);

    private final double minProgress;
    private final double maxProgress;
    private final double baseChance;
    private final double scaleMultiplier;
    private final double healthMultiplier;

    FishSizeTier(double minProgress, double maxProgress, double baseChance, double scaleMultiplier, double healthMultiplier) {
        this.minProgress = minProgress;
        this.maxProgress = maxProgress;
        this.baseChance = baseChance;
        this.scaleMultiplier = scaleMultiplier;
        this.healthMultiplier = healthMultiplier;
    }

    public boolean isMassive() {
        return this.ordinal() >= HUGE.ordinal();
    }

    public double rollProgress(double random) {
        return minProgress + ((maxProgress - minProgress) * Math.max(0.0D, Math.min(1.0D, random)));
    }

    public double getMinProgress() { return minProgress; }
    public double getMaxProgress() { return maxProgress; }
    public double getBaseChance() { return baseChance; }
    public double getScaleMultiplier() { return scaleMultiplier; }
    public double getHealthMultiplier() { return healthMultiplier; }
}
