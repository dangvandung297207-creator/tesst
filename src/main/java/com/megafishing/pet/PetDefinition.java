package com.megafishing.pet;

public class PetDefinition {
    private final String id;
    private final String displayName;
    private final double sellMultiplier;
    private final long price;
    private final double encounterRate;
    private final double biteSpeed;
    private final double rarityLuck;
    private final double largeFishChance;

    public PetDefinition(String id, String displayName, double sellMultiplier, long price,
                         double encounterRate, double biteSpeed, double rarityLuck, double largeFishChance) {
        this.id = id;
        this.displayName = displayName;
        this.sellMultiplier = sellMultiplier;
        this.price = price;
        this.encounterRate = encounterRate;
        this.biteSpeed = biteSpeed;
        this.rarityLuck = rarityLuck;
        this.largeFishChance = largeFishChance;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public double getSellMultiplier() { return sellMultiplier; }
    public long getPrice() { return price; }
    public double getEncounterRate() { return encounterRate; }
    public double getBiteSpeed() { return biteSpeed; }
    public double getRarityLuck() { return rarityLuck; }
    public double getLargeFishChance() { return largeFishChance; }
}
