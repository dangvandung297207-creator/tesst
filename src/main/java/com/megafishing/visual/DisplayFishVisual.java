package com.megafishing.visual;

import com.megafishing.fish.FishDefinition;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class DisplayFishVisual implements FishVisual {
    private final FishDefinition definition;
    private ItemDisplay display;

    public DisplayFishVisual(FishDefinition definition) {
        this.definition = definition;
    }

    @Override
    public void spawn(Location location, Vector direction, double scale, String animation) {
        if (location.getWorld() == null) {
            return;
        }
        this.display = location.getWorld().spawn(location, ItemDisplay.class, entity -> entity.setItemStack(new ItemStack(definition.getDisplayMaterial())));
        this.display.setPersistent(false);
        applyVisuals(location, direction, scale);
    }

    @Override
    public void update(Location location, Vector direction, double scale, String animation) {
        if (display != null && display.isValid()) {
            applyVisuals(location, direction, scale);
        }
    }

    private void applyVisuals(Location location, Vector direction, double scale) {
        Location target = location.clone();
        if (direction != null && direction.lengthSquared() > 0.0001D) {
            target.setDirection(direction);
        }
        display.teleport(target);
        display.setDisplayWidth((float) Math.max(0.4D, scale));
        display.setDisplayHeight((float) Math.max(0.4D, scale));
    }

    @Override
    public void remove() {
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    @Override
    public boolean isValid() {
        return display != null && display.isValid();
    }
}
