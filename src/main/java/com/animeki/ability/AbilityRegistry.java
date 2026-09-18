package com.animeki.ability;

import com.animeki.AnimeKi;
import com.animeki.ability.impl.ChargedKiBlastAbility;
import com.animeki.ability.impl.DashStrikeAbility;
import com.animeki.ability.impl.EnergyBeamAbility;
import com.animeki.ability.impl.GroundSlamAbility;
import com.animeki.ability.impl.KiBlastAbility;
import com.animeki.ability.impl.MeteorStrikeAbility;
import com.animeki.ability.impl.PowerClashAbility;
import com.animeki.ability.impl.UltimateSphereAbility;
import com.animeki.ability.impl.VanishAbility;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Central ability registry.
 *
 * <p>Skills are registered by id and looked up by the input layer, the configuration bindings and
 * the boss controller. Adding content is a one line registration, the combat core stays untouched.</p>
 */
public final class AbilityRegistry {
    private static final Map<String, Ability> ABILITIES = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private AbilityRegistry() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        register(new KiBlastAbility());
        register(new ChargedKiBlastAbility());
        register(new EnergyBeamAbility());
        register(new PowerClashAbility());
        register(new DashStrikeAbility());
        register(new VanishAbility());
        register(new GroundSlamAbility());
        register(new MeteorStrikeAbility());
        register(new UltimateSphereAbility());
        AnimeKi.LOGGER.info("Registered {} animeki abilities", ABILITIES.size());
    }

    public static void register(Ability ability) {
        if (ABILITIES.containsKey(ability.id())) {
            AnimeKi.LOGGER.warn("Duplicate ability id {}", ability.id());
            return;
        }
        ABILITIES.put(ability.id(), ability);
    }

    @Nullable
    public static Ability byId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        bootstrap();
        Ability ability = ABILITIES.get(id);
        if (ability != null) {
            return ability;
        }
        // Tolerate ids without a namespace.
        return ABILITIES.get(AnimeKi.MOD_ID + ":" + id);
    }

    public static Collection<Ability> all() {
        bootstrap();
        return ABILITIES.values();
    }

    /**
     * Abilities in registration order.
     *
     * <p>The cooldown snapshot the server sends follows exactly this order, which is how the client
     * HUD can label its cooldown pips without extra packets.</p>
     */
    public static java.util.List<Ability> ordered() {
        bootstrap();
        return new java.util.ArrayList<>(ABILITIES.values());
    }
}
