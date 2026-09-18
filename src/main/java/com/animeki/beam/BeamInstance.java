package com.animeki.beam;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * A live beam on the server.
 *
 * <p>Beams are <b>not</b> entities: one instance covers an entire beam with segment based collision,
 * which keeps a long clash between two players at two objects instead of thousands.</p>
 */
public final class BeamInstance {
    private final int id;
    private final int ownerId;
    private final String ownerName;
    private final BeamType type;
    private final double damageScale;
    private final long startTick;
    private final int maxDurationTicks;
    private final Map<Integer, Long> lastDamageTick = new HashMap<>();

    private Vec3 origin;
    private Vec3 direction;
    private double length;
    private double power;
    private boolean stopped;
    private int clashPartnerId = -1;
    private Vec3 clashPoint = Vec3.ZERO;
    private double clashProgress;
    private int clashTicks;

    public BeamInstance(int id, Entity owner, BeamType type, Vec3 origin, Vec3 direction,
                        double damageScale, int maxDurationTicks, long startTick) {
        this.id = id;
        this.ownerId = owner.getId();
        this.ownerName = owner.getName().getString();
        this.type = type;
        this.origin = origin;
        this.direction = direction.normalize();
        this.damageScale = damageScale;
        this.maxDurationTicks = maxDurationTicks;
        this.startTick = startTick;
        this.length = type.range();
        this.power = type.power(damageScale);
    }

    public int id() {
        return id;
    }

    public int ownerId() {
        return ownerId;
    }

    public String ownerName() {
        return ownerName;
    }

    public BeamType type() {
        return type;
    }

    public Vec3 origin() {
        return origin;
    }

    public void setOrigin(Vec3 origin) {
        this.origin = origin;
    }

    public Vec3 direction() {
        return direction;
    }

    public void setDirection(Vec3 direction) {
        this.direction = direction.normalize();
    }

    public double length() {
        return length;
    }

    public void setLength(double length) {
        this.length = Math.max(0.0D, length);
    }

    public Vec3 end() {
        return origin.add(direction.scale(length));
    }

    public double power() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public double damageScale() {
        return damageScale;
    }

    public long startTick() {
        return startTick;
    }

    public int age(long gameTime) {
        return (int) (gameTime - startTick);
    }

    public int maxDurationTicks() {
        return maxDurationTicks;
    }

    public boolean expired(long gameTime) {
        return maxDurationTicks > 0 && age(gameTime) >= maxDurationTicks;
    }

    public boolean stopped() {
        return stopped;
    }

    public void markStopped() {
        this.stopped = true;
    }

    // ------------------------------------------------------------- clash state

    public int clashPartnerId() {
        return clashPartnerId;
    }

    public boolean inClash() {
        return clashPartnerId >= 0;
    }

    public void setClash(@Nullable BeamInstance partner) {
        this.clashPartnerId = partner == null ? -1 : partner.id();
        if (partner != null) {
            this.clashPoint = partner.clashPoint();
            this.clashTicks++;
        } else {
            this.clashTicks = 0;
            this.clashProgress = 0.0D;
        }
    }

    public Vec3 clashPoint() {
        return clashPoint;
    }

    public void setClashPoint(Vec3 point) {
        this.clashPoint = point;
    }

    public double clashProgress() {
        return clashProgress;
    }

    public void setClashProgress(double progress) {
        this.clashProgress = Math.max(-1.0D, Math.min(1.0D, progress));
    }

    public int clashTicks() {
        return clashTicks;
    }

    public void tickClash() {
        this.clashTicks++;
    }

    // ------------------------------------------------------------- damage gate

    /** Per entity damage interval so a beam ticks damage every few ticks instead of every tick. */
    public boolean canDamage(Entity entity, long gameTime, int intervalTicks) {
        long last = lastDamageTick.getOrDefault(entity.getId(), Long.MIN_VALUE / 2);
        if (gameTime - last < intervalTicks) {
            return false;
        }
        lastDamageTick.put(entity.getId(), gameTime);
        return true;
    }

    public void forgetDamageHistory() {
        lastDamageTick.clear();
    }

    public void refreshOriginFromOwner(ServerLevel level) {
        Entity owner = level.getEntity(ownerId);
        if (owner != null && owner.isAlive()) {
            this.origin = owner.getEyePosition().add(direction.scale(0.8D));
            this.power = type.power(damageScale);
        }
    }

    @Nullable
    public Entity owner(ServerLevel level) {
        return level.getEntity(ownerId);
    }
}
