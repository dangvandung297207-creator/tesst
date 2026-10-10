package com.omnitrix.mod.alien;

import net.minecraft.network.chat.Component;

public enum AlienForm {
    NONE("none", "None", 0x888888, 100, 20.0, 1.0, 0.1, 0.0, 0.0),
    HEATBLAST("heatblast", "Heatblast", 0xFF6600, 100, 24.0, 1.0, 0.1, 4.0, 1.0),
    XLR8("xlr8", "XLR8", 0x00E5FF, 100, 20.0, 1.0, 0.28, 2.0, 0.0),
    FOUR_ARMS("four_arms", "Four Arms", 0xCC1111, 100, 32.0, 1.25, 0.09, 8.0, 4.0),
    DIAMONDHEAD("diamondhead", "Diamondhead", 0x00FFB3, 100, 26.0, 1.05, 0.095, 5.0, 6.0),
    CANNONBOLT("cannonbolt", "Cannonbolt", 0xFFDD33, 100, 28.0, 0.9, 0.12, 6.0, 8.0),
    UPGRADE("upgrade", "Upgrade", 0x33FF33, 100, 22.0, 1.0, 0.11, 3.5, 2.0),
    STINKFLY("stinkfly", "Stinkfly", 0x88CC00, 100, 20.0, 1.0, 0.12, 2.5, 0.0),
    WILDMUTT("wildmutt", "Wildmutt", 0xFF8800, 100, 24.0, 0.85, 0.16, 5.0, 2.0);

    private final String id;
    private final String displayName;
    private final int themeColor;
    private final int maxEnergy;
    private final double maxHealth;
    private final double modelScale;
    private final double movementSpeed;
    private final double attackDamage;
    private final double armor;

    AlienForm(String id, String displayName, int themeColor, int maxEnergy, double maxHealth, double modelScale, double movementSpeed, double attackDamage, double armor) {
        this.id = id;
        this.displayName = displayName;
        this.themeColor = themeColor;
        this.maxEnergy = maxEnergy;
        this.maxHealth = maxHealth;
        this.modelScale = modelScale;
        this.movementSpeed = movementSpeed;
        this.attackDamage = attackDamage;
        this.armor = armor;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getThemeColor() {
        return themeColor;
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getModelScale() {
        return modelScale;
    }

    public double getMovementSpeed() {
        return movementSpeed;
    }

    public double getAttackDamage() {
        return attackDamage;
    }

    public double getArmor() {
        return armor;
    }

    public boolean isAlien() {
        return this != NONE;
    }

    public Component getTranslation() {
        return Component.translatable("alien.omnitrix." + id);
    }

    public static AlienForm fromId(String id) {
        if (id == null) return NONE;
        for (AlienForm form : values()) {
            if (form.id.equalsIgnoreCase(id) || form.name().equalsIgnoreCase(id)) {
                return form;
            }
        }
        return NONE;
    }

    public static final AlienForm[] PLAYABLE_ALIENS = new AlienForm[] {
        HEATBLAST,
        XLR8,
        FOUR_ARMS,
        DIAMONDHEAD,
        CANNONBOLT,
        UPGRADE,
        STINKFLY,
        WILDMUTT
    };
}
