package com.megafishing.pet;

public class PetDefinition {
    private final String id;
    private final String displayName;
    private final double sellMultiplier;
    private final long price;

    public PetDefinition(String id, String displayName, double sellMultiplier, long price) {
        this.id = id;
        this.displayName = displayName;
        this.sellMultiplier = sellMultiplier;
        this.price = price;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public double getSellMultiplier() { return sellMultiplier; }
    public long getPrice() { return price; }
}
