package com.megafishing.economy;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.fish.FishDefinition;
import com.megafishing.fish.FishRarity;
import com.megafishing.persistence.CaughtFishRecord;
import com.megafishing.persistence.PlayerData;
import com.megafishing.pet.PetManager;
import com.megafishing.util.MathUtil;

public class EconomyManager {
    private final MegaFishingPlugin plugin;
    private final PetManager petManager;

    public EconomyManager(MegaFishingPlugin plugin, PetManager petManager) {
        this.plugin = plugin;
        this.petManager = petManager;
    }

    public long calculateValue(PlayerData playerData, FishDefinition fish, double weight, FishRarity rarity) {
        double rarityMultiplier = plugin.getConfig().getDouble("economy.rarity-multipliers." + rarity.name(), 1.0D);
        double petMultiplier = petManager.getSellMultiplier(playerData);
        double total = fish.getBaseValue() * weight * rarityMultiplier * petMultiplier * fish.getSellMultiplier();
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
}
