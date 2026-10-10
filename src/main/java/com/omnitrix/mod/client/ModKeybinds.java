package com.omnitrix.mod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.omnitrix.mod.OmnitrixMod;
import com.omnitrix.mod.client.gui.AlienSelectScreen;
import com.omnitrix.mod.data.OmnitrixPlayerData;
import com.omnitrix.mod.network.ModNetwork;
import com.omnitrix.mod.network.payload.OmnitrixAbilityPayload;
import com.omnitrix.mod.network.payload.OmnitrixRevertPayload;
import com.omnitrix.mod.network.payload.OmnitrixTransformPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = OmnitrixMod.MODID, value = Dist.CLIENT)
public class ModKeybinds {

    public static final String CATEGORY = "key.categories.omnitrix";

    public static final KeyMapping KEY_OPEN_SELECTOR = new KeyMapping(
            "key.omnitrix.open_selector",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    public static final KeyMapping KEY_TRANSFORM = new KeyMapping(
            "key.omnitrix.transform",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    public static final KeyMapping KEY_REVERT = new KeyMapping(
            "key.omnitrix.revert",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY
    );

    public static final KeyMapping KEY_ABILITY_1 = new KeyMapping(
            "key.omnitrix.ability_1",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            CATEGORY
    );

    public static final KeyMapping KEY_ABILITY_2 = new KeyMapping(
            "key.omnitrix.ability_2",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY
    );

    public static final KeyMapping KEY_ABILITY_3 = new KeyMapping(
            "key.omnitrix.ability_3",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY
    );

    public static final KeyMapping KEY_ULTIMATE = new KeyMapping(
            "key.omnitrix.ultimate",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(KEY_OPEN_SELECTOR);
        event.register(KEY_TRANSFORM);
        event.register(KEY_REVERT);
        event.register(KEY_ABILITY_1);
        event.register(KEY_ABILITY_2);
        event.register(KEY_ABILITY_3);
        event.register(KEY_ULTIMATE);
    }

    private static int ability2HoldTicks = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        OmnitrixPlayerData data = ClientOmnitrixHandler.getLocalPlayerData();

        // 1. Selector wheel toggle
        if (KEY_OPEN_SELECTOR.consumeClick()) {
            AlienSelectScreen.open();
        }

        // 2. Direct transform trigger
        if (KEY_TRANSFORM.consumeClick()) {
            ModNetwork.sendToServer(new OmnitrixTransformPayload(false));
        }

        // 3. Revert back to human
        if (KEY_REVERT.consumeClick()) {
            ModNetwork.sendToServer(new OmnitrixRevertPayload(true));
        }

        // 4. Ability 1
        if (KEY_ABILITY_1.consumeClick()) {
            ModNetwork.sendToServer(new OmnitrixAbilityPayload(1, false));
        }

        // 5. Ability 2 (supports charge when held!)
        if (KEY_ABILITY_2.isDown()) {
            ability2HoldTicks++;
        } else if (ability2HoldTicks > 0) {
            boolean isCharged = ability2HoldTicks >= 20; // 1 second charge
            ModNetwork.sendToServer(new OmnitrixAbilityPayload(2, isCharged));
            ability2HoldTicks = 0;
        }

        // 6. Ability 3
        if (KEY_ABILITY_3.consumeClick()) {
            ModNetwork.sendToServer(new OmnitrixAbilityPayload(3, false));
        }

        // 7. Ultimate
        if (KEY_ULTIMATE.consumeClick()) {
            ModNetwork.sendToServer(new OmnitrixAbilityPayload(4, false));
        }
    }
}
