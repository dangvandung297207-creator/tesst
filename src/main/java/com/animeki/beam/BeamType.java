package com.animeki.beam;

/**
 * Data driven description of a beam variant.
 *
 * <p>Registering a new beam type (a new character, a boss attack, a charged variant) is a single
 * entry - the collision, clash and rendering code only ever looks at these values.</p>
 *
 * @param id            unique id
 * @param coreColor     packed ARGB of the hot core
 * @param glowColor     packed ARGB of the surrounding glow
 * @param width         collision and render radius in blocks
 * @param damagePerTick damage applied per damage interval
 * @param kiDrainPerTick Ki consumed per tick while the beam is alive
 * @param range         maximum length in blocks
 * @param clashWeight   relative power in a beam versus beam clash
 * @param destructive   whether the beam erodes terrain at its impact point
 */
public record BeamType(
        String id,
        int coreColor,
        int glowColor,
        double width,
        double damagePerTick,
        double kiDrainPerTick,
        double range,
        double clashWeight,
        boolean destructive) {

    public double power(double outputMultiplier) {
        return clashWeight * damagePerTick * Math.max(0.2D, outputMultiplier);
    }
}
