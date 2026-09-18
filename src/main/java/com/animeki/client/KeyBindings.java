package com.animeki.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Every control the mod adds, in one place.
 *
 * <p>Defaults are chosen to avoid the vanilla bindings (attack/use are mouse buttons, jump is space,
 * sneak is shift): the Ki keys sit on R/F/V/C/G and the two heavy skills on X/Z, which keeps the
 * layout free for resource packs and for players who rebind.</p>
 */
public final class KeyBindings {
    public static final String CATEGORY = "key.categories.animeki";

    public static final KeyMapping KI_CHARGE = new KeyMapping("key.animeki.ki_charge", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping KI_BLAST = new KeyMapping("key.animeki.ki_blast", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F, CATEGORY);
    public static final KeyMapping PRIMARY_SKILL = new KeyMapping("key.animeki.primary_skill",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping SECONDARY_SKILL = new KeyMapping("key.animeki.secondary_skill",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY);
    public static final KeyMapping DASH = new KeyMapping("key.animeki.dash", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping FLIGHT_TOGGLE = new KeyMapping("key.animeki.flight_toggle",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);
    public static final KeyMapping TRANSFORM = new KeyMapping("key.animeki.transform", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping ULTIMATE = new KeyMapping("key.animeki.ultimate", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B, CATEGORY);

    private KeyBindings() {
    }

    public static KeyMapping[] all() {
        return new KeyMapping[] { KI_CHARGE, KI_BLAST, PRIMARY_SKILL, SECONDARY_SKILL, DASH, FLIGHT_TOGGLE,
                TRANSFORM, ULTIMATE };
    }
}
