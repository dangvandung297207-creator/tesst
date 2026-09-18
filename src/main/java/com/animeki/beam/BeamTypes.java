package com.animeki.beam;

import com.animeki.AnimeKi;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;

/** Registry of the shipped beam variants. */
public final class BeamTypes {
    public static final BeamType BASIC = new BeamType("animeki:beam/basic", 0xFFCFF4FF, 0x6699DDFF,
            0.9D, 2.0D, 6.0D, 40.0D, 1.0D, true);
    public static final BeamType CHARGED = new BeamType("animeki:beam/charged", 0xFFFFF7D0, 0x88FFD54A,
            1.35D, 3.2D, 8.0D, 52.0D, 1.6D, true);
    public static final BeamType DIVINE = new BeamType("animeki:beam/divine", 0xFFFFD3E4, 0x99FF4D7E,
            1.25D, 3.0D, 7.5D, 48.0D, 1.9D, true);
    public static final BeamType CELESTIAL = new BeamType("animeki:beam/celestial", 0xFFE8F6FF, 0x9933A8FF,
            1.5D, 3.8D, 9.0D, 58.0D, 2.3D, true);
    public static final BeamType TRANSCENDENT = new BeamType("animeki:beam/transcendent", 0xFFFFFFFF, 0xCCE8E8F0,
            1.7D, 4.4D, 10.0D, 64.0D, 2.8D, true);
    /** Wide defensive field used by the Power Clash ability. */
    public static final BeamType CLASH_FIELD = new BeamType("animeki:beam/clash_field", 0xFFFFF2C0, 0x66FFC447,
            2.1D, 2.6D, 7.0D, 34.0D, 2.2D, false);
    /** The Void Titan's own beam. */
    public static final BeamType VOID = new BeamType("animeki:beam/void", 0xFFE3CCFF, 0x88A15CFF,
            1.6D, 4.0D, 0.0D, 60.0D, 2.6D, true);

    private static final Map<String, BeamType> TYPES = new LinkedHashMap<>();

    private BeamTypes() {
    }

    static {
        register(BASIC);
        register(CHARGED);
        register(DIVINE);
        register(CELESTIAL);
        register(TRANSCENDENT);
        register(CLASH_FIELD);
        register(VOID);
    }

    public static void register(BeamType type) {
        TYPES.put(type.id(), type);
    }

    @Nullable
    public static BeamType byId(String id) {
        return id == null ? null : TYPES.get(id);
    }

    /** Beam variant matching a transformation id, falling back to the basic beam. */
    public static BeamType forTransformation(@Nullable String transformationId) {
        if (transformationId == null || transformationId.isEmpty()) {
            return BASIC;
        }
        return switch (transformationId) {
            case "ascended" -> CHARGED;
            case "divine" -> DIVINE;
            case "celestial" -> CELESTIAL;
            case "transcendent" -> TRANSCENDENT;
            default -> BASIC;
        };
    }

    public static void bootstrap() {
        AnimeKi.LOGGER.debug("Registered {} beam types", TYPES.size());
    }
}
