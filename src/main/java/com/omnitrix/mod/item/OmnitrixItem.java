package com.omnitrix.mod.item;

import com.omnitrix.mod.data.OmnitrixPlayerData;
import com.omnitrix.mod.network.ModNetwork;
import com.omnitrix.mod.network.payload.OmnitrixSyncPayload;
import com.omnitrix.mod.registry.ModRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class OmnitrixItem extends Item {

    public OmnitrixItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            OmnitrixPlayerData data = serverPlayer.getData(ModRegistry.OMNITRIX_DATA);

            // Bind/equip Omnitrix to player data
            if (!data.hasOmnitrix()) {
                data.setHasOmnitrix(true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModRegistry.SOUND_OMNITRIX_POPUP.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                player.sendSystemMessage(Component.translatable("message.omnitrix.equipped"));
                ModNetwork.sendToTrackingPlayers(serverPlayer, new OmnitrixSyncPayload(player.getId(), data));
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_OMNITRIX_POPUP.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        } else if (level.isClientSide) {
            // Client opens alien selection radial wheel
            com.omnitrix.mod.client.gui.AlienSelectScreen.open();
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.omnitrix.omnitrix.desc1"));
        tooltipComponents.add(Component.translatable("item.omnitrix.omnitrix.desc2"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
