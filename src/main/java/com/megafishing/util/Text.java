package com.megafishing.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.text.DecimalFormat;
import java.util.Locale;

public final class Text {
    private static final DecimalFormat DECIMAL = new DecimalFormat("0.##");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private Text() {
    }

    public static String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }

    public static Component component(String input) {
        return LEGACY.deserialize(color(input));
    }

    public static String plainEnum(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder builder = new StringBuilder(normalized.length());
        boolean upper = true;
        for (char c : normalized.toCharArray()) {
            if (upper && Character.isLetter(c)) {
                builder.append(Character.toUpperCase(c));
                upper = false;
            } else {
                builder.append(c);
            }
            if (c == ' ') {
                upper = true;
            }
        }
        return builder.toString();
    }

    public static String number(double value) {
        if (Math.abs(value) >= 1_000_000_000D) {
            return DECIMAL.format(value / 1_000_000_000D) + "b";
        }
        if (Math.abs(value) >= 1_000_000D) {
            return DECIMAL.format(value / 1_000_000D) + "m";
        }
        if (Math.abs(value) >= 1_000D) {
            return DECIMAL.format(value / 1_000D) + "k";
        }
        return DECIMAL.format(value);
    }

    public static String percent(double value) {
        return DECIMAL.format(value) + "%";
    }

    public static String bar(double current, double max, int length) {
        if (length <= 0) {
            return "";
        }
        double ratio = max <= 0.0D ? 0.0D : Math.max(0.0D, Math.min(1.0D, current / max));
        int filled = (int) Math.round(ratio * length);
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(i < filled ? '█' : '░');
        }
        return builder.toString();
    }
}
