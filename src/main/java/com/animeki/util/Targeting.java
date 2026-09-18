package com.animeki.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ray, cone and segment based targeting helpers.
 *
 * <p>All combat queries go through these methods: they use bounding boxes and closed form
 * geometry instead of spawning helper entities or scanning whole regions, which keeps large
 * beams and shockwaves cheap on multiplayer servers.</p>
 */
public final class Targeting {
    private Targeting() {
    }

    /** Closest point on segment {@code a -> b} to point {@code p}. */
    public static Vec3 closestPointOnSegment(Vec3 a, Vec3 b, Vec3 p) {
        Vec3 ab = b.subtract(a);
        double lengthSqr = ab.lengthSqr();
        if (lengthSqr < 1.0E-8D) {
            return a;
        }
        double t = MathUtil.clamp(p.subtract(a).dot(ab) / lengthSqr, 0.0D, 1.0D);
        return a.add(ab.scale(t));
    }

    public static double distanceToSegment(Vec3 a, Vec3 b, Vec3 p) {
        return closestPointOnSegment(a, b, p).distanceTo(p);
    }

    public static boolean segmentHitsSphere(Vec3 a, Vec3 b, Vec3 center, double radius) {
        return distanceToSegment(a, b, center) <= radius;
    }

    /**
     * Computes the closest approach between two segments.
     *
     * @return an array of {@code [distance, s, t]} where {@code s} and {@code t} are the normalised
     *         parameters on the first and second segment, or {@code null} if degenerate.
     */
    public static double[] segmentSegmentDistance(Vec3 p1, Vec3 q1, Vec3 p2, Vec3 q2) {
        Vec3 d1 = q1.subtract(p1);
        Vec3 d2 = q2.subtract(p2);
        Vec3 r = p1.subtract(p2);
        double a = d1.dot(d1);
        double e = d2.dot(d2);
        double f = d2.dot(r);
        double s;
        double t;
        if (a <= 1.0E-8D && e <= 1.0E-8D) {
            return new double[] { p1.distanceTo(p2), 0.0D, 0.0D };
        }
        if (a <= 1.0E-8D) {
            s = 0.0D;
            t = MathUtil.clamp(f / e, 0.0D, 1.0D);
        } else {
            double c = d1.dot(r);
            if (e <= 1.0E-8D) {
                t = 0.0D;
                s = MathUtil.clamp(-c / a, 0.0D, 1.0D);
            } else {
                double b = d1.dot(d2);
                double denominator = a * e - b * b;
                s = denominator != 0.0D ? MathUtil.clamp((b * f - c * e) / denominator, 0.0D, 1.0D) : 0.0D;
                t = (b * s + f) / e;
                if (t < 0.0D) {
                    t = 0.0D;
                    s = MathUtil.clamp(-c / a, 0.0D, 1.0D);
                } else if (t > 1.0D) {
                    t = 1.0D;
                    s = MathUtil.clamp((b - c) / a, 0.0D, 1.0D);
                }
            }
        }
        Vec3 c1 = p1.add(d1.scale(s));
        Vec3 c2 = p2.add(d2.scale(t));
        return new double[] { c1.distanceTo(c2), s, t };
    }

    /** Ray traces the world for the first solid block between the eye position and the target point. */
    public static BlockHitResult clipBlocks(ServerLevel level, Entity source, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
    }

    /** Entities inside a cone in front of the given direction. */
    public static List<LivingEntity> entitiesInCone(ServerLevel level, Vec3 origin, Vec3 direction,
                                                    double range, double halfAngleDegrees, @Nullable Entity exclude,
                                                    Predicate<LivingEntity> filter) {
        List<LivingEntity> result = new ArrayList<>();
        double cosLimit = Math.cos(Math.toRadians(halfAngleDegrees));
        AABB searchBox = new AABB(origin, origin.add(direction.scale(range))).inflate(range * 0.5D + 1.0D);
        for (Entity entity : level.getEntities(exclude, searchBox, e -> e instanceof LivingEntity)) {
            if (!(entity instanceof LivingEntity living) || living.isSpectator() || !living.isAlive()) {
                continue;
            }
            if (!filter.test(living)) {
                continue;
            }
            Vec3 toEntity = living.getBoundingBox().getCenter().subtract(origin);
            double dot = toEntity.normalize().dot(direction.normalize());
            if (dot >= cosLimit && toEntity.length() <= range + 1.0D) {
                result.add(living);
            }
        }
        return result;
    }

    /** Entities whose bounding box comes close enough to a segment, used for beams and dashes. */
    public static List<LivingEntity> entitiesAlongSegment(ServerLevel level, Vec3 from, Vec3 to, double radius,
                                                          @Nullable Entity exclude, Predicate<LivingEntity> filter) {
        List<LivingEntity> result = new ArrayList<>();
        AABB searchBox = new AABB(from, to).inflate(radius + 1.0D);
        for (Entity entity : level.getEntities(exclude, searchBox, e -> e instanceof LivingEntity)) {
            if (!(entity instanceof LivingEntity living) || living.isSpectator() || !living.isAlive()) {
                continue;
            }
            if (!filter.test(living)) {
                continue;
            }
            AABB box = living.getBoundingBox().inflate(radius);
            if (box.clip(from, to).isPresent() || distanceToSegment(from, to, box.getCenter()) <= radius + Math.max(box.getXsize(), box.getZsize()) * 0.5D) {
                result.add(living);
            }
        }
        return result;
    }

    /** Nearest living entity in front of an entity within a maximum range, ignoring the shooter. */
    @Nullable
    public static LivingEntity raycastEntity(ServerLevel level, Vec3 start, Vec3 end, @Nullable Entity exclude,
                                             double tolerance, Predicate<LivingEntity> filter) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        AABB searchBox = new AABB(start, end).inflate(tolerance + 1.0D);
        for (Entity entity : level.getEntities(exclude, searchBox, e -> e instanceof LivingEntity)) {
            if (!(entity instanceof LivingEntity living) || living.isSpectator() || !living.isAlive()) {
                continue;
            }
            if (!filter.test(living)) {
                continue;
            }
            AABB box = living.getBoundingBox().inflate(tolerance);
            HitResult hit = box.clip(start, end).orElse(null);
            double distance = hit == null ? distanceToSegment(start, end, box.getCenter()) : hit.getLocation().distanceTo(start);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = living;
            }
        }
        return best;
    }

    /** The entity a player is currently aiming at, or {@code null}. */
    @Nullable
    public static LivingEntity aimTarget(ServerLevel level, Entity source, double range, double tolerance,
                                         Predicate<LivingEntity> filter) {
        Vec3 eye = source.getEyePosition();
        Vec3 end = eye.add(source.getViewVector(1.0F).scale(range));
        return raycastEntity(level, eye, end, source, tolerance, filter);
    }

    /** Checks whether two entities should be able to damage each other. */
    public static boolean isHostile(Entity attacker, Entity target) {
        if (attacker == target || target == null) {
            return false;
        }
        if (attacker.isAlliedTo(target)) {
            return false;
        }
        if (attacker instanceof Player && target instanceof Player) {
            return true;
        }
        return target instanceof LivingEntity;
    }
}
