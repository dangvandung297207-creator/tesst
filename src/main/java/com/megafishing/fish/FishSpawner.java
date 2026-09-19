package com.megafishing.fish;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.fishing.FishingZone;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class FishSpawner {
    private final MegaFishingPlugin plugin;
    private final Random random = new Random();

    public FishSpawner(MegaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public FishController spawn(FishDefinition definition, FishingZone zone, Location anchor) {
        double weightRoll = Math.pow(random.nextDouble(), 1.65D);
        double weight = definition.getMinWeight() + ((definition.getMaxWeight() - definition.getMinWeight()) * weightRoll);
        return new FishController(definition, zone, anchor, weight, definition.getMaxHealth(), loadPhaseModifiers());
    }

    private Map<FishPhase, FishPhaseModifiers> loadPhaseModifiers() {
        Map<FishPhase, FishPhaseModifiers> modifiers = new EnumMap<>(FishPhase.class);
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("fish-phases");
        for (FishPhase phase : FishPhase.values()) {
            ConfigurationSection section = root == null ? null : root.getConfigurationSection(phase.name());
            modifiers.put(phase, new FishPhaseModifiers(
                    section == null ? defaultSpeed(phase) : section.getDouble("speed-multiplier", defaultSpeed(phase)),
                    section == null ? defaultPull(phase) : section.getDouble("pull-multiplier", defaultPull(phase)),
                    section == null ? defaultTurn(phase) : section.getDouble("turn-rate-multiplier", defaultTurn(phase))
            ));
        }
        return modifiers;
    }

    private double defaultSpeed(FishPhase phase) {
        return switch (phase) {
            case ENRAGED -> 1.15D;
            case FRENZY -> 1.35D;
            default -> 1.0D;
        };
    }

    private double defaultPull(FishPhase phase) {
        return switch (phase) {
            case ENRAGED -> 1.18D;
            case FRENZY -> 1.40D;
            default -> 1.0D;
        };
    }

    private double defaultTurn(FishPhase phase) {
        return switch (phase) {
            case ENRAGED -> 1.15D;
            case FRENZY -> 1.35D;
            default -> 1.0D;
        };
    }
}
