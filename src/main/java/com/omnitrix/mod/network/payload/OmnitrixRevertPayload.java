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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OmnitrixRevertPayload(boolean force) implements CustomPacketPayload {

    public static final Type<OmnitrixRevertPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OmnitrixMod.MODID, "omnitrix_revert"));

    public static final StreamCodec<ByteBuf, OmnitrixRevertPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(OmnitrixRevertPayload::new, OmnitrixRevertPayload::force);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OmnitrixRevertPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                OmnitrixPlayerData data = player.getData(ModRegistry.OMNITRIX_DATA);

                if (!data.isTransformed()) return;

                if (payload.force()) {
                    data.setCurrentForm(AlienForm.NONE);
                    data.setTransformTicksLeft(0);
                    data.setRechargeCooldown(400); // 20 seconds cooldown
                    data.setTransforming(false);
                    data.setReverting(false);
                    data.setCannonboltRolling(0);

                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModRegistry.SOUND_REVERT.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

                    ModNetwork.sendToTrackingPlayers(player,
                            new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.REVERSION_RED, 0xFF0000, 0));
                } else {
                    // Start reversion sequence
                    data.setReverting(true);
                }

                ModNetwork.sendToTrackingPlayers(player, new OmnitrixSyncPayload(player.getId(), data));
            }
        });
    }
}
