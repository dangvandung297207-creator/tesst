package com.animeki.client.input;

import com.animeki.ability.AbilityAction;
import com.animeki.client.KeyBindings;
import com.animeki.network.AnimeKiNetwork;
import com.animeki.network.Payloads;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Turns key presses into intent packets.
 *
 * <p>Nothing here decides anything: every button simply reports "start" / "release" plus the raw
 * movement input, and the server decides whether the action is legal. Two small quality of life
 * rules are applied locally: input is not sent while a screen is open, and the flight stream is only
 * sent when it changes (plus a periodic keep-alive).</p>
 */
public final class ClientInput {
    private static final int FLIGHT_KEEPALIVE_TICKS = 10;

    private static boolean chargingPressed;
    private static boolean blastPressed;
    private static boolean primaryPressed;
    private static boolean secondaryPressed;
    private static boolean ultimatePressed;

    private static float lastForward;
    private static float lastStrafe;
    private static float lastVertical;
    private static boolean lastBoost;
    private static int ticksSinceFlightPacket;

    private ClientInput() {
    }

    // ------------------------------------------------------------------ key events

    public static void onKey(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (event.getAction() == GLFW.GLFW_REPEAT) {
            return;
        }
        boolean down = event.getAction() == GLFW.GLFW_PRESS;
        handle(KeyBindings.KI_CHARGE, down, () -> send(AbilityAction.KI_CHARGE_START, false),
                () -> send(AbilityAction.KI_CHARGE_STOP, false), value -> chargingPressed = value, chargingPressed);
        handle(KeyBindings.KI_BLAST, down, () -> send(AbilityAction.KI_BLAST_START, false),
                () -> send(AbilityAction.KI_BLAST_RELEASE, false), value -> blastPressed = value, blastPressed);
        handle(KeyBindings.PRIMARY_SKILL, down, () -> send(AbilityAction.PRIMARY_START, false),
                () -> send(AbilityAction.PRIMARY_RELEASE, false), value -> primaryPressed = value, primaryPressed);
        handle(KeyBindings.SECONDARY_SKILL, down, () -> send(AbilityAction.SECONDARY_START, false),
                () -> send(AbilityAction.SECONDARY_RELEASE, false), value -> secondaryPressed = value,
                secondaryPressed);
        handle(KeyBindings.ULTIMATE, down, () -> send(AbilityAction.ULTIMATE_START, false),
                () -> send(AbilityAction.ULTIMATE_RELEASE, false), value -> ultimatePressed = value, ultimatePressed);

        if (down && KeyBindings.DASH.isDown()) {
            send(AbilityAction.DASH, true);
        }
        if (down && KeyBindings.FLIGHT_TOGGLE.isDown()) {
            send(AbilityAction.FLIGHT_TOGGLE, true);
        }
        if (down && KeyBindings.TRANSFORM.isDown()) {
            send(AbilityAction.TRANSFORM, true);
        }
    }

    private static void handle(KeyMapping mapping, boolean down, Runnable onPress, Runnable onRelease,
                               java.util.function.Consumer<Boolean> stateSetter, boolean currentState) {
        if (!mapping.isDown()) {
            return;
        }
        if (down && !currentState) {
            stateSetter.accept(true);
            onPress.run();
        } else if (!down && currentState) {
            stateSetter.accept(false);
            onRelease.run();
        }
    }

    /**
     * Left click reports the attack intent.
     *
     * <p>Vanilla still fires its own attack packet, but that only happens when something is actually
     * in reach - a whiffed punch, an airborne combo or a chained follow up would otherwise be lost.
     * This packet carries no kind hint (the server owns the combo chain) and the server side cooldown
     * gate makes a duplicate of the vanilla event in the same tick harmless, so nothing double hits.
     * Right click is left completely alone.</p>
     */
    public static void onMouseButton(InputEvent.MouseButton.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        AnimeKiNetwork.sendToServer(new Payloads.CombatInput((byte) -1, minecraft.player.isShiftKeyDown()));
    }

    // ------------------------------------------------------------------ per tick

    public static void onClientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        if (minecraft.screen != null) {
            return;
        }
        if (!ClientState.flying) {
            // Always sync the "stop flying input" transition once.
            if (lastForward != 0.0F || lastStrafe != 0.0F || lastVertical != 0.0F || lastBoost) {
                lastForward = 0.0F;
                lastStrafe = 0.0F;
                lastVertical = 0.0F;
                lastBoost = false;
                sendFlight(0.0F, 0.0F, 0.0F, false);
            }
            return;
        }
        float forward = axis(minecraft.options.keyUp.isDown(), minecraft.options.keyDown.isDown());
        float strafe = axis(minecraft.options.keyLeft.isDown(), minecraft.options.keyRight.isDown());
        float vertical = axis(minecraft.options.keyJump.isDown(), minecraft.options.keyShift.isDown());
        boolean boost = minecraft.options.keySprint.isDown();
        ticksSinceFlightPacket++;
        if (forward != lastForward || strafe != lastStrafe || vertical != lastVertical || boost != lastBoost
                || ticksSinceFlightPacket >= FLIGHT_KEEPALIVE_TICKS) {
            lastForward = forward;
            lastStrafe = strafe;
            lastVertical = vertical;
            lastBoost = boost;
            ticksSinceFlightPacket = 0;
            sendFlight(forward, strafe, vertical, boost);
        }
    }

    private static float axis(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        }
        return positive ? 1.0F : -1.0F;
    }

    // ------------------------------------------------------------------ sending

    private static void send(AbilityAction action, boolean ignoreMovement) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        float forward = player == null ? 0.0F : (float) player.zza;
        float strafe = player == null ? 0.0F : (float) player.xxa;
        AnimeKiNetwork.sendToServer(new Payloads.AbilityInput((byte) action.ordinal(), forward, strafe, 0.0F,
                player != null && player.isSprinting(), player != null && player.isShiftKeyDown(),
                minecraft.options.keyJump.isDown()));
    }

    private static void sendFlight(float forward, float strafe, float vertical, boolean boost) {
        AnimeKiNetwork.sendToServer(new Payloads.FlightInput(forward, strafe, vertical, boost));
    }
}
