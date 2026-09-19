package com.megafishing.combat;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.fishing.FishingSession;
import com.megafishing.rod.RodDefinition;

public class DamageController {
    private final MegaFishingPlugin plugin;

    public DamageController(MegaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public double applyTickDamage(FishingSession session, RodDefinition rod, DirectionController.CounterState counterState) {
        double damagePerTick = rod.getDps() / 20.0D;
        damagePerTick *= plugin.getConfig().getDouble("direction." + counterState.name().toLowerCase() + ".damage-multiplier", 1.0D);
        double safeMax = plugin.getConfig().getDouble("tension.safe-max", 60.0D);
        double powerMax = plugin.getConfig().getDouble("tension.power-max", 80.0D);
        if (session.getTension() >= safeMax && session.getTension() <= powerMax) {
            damagePerTick *= plugin.getConfig().getDouble("tension.power-dps-multiplier", 2.0D);
        }
        damagePerTick *= stateDamageMultiplier(session);
        session.getFish().setHealth(session.getFish().getHealth() - damagePerTick);
        return damagePerTick;
    }

    public void applyExternalDamage(FishingSession session, double damage) {
        session.getFish().setHealth(session.getFish().getHealth() - damage);
    }

    private double stateDamageMultiplier(FishingSession session) {
        String statePath = "combat.state-modifiers." + session.getFish().getState().name();
        return plugin.getConfig().getDouble(statePath + ".damage-multiplier", 1.0D);
    }
}
