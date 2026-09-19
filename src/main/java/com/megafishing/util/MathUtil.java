package com.megafishing.util;

public final class MathUtil {
    private MathUtil() {
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static long clampLong(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    public static long safeMultiplyRound(double left, double right) {
        double result = left * right;
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            return Long.MAX_VALUE;
        }
        if (result >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        if (result <= Long.MIN_VALUE) {
            return Long.MIN_VALUE;
        }
        return Math.round(result);
    }
}
