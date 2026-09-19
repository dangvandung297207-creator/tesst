package com.megafishing.combat;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.fishing.FishingSession;
import com.megafishing.rod.RodDefinition;

public class TugOfWarController {
    private final MegaFishingPlugin plugin;
    private final DirectionController directionController;
    private final TensionManager tensionManager;
    private final DamageController damageController;

    public TugOfWarController(MegaFishingPlugin plugin, DirectionController directionController, TensionManager tensionManager, DamageController damageController) {
        this.plugin = plugin;
        this.directionController = directionController;
        this.tensionManager = tensionManager;
        this.damageController = damageController;
    }

    public CombatTick tick(org.bukkit.entity.Player player, FishingSession session, RodDefinition rod, boolean obstructed, boolean tooFar) {
        DirectionController.CounterResult counter = directionController.measure(player, session.getFish());
        double tension = tensionManager.update(session, rod, counter.state(), obstructed, tooFar);
        double damage = damageController.applyTickDamage(session, rod, counter.state());
        return new CombatTick(counter, obstructed, tooFar, tension, damage);
    }

    public void reduceTension(FishingSession session, double amount) {
        tensionManager.reduce(session, amount);
    }

    public void decayExhausted(FishingSession session) {
        tensionManager.decayExhausted(session);
    }

    public void applyExternalDamage(FishingSession session, double damage) {
        damageController.applyExternalDamage(session, damage);
    }

    public boolean isPowerWindow(FishingSession session) {
        double safeMax = plugin.getConfig().getDouble("tension.safe-max", 60.0D);
        double powerMax = plugin.getConfig().getDouble("tension.power-max", 80.0D);
        return session.getTension() >= safeMax && session.getTension() <= powerMax;
    }

    public boolean isDanger(FishingSession session) {
        return session.getTension() > plugin.getConfig().getDouble("tension.power-max", 80.0D);
    }
}
