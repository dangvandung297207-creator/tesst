package com.animeki.ability;

/** Semantic input actions sent by the client. The server maps them to abilities. */
public enum AbilityAction {
    KI_CHARGE_START,
    KI_CHARGE_STOP,
    KI_BLAST_START,
    KI_BLAST_RELEASE,
    PRIMARY_START,
    PRIMARY_RELEASE,
    SECONDARY_START,
    SECONDARY_RELEASE,
    ULTIMATE_START,
    ULTIMATE_RELEASE,
    DASH,
    TRANSFORM,
    FLIGHT_TOGGLE,
    CANCEL;

    public static AbilityAction byOrdinal(int ordinal) {
        AbilityAction[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return CANCEL;
        }
        return values[ordinal];
    }
}
