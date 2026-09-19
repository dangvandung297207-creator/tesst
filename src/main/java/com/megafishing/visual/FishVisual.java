package com.megafishing.visual;

import org.bukkit.Location;

public interface FishVisual {
    void spawn(Location location);
    void update(Location location);
    void remove();
    boolean isValid();
}
