package com.animeki.vfx;

/**
 * Camera feedback kinds. Handled entirely by the local client (see {@code client.camera}).</p>
 */
public enum CameraEffectType {
    /** Rotational noise shake with intensity falloff. */
    SHAKE,
    /** Full screen colour flash. */
    FLASH,
    /** Short field of view punch used for impacts. */
    FOV_PUNCH,
    /** Sustained field of view change (flight speed, ultimate charge). */
    ZOOM,
    /** Directional impulse applied to the camera angle. */
    IMPULSE;

    public static CameraEffectType byOrdinal(int ordinal) {
        CameraEffectType[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return SHAKE;
        }
        return values[ordinal];
    }
}
