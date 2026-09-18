package com.animeki.ability;

import com.animeki.ability.FeedbackReason;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Cooldown bookkeeping for one player. Kept separate from the Ki pool on purpose. */
public class PlayerAbilityData implements INBTSerializable<CompoundTag> {
    private final Map<String, Long> cooldowns = new HashMap<>();
    private String activeAbilityId = "";
    private FeedbackReason lastFeedback = FeedbackReason.NONE;
    private String feedbackAbilityId = "";

    public long readyAt(String abilityId) {
        return cooldowns.getOrDefault(abilityId, 0L);
    }

    public void setCooldown(String abilityId, long readyAtGameTime) {
        cooldowns.put(abilityId, readyAtGameTime);
    }

    public int remaining(String abilityId, long gameTime) {
        long ready = readyAt(abilityId);
        return ready <= gameTime ? 0 : (int) (ready - gameTime);
    }

    public void clear() {
        cooldowns.clear();
    }

    public FeedbackReason lastFeedback() {
        return lastFeedback;
    }

    public void setLastFeedback(FeedbackReason reason) {
        this.lastFeedback = reason == null ? FeedbackReason.NONE : reason;
    }

    /** Ability id the last feedback belonged to, for HUD highlighting. */
    public String feedbackAbilityId() {
        return feedbackAbilityId;
    }

    public void setFeedbackAbilityId(String id) {
        this.feedbackAbilityId = id == null ? "" : id;
    }

    public String activeAbilityId() {
        return activeAbilityId;
    }

    public void setActiveAbilityId(String id) {
        this.activeAbilityId = id == null ? "" : id;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        CompoundTag cooldownTag = new CompoundTag();
        cooldowns.forEach(cooldownTag::putLong);
        tag.put("cooldowns", cooldownTag);
        tag.putString("active", activeAbilityId);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        cooldowns.clear();
        CompoundTag cooldownTag = tag.getCompound("cooldowns");
        for (String key : cooldownTag.getAllKeys()) {
            cooldowns.put(key, cooldownTag.getLong(key));
        }
        activeAbilityId = tag.getString("active");
    }
}
