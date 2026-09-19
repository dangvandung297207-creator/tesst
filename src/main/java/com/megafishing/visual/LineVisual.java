package com.megafishing.visual;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class LineVisual {
    public void render(Player player, Location bobber, Location fishAnchor) {
        if (player == null || bobber == null || fishAnchor == null || player.getWorld() == null) {
            return;
        }
        Location from = player.getEyeLocation().add(player.getLocation().getDirection().normalize().multiply(0.4D)).subtract(0.0D, 0.15D, 0.0D);
        draw(from, bobber, player);
        draw(bobber, fishAnchor, player);
    }

    private void draw(Location from, Location to, Player viewer) {
        Vector direction = to.toVector().subtract(from.toVector());
        double length = Math.max(0.1D, direction.length());
        direction.normalize();
        for (double step = 0.0D; step <= length; step += 0.85D) {
            Location point = from.clone().add(direction.clone().multiply(step));
            viewer.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(210, 210, 210), 0.7F));
        }
    }
}
