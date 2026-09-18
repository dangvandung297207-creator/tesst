package com.animeki.ki;

import com.animeki.config.AnimeKiServerConfig;
import com.animeki.registry.ModAttachments;
import com.animeki.transformation.PlayerTransformation;
import com.animeki.transformation.Transformation;
import net.minecraft.world.entity.player.Player;

/**
 * Central place where every Ki value is derived.
 *
 * <p>Nothing in the mod hardcodes a balance number: Ki costs, regeneration, output and the
 * transformation multipliers all resolve here from the server configuration and the active form.
 * That also makes the "output multiplier" rule easy to reason about - it is the product of the
 * global multiplier, the form's multiplier, the charge bonus and the exhaustion penalty.</p>
 */
public final class KiMath {
    /** Damage/max-Ki penalty applied while the player is exhausted. */
    public static final double EXHAUSTION_OUTPUT_PENALTY = 0.55D;

    private KiMath() {
    }

    /** Maximum Ki, including the current form's bonus. */
    public static double max(Player player) {
        return ModAttachments.ki(player).max(player);
    }

    /** Ki multiplier of the active form (1 when untransformed). */
    public static double kiMultiplier(Player player) {
        PlayerTransformation state = ModAttachments.transformation(player);
        Transformation form = state.active();
        return form == null ? 1.0D : form.kiMultiplier();
    }

    public static double damageMultiplier(Player player) {
        PlayerTransformation state = ModAttachments.transformation(player);
        Transformation form = state.active();
        return form == null ? 1.0D : form.damageMultiplier();
    }

    public static double defenseMultiplier(Player player) {
        PlayerTransformation state = ModAttachments.transformation(player);
        Transformation form = state.active();
        return form == null ? 1.0D : form.defenseMultiplier();
    }

    public static double speedMultiplier(Player player) {
        PlayerTransformation state = ModAttachments.transformation(player);
        Transformation form = state.active();
        return form == null ? 1.0D : form.speedMultiplier();
    }

    public static double flightMultiplier(Player player) {
        PlayerTransformation state = ModAttachments.transformation(player);
        Transformation form = state.active();
        return form == null ? 1.0D : form.flightMultiplier();
    }

    public static double regenerationMultiplier(Player player) {
        PlayerTransformation state = ModAttachments.transformation(player);
        Transformation form = state.active();
        return form == null ? 1.0D : form.regenerationMultiplier();
    }

    /** Regeneration per tick, already including exhaustion and the form bonus. */
    public static double regenerationPerTick(Player player) {
        double base = AnimeKiServerConfig.KI.regenerationPerTick.get() * regenerationMultiplier(player);
        if (ModAttachments.ki(player).isExhausted(player.level().getGameTime())) {
            base *= AnimeKiServerConfig.KI.exhaustionRegenerationMultiplier.get();
        }
        return base;
    }

    /** Ki gained per tick while charging. */
    public static double chargePerTick(Player player) {
        double base = AnimeKiServerConfig.KI.chargePerTick.get();
        if (player.isShiftKeyDown()) {
            base *= 0.6D;
        }
        return base * Math.max(0.5D, kiMultiplier(player));
    }

    /**
     * Multiplier applied to every Ki attack and to melee damage while charging.
     *
     * <p>Charging therefore matters offensively as well as defensively: an attack fired while
     * charging hits harder, which is exactly the anime trope we want to support.</p>
     */
    public static double outputMultiplier(Player player) {
        double output = AnimeKiServerConfig.KI.globalOutputMultiplier.get();
        output *= damageMultiplier(player);
        PlayerKi ki = ModAttachments.ki(player);
        if (ki.isCharging()) {
            output *= 1.0D + AnimeKiServerConfig.KI.chargeOutputBonus.get();
        }
        if (ki.isExhausted(player.level().getGameTime())) {
            output *= EXHAUSTION_OUTPUT_PENALTY;
        }
        return output;
    }

    /** Melee damage multiplier (output plus the combo damage bonus is applied by the combat code). */
    public static double meleeMultiplier(Player player) {
        return outputMultiplier(player);
    }

    /** Flight speed multiplier of the current form. */
    public static double flightSpeedMultiplier(Player player) {
        return flightMultiplier(player);
    }

    /** How much damage is reduced while the player is charging (brace mechanic). */
    public static double chargingDamageReduction(Player player) {
        return ModAttachments.ki(player).isCharging() ? 0.25D : 0.0D;
    }

    /** Whether the player has enough Ki for a cost. */
    public static boolean has(Player player, double cost) {
        return ModAttachments.ki(player).current() >= cost;
    }

    /** Percentage of the Ki bar for HUD rendering. */
    public static float ratio(Player player) {
        return (float) ModAttachments.ki(player).ratio(player);
    }
}
