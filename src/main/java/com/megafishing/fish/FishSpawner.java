package com.megafishing.fish;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.fishing.FishingEnvironment;
import com.megafishing.fishing.FishingZone;
import com.megafishing.util.RandomUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class FishSpawner {
    private final MegaFishingPlugin plugin;
    private final Random random = new Random();

    public FishSpawner(MegaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public FishController spawn(FishDefinition definition, FishingEnvironment environment, Location anchor) {
        FishingZone zone = environment.getZone();
        FishSizeTier sizeTier = rollSizeTier(environment);
        SizeTierSettings settings = settings(sizeTier);
        double weightProgress = settings.minProgress() + ((settings.maxProgress() - settings.minProgress()) * random.nextDouble());
        double baseWeight = definition.getMinWeight() + ((definition.getMaxWeight() - definition.getMinWeight()) * weightProgress);
        double weight = Math.max(0.01D, baseWeight * environment.getFishSizeMultiplier());
        double health = definition.getMaxHealth() * environment.getFishHpMultiplier() * settings.healthMultiplier();
        double scaleCap = plugin.getConfig().getDouble("visuals.fish-scale-cap", 8.0D);
        double visualScale = Math.min(scaleCap, definition.getModel().getScaleBase() * settings.scaleMultiplier());
        return new FishController(definition, zone, environment, anchor, weight, health, loadPhaseModifiers(), sizeTier, visualScale);
    }

    private FishSizeTier rollSizeTier(FishingEnvironment environment) {
        Map<FishSizeTier, Double> weights = new LinkedHashMap<>();
        double shift = Math.max(0.10D, environment.getLargeFishMultiplier());
        for (FishSizeTier tier : FishSizeTier.values()) {
            weights.put(tier, settings(tier).chance() * Math.pow(shift, tier.ordinal()));
        }
        FishSizeTier chosen = RandomUtil.weighted(random, weights);
        return chosen == null ? FishSizeTier.SMALL : chosen;
    }

    private SizeTierSettings settings(FishSizeTier tier) {
        String root = "fish-size-tiers." + tier.name().toLowerCase();
        return new SizeTierSettings(
                plugin.getConfig().getDouble(root + ".min-progress", tier.getMinProgress()),
                plugin.getConfig().getDouble(root + ".max-progress", tier.getMaxProgress()),
                plugin.getConfig().getDouble(root + ".chance", tier.getBaseChance()),
                plugin.getConfig().getDouble(root + ".scale", tier.getScaleMultiplier()),
                plugin.getConfig().getDouble(root + ".health-multiplier", tier.getHealthMultiplier())
        );
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

    private record SizeTierSettings(double minProgress, double maxProgress, double chance,
                                    double scaleMultiplier, double healthMultiplier) {
    }
}
