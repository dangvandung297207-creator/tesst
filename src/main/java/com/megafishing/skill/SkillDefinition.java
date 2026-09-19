package com.megafishing.skill;

public class SkillDefinition {
    private final String id;
    private final String displayName;
    private final int cooldownSeconds;
    private final double damageMultiplier;
    private final double tensionReduction;

    public SkillDefinition(String id, String displayName, int cooldownSeconds, double damageMultiplier, double tensionReduction) {
        this.id = id;
        this.displayName = displayName;
        this.cooldownSeconds = cooldownSeconds;
        this.damageMultiplier = damageMultiplier;
        this.tensionReduction = tensionReduction;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public double getTensionReduction() { return tensionReduction; }
}
