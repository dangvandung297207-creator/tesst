package com.omnitrix.mod.network.payload;

import com.omnitrix.mod.OmnitrixMod;
import com.omnitrix.mod.alien.AlienForm;
import com.omnitrix.mod.data.OmnitrixPlayerData;
import com.omnitrix.mod.registry.ModRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OmnitrixSelectPayload(String formId) implements CustomPacketPayload {

    public static final Type<OmnitrixSelectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OmnitrixMod.MODID, "omnitrix_select"));

    public static final StreamCodec<ByteBuf, OmnitrixSelectPayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(OmnitrixSelectPayload::new, OmnitrixSelectPayload::formId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OmnitrixSelectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                OmnitrixPlayerData data = player.getData(ModRegistry.OMNITRIX_DATA);
                AlienForm form = AlienForm.fromId(payload.formId());
                data.setSelectedForm(form);
                // Sync back to player
                com.omnitrix.mod.network.ModNetwork.sendToPlayer(player,
                        new OmnitrixSyncPayload(player.getId(), data));
            }
        });
    }
}
