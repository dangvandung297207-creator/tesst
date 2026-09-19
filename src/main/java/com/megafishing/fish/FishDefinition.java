package com.megafishing.fish;

import org.bukkit.Material;

public class FishDefinition {
    private final String id;
    private final String displayName;
    private final FishRarity rarity;
    private final Material displayMaterial;
    private final double minWeight;
    private final double maxWeight;
    private final double maxHealth;
    private final double pullPower;
    private final double swimSpeed;
    private final double sellMultiplier;
    private final double baseValue;
    private final double directionChangeMin;
    private final double directionChangeMax;
    private final boolean boss;

    public FishDefinition(String id, String displayName, FishRarity rarity, Material displayMaterial, double minWeight,
                          double maxWeight, double maxHealth, double pullPower, double swimSpeed, double sellMultiplier,
                          double baseValue, double directionChangeMin, double directionChangeMax, boolean boss) {
        this.id = id;
        this.displayName = displayName;
        this.rarity = rarity;
        this.displayMaterial = displayMaterial;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
        this.maxHealth = maxHealth;
        this.pullPower = pullPower;
        this.swimSpeed = swimSpeed;
        this.sellMultiplier = sellMultiplier;
        this.baseValue = baseValue;
        this.directionChangeMin = directionChangeMin;
        this.directionChangeMax = directionChangeMax;
        this.boss = boss;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public FishRarity getRarity() { return rarity; }
    public Material getDisplayMaterial() { return displayMaterial; }
    public double getMinWeight() { return minWeight; }
    public double getMaxWeight() { return maxWeight; }
    public double getMaxHealth() { return maxHealth; }
    public double getPullPower() { return pullPower; }
    public double getSwimSpeed() { return swimSpeed; }
    public double getSellMultiplier() { return sellMultiplier; }
    public double getBaseValue() { return baseValue; }
    public double getDirectionChangeMin() { return directionChangeMin; }
    public double getDirectionChangeMax() { return directionChangeMax; }
    public boolean isBoss() { return boss; }
}
