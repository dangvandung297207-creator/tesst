package com.megafishing.anti;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Optional;

public class RayTraceManager {
    public Optional<Location> findCastLocation(Player player, double range) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        for (double d = 0.5D; d <= range; d += 0.35D) {
            Location sample = eye.clone().add(direction.clone().multiply(d));
            Block block = sample.getBlock();
            if (block.getType() == Material.WATER) {
                return Optional.of(findWaterSurface(block));
            }
            if (block.getType().isSolid() || block.getType() == Material.LAVA) {
                break;
            }
        }
        return Optional.empty();
    }

    public boolean hasLineOfSight(Location from, Location to) {
        if (from.getWorld() == null || to.getWorld() == null || from.getWorld() != to.getWorld()) {
            return false;
        }
        Vector delta = to.toVector().subtract(from.toVector());
        double distance = delta.length();
        if (distance <= 0.05D) {
            return true;
        }
        RayTraceResult result = from.getWorld().rayTraceBlocks(from, delta.normalize(), distance, FluidCollisionMode.NEVER, true);
        return result == null;
    }

    public boolean isWater(World world, Vector vector) {
        if (world == null) {
            return false;
        }
        Material material = world.getBlockAt((int) Math.floor(vector.getX()), (int) Math.floor(vector.getY()), (int) Math.floor(vector.getZ())).getType();
        return material == Material.WATER;
    }

    public Vector findNearestWater(World world, Vector preferred, Vector anchor, double radius) {
        Vector clamped = clamp(preferred, anchor, radius);
        if (isWater(world, clamped)) {
            return clamped;
        }
        for (double search = 0.5D; search <= radius; search += 0.5D) {
            for (int angle = 0; angle < 360; angle += 15) {
                double radians = Math.toRadians(angle);
                Vector candidate = anchor.clone().add(new Vector(Math.cos(radians) * search, -0.6D - (search * 0.12D), Math.sin(radians) * search));
                if (isWater(world, candidate)) {
                    return candidate;
                }
            }
        }
        return anchor.clone().add(new Vector(0.0D, -0.8D, 0.0D));
    }

    private Location findWaterSurface(Block block) {
        Block current = block;
        while (current.getY() < current.getWorld().getMaxHeight() - 1 && current.getRelative(0, 1, 0).getType() == Material.WATER) {
            current = current.getRelative(0, 1, 0);
        }
        return current.getLocation().add(0.5D, 0.85D, 0.5D);
    }

    private Vector clamp(Vector target, Vector anchor, double radius) {
        Vector delta = target.clone().subtract(anchor);
        if (delta.lengthSquared() <= radius * radius) {
            return target;
        }
        return anchor.clone().add(delta.normalize().multiply(radius));
    }
}
