package com.animeki.client.camera;

import com.animeki.client.ClientState;
import com.animeki.config.AnimeKiClientConfig;
import com.animeki.vfx.CameraEffectType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Local camera feedback: shake, fov punch, zoom, impulse and flash.
 *
 * <p>Completely client side and per player - no camera value ever travels over the network, only the
 * request to play an effect does. Amplitudes are config driven and can be scaled down or disabled
 * for motion sensitivity.</p>
 *
 * <p>Slow motion note: this class never touches the client tick rate. A cinematic feel is produced
 * with fov narrowing, reduced shake and a vignette instead, which keeps the client in sync with the
 * server - real time dilation would desync movement and is intentionally not used.</p>
 */
public final class CameraManager {
    private static final List<Effect> EFFECTS = new ArrayList<>();

    private static float shake;
    private static float fovMultiplier = 1.0F;
    private static float flash;
    private static float vignette;
    private static float impulseYaw;
    private static float impulsePitch;
    private static float time;

    /** One running camera effect; mutable so the list can be walked without reallocation. */
    private static final class Effect {
        private final CameraEffectType type;
        private final float intensity;
        private final int duration;
        private int age;

        private Effect(CameraEffectType type, float intensity, int duration) {
            this.type = type;
            this.intensity = intensity;
            this.duration = duration;
        }
    }

    private CameraManager() {
    }

    public static void add(CameraEffectType type, float intensity, int duration, int seed) {
        float scale = AnimeKiClientConfig.CAMERA.screenShakeScale.get().floatValue();
        int cappedDuration = Math.min(duration, 200);
        float value = Math.max(0.0F, intensity);
        switch (type) {
            case SHAKE -> {
                if (!AnimeKiClientConfig.CAMERA.screenShakeEnabled.get()) {
                    return;
                }
                value *= scale;
            }
            case IMPULSE -> value *= scale;
            case FLASH -> {
                if (!AnimeKiClientConfig.CAMERA.flashEnabled.get()) {
                    return;
                }
            }
            case FOV_PUNCH, ZOOM -> {
                if (!AnimeKiClientConfig.CAMERA.fovEffects.get()) {
                    return;
                }
            }
        }
        // Very small fov punches are the "hit zoom" flavour and can be turned off separately.
        if (type == CameraEffectType.FOV_PUNCH && value < 0.12F && !AnimeKiClientConfig.CAMERA.hitZoom.get()) {
            return;
        }
        EFFECTS.add(new Effect(type, value, Math.max(1, cappedDuration)));
    }

    /** A tiny shake used by melee hits so every impact reads on screen. */
    public static void punch(float intensity, int duration) {
        add(CameraEffectType.SHAKE, intensity, duration, 0);
    }

    public static void tick() {
        time += 1.0F;
        float shakeSum = 0.0F;
        float flashMax = 0.0F;
        float fovFactor = 1.0F;
        float yawImpulse = 0.0F;
        float pitchImpulse = 0.0F;

        Iterator<Effect> iterator = EFFECTS.iterator();
        while (iterator.hasNext()) {
            Effect effect = iterator.next();
            if (effect.age > effect.duration) {
                iterator.remove();
                continue;
            }
            float progress = effect.age / (float) effect.duration;
            float falloff = (1.0F - progress) * (1.0F - progress);
            switch (effect.type) {
                case SHAKE -> shakeSum += effect.intensity * falloff;
                case FOV_PUNCH -> {
                    float curve = progress < 0.25F ? progress / 0.25F : (1.0F - progress) / 0.75F;
                    fovFactor += effect.intensity * Math.max(0.0F, curve);
                }
                case ZOOM -> fovFactor -= effect.intensity * falloff;
                case FLASH -> flashMax = Math.max(flashMax, effect.intensity * falloff);
                case IMPULSE -> {
                    float wobble = Mth.sin(progress * (float) Math.PI) * effect.intensity;
                    yawImpulse += wobble * 6.0F;
                    pitchImpulse += wobble * 3.0F;
                }
            }
            effect.age++;
        }

        shake = shakeSum;
        flash = flashMax;
        fovMultiplier = fovFactor;
        impulseYaw = yawImpulse;
        impulsePitch = pitchImpulse;

        // Sustained flight / charge feedback: a subtle framing change instead of a discrete effect.
        float surge = ClientState.charging ? 0.03F : 0.0F;
        if (ClientState.flying) {
            surge += (float) (ClientState.boosting ? 0.06D : 0.03D) * (ClientState.flightBoostMultiplier > 0.0D ? 1.0F : 0.0F);
        }
        if (surge > 0.0F && AnimeKiClientConfig.CAMERA.fovEffects.get()) {
            fovMultiplier += surge;
        }
        vignette = Math.min(0.35F, (ClientState.charging ? 0.18F : 0.0F) + flash * 0.4F);
    }

    public static void applyAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shake <= 0.001F && impulseYaw == 0.0F && impulsePitch == 0.0F) {
            return;
        }
        float seed = time * 1.7F;
        float yaw = Mth.sin(seed * 3.1F) * shake * 0.9F + Mth.sin(seed * 7.3F) * shake * 0.4F;
        float pitch = Mth.cos(seed * 2.6F) * shake * 0.7F + Mth.cos(seed * 6.1F) * shake * 0.3F;
        float roll = Mth.sin(seed * 4.4F) * shake * 1.6F;
        event.setYaw(event.getYaw() + yaw + impulseYaw);
        event.setPitch(event.getPitch() + pitch + impulsePitch);
        event.setRoll(event.getRoll() + roll);
    }

    public static void applyFov(ViewportEvent.ComputeFov event) {
        if (fovMultiplier == 1.0F) {
            return;
        }
        event.setFOV(event.getFOV() * fovMultiplier);
    }

    public static float flash() {
        return flash;
    }

    public static float vignette() {
        return vignette;
    }

    public static boolean isShaking() {
        return shake > 0.01F;
    }

    public static float shakeAmount() {
        return shake;
    }

    public static void clear() {
        EFFECTS.clear();
        shake = 0.0F;
        flash = 0.0F;
        vignette = 0.0F;
        fovMultiplier = 1.0F;
        impulseYaw = 0.0F;
        impulsePitch = 0.0F;
    }
}
