package com.megafishing.rod;

public class RodDefinition {
    private final String id;
    private final String displayName;
    private final long price;
    private final double dps;
    private final double tensionResistance;
    private final double castRange;
    private final String skillId;
    private final int skillCooldown;

    public RodDefinition(String id, String displayName, long price, double dps, double tensionResistance, double castRange, String skillId, int skillCooldown) {
        this.id = id;
        this.displayName = displayName;
        this.price = price;
        this.dps = dps;
        this.tensionResistance = tensionResistance;
        this.castRange = castRange;
        this.skillId = skillId;
        this.skillCooldown = skillCooldown;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public long getPrice() { return price; }
    public double getDps() { return dps; }
    public double getTensionResistance() { return tensionResistance; }
    public double getCastRange() { return castRange; }
    public String getSkillId() { return skillId; }
    public int getSkillCooldown() { return skillCooldown; }
}
