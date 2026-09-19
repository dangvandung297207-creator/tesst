package com.megafishing.skill;

import com.megafishing.combat.TugOfWarController;
import com.megafishing.fishing.FishingSession;
import com.megafishing.fishing.FishingState;
import com.megafishing.rod.RodDefinition;

public class SkillController {
    public record Result(boolean success, String reasonKey, double damage, double tensionReduction, long cooldownSeconds) {}

    public Result activate(FishingSession session, SkillDefinition skill, RodDefinition rod, long currentTick, TugOfWarController tugOfWarController) {
        if (session == null || skill == null || rod == null) {
            return new Result(false, "invalid", 0.0D, 0.0D, 0L);
        }
        if (session.getState() != FishingState.FIGHTING && session.getState() != FishingState.EXHAUSTED) {
            return new Result(false, "invalid_state", 0.0D, 0.0D, 0L);
        }
        if (currentTick < session.getSkillCooldownEndTick()) {
            long seconds = (long) Math.ceil((session.getSkillCooldownEndTick() - currentTick) / 20.0D);
            return new Result(false, "cooldown", 0.0D, 0.0D, seconds);
        }
        double damage = rod.getDps() * skill.getDamageMultiplier();
        tugOfWarController.applyExternalDamage(session, damage);
        tugOfWarController.reduceTension(session, skill.getTensionReduction());
        int cooldown = Math.max(skill.getCooldownSeconds(), rod.getSkillCooldown());
        session.setSkillCooldownEndTick(currentTick + cooldown * 20L);
        return new Result(true, "success", damage, skill.getTensionReduction(), cooldown);
    }
}
