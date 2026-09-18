package com.animeki.client;

import com.animeki.vfx.VfxEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Everything the client knows about the mod, refreshed by server packets.
 *
 * <p>This is a pure cache: it never decides gameplay, it only feeds the HUD, camera and renderers.
 * Keeping it in one place means a resource pack author can inspect exactly what the client is told
 * (and nothing more) - the server is the only source of truth.</p>
 */
public final class ClientState {
    // ------------------------------------------------------------------ local player
    public static double ki = 0.0D;
    public static double maxKi = 100.0D;
    public static boolean charging;
    public static boolean exhausted;
    public static int chargeTicks;
    public static String transformationId = "";
    public static byte transformationPhase;
    public static float auraIntensity;
    public static boolean flying;
    public static boolean boosting;
    public static int comboStep;
    public static int comboHits;
    public static String activeAbility = "";
    public static float abilityProgress;
    public static byte feedback;
    public static String feedbackAbility = "";
    /** Tick at which the last HUD feedback arrived, used for the "not enough Ki" pulse. */
    public static long lastFeedbackTick;
    public static int[] cooldowns = new int[0];

    // ------------------------------------------------------------------ boss overlay cache
    public static int bossEntityId = -1;
    public static String bossName = "";
    public static float bossHealth;
    public static float bossMaxHealth;
    public static int bossPhase;
    public static boolean bossRaging;

    // ------------------------------------------------------------------ authoritative flight params
    public static double flightSpeed = 0.6D;
    public static double flightBoostMultiplier = 1.6D;
    public static double flightVerticalSpeed = 0.45D;
    public static boolean flightEnabled;

    // ------------------------------------------------------------------ other players
    private static final Map<Integer, VisualState> VISUALS = new HashMap<>();

    /** A synced appearance for one entity. */
    public record VisualState(String transformationId, byte phase, float auraIntensity, boolean charging,
                              boolean flying, boolean boosting, int comboStep) {
        public boolean transformed() {
            return !transformationId.isEmpty() && phase > 0;
        }
    }

    // ------------------------------------------------------------------ beams
    public static final List<BeamView> BEAMS = new ArrayList<>();

    /** A beam as currently drawn, interpolated from the last two snapshots. */
    public static final class BeamView {
        public final int id;
        public final int ownerId;
        public final String typeId;
        public double originX;
        public double originY;
        public double originZ;
        public float dirX = 1.0F;
        public float dirY;
        public float dirZ;
        public float length;
        public float previousLength;
        public float width = 0.6F;
        public int coreColor = 0xFFFFFFFF;
        public int glowColor = 0x88FFFFFF;
        public float power = 1.0F;
        public float clashRatio;
        public double clashX;
        public double clashY;
        public double clashZ;
        public int clashPartner = -1;
        public boolean seen;

        public BeamView(int id, int ownerId, String typeId) {
            this.id = id;
            this.ownerId = ownerId;
            this.typeId = typeId;
        }

        public void update(double ox, double oy, double oz, float dx, float dy, float dz, float newLength,
                           float newWidth, int newCore, int newGlow, float newPower, float newClashRatio,
                           double cx, double cy, double cz, int partner) {
            double moved = Math.abs(ox - originX) + Math.abs(oy - originY) + Math.abs(oz - originZ);
            if (moved > 0.05D) {
                // The emitter jumped (teleport / new tick): do not interpolate across the gap.
                previousLength = newLength;
            } else {
                previousLength = length;
            }
            this.originX = ox;
            this.originY = oy;
            this.originZ = oz;
            this.dirX = dx;
            this.dirY = dy;
            this.dirZ = dz;
            this.length = newLength;
            this.width = newWidth;
            this.coreColor = newCore;
            this.glowColor = newGlow;
            this.power = newPower;
            this.clashRatio = newClashRatio;
            this.clashX = cx;
            this.clashY = cy;
            this.clashZ = cz;
            this.clashPartner = partner;
            this.seen = true;
        }

        /** Beam length for the current frame; the client interpolates between snapshots. */
        public float renderLength(float partialTick) {
            return previousLength + (length - previousLength) * partialTick;
        }
    }

    // ------------------------------------------------------------------ transient vfx
    /** Client side visual effects queued by packets; consumed by the renderers. */
    public static final List<ActiveVfx> ACTIVE_VFX = new ArrayList<>();

    /** One vfx instance with the data needed to draw and animate it. */
    public static final class ActiveVfx {
        public final VfxEvent event;
        public final double x;
        public final double y;
        public final double z;
        public final float dx;
        public final float dy;
        public final float dz;
        public final float scale;
        public final int color;
        public final int duration;
        public final int entityId;
        public int age;

        public ActiveVfx(VfxEvent event, double x, double y, double z, float dx, float dy, float dz, float scale,
                         int color, int duration, int entityId) {
            this.event = event;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.scale = scale;
            this.color = color;
            this.duration = duration;
            this.entityId = entityId;
        }

        public float progress() {
            return duration <= 0 ? 1.0F : Math.min(1.0F, age / (float) duration);
        }

        public boolean finished() {
            return duration > 0 && age >= duration + 20;
        }
    }

    private ClientState() {
    }

    // ------------------------------------------------------------------ accessors

    public static void handleKiState(double newKi, double newMax, boolean newCharging, boolean newExhausted,
                                     int newChargeTicks, String newTransformation, byte newPhase, float newAura,
                                     boolean newFlying, boolean newBoosting, int newComboStep, int newComboHits,
                                     int[] newCooldowns, String newActiveAbility, float newAbilityProgress,
                                     byte newFeedback, String newFeedbackAbility, long gameTime) {
        ki = newKi;
        maxKi = Math.max(1.0E-3D, newMax);
        charging = newCharging;
        exhausted = newExhausted;
        chargeTicks = newChargeTicks;
        transformationId = newTransformation == null ? "" : newTransformation;
        transformationPhase = newPhase;
        auraIntensity = newAura;
        flying = newFlying;
        boosting = newBoosting;
        comboStep = newComboStep;
        comboHits = newComboHits;
        cooldowns = newCooldowns == null ? new int[0] : newCooldowns;
        activeAbility = newActiveAbility == null ? "" : newActiveAbility;
        abilityProgress = newAbilityProgress;
        feedback = newFeedback;
        feedbackAbility = newFeedbackAbility == null ? "" : newFeedbackAbility;
        lastFeedbackTick = gameTime;
    }

    public static void handleVisualState(int entityId, String transformation, byte phase, float aura, boolean charging,
                                         boolean flying, boolean boosting, int comboStep) {
        VISUALS.put(entityId, new VisualState(transformation == null ? "" : transformation, phase, aura, charging,
                flying, boosting, comboStep));
    }

    @Nullable
    public static VisualState visual(int entityId) {
        return VISUALS.get(entityId);
    }

    public static void clear() {
        VISUALS.clear();
        BEAMS.clear();
        ACTIVE_VFX.clear();
        ki = 0.0D;
        charging = false;
        exhausted = false;
        transformationId = "";
        auraIntensity = 0.0F;
        activeAbility = "";
    }

    /** Ki ratio for the HUD, always in [0, 1]. */
    public static float kiRatio() {
        return (float) Math.max(0.0D, Math.min(1.0D, ki / maxKi));
    }
}
