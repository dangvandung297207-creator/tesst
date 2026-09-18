package com.animeki.ki;

import com.animeki.config.AnimeKiServerConfig;
import com.animeki.registry.ModAttachments;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * The player's Ki pool and the transient state around it.
 *
 * <p>Stored as a data attachment, so it survives respawn (see {@code ModAttachments}) and is saved
 * with the player. The current value is never synced directly: the HUD receives a purpose built
 * snapshot at a low rate instead.</p>
 */
public class PlayerKi implements INBTSerializable<CompoundTag> {
    private double current;
    private double bonusMax;
    private boolean charging;
    private int chargeTicks;
    private long exhaustedUntil = Long.MIN_VALUE;
    private long regenerationBlockedUntil = Long.MIN_VALUE;
    private boolean enabled = true;

    // ------------------------------------------------------------------ values

    public double current() {
        return current;
    }

    public void setCurrent(double value) {
        this.current = Math.max(0.0D, value);
    }

    /** Convenience overload that clamps against the player's current maximum. */
    public void setCurrent(double value, Player player) {
        this.current = Math.max(0.0D, Math.min(value, max(player)));
    }

    public double max(Player player) {
        return Math.max(1.0D, AnimeKiServerConfig.KI.maxKi.get() * KiMath.kiMultiplier(player) + bonusMax);
    }

    public void clampToMax(Player player) {
        this.current = Math.max(0.0D, Math.min(current, max(player)));
    }

    public double ratio(Player player) {
        return current / max(player);
    }

    public void add(double amount, Player player) {
        this.current = Math.max(0.0D, Math.min(current + amount, max(player)));
    }

    public void set(double amount, Player player) {
        this.current = Math.max(0.0D, Math.min(amount, max(player)));
    }

    public double bonusMax() {
        return bonusMax;
    }

    public void setBonusMax(double value) {
        this.bonusMax = Math.max(0.0D, value);
    }

    // ------------------------------------------------------------------ flags

    public boolean isCharging() {
        return charging;
    }

    public void setCharging(boolean value) {
        this.charging = value;
        if (!value) {
            this.chargeTicks = 0;
        }
    }

    public int chargeTicks() {
        return chargeTicks;
    }

    public void incrementChargeTicks() {
        this.chargeTicks++;
    }

    public long exhaustedUntil() {
        return exhaustedUntil;
    }

    public void setExhaustedUntil(long gameTime) {
        this.exhaustedUntil = gameTime;
    }

    public boolean isExhausted(long gameTime) {
        return gameTime < exhaustedUntil;
    }

    public long regenerationBlockedUntil() {
        return regenerationBlockedUntil;
    }

    public void blockRegeneration(long untilGameTime) {
        this.regenerationBlockedUntil = Math.max(regenerationBlockedUntil, untilGameTime);
    }

    public boolean isRegenerationBlocked(long gameTime) {
        return gameTime < regenerationBlockedUntil;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    public boolean isFull(Player player) {
        return current >= max(player) - 1.0E-4D;
    }

    // ------------------------------------------------------------------ copies

    public void copyFrom(PlayerKi other) {
        this.current = other.current;
        this.bonusMax = other.bonusMax;
        this.charging = false;
        this.chargeTicks = 0;
        this.exhaustedUntil = other.exhaustedUntil;
        this.regenerationBlockedUntil = other.regenerationBlockedUntil;
        this.enabled = other.enabled;
    }

    // ------------------------------------------------------------------ nbt

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Current", current);
        tag.putDouble("BonusMax", bonusMax);
        tag.putLong("ExhaustedUntil", exhaustedUntil);
        tag.putLong("RegenBlockedUntil", regenerationBlockedUntil);
        tag.putBoolean("Enabled", enabled);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.current = tag.getDouble("Current");
        this.bonusMax = tag.getDouble("BonusMax");
        this.exhaustedUntil = tag.contains("ExhaustedUntil") ? tag.getLong("ExhaustedUntil") : Long.MIN_VALUE;
        this.regenerationBlockedUntil = tag.contains("RegenBlockedUntil") ? tag.getLong("RegenBlockedUntil")
                : Long.MIN_VALUE;
        this.enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        this.charging = false;
        this.chargeTicks = 0;
    }

    /** Convenience for systems that only have the player at hand. */
    public static PlayerKi of(Player player) {
        return ModAttachments.ki(player);
    }
}
