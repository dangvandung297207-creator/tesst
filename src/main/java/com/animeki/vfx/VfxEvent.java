package com.animeki.vfx;

/**
 * Reusable combat impact events.
 *
 * <p>Abilities never draw anything themselves: they emit one of these events and the client
 * resolves it to a data driven visual (see {@code assets/animeki/vfx/*.json}), optional vanilla
 * particles, a sound and a camera reaction. That keeps the ability code small and lets resource
 * packs replace visuals - including with Blender exported meshes and animations - without a single
 * code change.</p>
 */
public enum VfxEvent {
    LIGHT_HIT("light_hit", 0.30F, 4, 0xFFFFF2B0),
    COMBO_HIT("combo_hit", 0.45F, 6, 0xFFFFD166),
    HEAVY_HIT("heavy_hit", 0.85F, 9, 0xFFFFB347),
    DASH_IMPACT("dash_impact", 0.55F, 7, 0xFF9BE7FF),
    GROUND_IMPACT("ground_impact", 1.10F, 14, 0xFFFFE08A),
    KI_CHARGE_TICK("ki_charge_tick", 0.0F, 0, 0xFFFFFFFF),
    CHARGE_READY("charge_ready", 0.20F, 5, 0xFFFFF8C0),
    KI_BURNOUT("ki_burnout", 0.35F, 8, 0xFFB0B0B0),
    KI_BLAST_FIRE("ki_blast_fire", 0.22F, 4, 0xFFB9E6FF),
    KI_BLAST_IMPACT("ki_blast_impact", 0.60F, 8, 0xFF9FD8FF),
    BEAM_CHARGE("beam_charge", 0.30F, 6, 0xFFCDEBFF),
    BEAM_FIRE("beam_fire", 0.45F, 7, 0xFFA8E0FF),
    BEAM_IMPACT("beam_impact", 0.70F, 10, 0xFFD6F0FF),
    CLASH_SPARK("clash_spark", 0.45F, 6, 0xFFFFFFFF),
    CLASH_BURST("clash_burst", 1.50F, 26, 0xFFFFFFFF),
    TRANSFORMATION_SHOCKWAVE("transformation_shockwave", 1.60F, 30, 0xFFFFFFFF),
    AURA_BURST("aura_burst", 0.90F, 12, 0xFFFFFFFF),
    ULTIMATE_CHARGE("ultimate_charge", 0.50F, 12, 0xFFFFD9A0),
    ULTIMATE_IMPACT("ultimate_impact", 2.40F, 46, 0xFFFFE9B0),
    VANISH("vanish", 0.12F, 4, 0xFFC9B6FF),
    TARGET_MARKER("target_marker", 0.0F, 0, 0xFFFFD166),
    APPEAR("appear", 0.25F, 6, 0xFFE0D4FF),
    DEBRIS("debris", 0.0F, 0, 0xFF8C8C8C),
    SPARK("spark", 0.0F, 0, 0xFFFFFFFF),
    FLIGHT_BOOST("flight_boost", 0.25F, 6, 0xFFCFEFFF),
    SONIC_BOOM("sonic_boom", 0.95F, 12, 0xFFE8F6FF),
    BOSS_ROAR("boss_roar", 1.20F, 16, 0xFF9B5CFF),
    BOSS_PHASE("boss_phase", 1.80F, 30, 0xFFB07CFF);

    private final String id;
    private final float cameraShake;
    private final int cameraShakeDuration;
    private final int defaultColor;

    VfxEvent(String id, float cameraShake, int cameraShakeDuration, int defaultColor) {
        this.id = id;
        this.cameraShake = cameraShake;
        this.cameraShakeDuration = cameraShakeDuration;
        this.defaultColor = defaultColor;
    }

    /** Stable id used by the data driven definition files. */
    public String id() {
        return id;
    }

    public float cameraShake() {
        return cameraShake;
    }

    public int cameraShakeDuration() {
        return cameraShakeDuration;
    }

    public int defaultColor() {
        return defaultColor;
    }

    public static VfxEvent byOrdinal(int ordinal) {
        VfxEvent[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return SPARK;
        }
        return values[ordinal];
    }
}
