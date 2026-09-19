package com.megafishing.visual;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public interface FishVisual {
    void spawn(Location location, Vector direction, double scale, String animation);
    void update(Location location, Vector direction, double scale, String animation);
    void remove();
    boolean isValid();
}
