package com.megafishing.combat;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.fishing.FishingSession;
import com.megafishing.rod.RodDefinition;
import com.megafishing.util.MathUtil;

public class TensionManager {
    private final MegaFishingPlugin plugin;

    public TensionManager(MegaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public double update(FishingSession session, RodDefinition rod, DirectionController.CounterState counterState, boolean obstructed, boolean tooFar) {
        double delta = session.getFish().getDefinition().getPullPower() * session.getFish().getCurrentPullMultiplier();
        delta -= session.getFish().getDefinition().getPullPower() * rod.getTensionResistance();
        delta *= plugin.getConfig().getDouble("direction." + counterState.name().toLowerCase() + ".tension-multiplier", 1.0D);

        if (counterState == DirectionController.CounterState.GOOD) {
            delta -= plugin.getConfig().getDouble("direction.good.tension-decay-per-second", 5.0D) / 20.0D;
        } else if (counterState == DirectionController.CounterState.NEUTRAL) {
            delta -= plugin.getConfig().getDouble("direction.neutral.tension-decay-per-second", 1.0D) / 20.0D;
        } else {
            delta += plugin.getConfig().getDouble("direction.bad.tension-gain-per-second", 5.0D) / 20.0D;
        }
        delta += stateTensionBonus(session);
        if (obstructed) {
            delta += plugin.getConfig().getDouble("tension.obstruction-gain-per-second", 8.0D) / 20.0D;
        }
        if (tooFar) {
            delta += plugin.getConfig().getDouble("tension.out-of-range-gain-per-second", 12.0D) / 20.0D;
        }
        double result = MathUtil.clamp(session.getTension() + delta, 0.0D, plugin.getConfig().getDouble("tension.break-point", 100.0D));
        session.setTension(result);
        return result;
    }

    public void reduce(FishingSession session, double amount) {
        session.setTension(Math.max(0.0D, session.getTension() - amount));
    }

    public void decayExhausted(FishingSession session) {
        session.setTension(Math.max(0.0D, session.getTension() - (plugin.getConfig().getDouble("tension.exhausted-decay-per-second", 12.0D) / 20.0D)));
    }

    private double stateTensionBonus(FishingSession session) {
        String statePath = "combat.state-modifiers." + session.getFish().getState().name();
        return plugin.getConfig().getDouble(statePath + ".tension-bonus-per-second", 0.0D) / 20.0D;
    }
}
