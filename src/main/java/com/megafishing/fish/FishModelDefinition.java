package com.megafishing.fish;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class FishModelDefinition {
    private final String id;
    private final double scaleBase;
    private final Map<String, String> animations;

    public FishModelDefinition(String id, double scaleBase, Map<String, String> animations) {
        this.id = id;
        this.scaleBase = scaleBase;
        this.animations = animations == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(animations));
    }

    public String animation(String key, String fallback) {
        return animations.getOrDefault(key, fallback);
    }

    public String getId() { return id; }
    public double getScaleBase() { return scaleBase; }
    public Map<String, String> getAnimations() { return animations; }
}
