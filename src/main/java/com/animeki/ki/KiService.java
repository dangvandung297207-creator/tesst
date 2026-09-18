package com.animeki.ki;

import com.animeki.AnimeKi;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.network.AnimeKiNetwork;
import com.animeki.registry.ModAttachments;
import com.animeki.transformation.TransformationService;
import com.animeki.util.MathUtil;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Server side Ki simulation: regeneration, charging, spending and burnout.
 *
 * <p>The server is the only authority for Ki. Clients never send amounts; they send intents
 * ("start charging", "fire the beam") and receive a HUD snapshot.</p>
 */
public final class KiService {
    /** Feedback effect rate while charging. */
    private static final int CHARGE_VFX_INTERVAL = 6;

    private KiService() {
    }

    // ------------------------------------------------------------------ tick

    public static void tick(ServerPlayer player) {
        PlayerKi ki = ModAttachments.ki(player);
        long gameTime = player.level().getGameTime();

        if (!AnimeKiServerConfig.KI.enableForAllPlayers.get() && !ki.isEnabled()) {
            return;
        }

        if (ki.isCharging()) {
            tickCharging(player, ki, gameTime);
        } else {
            double regeneration = KiMath.regenerationPerTick(player);
            if (!ki.isRegenerationBlocked(gameTime) && !ki.isFull(player) && regeneration > 0.0D) {
                ki.add(regeneration, player);
            }
        }

        ki.clampToMax(player);
        if (ki.current() <= 0.0D && !ki.isExhausted(gameTime)) {
            applyExhaustion(player, ki, gameTime);
        }
    }

    private static void tickCharging(ServerPlayer player, PlayerKi ki, long gameTime) {
        AnimeKiServerConfig.Ki config = AnimeKiServerConfig.KI;

        if (config.chargingRequiresStandingStill.get()) {
            Vec3 movement = player.getDeltaMovement();
            double horizontal = MathUtil.horizontalLength(movement);
            if (horizontal > config.movementInterruptTolerance.get()) {
                stopCharging(player);
                return;
            }
        }
        if (config.interruptChargeOnDamage.get() && player.hurtTime > 0) {
            stopCharging(player);
            return;
        }

        ki.incrementChargeTicks();
        if (!ki.isFull(player)) {
            ki.add(KiMath.chargePerTick(player), player);
        }

        if (ki.chargeTicks() % CHARGE_VFX_INTERVAL == 0) {
            VfxDispatcher.playOnPlayer(player, VfxEvent.KI_CHARGE_TICK, Vec3.ZERO,
                    0.6F + (float) ki.ratio(player));
            AnimeKiNetwork.syncVisualState(player);
        }
    }

    private static void applyExhaustion(ServerPlayer player, PlayerKi ki, long gameTime) {
        int duration = AnimeKiServerConfig.KI.exhaustionDurationTicks.get();
        ki.setExhaustedUntil(gameTime + duration);
        ki.setCharging(false);
        VfxDispatcher.playOnPlayer(player, VfxEvent.KI_BURNOUT, Vec3.ZERO, 1.0F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                com.animeki.registry.ModSounds.KI_BLAST_IMPACT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F,
                0.55F);
        AnimeKiNetwork.syncPlayerState(player);
    }

    // ------------------------------------------------------------------ charging

    public static boolean startCharging(ServerPlayer player) {
        PlayerKi ki = ModAttachments.ki(player);
        long gameTime = player.level().getGameTime();
        if (ki.isCharging() || ki.isExhausted(gameTime)) {
            return false;
        }
        if (!AnimeKiServerConfig.KI.enableForAllPlayers.get() && !ki.isEnabled()) {
            return false;
        }
        TransformationService.get(player);
        ki.setCharging(true);
        AnimeKiNetwork.syncPlayerState(player);
        AnimeKiNetwork.syncVisualState(player);
        return true;
    }

    public static void stopCharging(ServerPlayer player) {
        PlayerKi ki = ModAttachments.ki(player);
        if (!ki.isCharging()) {
            return;
        }
        ki.setCharging(false);
        AnimeKiNetwork.syncPlayerState(player);
        AnimeKiNetwork.syncVisualState(player);
    }

    public static boolean isCharging(ServerPlayer player) {
        return ModAttachments.ki(player).isCharging();
    }

