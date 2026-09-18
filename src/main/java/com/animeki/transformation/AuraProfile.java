package com.animeki.transformation;

/**
 * Data driven appearance of a transformation aura.
 *
 * <p>The renderer only consumes this description, which means new forms (or resource pack overrides
 * shipped with Blender exported meshes) never require touching rendering code.</p>
 *
 * @param coreColor     packed ARGB of the inner glow
 * @param glowColor     packed ARGB of the outer shell
 * @param sparkColor    packed ARGB of floating sparks
 * @param intensity     base intensity multiplier (1.0 = standard aura)
 * @param scale         base scale around the wearer
 * @param rings         number of rotating energy rings
 * @param ringSpeed     rotation speed of the rings in degrees per tick
 * @param flames        number of rising energy flame layers
 * @param lightning     whether procedural lightning is drawn around the aura
 * @param groundAura    whether a ground energy ring is drawn below the wearer
 * @param meshRef       optional external mesh id (Blender exported) used instead of the procedural aura
 * @param animationRef  optional external animation id played by the aura mesh
 */
public record AuraProfile(
        int coreColor,
        int glowColor,
        int sparkColor,
        float intensity,
        float scale,
        int rings,
        float ringSpeed,
        int flames,
        boolean lightning,
        boolean groundAura,
        String meshRef,
        String animationRef) {

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder so transformation definitions read like data. */
    public static final class Builder {
        private int coreColor = 0xFFFFFFFF;
        private int glowColor = 0x80FFFFFF;
        private int sparkColor = 0xFFFFFFFF;
        private float intensity = 1.0F;
        private float scale = 1.0F;
        private int rings = 2;
        private float ringSpeed = 6.0F;
        private int flames = 3;
        private boolean lightning;
        private boolean groundAura = true;
        private String meshRef = "";
        private String animationRef = "";

        public Builder colors(int core, int glow, int spark) {
            this.coreColor = core;
            this.glowColor = glow;
            this.sparkColor = spark;
            return this;
        }

        public Builder intensity(float value) {
            this.intensity = value;
            return this;
        }

        public Builder scale(float value) {
            this.scale = value;
            return this;
        }

        public Builder rings(int count, float speed) {
            this.rings = count;
            this.ringSpeed = speed;
            return this;
        }

        public Builder flames(int count) {
            this.flames = count;
            return this;
        }

        public Builder lightning(boolean value) {
            this.lightning = value;
            return this;
        }

        public Builder groundAura(boolean value) {
            this.groundAura = value;
            return this;
        }

        public Builder mesh(String meshRef, String animationRef) {
            this.meshRef = meshRef == null ? "" : meshRef;
            this.animationRef = animationRef == null ? "" : animationRef;
            return this;
        }

        public AuraProfile build() {
            return new AuraProfile(coreColor, glowColor, sparkColor, intensity, scale, rings, ringSpeed,
                    flames, lightning, groundAura, meshRef, animationRef);
        }
    }
}
