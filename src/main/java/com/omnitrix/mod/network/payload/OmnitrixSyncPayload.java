package com.omnitrix.mod.network.payload;

import com.omnitrix.mod.OmnitrixMod;
import com.omnitrix.mod.client.ClientOmnitrixHandler;
import com.omnitrix.mod.data.OmnitrixPlayerData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OmnitrixSyncPayload(int entityId, OmnitrixPlayerData data) implements CustomPacketPayload {

    public static final Type<OmnitrixSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OmnitrixMod.MODID, "omnitrix_sync"));

    public static final StreamCodec<ByteBuf, OmnitrixSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OmnitrixSyncPayload::entityId,
            OmnitrixPlayerData.STREAM_CODEC, OmnitrixSyncPayload::data,
            OmnitrixSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OmnitrixSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientOmnitrixHandler.handleSync(payload.entityId(), payload.data());
        });
    }
}
