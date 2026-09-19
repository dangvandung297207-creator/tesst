package com.megafishing.combat;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.fish.FishController;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class DirectionController {
    public enum CounterState {
        GOOD,
        NEUTRAL,
        BAD
    }

    public record CounterResult(CounterState state, double dot, String arrow) {
    }

    private final MegaFishingPlugin plugin;

    public DirectionController(MegaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public CounterResult measure(Player player, FishController fish) {
        Vector playerLook = player.getLocation().getDirection().clone().setY(0);
        Vector fishDirection = fish.getDirection().clone().setY(0);
        if (playerLook.lengthSquared() < 0.0001D || fishDirection.lengthSquared() < 0.0001D) {
            return new CounterResult(CounterState.NEUTRAL, 0.0D, "↔");
        }
        playerLook.normalize();
        fishDirection.normalize();
        double dot = playerLook.dot(fishDirection);
        double goodMax = plugin.getConfig().getDouble("direction.good-dot-max", -0.5D);
        double neutralMax = plugin.getConfig().getDouble("direction.neutral-dot-max", 0.5D);
        CounterState state = dot <= goodMax ? CounterState.GOOD : dot < neutralMax ? CounterState.NEUTRAL : CounterState.BAD;
        double cross = playerLook.getX() * fishDirection.getZ() - playerLook.getZ() * fishDirection.getX();
        String arrow = fish.getState() == com.megafishing.fish.FishState.DIVE ? "↑ COUNTER!" : cross > 0 ? "→ COUNTER!" : "← COUNTER!";
        return new CounterResult(state, dot, arrow);
    }
}
