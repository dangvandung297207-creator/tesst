package com.omnitrix.mod.network;

import com.omnitrix.mod.OmnitrixMod;
import com.omnitrix.mod.network.payload.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(OmnitrixMod.MODID).versioned("1.0");

        // Client -> Server
        registrar.playToServer(OmnitrixSelectPayload.TYPE, OmnitrixSelectPayload.STREAM_CODEC, OmnitrixSelectPayload::handle);
        registrar.playToServer(OmnitrixTransformPayload.TYPE, OmnitrixTransformPayload.STREAM_CODEC, OmnitrixTransformPayload::handle);
        registrar.playToServer(OmnitrixRevertPayload.TYPE, OmnitrixRevertPayload.STREAM_CODEC, OmnitrixRevertPayload::handle);
        registrar.playToServer(OmnitrixAbilityPayload.TYPE, OmnitrixAbilityPayload.STREAM_CODEC, OmnitrixAbilityPayload::handle);

        // Server -> Client
        registrar.playToClient(OmnitrixSyncPayload.TYPE, OmnitrixSyncPayload.STREAM_CODEC, OmnitrixSyncPayload::handle);
        registrar.playToClient(OmnitrixVfxPayload.TYPE, OmnitrixVfxPayload.STREAM_CODEC, OmnitrixVfxPayload::handle);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToTrackingPlayers(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntity(player, payload);
        // Also send to the player themselves
        PacketDistributor.sendToPlayer(player, payload);
    }
}
