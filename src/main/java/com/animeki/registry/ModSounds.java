package com.animeki.registry;

import com.animeki.AnimeKi;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sound hooks.
 *
 * <p>The sound events (and therefore the sound ids used by abilities and VFX definitions) are
 * registered by code while the actual {@code .ogg} files live in
 * {@code assets/animeki/sounds/}. Placeholder audio ships with the mod and can be replaced by any
 * DAW / Blender audio export without touching code.</p>
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, AnimeKi.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> KI_CHARGE = register("ki_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> KI_CHARGE_LOOP = register("ki_charge_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> KI_BLAST_FIRE = register("ki_blast_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> KI_BLAST_IMPACT = register("ki_blast_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEAM_CHARGE = register("beam_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEAM_FIRE = register("beam_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEAM_IMPACT = register("beam_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLASH_LOOP = register("clash_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLASH_BURST = register("clash_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> DASH = register("dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONIC_BOOM = register("sonic_boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANISH = register("vanish");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRANSFORM_BUILDUP = register("transform_buildup");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRANSFORM_BURST = register("transform_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTIMATE_CHARGE = register("ultimate_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTIMATE_IMPACT = register("ultimate_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROUND_SLAM = register("ground_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> MELEE_LIGHT = register("melee_light");
    public static final DeferredHolder<SoundEvent, SoundEvent> MELEE_HEAVY = register("melee_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLIGHT_LOOP = register("flight_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_ROAR = register("boss_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_PHASE = register("boss_phase");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = AnimeKi.id(name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
