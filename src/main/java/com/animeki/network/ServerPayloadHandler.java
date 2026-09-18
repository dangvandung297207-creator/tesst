package com.animeki.network;

import com.animeki.ability.AbilityAction;
import com.animeki.ability.AbilityService;
import com.animeki.combat.CombatService;
import com.animeki.flight.FlightService;
import com.animeki.util.MathUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Handles everything the client is allowed to ask for.
 *
 * <p>The client can only express <b>intent</b>: which skill button was pressed, where it is aiming
 * roughly and how it is moving. Damage, costs, cooldowns, teleport destinations and transformations
 * are all decided server side, and every input is validated for plausibility before it reaches a
 * service.</p>
 */
public final class ServerPayloadHandler {
    private ServerPayloadHandler() {
    }

    private static boolean invalid(ServerPlayer player) {
        return player == null || !player.isAlive() || player.isSpectator();
    }

    public static void handleAbilityInput(Payloads.AbilityInput payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || invalid(player)) {
            return;
        }
        AbilityAction action = AbilityAction.byOrdinal(payload.action());
        Vec3 input = new Vec3(MathUtil.clamp(payload.forward(), -1.0F, 1.0F), 0.0D,
                MathUtil.clamp(payload.strafe(), -1.0F, 1.0F));
        AbilityService.handleInput(player, action, input);
    }

    public static void handleFlightInput(Payloads.FlightInput payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || invalid(player)) {
            return;
        }
        FlightService.handleInput(player, payload.forward(), payload.strafe(), payload.vertical(), payload.boost());
    }

    public static void handleCombatInput(Payloads.CombatInput payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || invalid(player)) {
            return;
        }
        // A negative kind means "decide for me": the server then walks the combo chain itself.
        CombatService.handleInput(player, AttackKind.byOrdinalOrNull(payload.kind()), payload.heavy());
    }

    public static void handleStateRequest(Payloads.StateRequest payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || invalid(player)) {
            return;
        }
        AnimeKiNetwork.syncEverything(player);
    }
}
