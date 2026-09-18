package com.animeki.ability;

/** Why an ability stopped running. Passed to {@link Ability#onStop}. */
public enum AbilityStopReason {
    COMPLETED,
    RELEASED,
    CANCELLED,
    INTERRUPTED,
    KI_DEPLETED,
    DEATH,
    LOGOUT,
    REPLACED
}
