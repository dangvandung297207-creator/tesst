package com.animeki.combat;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Per player state of the melee combo chain.
 *
 * <p>Keeps the combo index, the timing of the last swing so the chain can expire, the last target
 * (used by the chase attack) and the post vanish damage window.</p>
 */
public class ComboState implements INBTSerializable<CompoundTag> {
    private int step;
    private int hits;
    private int displayHits;
    private long lastAttackTick = Long.MIN_VALUE;
    private long lastHitTick = Long.MIN_VALUE;
    private long vanishBonusUntil = Long.MIN_VALUE;
    private long dodgeUntil = Long.MIN_VALUE;
    @Nullable
    private UUID lastTarget;

    public int step() {
        return step;
    }

    public void setStep(int value) {
        this.step = Math.max(0, value);
    }

    public int hits() {
        return hits;
    }

    public void setHits(int value) {
        this.hits = Math.max(0, value);
        this.displayHits = this.hits;
    }

    public void incrementHits() {
        this.hits++;
        this.displayHits = this.hits;
    }

    /** Number shown on the HUD (kept separate so the counter can fade out). */
    public int displayHits() {
        return displayHits;
    }

    public long lastAttackTick() {
        return lastAttackTick;
    }

    public void setLastAttackTick(long tick) {
        this.lastAttackTick = tick;
    }

    public long lastHitTick() {
        return lastHitTick;
    }

    public void setLastHitTick(long tick) {
        this.lastHitTick = tick;
    }

    public long vanishBonusUntil() {
        return vanishBonusUntil;
    }

    public void setVanishBonusUntil(long tick) {
        this.vanishBonusUntil = tick;
    }

    public void setVanishBonusUntil(long tick) {
        this.vanishBonusUntil = tick;
    }

    public boolean hasVanishBonus(long gameTime) {
        return gameTime < vanishBonusUntil;
    }

    public long dodgeUntil() {
        return dodgeUntil;
    }

    public void setDodgeUntil(long tick) {
        this.dodgeUntil = tick;
    }

    public boolean isDodging(long gameTime) {
        return gameTime < dodgeUntil;
    }

    @Nullable
    public UUID lastTarget() {
        return lastTarget;
    }

    public void setLastTarget(@Nullable UUID target) {
        this.lastTarget = target;
    }

    public boolean comboActive(long gameTime, int windowTicks) {
        return step > 0 && gameTime - lastAttackTick <= windowTicks;
    }

    public void reset() {
        this.step = 0;
        this.hits = 0;
        this.lastTarget = null;
    }

    public void resetComboOnly() {
        this.step = 0;
        this.hits = 0;
        this.displayHits = 0;
    }

    public void copyFrom(ComboState other) {
        this.step = other.step;
        this.hits = other.hits;
        this.lastAttackTick = other.lastAttackTick;
        this.lastHitTick = other.lastHitTick;
        this.vanishBonusUntil = other.vanishBonusUntil;
        this.dodgeUntil = other.dodgeUntil;
        this.lastTarget = other.lastTarget;
    }

    // ------------------------------------------------------------------ nbt

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Step", step);
        tag.putInt("Hits", hits);
        tag.putLong("VanishBonusUntil", vanishBonusUntil);
        tag.putLong("DodgeUntil", dodgeUntil);
        if (lastTarget != null) {
            tag.putUUID("LastTarget", lastTarget);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.step = tag.getInt("Step");
        this.hits = tag.getInt("Hits");
        this.displayHits = this.hits;
        this.vanishBonusUntil = tag.contains("VanishBonusUntil") ? tag.getLong("VanishBonusUntil") : Long.MIN_VALUE;
        this.dodgeUntil = tag.contains("DodgeUntil") ? tag.getLong("DodgeUntil") : Long.MIN_VALUE;
        this.lastTarget = tag.hasUUID("LastTarget") ? tag.getUUID("LastTarget") : null;
        this.lastAttackTick = Long.MIN_VALUE;
        this.lastHitTick = Long.MIN_VALUE;
    }
}
