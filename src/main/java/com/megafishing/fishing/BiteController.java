package com.megafishing.fishing;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.fish.FishController;
import com.megafishing.fish.FishManager;
import com.megafishing.util.MathUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Random;

public class BiteController {
    private final MegaFishingPlugin plugin;
    private final FishManager fishManager;
    private final Random random = new Random();

    public BiteController(MegaFishingPlugin plugin, FishManager fishManager) {
        this.plugin = plugin;
        this.fishManager = fishManager;
    }

    public void schedule(FishingSession session, long currentTick) {
        double min = plugin.getConfig().getDouble("fishing.bite.min-delay", 2.0D);
        double max = plugin.getConfig().getDouble("fishing.bite.max-delay", 8.0D);
        if (max < min) {
            max = min;
        }
        double baseSeconds = min + (random.nextDouble() * (max - min));
        double biteSeconds = baseSeconds;
        if (session.getEnvironment() != null) {
            biteSeconds *= session.getEnvironment().getBiteTimeMultiplier();
            biteSeconds /= Math.max(0.05D, session.getEnvironment().getEncounterMultiplier());
            biteSeconds = MathUtil.clamp(biteSeconds,
                    session.getEnvironment().getProfile().getMinBiteSeconds(),
                    session.getEnvironment().getProfile().getMaxBiteSeconds());
        }
        session.setBiteTick(currentTick + Math.round(biteSeconds * 20.0D));
    }

    public boolean shouldBite(FishingSession session, long currentTick) {
        return currentTick >= session.getBiteTick();
    }

    public void bite(Player player, FishingSession session, long currentTick) {
        FishController fish = fishManager.rollAndSpawn(session.getEnvironment(), session.getCastLocation());
        if (fish == null) {
            schedule(session, currentTick);
            return;
        }
        session.setFish(fish);
        fish.spawnVisual();
        session.setState(FishingState.BITE);
        session.setBiteReactionEndTick(currentTick + Math.round(plugin.getConfig().getDouble("fishing.bite.reaction-window", 2.0D) * 20.0D));
        player.getWorld().spawnParticle(Particle.SPLASH, session.getCastLocation(), 12, 0.25D, 0.2D, 0.25D, 0.02D);
        player.playSound(session.getCastLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0F, 1.0F);
        plugin.messages().send(player, "bite");
    }
}
