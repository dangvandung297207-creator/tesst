package com.animeki.transformation;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

/** Server side transformation state for a single player. */
public class PlayerTransformation implements INBTSerializable<CompoundTag> {
    private String activeId = "";
    private TransformationPhase phase = TransformationPhase.NONE;
    private int phaseTick;
    private int activeTicks;
    private float auraIntensity;
    /** Last form the player used, so the transform key can walk up the tiers instead of restarting. */
    private String lastFormId = "";

    /** The form that is currently granting stats, or {@code null}. */
    @Nullable
    public Transformation active() {
        return activeId.isEmpty() ? null : Transformations.byId(activeId);
    }

    public String activeId() {
        return activeId;
    }

    public boolean isActive() {
        return !activeId.isEmpty();
    }

    public boolean isTransforming() {
        return phase.isRunning();
    }

    public void begin(String id) {
        this.activeId = id;
        this.phase = TransformationPhase.CHARGING;
        this.phaseTick = 0;
        this.activeTicks = 0;
        this.auraIntensity = 0.0F;
    }

    public void setPhase(TransformationPhase phase) {
        this.phase = phase;
        this.phaseTick = 0;
    }

    public TransformationPhase phase() {
        return phase;
    }

    public int phaseTick() {
        return phaseTick;
    }

    public void incrementPhaseTick() {
        this.phaseTick++;
    }

    public int activeTicks() {
        return activeTicks;
    }

    public void incrementActiveTicks() {
        this.activeTicks++;
    }

    public String lastFormId() {
        return lastFormId;
    }

    public void setLastFormId(String id) {
        this.lastFormId = id == null ? "" : id;
    }

    public float auraIntensity() {
        return auraIntensity;
    }

    public void setAuraIntensity(float value) {
        this.auraIntensity = value;
    }

    public void clear() {
        lastFormId = activeId.isEmpty() ? lastFormId : activeId;
        this.activeId = "";
        this.phase = TransformationPhase.NONE;
        this.phaseTick = 0;
        this.activeTicks = 0;
        this.auraIntensity = 0.0F;
    }

    public void copyFrom(PlayerTransformation other) {
        this.activeId = other.activeId;
        this.phase = other.phase == TransformationPhase.ACTIVE ? TransformationPhase.ACTIVE : TransformationPhase.NONE;
        this.phaseTick = 0;
        this.activeTicks = other.activeTicks;
        this.auraIntensity = other.auraIntensity;
        this.lastFormId = other.lastFormId;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("activeId", activeId);
        tag.putString("phase", phase.name());
        tag.putInt("phaseTick", phaseTick);
        tag.putInt("activeTicks", activeTicks);
        tag.putFloat("auraIntensity", auraIntensity);
        tag.putString("lastFormId", lastFormId);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        activeId = tag.getString("activeId");
        lastFormId = tag.getString("lastFormId");
        try {
            phase = TransformationPhase.valueOf(tag.getString("phase"));
        } catch (IllegalArgumentException exception) {
            phase = activeId.isEmpty() ? TransformationPhase.NONE : TransformationPhase.ACTIVE;
        }
        phaseTick = tag.getInt("phaseTick");
        activeTicks = tag.getInt("activeTicks");
        auraIntensity = tag.getFloat("auraIntensity");
    }
}
