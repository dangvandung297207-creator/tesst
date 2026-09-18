package com.animeki.client.render;

import net.minecraft.client.renderer.RenderType;

/**
 * The one render type all custom geometry uses.
 *
 * <p>{@code RenderType.lightning()} is a vertex coloured, additive, unlit quad type that vanilla
 * already ships, so the mod needs no custom shader or texture to draw auras, beams and shockwaves -
 * which also means it keeps working with OptiFine-style renderers and shader packs that only know
 * vanilla types.</p>
 *
 * <p>Resource packs that want textured effects can point
 * {@code AnimeKiClientConfig.VFX.externalAssets} at their own definitions; the geometry itself is
 * type agnostic.</p>
 */
public final class GlowRenderType {
    private static final RenderType GLOW = RenderType.lightning();

    private GlowRenderType() {
    }

    public static RenderType get() {
        return GLOW;
    }
}
