package com.megafishing.util;

import java.util.Map;
import java.util.Random;

public final class RandomUtil {
    private RandomUtil() {
    }

    public static <T> T weighted(Random random, Map<T, ? extends Number> weights) {
        double total = 0.0D;
        for (Number number : weights.values()) {
            total += Math.max(0.0D, number.doubleValue());
        }
        if (total <= 0.0D) {
            return weights.keySet().stream().findFirst().orElse(null);
        }
        double roll = random.nextDouble() * total;
        double cursor = 0.0D;
        for (Map.Entry<T, ? extends Number> entry : weights.entrySet()) {
            cursor += Math.max(0.0D, entry.getValue().doubleValue());
            if (roll <= cursor) {
                return entry.getKey();
            }
        }
        return weights.keySet().stream().reduce((first, second) -> second).orElse(null);
    }
}
