package com.megafishing.visual;

import com.megafishing.fish.FishDefinition;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

public class DisplayFishVisual implements FishVisual {
    private final FishDefinition definition;
    private ItemDisplay display;

    public DisplayFishVisual(FishDefinition definition) {
        this.definition = definition;
    }

    @Override
    public void spawn(Location location) {
        if (location.getWorld() == null) {
            return;
        }
        this.display = location.getWorld().spawn(location, ItemDisplay.class, entity -> entity.setItemStack(new ItemStack(definition.getDisplayMaterial())));
        this.display.setPersistent(false);
    }

    @Override
    public void update(Location location) {
        if (display != null && display.isValid()) {
            display.teleport(location);
        }
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
