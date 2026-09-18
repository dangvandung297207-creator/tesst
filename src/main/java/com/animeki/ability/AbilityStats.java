package com.animeki.ability;

/**
 * Balance data of an ability.
 *
 * <p>Values are supplied by the configuration at bootstrap time, never hardcoded inside the ability
 * implementation itself.</p>
 */
public record AbilityStats(
        double kiCost,
        int cooldownTicks,
        int castTicks,
        int chargeTicks,
        double range,
        double damage,
        CastType castType) {

    public static Builder builder() {
        return new Builder();
    }

    /** How the ability consumes its input. */
    public enum CastType {
        /** Fires immediately on press. */
        INSTANT,
        /** Charges while held and fires on release. */
        CHARGE_RELEASE,
        /** Runs continuously while held (beam style). */
        CHANNEL,
        /** Scripted sequence driven by the ability itself. */
        SCRIPTED
    }

    public static final class Builder {
        private double kiCost;
        private int cooldownTicks;
        private int castTicks;
        private int chargeTicks;
        private double range;
        private double damage;
        private CastType castType = CastType.INSTANT;

        public Builder kiCost(double value) {
            this.kiCost = value;
            return this;
        }

        public Builder cooldown(int ticks) {
            this.cooldownTicks = ticks;
            return this;
        }

        public Builder castTicks(int ticks) {
            this.castTicks = ticks;
            return this;
        }

        public Builder chargeTicks(int ticks) {
            this.chargeTicks = ticks;
            return this;
        }

        public Builder range(double value) {
            this.range = value;
            return this;
        }

        public Builder damage(double value) {
            this.damage = value;
            return this;
        }

        public Builder castType(CastType value) {
            this.castType = value;
            return this;
        }

        public AbilityStats build() {
            return new AbilityStats(kiCost, cooldownTicks, castTicks, chargeTicks, range, damage, castType);
        }
    }
}
