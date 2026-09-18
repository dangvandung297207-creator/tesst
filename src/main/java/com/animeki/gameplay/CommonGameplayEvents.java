package com.animeki.gameplay;

import com.animeki.ability.AbilityService;
import com.animeki.beam.BeamService;
import com.animeki.command.KiCommand;
import com.animeki.combat.CombatService;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.destruction.DestructionService;
import com.animeki.flight.DashService;
import com.animeki.flight.FlightService;
import com.animeki.ki.KiMath;
import com.animeki.ki.KiService;
import com.animeki.network.AnimeKiNetwork;
import com.animeki.registry.ModAttachments;
import com.animeki.transformation.TransformationService;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The single place where gameplay hooks are wired up.
 *
 * <p>All systems are ticked from here in a fixed order (Ki first, then movement, then abilities), so
 * the server side state transitions stay predictable. Everything below is server authoritative:
 * damage is scaled here, invulnerability frames are granted here and nothing the client sends is
 * trusted.</p>
 */
public final class CommonGameplayEvents {
    /** HUD snapshots are only sent when a player is actually doing something. */
    private static final int ACTIVE_SYNC_INTERVAL = 4;
    private static final int IDLE_SYNC_INTERVAL = 40;
    private static final int VISUAL_SYNC_INTERVAL = 10;
    private static final int IDLE_VISUAL_SYNC_INTERVAL = 60;

    private static int tickCounter;

    private CommonGameplayEvents() {
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        tickCounter++;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        KiService.tick(player);
        FlightService.tick(player);
        DashService.tick(player);
        CombatService.tick(player);
        TransformationService.tick(player);
        AbilityService.tick(player);

        if (shouldSync(player)) {
            AnimeKiNetwork.syncPlayerState(player);
        }
        // Appearance is broadcast much less often: other clients only need the aura/form/flight state
        // and they interpolate everything else themselves.
        if (tickCounter % (hasActiveVisual(player) ? VISUAL_SYNC_INTERVAL : IDLE_VISUAL_SYNC_INTERVAL) == 0) {
            AnimeKiNetwork.syncVisualState(player);
        }
    }

    /** Whether this player currently has anything worth showing to other clients. */
    private static boolean hasActiveVisual(ServerPlayer player) {
        return ModAttachments.transformation(player).isActive()
                || ModAttachments.ki(player).isCharging()
                || ModAttachments.flight(player).isActive()
                || ModAttachments.combat(player).step() > 0;
    }

    private static boolean shouldSync(ServerPlayer player) {
        boolean active = ModAttachments.ki(player).isCharging()
                || ModAttachments.flight(player).isActive()
                || ModAttachments.flight(player).isDashing()
                || ModAttachments.transformation(player).isActive()
                || AbilityService.isBusy(player)
                || ModAttachments.combat(player).step() > 0
                || !ModAttachments.ki(player).isFull(player);
        int interval = active ? ACTIVE_SYNC_INTERVAL : IDLE_SYNC_INTERVAL;
        return tickCounter % interval == 0;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BeamService.tick(level);
            DestructionService.tick(level);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BeamService.stopLevel(level);
            DestructionService.clear(level);
        }
    }

    // ------------------------------------------------------------------ damage

    /**
     * Applies defensive multipliers and the dash / vanish invulnerability frames.
     *
     * <p>Client supplied values never reach this point: the damage number comes from the vanilla
     * pipeline and is only ever scaled by server side state.</p>
     */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        long gameTime = player.serverLevel().getGameTime();
        if (DashService.isInvulnerable(player) || ModAttachments.combat(player).isDodging(gameTime)) {
            event.setCanceled(true);
            VfxDispatcher.playOnPlayer(player, VfxEvent.APPEAR, Vec3.ZERO, 0.6F);
            return;
        }
        float amount = event.getAmount();
        amount *= (float) KiMath.defenseMultiplier(player);
        amount *= (float) (1.0D - KiMath.chargingDamageReduction(player));
        event.setAmount(amount);
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && event.getEntity() != attacker
                && event.getEntity() instanceof LivingEntity) {
            event.setNewDamage((float) (event.getNewDamage() * KiMath.meleeMultiplier(attacker)));
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            float taken = event.getNewDamage();
            KiService.onHurt(player, taken);
            AbilityService.onPlayerHurt(player, taken);
            FlightService.onDamage(player, taken);
            if (taken > 0.0F) {
                VfxDispatcher.camera(player, com.animeki.vfx.CameraEffectType.SHAKE,
                        0.15F + Math.min(0.5F, taken * 0.03F), 5);
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Any attack ends a Ki charge, which is what keeps charging a commitment.
            KiService.onAttack(player);
            if (AnimeKiServerConfig.COMBAT.replaceVanillaAttack.get()) {
                // The mod runs its own hit detection, so the vanilla swing must not deal damage on
                // top of it. Sneaking asks for the heavy variant of the current combo step.
                CombatService.attack(player, player.isShiftKeyDown());
                event.setCanceled(true);
            }
        }
    }

    // ------------------------------------------------------------------ lifecycle

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        ModAttachments.copyOnClone(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyAccessRules(player);
            AnimeKiNetwork.syncEverything(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AbilityService.onPlayerRemoved(player);
            FlightService.setActive(player, false, false);
            TransformationService.cancel(player, false);
            ModAttachments.ki(player).setCharging(false);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyAccessRules(player);
            AnimeKiNetwork.syncEverything(player);
        }
    }

    @SubscribeEvent
    public static void onCommandRegistration(RegisterCommandsEvent event) {
        KiCommand.register(event.getDispatcher());
    }

    /** When {@code ki.enableForAllPlayers} is false only operators get a Ki pool. */
    private static void applyAccessRules(ServerPlayer player) {
        boolean enabled = AnimeKiServerConfig.KI.enableForAllPlayers.get() || player.hasPermissions(2);
        ModAttachments.ki(player).setEnabled(enabled);
    }

    /** Helper so other systems can show the standard "player is doing something" feedback. */
    public static boolean isBusy(Player player) {
        return player instanceof ServerPlayer serverPlayer && (AbilityService.isBusy(serverPlayer)
                || ModAttachments.flight(serverPlayer).isDashing());
    }
}
