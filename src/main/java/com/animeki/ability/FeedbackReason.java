package com.animeki.ability;

/** Feedback codes sent to the client so the HUD can explain why a skill refused to fire. */
public enum FeedbackReason {
    NONE,
    NOT_ENOUGH_KI,
    ON_COOLDOWN,
    EXHAUSTED,
    REFUSED,
    DISABLED,
    MISSING_TARGET,
    BUSY;

    public static FeedbackReason byOrdinal(int ordinal) {
        FeedbackReason[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return NONE;
        }
        return values[ordinal];
    }
}
