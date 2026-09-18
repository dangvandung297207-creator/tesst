package com.animeki.client.render;

import com.animeki.util.MathUtil;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Small, allocation light geometry helpers for the mod's world effects.
 *
 * <p>Every effect in the mod is built from a handful of primitives (quad, billboard, disc, ring,
 * tube, pillar, orb) emitted into a single additive render type. That keeps the draw call count tiny
 * and - more importantly - keeps this file free of any renderer specific state, so resource packs and
 * future Blender meshes can be dropped in next to it.</p>
 */
public final class RenderGeometry {
    private RenderGeometry() {
    }

    /** An arbitrary quad, wound consistently so it shows up additively from both sides. */
    public static void quad(VertexConsumer consumer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb) {
        vertex(consumer, matrix, a, argb);
        vertex(consumer, matrix, b, argb);
        vertex(consumer, matrix, c, argb);
        vertex(consumer, matrix, d, argb);
    }

    /** A camera facing quad - the workhorse for auras and sparks. */
    public static void billboard(VertexConsumer consumer, Matrix4f matrix, Vec3 center, Vec3 right, Vec3 up,
                                 float halfWidth, float halfHeight, int argb) {
        Vec3 r = right.scale(halfWidth);
        Vec3 u = up.scale(halfHeight);
        quad(consumer, matrix, center.subtract(r).subtract(u), center.add(r).subtract(u), center.add(r).add(u),
                center.subtract(r).add(u), argb);
    }

    /**
     * A soft "energy ball": three crossed billboards. Reads as a sphere with additive blending while
     * costing 6 triangles instead of a real sphere.
     */
    public static void orb(VertexConsumer consumer, Matrix4f matrix, Vec3 center, float radius, Vec3 viewRight,
                           Vec3 viewUp, int argb) {
        Vec3 forward = viewRight.cross(viewUp).normalize();
        billboard(consumer, matrix, center, viewRight, viewUp, radius, radius, argb);
        billboard(consumer, matrix, center, forward, viewUp, radius, radius, argb);
        billboard(consumer, matrix, center, viewRight, forward, radius, radius, argb);
    }

    /** A flat disc facing {@code normal}. */
    public static void disc(VertexConsumer consumer, Matrix4f matrix, Vec3 center, Vec3 normal, float radius,
                            int segments, int argb) {
        Vec3 axis = Math.abs(normal.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = normal.cross(axis).normalize();
        Vec3 up = normal.cross(right).normalize();
        for (int i = 0; i < segments; i++) {
            double a0 = (i / (double) segments) * Math.PI * 2.0D;
            double a1 = ((i + 1) / (double) segments) * Math.PI * 2.0D;
            Vec3 p0 = center.add(right.scale(Math.cos(a0) * radius)).add(up.scale(Math.sin(a0) * radius));
            Vec3 p1 = center.add(right.scale(Math.cos(a1) * radius)).add(up.scale(Math.sin(a1) * radius));
            quad(consumer, matrix, center, p0, p1, p1, argb);
        }
    }

    /** A flat annulus (shockwave front). */
    public static void ring(VertexConsumer consumer, Matrix4f matrix, Vec3 center, Vec3 normal, float innerRadius,
                            float outerRadius, int segments, int argb) {
        Vec3 axis = Math.abs(normal.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = normal.cross(axis).normalize();
        Vec3 up = normal.cross(right).normalize();
        for (int i = 0; i < segments; i++) {
            double a0 = (i / (double) segments) * Math.PI * 2.0D;
            double a1 = ((i + 1) / (double) segments) * Math.PI * 2.0D;
            Vec3 i0 = center.add(right.scale(Math.cos(a0) * innerRadius)).add(up.scale(Math.sin(a0) * innerRadius));
            Vec3 i1 = center.add(right.scale(Math.cos(a1) * innerRadius)).add(up.scale(Math.sin(a1) * innerRadius));
            Vec3 o0 = center.add(right.scale(Math.cos(a0) * outerRadius)).add(up.scale(Math.sin(a0) * outerRadius));
            Vec3 o1 = center.add(right.scale(Math.cos(a1) * outerRadius)).add(up.scale(Math.sin(a1) * outerRadius));
            quad(consumer, matrix, i0, o0, o1, i1, argb);
        }
    }

    /**
     * A beam: a square tube around the segment origin -> origin + direction * length.
     *
     * <p>The tube is drawn as {@code segments} crossed billboards around the axis, which stays stable
     * while the player turns and needs only a few dozen vertices for a very long beam.</p>
     */
    public static void tube(VertexConsumer consumer, Matrix4f matrix, Vec3 origin, Vec3 direction, float length,
                            float radius, int sides, int argb, Vec3 viewRight, Vec3 viewUp) {
        Vec3 axis = direction.normalize();
        Vec3 end = origin.add(axis.scale(length));
        for (int i = 0; i < sides; i++) {
            // Alternate between the two camera planes so the beam has volume from every angle.
            double angle = (i / (double) sides) * Math.PI;
            Vec3 base = i % 2 == 0 ? viewRight : viewUp;
            Vec3 other = i % 2 == 0 ? viewUp : viewRight;
            Vec3 offset = rotateAroundAxis(base, other, axis, angle).normalize().scale(radius);
            quad(consumer, matrix, origin.subtract(offset), end.subtract(offset), end.add(offset), origin.add(offset),
                    argb);
        }
    }

    /** A vertical pillar used by ground impacts and ultimate charges. */
    public static void pillar(VertexConsumer consumer, Matrix4f matrix, Vec3 base, float height, float radius,
                              int sides, int argb, Vec3 viewRight) {
        Vec3 top = base.add(0.0D, height, 0.0D);
        Vec3 right = MathUtil.horizontal(viewRight);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        Vec3 depth = new Vec3(-right.z, 0.0D, right.x);
        for (int i = 0; i < sides; i++) {
            double angle = (i / (double) sides) * Math.PI * 2.0D;
            Vec3 offset = right.scale(Math.cos(angle) * radius).add(depth.scale(Math.sin(angle) * radius));
            quad(consumer, matrix, base.subtract(offset), top.subtract(offset), top.add(offset), base.add(offset),
                    argb);
        }
    }

    /** Minimal Rodrigues rotation used to spread a beam's sides around its axis. */
    private static Vec3 rotateAroundAxis(Vec3 vector, Vec3 helper, Vec3 axis, double angle) {
        Vec3 tangent = axis.cross(vector);
        if (tangent.lengthSqr() < 1.0E-6D) {
            tangent = axis.cross(helper);
        }
        if (tangent.lengthSqr() < 1.0E-6D) {
            tangent = new Vec3(0.0D, 1.0D, 0.0D);
        }
        tangent = tangent.normalize();
        Vec3 normal = axis.cross(tangent).normalize();
        return tangent.scale(Math.cos(angle)).add(normal.scale(Math.sin(angle))).add(axis.scale(0.0D));
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position, int argb) {
        consumer.addVertex(matrix, (float) position.x, (float) position.y, (float) position.z).setColor(argb);
    }

    /** Scales the alpha channel of a packed ARGB colour. */
    public static int withAlpha(int argb, float factor) {
        int alpha = (argb >>> 24) & 0xFF;
        int scaled = Math.max(0, Math.min(255, (int) (alpha * MathUtil.clamp(factor, 0.0F, 4.0F))));
        return (scaled << 24) | (argb & 0x00FFFFFF);
    }

    /** Blends two packed ARGB colours. */
    public static int mix(int from, int to, float delta) {
        return MathUtil.mix(from, to, delta);
    }
}
