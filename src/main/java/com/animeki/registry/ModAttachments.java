package com.animeki.registry;

import com.animeki.AnimeKi;
import com.animeki.ability.PlayerAbilityData;
import com.animeki.combat.ComboState;
import com.animeki.flight.FlightState;
import com.animeki.ki.PlayerKi;
import com.animeki.transformation.PlayerTransformation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Data attachments that store all per player combat state.
 *
 * <p>Using attachments (instead of a capability or a mixin) keeps the mod dependency free and makes
 * the state automatically persistent and copied on death where it matters.</p>
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, AnimeKi.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerKi>> KI =
            ATTACHMENT_TYPES.register("ki", () -> AttachmentType.serializable(PlayerKi::new)
                    .copyOnDeath()
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ComboState>> COMBAT =
            ATTACHMENT_TYPES.register("combat", () -> AttachmentType.serializable(ComboState::new).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FlightState>> FLIGHT =
            ATTACHMENT_TYPES.register("flight", () -> AttachmentType.serializable(FlightState::new).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerTransformation>> TRANSFORMATION =
            ATTACHMENT_TYPES.register("transformation", () -> AttachmentType.serializable(PlayerTransformation::new)
                    .copyOnDeath()
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerAbilityData>> ABILITIES =
            ATTACHMENT_TYPES.register("abilities", () -> AttachmentType.serializable(PlayerAbilityData::new).build());

    private ModAttachments() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }

    // ---------------------------------------------------------------- accessors

    public static PlayerKi ki(Player player) {
        return player.getData(KI.get());
    }

    public static ComboState combat(Player player) {
        return player.getData(COMBAT.get());
    }

    public static FlightState flight(Player player) {
        return player.getData(FLIGHT.get());
    }

    public static PlayerTransformation transformation(Player player) {
        return player.getData(TRANSFORMATION.get());
    }

    public static PlayerAbilityData abilities(Player player) {
        return player.getData(ABILITIES.get());
    }

    /** Copies state across respawn / dimension change. */
    public static void copyOnClone(Player original, Player replacement) {
        ki(replacement).copyFrom(ki(original));
        transformation(replacement).copyFrom(transformation(original));
        abilities(replacement).deserializeNBT(original.registryAccess(), abilities(original).serializeNBT(original.registryAccess()));
        flight(replacement).setActive(false);
        combat(replacement).reset();
    }
}
