package com.animeki.util;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Small numeric helpers shared by the whole mod (no third party dependencies). */
public final class MathUtil {
    private MathUtil() {
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    public static double lerp(double from, double to, double delta) {
        return from + (to - from) * clamp(delta, 0.0D, 1.0D);
    }

    public static float lerp(float from, float to, float delta) {
        return from + (to - from) * clamp(delta, 0.0F, 1.0F);
    }

    public static Vec3 lerp(Vec3 from, Vec3 to, double delta) {
        double d = clamp(delta, 0.0D, 1.0D);
        return new Vec3(from.x + (to.x - from.x) * d, from.y + (to.y - from.y) * d, from.z + (to.z - from.z) * d);
    }

    /** Smooth 3t^2-2t^3 curve, useful for easing effects in and out. */
    public static double smoothStep(double delta) {
        double d = clamp(delta, 0.0D, 1.0D);
        return d * d * (3.0D - 2.0D * d);
    }

    public static double easeOutCubic(double delta) {
        double d = clamp(delta, 0.0D, 1.0D);
        double inverse = 1.0D - d;
        return 1.0D - inverse * inverse * inverse;
    }

    public static double easeInCubic(double delta) {
        double d = clamp(delta, 0.0D, 1.0D);
        return d * d * d;
    }

    /** Moves {@code current} towards {@code target} by at most {@code maxDelta}. */
    public static double approach(double current, double target, double maxDelta) {
        if (current < target) {
            return Math.min(current + maxDelta, target);
        }
        return Math.max(current - maxDelta, target);
    }

    public static Vec3 approach(Vec3 current, Vec3 target, double maxDelta) {
        Vec3 delta = target.subtract(current);
        double length = delta.length();
        if (length <= maxDelta || length < 1.0E-6D) {
            return target;
        }
        return current.add(delta.scale(maxDelta / length));
    }

    public static double randomRange(RandomSource random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    public static float randomRange(RandomSource random, float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    public static Vec3 horizontal(Vec3 vector) {
        return new Vec3(vector.x, 0.0D, vector.z);
    }

    public static double horizontalLength(Vec3 vector) {
        return Math.sqrt(vector.x * vector.x + vector.z * vector.z);
    }

    /** Yaw in degrees pointing along the vector. */
    public static float yawOf(Vec3 direction) {
        return (float) (Mth.atan2(direction.z, direction.x) * (180.0D / Math.PI)) - 90.0F;
    }

    /** Pitch in degrees for the given direction. */
    public static float pitchOf(Vec3 direction) {
        double horizontal = horizontalLength(direction);
        return (float) (-(Mth.atan2(direction.y, horizontal) * (180.0D / Math.PI)));
    }

    public static int packColor(float alpha, float red, float green, float blue) {
        int a = (int) (clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24;
        int r = (int) (clamp(red, 0.0F, 1.0F) * 255.0F) << 16;
        int g = (int) (clamp(green, 0.0F, 1.0F) * 255.0F) << 8;
        int b = (int) (clamp(blue, 0.0F, 1.0F) * 255.0F);
        return a | r | g | b;
    }

    public static float alphaOf(int argb) {
        return ((argb >> 24) & 0xFF) / 255.0F;
    }

    public static float redOf(int argb) {
        return ((argb >> 16) & 0xFF) / 255.0F;
    }

    public static float greenOf(int argb) {
        return ((argb >> 8) & 0xFF) / 255.0F;
    }

    public static float blueOf(int argb) {
        return (argb & 0xFF) / 255.0F;
    }

    public static int withAlpha(int argb, float alpha) {
        int a = (int) (clamp(alpha, 0.0F, 1.0F) * 255.0F);
        return (argb & 0x00FFFFFF) | (a << 24);
    }

    /** Rotates a vector around the Y axis by the given angle in degrees. */
    public static Vec3 rotateY(Vec3 vector, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(vector.x * cos + vector.z * sin, vector.y, -vector.x * sin + vector.z * cos);
    }

    /** Linear blend of two packed ARGB colours. */
    public static int mix(int from, int to, float delta) {
        float d = clamp(delta, 0.0F, 1.0F);
        int a = (int) (alphaOf(from) * 255.0F + (alphaOf(to) - alphaOf(from)) * 255.0F * d) << 24;
        int r = (int) (redOf(from) * 255.0F + (redOf(to) - redOf(from)) * 255.0F * d) << 16;
        int g = (int) (greenOf(from) * 255.0F + (greenOf(to) - greenOf(from)) * 255.0F * d) << 8;
        int b = (int) (blueOf(from) * 255.0F + (blueOf(to) - blueOf(from)) * 255.0F * d);
        return a | r | g | b;
    }

    /** Frame independent exponential decay factor. */
    public static double dampen(double current, double target, double factor) {
        return target + (current - target) * clamp(factor, 0.0D, 1.0D);
    }
}
