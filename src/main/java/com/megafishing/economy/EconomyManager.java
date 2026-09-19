package com.megafishing.economy;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.fish.FishDefinition;
import com.megafishing.fish.FishRarity;
import com.megafishing.fishing.FishingEnvironment;
import com.megafishing.persistence.CaughtFishRecord;
import com.megafishing.persistence.PlayerData;
import com.megafishing.pet.PetManager;
import com.megafishing.util.MathUtil;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EconomyManager {
    private final MegaFishingPlugin plugin;
    private final PetManager petManager;

    public EconomyManager(MegaFishingPlugin plugin, PetManager petManager) {
        this.plugin = plugin;
        this.petManager = petManager;
    }

    public long calculateValue(PlayerData playerData, FishDefinition fish, double weight, FishRarity rarity, FishingEnvironment environment) {
        double rarityMultiplier = plugin.getConfig().getDouble("economy.rarity-multipliers." + rarity.name(), 1.0D);
        double petMultiplier = petManager.getSellMultiplier(playerData);
        double worldMultiplier = environment == null ? 1.0D : environment.getFishValueMultiplier();
        double bossMultiplier = fish.isBoss() ? plugin.getConfig().getDouble("economy.boss-multiplier", 1.0D) : 1.0D;
        double weightExponent = MathUtil.clamp(plugin.getConfig().getDouble("economy.weight-exponent", 1.0D), 0.50D, 1.25D);
        double effectiveWeight = Math.pow(Math.max(1.0D, weight), weightExponent);
        double total = fish.getBaseValue() * effectiveWeight * rarityMultiplier * worldMultiplier * petMultiplier * fish.getSellMultiplier() * bossMultiplier;
        total = applySoftCaps(total);
        return MathUtil.safeMultiplyRound(total, 1.0D);
    }

    public record SellResult(int soldCount, long totalCoins) {}

    public SellResult sellAll(PlayerData data) {
        long total = 0L;
        int count = 0;
        for (CaughtFishRecord record : data.getFishBag().snapshot()) {
            total += record.getValue();
            count++;
        }
        if (count <= 0) {
            return new SellResult(0, 0L);
        }
        data.getFishBag().clear();
        data.addCoins(total);
        data.getStatistics().addCoinsEarned(total);
        if (total > 0) {
            data.getStatistics().updateHighestValue(total);
        }
        return new SellResult(count, total);
    }

    private double applySoftCaps(double value) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("economy.softcaps");
        if (section == null) {
            return value;
        }
        List<SoftCap> caps = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection cap = section.getConfigurationSection(key);
            if (cap == null) {
                continue;
            }
            caps.add(new SoftCap(
                    Math.max(0.0D, cap.getDouble("threshold", 0.0D)),
                    MathUtil.clamp(cap.getDouble("overflow-multiplier", 1.0D), 0.0D, 1.0D)
            ));
        }
        caps.sort(Comparator.comparingDouble(SoftCap::threshold));
        double adjusted = value;
        for (SoftCap cap : caps) {
            adjusted = applySoftCap(adjusted, cap.threshold(), cap.overflowMultiplier());
        }
        return adjusted;
    }

    private double applySoftCap(double value, double threshold, double overflowMultiplier) {
        if (threshold <= 0.0D || value <= threshold) {
            return value;
        }
        return threshold + ((value - threshold) * overflowMultiplier);
    }

    private record SoftCap(double threshold, double overflowMultiplier) {
    }
}
