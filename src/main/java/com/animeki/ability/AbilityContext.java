package com.animeki.ability;

import com.animeki.ki.KiMath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Snapshot of the state an ability needs.
 *
 * <p>Created on the server when an ability starts: origin, aim direction and the Ki output
 * multiplier are frozen at cast time (so a player cannot inflate damage by transforming mid cast),
 * while the raw input flags are kept for movement driven abilities.</p>
 */
public final class AbilityContext {
    private final ServerLevel level;
    private final ServerPlayer player;
    private final Vec3 origin;
    private final Vec3 direction;
    private final double power;
    private final float inputForward;
    private final float inputStrafe;
    private final boolean sprinting;
    private final boolean sneaking;
    private final boolean jumping;

    private AbilityContext(ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 direction, double power,
                           float inputForward, float inputStrafe, boolean sprinting, boolean sneaking, boolean jumping) {
        this.level = level;
        this.player = player;
        this.origin = origin;
        this.direction = direction;
        this.power = power;
        this.inputForward = inputForward;
        this.inputStrafe = inputStrafe;
        this.sprinting = sprinting;
        this.sneaking = sneaking;
        this.jumping = jumping;
    }

    public static AbilityContext of(ServerPlayer player, float inputForward, float inputStrafe,
                                    boolean sprinting, boolean sneaking, boolean jumping) {
        Vec3 eyes = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        return new AbilityContext((ServerLevel) player.level(), player, eyes, look, KiMath.outputMultiplier(player),
                inputForward, inputStrafe, sprinting, sneaking, jumping);
    }

    public ServerLevel level() {
        return level;
    }

    public ServerPlayer player() {
        return player;
    }

    /** Where the ability starts (usually the eyes). */
    public Vec3 origin() {
        return origin;
    }

    /** Normalised aim direction at cast time. */
    public Vec3 direction() {
        return direction;
    }

    /** Ki output multiplier at cast time. */
    public double power() {
        return power;
    }

    public float inputForward() {
        return inputForward;
    }

    public float inputStrafe() {
        return inputStrafe;
    }

    public boolean sprinting() {
        return sprinting;
    }

    public boolean sneaking() {
        return sneaking;
    }

    public boolean jumping() {
        return jumping;
    }

    /** Origin offset towards the aim direction, useful for spawning effects in front of the player. */
    public Vec3 originAhead(double distance) {
        return origin.add(direction.scale(distance));
    }

    /** Stops the ability and notifies it with the given reason. */
    public void stop(AbilityStopReason reason) {
        AbilityService.cancel(player, reason);
    }
}
