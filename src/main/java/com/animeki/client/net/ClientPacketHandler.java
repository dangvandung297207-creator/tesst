package com.animeki.client.net;

import com.animeki.client.ClientState;
import com.animeki.client.camera.CameraManager;
import com.animeki.client.vfx.ClientVfx;
import com.animeki.network.Payloads;
import com.animeki.vfx.CameraEffectType;
import com.animeki.vfx.VfxEvent;
import net.minecraft.client.Minecraft;

/**
 * Turns server packets into client state, particles and camera feedback.
 *
 * <p>Runs on the client thread only (the bridge class guards the dist check), so nothing here has to
 * worry about threads - but it also never makes gameplay decisions.</p>
 */
public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handleKiState(Payloads.KiState payload) {
        ClientState.handleKiState(payload.current(), payload.max(), payload.charging(), payload.exhausted(),
                payload.chargeTicks(), payload.transformationId(), payload.transformationPhase(),
                payload.auraIntensity(), payload.flying(), payload.boosting(), payload.comboStep(),
                payload.comboHits(), payload.cooldowns(), payload.activeAbility(), payload.abilityProgress(),
                payload.feedback(), payload.feedbackAbility(), gameTime());
    }

    public static void handleVisualState(Payloads.VisualState payload) {
        ClientState.handleVisualState(payload.entityId(), payload.transformationId(), payload.phase(),
                payload.auraIntensity(), payload.charging(), payload.flying(), payload.boosting(), payload.comboStep());
    }

    public static void handleVfx(Payloads.Vfx payload) {
        ClientVfx.spawn(payload);
    }

    public static void handleCameraEffect(Payloads.CameraEffect payload) {
        CameraManager.add(CameraEffectType.byOrdinal(payload.type()), payload.intensity(), payload.duration(),
                payload.seed());
    }

    public static void handleBeamSync(Payloads.BeamSync payload) {
        ClientState.BEAMS.forEach(beam -> beam.seen = false);
        for (Payloads.BeamSync.BeamState state : payload.beams()) {
            ClientState.BeamView view = null;
            for (ClientState.BeamView candidate : ClientState.BEAMS) {
                if (candidate.id == state.id()) {
                    view = candidate;
                    break;
                }
            }
            if (view == null) {
                view = new ClientState.BeamView(state.id(), state.ownerId(), state.typeId());
                ClientState.BEAMS.add(view);
            }
            view.update(state.originX(), state.originY(), state.originZ(), state.dirX(), state.dirY(), state.dirZ(),
                    state.length(), state.width(), state.coreColor(), state.glowColor(), state.power(),
                    state.clashRatio(), state.clashX(), state.clashY(), state.clashZ(), state.clashPartner());
        }
        // Beams that were missing from the snapshot have ended: let them fade out.
        ClientState.BEAMS.removeIf(beam -> !beam.seen);
    }

    public static void handleAbilityFeedback(Payloads.AbilityFeedback payload) {
        ClientState.feedback = payload.reason();
        ClientState.feedbackAbility = payload.abilityId();
        ClientState.lastFeedbackTick = gameTime();
        if (payload.reason() > 0) {
            ClientVfx.feedbackPing();
        }
    }

    public static void handleBossState(Payloads.BossState payload) {
        ClientState.bossEntityId = payload.entityId();
        ClientState.bossName = payload.nameKey();
        ClientState.bossHealth = payload.health();
        ClientState.bossMaxHealth = payload.maxHealth();
        ClientState.bossPhase = payload.phase();
        ClientState.bossRaging = payload.raging();
    }

    public static void handleFlightParams(Payloads.FlightParams payload) {
        // Authoritative movement parameters: the client only uses them for smooth prediction.
        ClientState.flightSpeed = payload.speed();
        ClientState.flightBoostMultiplier = payload.boostMultiplier();
        ClientState.flightVerticalSpeed = payload.verticalSpeed();
        ClientState.flightEnabled = payload.enabled();
    }

    /** Local game time used for HUD pulses. */
    private static long gameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }

    /** Convenience used by the debug overlay. */
    public static String describeVfx(VfxEvent event) {
        return event.id();
    }
}
