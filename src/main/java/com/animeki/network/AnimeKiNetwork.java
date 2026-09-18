package com.animeki.network;

import com.animeki.AnimeKi;
import com.animeki.ability.AbilityService;
import com.animeki.combat.CombatService;
import com.animeki.flight.FlightService;
import com.animeki.ki.KiMath;
import com.animeki.ki.PlayerKi;
import com.animeki.registry.ModAttachments;
import com.animeki.transformation.PlayerTransformation;
import com.animeki.transformation.TransformationPhase;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Packet registration and the small set of send helpers used across the mod.
 */
public final class AnimeKiNetwork {
    private AnimeKiNetwork() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(AnimeKiNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Client -> server (intent only).
        registrar.playToServer(Payloads.AbilityInput.TYPE, Payloads.AbilityInput.CODEC,
                (payload, context) -> context.enqueueWork(() -> ServerPayloadHandler.handleAbilityInput(payload, context)));
        registrar.playToServer(Payloads.FlightInput.TYPE, Payloads.FlightInput.CODEC,
                (payload, context) -> context.enqueueWork(() -> ServerPayloadHandler.handleFlightInput(payload, context)));
        registrar.playToServer(Payloads.CombatInput.TYPE, Payloads.CombatInput.CODEC,
                (payload, context) -> context.enqueueWork(() -> ServerPayloadHandler.handleCombatInput(payload, context)));
        registrar.playToServer(Payloads.StateRequest.TYPE, Payloads.StateRequest.CODEC,
                (payload, context) -> context.enqueueWork(() -> ServerPayloadHandler.handleStateRequest(payload, context)));

        // Server -> client.
        registrar.playToClient(Payloads.KiState.TYPE, Payloads.KiState.CODEC,
                (payload, context) -> ClientPayloadBridge.handle(payload));
        registrar.playToClient(Payloads.VisualState.TYPE, Payloads.VisualState.CODEC,
                (payload, context) -> ClientPayloadBridge.handle(payload));
        registrar.playToClient(Payloads.Vfx.TYPE, Payloads.Vfx.CODEC,
                (payload, context) -> ClientPayloadBridge.handle(payload));
        registrar.playToClient(Payloads.CameraEffect.TYPE, Payloads.CameraEffect.CODEC,
                (payload, context) -> ClientPayloadBridge.handle(payload));
        registrar.playToClient(Payloads.BeamSync.TYPE, Payloads.BeamSync.CODEC,
                (payload, context) -> ClientPayloadBridge.handle(payload));
        registrar.playToClient(Payloads.AbilityFeedback.TYPE, Payloads.AbilityFeedback.CODEC,
                (payload, context) -> ClientPayloadBridge.handle(payload));
        registrar.playToClient(Payloads.BossState.TYPE, Payloads.BossState.CODEC,
                (payload, context) -> ClientPayloadBridge.handle(payload));
        registrar.playToClient(Payloads.FlightParams.TYPE, Payloads.FlightParams.CODEC,
                (payload, context) -> ClientPayloadBridge.handle(payload));
    }

    // ------------------------------------------------------------------ sending

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
    }

    public static void sendToNear(ServerLevel level, Vec3 position, double radius, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersNear(level, null, position.x, position.y, position.z, radius, payload);
    }

    // ------------------------------------------------------------------ HUD state

    /**
     * Pushes the owning player's HUD snapshot.
     *
     * <p>Called on meaningful changes and at a low fixed rate - never per tick for every player -
     * so a large server does not drown in tiny packets.</p>
     */
    public static void syncPlayerState(ServerPlayer player) {
        PlayerKi ki = ModAttachments.ki(player);
        PlayerTransformation transformation = ModAttachments.transformation(player);
        double max = KiMath.max(player);
        boolean exhausted = ki.isExhausted(player.level().getGameTime());
        Payloads.KiState payload = new Payloads.KiState(
                ki.current(),
                max,
                ki.isCharging(),
                exhausted,
                ki.chargeTicks(),
                transformation.activeId(),
                (byte) transformation.phase().ordinal(),
                transformation.auraIntensity(),
                ModAttachments.flight(player).isActive(),
                ModAttachments.flight(player).isBoosting(),
                ModAttachments.combat(player).step(),
                ModAttachments.combat(player).hits(),
                AbilityService.cooldownSnapshot(player),
                AbilityService.activeAbilityId(player),
                AbilityService.activeAbilityProgress(player),
                (byte) ModAttachments.abilities(player).lastFeedback().ordinal(),
                ModAttachments.abilities(player).feedbackAbilityId());
        sendToPlayer(player, payload);
    }

    /** Broadcasts how a player should look to everyone who can see them (aura, form, flight). */
    public static void syncVisualState(ServerPlayer player) {
        PlayerTransformation transformation = ModAttachments.transformation(player);
        Payloads.VisualState payload = new Payloads.VisualState(
                player.getId(),
                transformation.activeId(),
                (byte) transformation.phase().ordinal(),
                transformation.auraIntensity(),
                ModAttachments.ki(player).isCharging(),
                ModAttachments.flight(player).isActive(),
                ModAttachments.flight(player).isBoosting(),
                ModAttachments.combat(player).step());
        sendToTrackingAndSelf(player, payload);
    }

    /** Full refresh after login, respawn or dimension change. */
    public static void syncEverything(ServerPlayer player) {
        syncPlayerState(player);
        syncVisualState(player);
        CombatService.sync(player);
        FlightService.sync(player);
    }

    static void warnMissingClient() {
        AnimeKi.LOGGER.warn("Received a client bound animeki packet on a dedicated server - no handler installed");
    }
}
