package com.animeki.destruction;

/**
 * Description of one controlled terrain impact.
 *
 * <p>Abilities never touch blocks directly: they describe the shape and size they want and the
 * {@link DestructionService} enforces every limit (radius cap, block budget, protected blocks,
 * per tick throttling) so a single ultimate can never destroy an unbounded area.</p>
 */
public record DestructionProfile(double radius, int maxBlocks, double density, Shape shape, boolean dropBlocks) {

    public enum Shape {
        /** Full sphere around the impact point. */
        SPHERE,
        /** Bowl shaped crater that follows the terrain surface. */
        CRATER,
        /** Hollow shell, cheap and great for shockwaves that scar the ground. */
        SHELL
    }

    public static DestructionProfile heavyImpact(double radius) {
        return new DestructionProfile(radius, 96, 0.85D, Shape.CRATER, false);
    }

    public static DestructionProfile groundSlam(double radius) {
        return new DestructionProfile(radius, 220, 0.9D, Shape.CRATER, false);
    }

    public static DestructionProfile transformationBlast(double radius) {
        return new DestructionProfile(radius, 140, 0.55D, Shape.SHELL, false);
    }

    public static DestructionProfile ultimate(double radius) {
        return new DestructionProfile(radius, 900, 0.95D, Shape.SPHERE, false);
    }

    public static DestructionProfile beamErosion(double radius) {
        return new DestructionProfile(radius, 12, 1.0D, Shape.SPHERE, false);
    }

    public static DestructionProfile meteorImpact(double radius) {
        return new DestructionProfile(radius, 1200, 1.0D, Shape.CRATER, false);
    }

    public DestructionProfile withRadius(double newRadius) {
        return new DestructionProfile(newRadius, maxBlocks, density, shape, dropBlocks);
    }

    public DestructionProfile withShape(Shape newShape) {
        return new DestructionProfile(radius, maxBlocks, density, newShape, dropBlocks);
    }
}
