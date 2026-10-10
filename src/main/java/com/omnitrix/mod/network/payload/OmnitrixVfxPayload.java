package com.omnitrix.mod.network.payload;

import com.omnitrix.mod.OmnitrixMod;
import com.omnitrix.mod.client.ClientVfxHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OmnitrixVfxPayload(int entityId, VfxType vfxType, int color, int extraData) implements CustomPacketPayload {

    public enum VfxType {
        FLASH_SHOCKWAVE,
        TRANSFORM_SPIRAL,
        REVERSION_RED,
        SPEED_AFTERIMAGE,
        GROUND_SLAM_DUST,
        FIRE_SLAM_BURST,
        CRYSTAL_WALL_SUMMON,
        SONAR_RADAR_PULSE,
        TECH_GLOW_SURGE,
        SLIME_SPLATTER
    }

    public static final Type<OmnitrixVfxPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OmnitrixMod.MODID, "omnitrix_vfx"));

    public static final StreamCodec<ByteBuf, OmnitrixVfxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OmnitrixVfxPayload::entityId,
            ByteBufCodecs.VAR_INT.map(i -> VfxType.values()[i], VfxType::ordinal), OmnitrixVfxPayload::vfxType,
            ByteBufCodecs.VAR_INT, OmnitrixVfxPayload::color,
            ByteBufCodecs.VAR_INT, OmnitrixVfxPayload::extraData,
            OmnitrixVfxPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OmnitrixVfxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientVfxHandler.handleVfx(payload);
        });
    }
}
