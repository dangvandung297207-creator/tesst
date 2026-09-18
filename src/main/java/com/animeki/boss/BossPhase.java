package com.animeki.boss;

/**
 * Phases shared by every boss.
 *
 * <p>A phase describes how dangerous the boss is: damage, movement, whether it flies and whether
 * its ultimate is unlocked. The thresholds themselves live in the config
 * ({@code boss.phase2Threshold} / {@code boss.phase3Threshold}).</p>
 */
public enum BossPhase {
    /** Grounded brawler: melee combos, dashes and ground slams. */
    PHASE_1(0, 1.0D, 1.0D, false, false, 1.0F),
    /** Awakened: takes to the air, uses Ki volleys and beams. */
    PHASE_2(1, 1.2D, 1.15D, true, false, 1.15F),
    /** Final form: transforms, moves much faster, stronger aura and the ultimate. */
    PHASE_3(2, 1.5D, 1.35D, true, true, 1.35F);

    private final int index;
    private final double damageMultiplier;
    private final double speedMultiplier;
    private final boolean flying;
    private final boolean ultimate;
    private final float auraIntensity;

    BossPhase(int index, double damageMultiplier, double speedMultiplier, boolean flying, boolean ultimate,
              float auraIntensity) {
        this.index = index;
        this.damageMultiplier = damageMultiplier;
        this.speedMultiplier = speedMultiplier;
        this.flying = flying;
        this.ultimate = ultimate;
        this.auraIntensity = auraIntensity;
    }

    public int index() {
        return index;
    }

    public double damageMultiplier() {
        return damageMultiplier;
    }

    public double speedMultiplier() {
        return speedMultiplier;
    }

    public boolean flying() {
        return flying;
    }

    public boolean ultimateUnlocked() {
        return ultimate;
    }

    public float auraIntensity() {
        return auraIntensity;
    }

    public static BossPhase byIndex(int index) {
        BossPhase[] values = values();
        if (index < 0 || index >= values.length) {
            return PHASE_1;
        }
        return values[index];
    }
}
