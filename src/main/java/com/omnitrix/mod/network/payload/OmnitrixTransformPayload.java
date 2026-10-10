package com.omnitrix.mod.network.payload;

import com.omnitrix.mod.OmnitrixMod;
import com.omnitrix.mod.alien.AlienForm;
import com.omnitrix.mod.data.OmnitrixPlayerData;
import com.omnitrix.mod.network.ModNetwork;
import com.omnitrix.mod.registry.ModRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OmnitrixTransformPayload(boolean quick) implements CustomPacketPayload {

    public static final Type<OmnitrixTransformPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OmnitrixMod.MODID, "omnitrix_transform"));

    public static final StreamCodec<ByteBuf, OmnitrixTransformPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(OmnitrixTransformPayload::new, OmnitrixTransformPayload::quick);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OmnitrixTransformPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                OmnitrixPlayerData data = player.getData(ModRegistry.OMNITRIX_DATA);

                // Validation: Cannot transform if in cooldown or already transforming
                if (data.isCoolingDown() || data.isTransforming() || data.isReverting()) {
                    return;
                }

                // If already transformed to the same alien, ignore
                if (data.getCurrentForm() == data.getSelectedForm()) {
                    return;
                }

                ServerLevel level = player.serverLevel();

                if (payload.quick()) {
                    // Quick transformation: instantaneous
                    data.setCurrentForm(data.getSelectedForm());
                    data.setTransformTicksLeft(1200); // 60 seconds of alien form
                    data.setTransforming(false);
                    data.setTransformSequenceTick(0);

                    // Sound & VFX
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModRegistry.SOUND_TRANSFORM_BURST.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

                    ModNetwork.sendToTrackingPlayers(player,
                            new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.FLASH_SHOCKWAVE, data.getCurrentForm().getThemeColor(), 0));
                } else {
                    // Cinematic transformation start
                    data.setTransforming(true);
                    data.setTransformSequenceTick(1);

                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModRegistry.SOUND_TRANSFORM_START.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

                    ModNetwork.sendToTrackingPlayers(player,
                            new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.TRANSFORM_SPIRAL, data.getSelectedForm().getThemeColor(), 0));
                }

                // Synchronize state across all players tracking this entity
                ModNetwork.sendToTrackingPlayers(player, new OmnitrixSyncPayload(player.getId(), data));
            }
        });
    }
}
