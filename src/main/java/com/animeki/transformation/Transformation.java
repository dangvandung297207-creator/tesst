package com.animeki.transformation;

/**
 * Immutable description of a transformation form.
 *
 * <p>A form is pure data: stats, aura description and sequence timings. The sequence itself lives
 * in {@link TransformationService} and is identical for every form, so adding a new
 * transformation is a single registration call.</p>
 */
public final class Transformation {
    private final String id;
    private final String displayKey;
    private final int tier;
    private final double kiMultiplier;
    private final double damageMultiplier;
    private final double defenseMultiplier;
    private final double speedMultiplier;
    private final double flightMultiplier;
    private final double regenerationMultiplier;
    private final double activationKiCost;
    private final double drainPerTick;
    private final double requiredKi;
    private final int durationTicks;
    private final int chargeTicks;
    private final int buildupTicks;
    private final int transformTicks;
    private final int shockwaveTicks;
    private final boolean destructiveActivation;
    private final AuraProfile aura;

    private Transformation(Builder builder) {
        this.id = builder.id;
        this.displayKey = builder.displayKey;
        this.tier = builder.tier;
        this.kiMultiplier = builder.kiMultiplier;
        this.damageMultiplier = builder.damageMultiplier;
        this.defenseMultiplier = builder.defenseMultiplier;
        this.speedMultiplier = builder.speedMultiplier;
        this.flightMultiplier = builder.flightMultiplier;
        this.regenerationMultiplier = builder.regenerationMultiplier;
        this.activationKiCost = builder.activationKiCost;
        this.drainPerTick = builder.drainPerTick;
        this.requiredKi = builder.requiredKi;
        this.durationTicks = builder.durationTicks;
        this.chargeTicks = builder.chargeTicks;
        this.buildupTicks = builder.buildupTicks;
        this.transformTicks = builder.transformTicks;
        this.shockwaveTicks = builder.shockwaveTicks;
        this.destructiveActivation = builder.destructiveActivation;
        this.aura = builder.aura;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() {
        return id;
    }

    public String displayKey() {
        return displayKey;
    }

    public int tier() {
        return tier;
    }

    public double kiMultiplier() {
        return kiMultiplier;
    }

    public double damageMultiplier() {
        return damageMultiplier;
    }

    public double defenseMultiplier() {
        return defenseMultiplier;
    }

    public double speedMultiplier() {
        return speedMultiplier;
    }

    public double flightMultiplier() {
        return flightMultiplier;
    }

    public double regenerationMultiplier() {
        return regenerationMultiplier;
    }

    public double activationKiCost() {
        return activationKiCost;
    }

    public double drainPerTick() {
        return drainPerTick;
    }

    public double requiredKi() {
        return requiredKi;
    }

    /** {@code 0} means the form lasts until the player cancels or runs out of Ki. */
    public int durationTicks() {
        return durationTicks;
    }

    public int chargeTicks() {
        return chargeTicks;
    }

    public int buildupTicks() {
        return buildupTicks;
    }

    public int transformTicks() {
        return transformTicks;
    }

    public int shockwaveTicks() {
        return shockwaveTicks;
    }

    public boolean destructiveActivation() {
        return destructiveActivation;
    }

    public AuraProfile aura() {
        return aura;
    }

    /** Duration of a sequence phase. */
    public int phaseDuration(TransformationPhase phase) {
        return switch (phase) {
            case CHARGING -> chargeTicks;
            case ENERGY_BUILDUP -> buildupTicks;
            case TRANSFORMATION -> transformTicks;
            case SHOCKWAVE -> shockwaveTicks;
            default -> 0;
        };
    }

    /** Fluent builder used by the registry when it turns configuration values into a form. */
    public static final class Builder {
        private final String id;
        private String displayKey;
        private int tier = 1;
        private double kiMultiplier = 1.2D;
        private double damageMultiplier = 1.2D;
        private double defenseMultiplier = 1.1D;
        private double speedMultiplier = 1.1D;
        private double flightMultiplier = 1.15D;
        private double regenerationMultiplier = 1.2D;
        private double activationKiCost = 100.0D;
        private double drainPerTick = 0.3D;
        private double requiredKi = 150.0D;
        private int durationTicks;
        private int chargeTicks = 20;
        private int buildupTicks = 40;
        private int transformTicks = 20;
        private int shockwaveTicks = 10;
        private boolean destructiveActivation;
        private AuraProfile aura = AuraProfile.builder().build();

        private Builder(String id) {
            this.id = id;
            this.displayKey = "animeki.transformation." + id;
        }

        public Builder displayKey(String key) {
            this.displayKey = key;
            return this;
        }

        public Builder tier(int value) {
            this.tier = value;
            return this;
        }

        public Builder multipliers(double ki, double damage, double defense, double speed, double flight, double regeneration) {
            this.kiMultiplier = ki;
            this.damageMultiplier = damage;
            this.defenseMultiplier = defense;
            this.speedMultiplier = speed;
            this.flightMultiplier = flight;
            this.regenerationMultiplier = regeneration;
            return this;
        }

        public Builder cost(double activationCost, double requiredKi, double drainPerTick, int durationTicks) {
            this.activationKiCost = activationCost;
            this.requiredKi = requiredKi;
            this.drainPerTick = drainPerTick;
            this.durationTicks = durationTicks;
            return this;
        }

        public Builder sequence(int charge, int buildup, int transform, int shockwave) {
            this.chargeTicks = charge;
            this.buildupTicks = buildup;
            this.transformTicks = transform;
            this.shockwaveTicks = shockwave;
            return this;
        }

        public Builder destructiveActivation(boolean value) {
            this.destructiveActivation = value;
            return this;
        }

        public Builder aura(AuraProfile profile) {
            this.aura = profile;
            return this;
        }

        public Transformation build() {
            return new Transformation(this);
        }
    }
}
