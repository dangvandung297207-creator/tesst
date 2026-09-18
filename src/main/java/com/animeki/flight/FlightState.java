package com.animeki.flight;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Per player flight and dash state.
 *
 * <p>The server owns this; the client mirrors it to predict movement but never decides it. The
 * velocity field is also used by the dash system so both mechanics share one source of truth.</p>
 */
public class FlightState implements INBTSerializable<CompoundTag> {
    private boolean active;
    private boolean boosting;
    private boolean hovering;
    private int ticks;
    private int airDashCharges;
    private Vec3 velocity = Vec3.ZERO;
    private int dashTicks;
    private Vec3 dashDirection = Vec3.ZERO;
    private double dashSpeed;
    private long lastDashTick = Long.MIN_VALUE;
    private long disabledUntil = Long.MIN_VALUE;
    private float forwardInput;
    private float strafeInput;
    private float verticalInput;
    private long lastInputTick = Long.MIN_VALUE;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean value) {
        this.active = value;
        if (!value) {
            this.boosting = false;
            this.hovering = false;
            this.forwardInput = 0.0F;
            this.strafeInput = 0.0F;
            this.verticalInput = 0.0F;
        }
    }

    public boolean isBoosting() {
        return boosting;
    }

    public void setBoosting(boolean value) {
        this.boosting = value;
    }

    public boolean isHovering() {
        return hovering;
    }

    public void setHovering(boolean value) {
        this.hovering = value;
    }

    public int ticks() {
        return ticks;
    }

    public void setTicks(int value) {
        this.ticks = value;
    }

    public int airDashCharges() {
        return airDashCharges;
    }

    public void setAirDashCharges(int value) {
        this.airDashCharges = Math.max(0, value);
    }

    public int dashTicks() {
        return dashTicks;
    }

    /** Advances an in-flight dash by one tick. Returns the remaining ticks. */
    public int tickDash() {
        if (dashTicks > 0) {
            dashTicks--;
        }
        return dashTicks;
    }

    public void setDashTicks(int value) {
        this.dashTicks = Math.max(0, value);
    }

    public Vec3 dashDirection() {
        return dashDirection;
    }

    public double dashSpeed() {
        return dashSpeed;
    }

    public void setDash(Vec3 direction, double speed, int ticks) {
        this.dashDirection = direction.normalize();
        this.dashSpeed = speed;
        this.dashTicks = Math.max(0, ticks);
    }

    /** Starts a dash and records when it began (used for cooldown feedback). */
    public void startDash(Vec3 direction, double speed, int ticks, long gameTime) {
        setDash(direction, speed, ticks);
        this.lastDashTick = gameTime;
    }

    /** Alias kept for readability at the call sites. */
    public void setBoost(boolean value) {
        this.boosting = value;
    }

    /** Alias kept for readability at the call sites. */
    public void disableUntil(long gameTime) {
        this.disabledUntil = gameTime;
    }

    /** Alias kept for readability at the call sites. */
    public float inputVertical() {
        return verticalInput;
    }

    public long lastDashTick() {
        return lastDashTick;
    }

    public void setLastDashTick(long tick) {
        this.lastDashTick = tick;
    }

    public boolean isDashing() {
        return dashTicks > 0;
    }

    public long disabledUntil() {
        return disabledUntil;
    }

    public void setDisabledUntil(long tick) {
        this.disabledUntil = tick;
    }

    public boolean isDisabled(long gameTime) {
        return gameTime < disabledUntil;
    }

    public Vec3 velocity() {
        return velocity;
    }

    public void setVelocity(Vec3 value) {
        this.velocity = value;
    }

    public float forwardInput() {
        return forwardInput;
    }

    public float strafeInput() {
        return strafeInput;
    }

    public float verticalInput() {
        return verticalInput;
    }

    public long lastInputTick() {
        return lastInputTick;
    }

    public void setInput(float forward, float strafe, float vertical, long gameTime) {
        this.forwardInput = forward;
        this.strafeInput = strafe;
        this.verticalInput = vertical;
        this.lastInputTick = gameTime;
    }

    public void clearInput() {
        this.forwardInput = 0.0F;
        this.strafeInput = 0.0F;
        this.verticalInput = 0.0F;
    }

    public boolean hasRecentInput(long gameTime, int timeoutTicks) {
        return gameTime - lastInputTick <= timeoutTicks;
    }

    public void copyFrom(FlightState other) {
        this.active = other.active;
        this.boosting = other.boosting;
        this.hovering = other.hovering;
        this.ticks = other.ticks;
        this.airDashCharges = other.airDashCharges;
        this.velocity = other.velocity;
        this.dashTicks = other.dashTicks;
        this.dashDirection = other.dashDirection;
        this.dashSpeed = other.dashSpeed;
        this.disabledUntil = other.disabledUntil;
    }

    // ------------------------------------------------------------------ nbt

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Active", active);
        tag.putBoolean("Boosting", boosting);
        tag.putInt("Ticks", ticks);
        tag.putInt("AirDashCharges", airDashCharges);
        tag.putLong("DisabledUntil", disabledUntil);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.active = tag.getBoolean("Active");
        this.boosting = tag.getBoolean("Boosting");
        this.ticks = tag.getInt("Ticks");
        this.airDashCharges = tag.getInt("AirDashCharges");
        this.disabledUntil = tag.contains("DisabledUntil") ? tag.getLong("DisabledUntil") : Long.MIN_VALUE;
        this.velocity = Vec3.ZERO;
        this.dashTicks = 0;
        this.clearInput();
    }
}
