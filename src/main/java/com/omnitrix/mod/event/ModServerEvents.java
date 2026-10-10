package com.omnitrix.mod.event;

import com.omnitrix.mod.OmnitrixMod;
import com.omnitrix.mod.alien.AlienForm;
import com.omnitrix.mod.data.OmnitrixPlayerData;
import com.omnitrix.mod.network.ModNetwork;
import com.omnitrix.mod.network.payload.OmnitrixSyncPayload;
import com.omnitrix.mod.network.payload.OmnitrixVfxPayload;
import com.omnitrix.mod.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = OmnitrixMod.MODID)
public class ModServerEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();

        // Run logic only on the logical server
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) return;

        OmnitrixPlayerData data = serverPlayer.getData(ModRegistry.OMNITRIX_DATA);

        // 1. Cooldown decrements
        if (data.getAbility1Cooldown() > 0) data.setAbility1Cooldown(data.getAbility1Cooldown() - 1);
        if (data.getAbility2Cooldown() > 0) data.setAbility2Cooldown(data.getAbility2Cooldown() - 1);
        if (data.getAbility3Cooldown() > 0) data.setAbility3Cooldown(data.getAbility3Cooldown() - 1);

        // Recharge cooldown decrement when in human form
        if (data.getRechargeCooldown() > 0) {
            data.setRechargeCooldown(data.getRechargeCooldown() - 1);
            if (data.getRechargeCooldown() == 0) {
                // Beep ready sound
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModRegistry.SOUND_OMNITRIX_POPUP.get(), SoundSource.PLAYERS, 1.0f, 1.2f);
                ModNetwork.sendToTrackingPlayers(serverPlayer, new OmnitrixSyncPayload(serverPlayer.getId(), data));
            }
        }

        // 2. Multi-stage cinematic transformation progression
        if (data.isTransforming()) {
            int seq = data.getTransformSequenceTick();
            data.setTransformSequenceTick(seq + 1);

            // Controlled immobilization during cinematic sequence
            serverPlayer.setDeltaMovement(0, serverPlayer.getDeltaMovement().y * 0.5, 0);

            // Stage 1 (Ticks 1-15): Light energy accumulation & sound
            // Stage 2 (Ticks 16-30): Spirals around body
            // Stage 3 (Tick 30): Controlled flash, shockwave burst, model transformation!
            if (seq >= 30) {
                data.setCurrentForm(data.getSelectedForm());
                data.setTransformTicksLeft(1200); // 60 seconds
                data.setTransforming(false);
                data.setTransformSequenceTick(0);

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModRegistry.SOUND_TRANSFORM_BURST.get(), SoundSource.PLAYERS, 1.2f, 1.0f);

                ModNetwork.sendToTrackingPlayers(serverPlayer,
                        new OmnitrixVfxPayload(serverPlayer.getId(), OmnitrixVfxPayload.VfxType.FLASH_SHOCKWAVE, data.getCurrentForm().getThemeColor(), 4));
                ModNetwork.sendToTrackingPlayers(serverPlayer, new OmnitrixSyncPayload(serverPlayer.getId(), data));
            }
        }

        // 3. Transformation countdown & active Alien effects
        if (data.isTransformed()) {
            int ticksLeft = data.getTransformTicksLeft();

            // Timeout warning beeps at last 5 seconds (100 ticks)
            if (ticksLeft <= 100 && ticksLeft > 0 && ticksLeft % 20 == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModRegistry.SOUND_TIMEOUT_WARNING.get(), SoundSource.PLAYERS, 1.0f, 1.5f);
            }

            if (ticksLeft > 0) {
                data.setTransformTicksLeft(ticksLeft - 1);
                applyAlienTickBuffs(serverPlayer, data);
            } else {
                // Timer expired -> Revert to human!
                data.setCurrentForm(AlienForm.NONE);
                data.setRechargeCooldown(400); // 20 seconds cooldown
                data.setCannonboltRolling(0);

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModRegistry.SOUND_REVERT.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

                ModNetwork.sendToTrackingPlayers(serverPlayer,
                        new OmnitrixVfxPayload(serverPlayer.getId(), OmnitrixVfxPayload.VfxType.REVERSION_RED, 0xFF0000, 0));
                ModNetwork.sendToTrackingPlayers(serverPlayer, new OmnitrixSyncPayload(serverPlayer.getId(), data));
            }
        }
    }

    private static void applyAlienTickBuffs(ServerPlayer player, OmnitrixPlayerData data) {
        AlienForm form = data.getCurrentForm();

        switch (form) {
            case HEATBLAST -> {
                player.clearFire();
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, false));
            }
            case XLR8 -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 3, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 1, false, false, false));
            }
            case FOUR_ARMS -> {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 2, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false, false));
            }
            case DIAMONDHEAD -> {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 2, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, false));
            }
            case CANNONBOLT -> {
                if (data.getCannonboltRolling() > 0) {
                    data.setCannonboltRolling(data.getCannonboltRolling() - 1);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 2, false, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 3, false, false, false));
                }
            }
            case UPGRADE -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, false, false, false));
            }
            case STINKFLY -> {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, false));
            }
            case WILDMUTT -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, false));
            }
            default -> {}
        }
    }

    // Alien Damage Resistances and Immunities
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OmnitrixPlayerData data = player.getData(ModRegistry.OMNITRIX_DATA);
            if (data.isTransformed()) {
                DamageSource source = event.getSource();

                // Heatblast complete fire/lava immunity
                if (data.getCurrentForm() == AlienForm.HEATBLAST) {
                    if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.LAVA)) {
                        event.setCanceled(true);
                        return;
                    }
                }

                // Cannonbolt high physical defense when rolling
                if (data.getCurrentForm() == AlienForm.CANNONBOLT && data.getCannonboltRolling() > 0) {
                    event.setNewDamage(event.getOriginalDamage() * 0.35f);
                }

                // Diamondhead projectile and fall damage resistance
                if (data.getCurrentForm() == AlienForm.DIAMONDHEAD) {
                    if (source.is(DamageTypes.FALL) || source.is(DamageTypes.ARROW)) {
                        event.setNewDamage(event.getOriginalDamage() * 0.4f);
                    }
                }
            }
        }
    }

    // Sync on Player Join, Respawn, and Dimension Change
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OmnitrixPlayerData data = player.getData(ModRegistry.OMNITRIX_DATA);
            ModNetwork.sendToPlayer(player, new OmnitrixSyncPayload(player.getId(), data));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OmnitrixPlayerData data = player.getData(ModRegistry.OMNITRIX_DATA);
            // Revert back to human on death/respawn
            data.setCurrentForm(AlienForm.NONE);
            data.setTransformTicksLeft(0);
            data.setRechargeCooldown(200);
            data.setTransforming(false);
            ModNetwork.sendToPlayer(player, new OmnitrixSyncPayload(player.getId(), data));
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OmnitrixPlayerData data = player.getData(ModRegistry.OMNITRIX_DATA);
            ModNetwork.sendToTrackingPlayers(player, new OmnitrixSyncPayload(player.getId(), data));
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer targetPlayer && event.getEntity() instanceof ServerPlayer observer) {
            OmnitrixPlayerData data = targetPlayer.getData(ModRegistry.OMNITRIX_DATA);
            ModNetwork.sendToPlayer(observer, new OmnitrixSyncPayload(targetPlayer.getId(), data));
        }
    }
}
