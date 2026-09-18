package com.animeki.network;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Thin side guard between the shared network layer and the client only handlers.
 *
 * <p>The client handler class is only referenced from inside the dist check, so a dedicated server
 * never loads any rendering code.</p>
 */
final class ClientPayloadBridge {
    private ClientPayloadBridge() {
    }

    static void handle(Payloads.KiState payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.animeki.client.net.ClientPacketHandler.handleKiState(payload);
        }
    }

    static void handle(Payloads.VisualState payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.animeki.client.net.ClientPacketHandler.handleVisualState(payload);
        }
    }

    static void handle(Payloads.Vfx payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.animeki.client.net.ClientPacketHandler.handleVfx(payload);
        }
    }

    static void handle(Payloads.CameraEffect payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.animeki.client.net.ClientPacketHandler.handleCameraEffect(payload);
        }
    }

    static void handle(Payloads.BeamSync payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.animeki.client.net.ClientPacketHandler.handleBeamSync(payload);
        }
    }

    static void handle(Payloads.AbilityFeedback payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.animeki.client.net.ClientPacketHandler.handleAbilityFeedback(payload);
        }
    }

    static void handle(Payloads.BossState payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.animeki.client.net.ClientPacketHandler.handleBossState(payload);
        }
    }

    static void handle(Payloads.FlightParams payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.animeki.client.net.ClientPacketHandler.handleFlightParams(payload);
        }
    }
}
