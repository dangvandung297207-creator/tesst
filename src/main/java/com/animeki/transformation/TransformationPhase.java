package com.animeki.transformation;

/**
 * Cinematic transformation sequence.
 *
 * <pre>
 * IDLE -> CHARGING -> ENERGY_BUILDUP -> TRANSFORMATION -> SHOCKWAVE -> ACTIVE -> EXITING -> IDLE
 * </pre>
 *
 * <p>Every phase has its own duration (configurable per form), its own visuals and its own movement
 * restrictions, so a transformation reads as an event instead of a boolean toggle.</p>
 */
public enum TransformationPhase {
    NONE,
    CHARGING,
    ENERGY_BUILDUP,
    TRANSFORMATION,
    SHOCKWAVE,
    ACTIVE,
    EXITING;

    public boolean locksMovement() {
        return this == CHARGING || this == ENERGY_BUILDUP || this == TRANSFORMATION || this == SHOCKWAVE;
    }

    public boolean isRunning() {
        return this != NONE && this != ACTIVE;
    }
}