    // ------------------------------------------------------------------ spending

    /**
     * All or nothing Ki spending.
     *
     * @param interruptCharge whether spending should end a charge (attacks do, passive drains do not)
     * @return {@code true} if the cost was paid
     */
    public static boolean tryConsume(ServerPlayer player, double amount, boolean interruptCharge) {
        PlayerKi ki = ModAttachments.ki(player);
        long gameTime = player.level().getGameTime();
        if (amount <= 0.0D) {
            return true;
        }
        if (ki.isExhausted(gameTime)) {
            return false;
        }
        if (ki.current() < amount) {
            return false;
        }
        ki.setCurrent(ki.current() - amount, player);
        ki.blockRegeneration(gameTime + AnimeKiServerConfig.KI.regenerationDelayTicks.get());
        if (interruptCharge && ki.isCharging()) {
            ki.setCharging(false);
            AnimeKiNetwork.syncVisualState(player);
        }
        return true;
    }

    /** Continuous drain used by channels (beams, flight, transformations). */
    public static boolean drain(ServerPlayer player, double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        PlayerKi ki = ModAttachments.ki(player);
        long gameTime = player.level().getGameTime();
        if (ki.current() < amount) {
            ki.setCurrent(0.0D, player);
            if (!ki.isExhausted(gameTime)) {
                applyExhaustion(player, ki, gameTime);
            }
            AnimeKiNetwork.syncPlayerState(player);
            return false;
        }
        ki.setCurrent(ki.current() - amount, player);
        ki.blockRegeneration(gameTime + Math.min(20, AnimeKiServerConfig.KI.regenerationDelayTicks.get()));
        return true;
    }

    /** Charges a Ki cost without finishing an active charge (used by melee). */
    public static boolean spend(ServerPlayer player, double amount) {
        return tryConsume(player, amount, false);
    }

    // ------------------------------------------------------------------ events

    public static void onHurt(ServerPlayer player, float amount) {
        PlayerKi ki = ModAttachments.ki(player);
        long gameTime = player.level().getGameTime();
        // Losing Ki when hit rewards staying on the offensive.
        if (AnimeKiServerConfig.KI.regenerationDelayTicks.get() > 0) {
            ki.blockRegeneration(gameTime + AnimeKiServerConfig.KI.regenerationDelayTicks.get());
        }
        if (ModAttachments.ki(player).isCharging()) {
            stopCharging(player);
        }
    }

    public static void onAttack(ServerPlayer player) {
        stopCharging(player);
    }

    // ------------------------------------------------------------------ admin

    public static void set(ServerPlayer player, double amount) {
        PlayerKi ki = ModAttachments.ki(player);
        ki.set(amount, player);
        AnimeKiNetwork.syncPlayerState(player);
    }

    public static void give(ServerPlayer player, double amount) {
        PlayerKi ki = ModAttachments.ki(player);
        ki.add(amount, player);
        AnimeKiNetwork.syncPlayerState(player);
    }

    public static void reset(ServerPlayer player) {
        PlayerKi ki = ModAttachments.ki(player);
        ki.setExhaustedUntil(Long.MIN_VALUE);
        ki.setCharging(false);
        ki.set(ki.max(player), player);
        AnimeKiNetwork.syncPlayerState(player);
    }

    public static double current(ServerPlayer player) {
        return ModAttachments.ki(player).current();
    }

    public static double max(ServerPlayer player) {
        return ModAttachments.ki(player).max(player);
    }

    /** Debug snapshot for {@code /ki debug}. */
    public static String debug(ServerPlayer player) {
        PlayerKi ki = ModAttachments.ki(player);
        long gameTime = player.level().getGameTime();
        return String.format(
                "ki=%.1f/%.1f charging=%s chargeTicks=%d exhausted=%s regenBlockedFor=%d output=%.2f form=%s",
                ki.current(), ki.max(player), ki.isCharging(), ki.chargeTicks(),
                ki.isExhausted(gameTime) ? (ki.exhaustedUntil() - gameTime) + "t" : "no",
                Math.max(0L, ki.regenerationBlockedUntil() - gameTime), KiMath.outputMultiplier(player),
                ModAttachments.transformation(player).activeId());
    }

    static {
        AnimeKi.LOGGER.debug("animeki ki service ready");
    }
}
