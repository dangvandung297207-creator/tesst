package com.animeki.boss;

import com.animeki.config.AnimeKiServerConfig;

/**
 * The moves a boss can choose from.
 *
 * <p>Each entry declares the earliest phase that unlocks it, its base cooldown and a selection
 * weight, so the controller stays generic: new bosses (and new moves) plug in without touching the
 * state machine.</p>
 */
public enum BossAbility {
    /** Three hit melee chain with knockback. */
    MELEE_COMBO(0, 40, 3),
    /** Leaping dash that craters the ground. */
    DASH_SLAM(0, 150, 2),
    /** Shockwave punch that knocks nearby fighters into the air. */
    GROUND_SLAM(0, 190, 2),
    /** Fan of Ki blasts. */
    KI_VOLLEY(1, 130, 3),
    /** Sweeping beam that follows the target. */
    BEAM_SWEEP(1, 280, 2),
    /** Short range teleport used to close distance or escape. */
    TELEPORT(1, 220, 1),
    /** Phase transition shockwave, used when a new phase starts. */
    AURA_BURST(1, 260, 1),
    /** The Void Titan's ultimate: rise, charge, descend, annihilate. */
    ULTIMATE(2, 420, 2);

    private final int minPhase;
    private final int baseCooldown;
    private final int weight;

    BossAbility(int minPhase, int baseCooldown, int weight) {
        this.minPhase = minPhase;
        this.baseCooldown = baseCooldown;
        this.weight = weight;
    }

    public int minPhaseIndex() {
        return minPhase;
    }

    public int weight() {
        return weight;
    }

    /** Cooldown in ticks, scaled by the difficulty config and the current phase. */
    public int cooldownTicks(BossPhase phase) {
        double multiplier = AnimeKiServerConfig.BOSS.abilityCooldownMultiplier.get();
        double phaseFactor = 1.0D / Math.max(0.2D, phase.speedMultiplier());
        return (int) Math.max(10.0D, baseCooldown * multiplier * phaseFactor);
    }

    public boolean available(BossPhase phase) {
        return phase.index() >= minPhase;
    }
}
