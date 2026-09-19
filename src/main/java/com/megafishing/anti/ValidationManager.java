package com.megafishing.anti;

import com.megafishing.fishing.FishingZone;
import com.megafishing.persistence.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;

public class ValidationManager {
    public boolean isValidCastLocation(Location location, FishingZone zone) {
        if (location == null || location.getWorld() == null || zone == null) {
            return false;
        }
        if (location.getBlock().getType() != Material.WATER) {
            return false;
        }
        return location.getY() >= location.getWorld().getMinHeight() && location.getY() <= location.getWorld().getMaxHeight();
    }

    public boolean canStoreFish(PlayerData data) {
        return data != null && !data.getFishBag().isFull();
    }
}
