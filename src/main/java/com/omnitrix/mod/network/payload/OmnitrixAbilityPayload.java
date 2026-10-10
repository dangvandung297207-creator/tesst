package com.omnitrix.mod.network.payload;

import com.omnitrix.mod.OmnitrixMod;
import com.omnitrix.mod.alien.AlienAbilities;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OmnitrixAbilityPayload(int abilitySlot, boolean charged) implements CustomPacketPayload {

    public static final Type<OmnitrixAbilityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OmnitrixMod.MODID, "omnitrix_ability"));

    public static final StreamCodec<ByteBuf, OmnitrixAbilityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OmnitrixAbilityPayload::abilitySlot,
            ByteBufCodecs.BOOL, OmnitrixAbilityPayload::charged,
            OmnitrixAbilityPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OmnitrixAbilityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                AlienAbilities.handleAbilityTrigger(player, payload.abilitySlot(), payload.charged());
            }
        });
    }
}
