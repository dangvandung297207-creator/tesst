package com.animeki.transformation;

import com.animeki.AnimeKi;
import com.animeki.config.AnimeKiServerConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Registry of the built in transformation forms.
 *
 * <p>The four shipped forms are original (ASCENDED, DIVINE, CELESTIAL, TRANSCENDENT) and all of
 * their numbers come from the server configuration. Addons may register extra forms with
 * {@link #register(Transformation)} - the sequence logic in {@link TransformationService} is shared
 * and never needs to know about specific forms.</p>
 */
public final class Transformations {
    public static final String ASCENDED = "ascended";
    public static final String DIVINE = "divine";
    public static final String CELESTIAL = "celestial";
    public static final String TRANSCENDENT = "transcendent";

    private static final Map<String, Transformation> FORMS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private Transformations() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;

        AnimeKiServerConfig.Transformations config = AnimeKiServerConfig.TRANSFORMATIONS;
        int drainInterval = Math.max(1, config.globalDrainInterval.get());
        int speed = Math.max(4, config.ascended.sequenceSpeed.get());

        // Golden, aggressive and cheap: the first form every fighter learns.
        registerFrom(ASCENDED, 1, config.ascended, speed, drainInterval,
                AuraProfile.builder()
                        .colors(0xFFFFF3B0, 0x88FFD54A, 0xFFFFE680)
                        .intensity(1.0F)
                        .scale(1.05F)
                        .rings(2, 6.0F)
                        .flames(3)
                        .groundAura(true)
                        .build());

        // Crimson/pink, focused on Ki control and flight.
        registerFrom(DIVINE, 2, config.divine, speed, drainInterval,
                AuraProfile.builder()
                        .colors(0xFFFFC8DA, 0x99FF4D7E, 0xFFFF9EC4)
                        .intensity(1.15F)
                        .scale(1.1F)
                        .rings(3, 9.0F)
                        .flames(4)
                        .groundAura(true)
                        .build());

        // Blue/white, huge Ki output and stronger beams.
        registerFrom(CELESTIAL, 3, config.celestial, speed, drainInterval,
                AuraProfile.builder()
                        .colors(0xFFE6F4FF, 0x9933A8FF, 0xFF9FD8FF)
                        .intensity(1.3F)
                        .scale(1.15F)
                        .rings(4, 12.0F)
                        .flames(5)
                        .lightning(true)
                        .build());

        // White/silver, dodge oriented with the heaviest melee bonuses.
        registerFrom(TRANSCENDENT, 4, config.transcendent, speed, drainInterval,
                AuraProfile.builder()
                        .colors(0xFFFFFFFF, 0xCCE8E8F0, 0xFFFFFFFF)
                        .intensity(1.5F)
                        .scale(1.25F)
                        .rings(5, 15.0F)
                        .flames(6)
                        .lightning(true)
                        .build());

        AnimeKi.LOGGER.info("Registered {} transformation forms", FORMS.size());
    }

    private static void registerFrom(String id, int tier, AnimeKiServerConfig.Transformations.Form form,
                                     int speed, int drainInterval, AuraProfile aura) {
        if (!form.enabled.get()) {
            return;
        }
        register(Transformation.builder(id)
                .displayKey("animeki.transformation." + id)
                .tier(tier)
                .multipliers(form.kiMultiplier.get(), form.damageMultiplier.get(), form.defenseMultiplier.get(),
                        form.speedMultiplier.get(), form.flightMultiplier.get(), form.regenerationMultiplier.get())
                .cost(form.activationKiCost.get(), form.activationKiCost.get() * 1.25D,
                        form.drainPerSecond.get() / drainInterval, form.durationTicks.get())
                .sequence(speed, speed * 2, speed, Math.max(6, speed / 2))
                .destructiveActivation(tier >= 3)
                .aura(aura)
                .build());
    }

    public static void register(Transformation transformation) {
        FORMS.put(transformation.id(), transformation);
    }

    @Nullable
    public static Transformation byId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        bootstrap();
        return FORMS.get(id);
    }

    public static Collection<Transformation> all() {
        bootstrap();
        return FORMS.values();
    }

    /** Next form in tier order, wrapping around; used when the Transform key is pressed. */
    @Nullable
    public static Transformation next(@Nullable String currentId) {
        bootstrap();
        if (FORMS.isEmpty()) {
            return null;
        }
        List<Transformation> ordered = new ArrayList<>(FORMS.values());
        ordered.sort((a, b) -> Integer.compare(a.tier(), b.tier()));
        if (currentId == null || currentId.isEmpty()) {
            return ordered.get(0);
        }
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).id().equals(currentId)) {
                return ordered.get((i + 1) % ordered.size());
            }
        }
        return ordered.get(0);
    }
}
