package com.animeki.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client only presentation configuration: visual intensity, camera feedback and HUD layout.
 *
 * <p>These values never influence gameplay - the server is authoritative for everything that
 * matters. They let players tune the mod for low end machines or motion sensitivity.</p>
 */
public final class AnimeKiClientConfig {
    public static final ModConfigSpec SPEC;
    public static final Vfx VFX;
    public static final Camera CAMERA;
    public static final Hud HUD;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        AnimeKiClientConfig instance = new AnimeKiClientConfig(builder);
        SPEC = builder.build();
        VFX = instance.vfx;
        CAMERA = instance.camera;
        HUD = instance.hud;
    }

    public final Vfx vfx;
    public final Camera camera;
    public final Hud hud;

    private AnimeKiClientConfig(ModConfigSpec.Builder builder) {
        this.vfx = new Vfx(builder);
        this.camera = new Camera(builder);
        this.hud = new Hud(builder);
    }

    /** Visual effect intensity. */
    public static final class Vfx {
        public final ModConfigSpec.DoubleValue intensity;
        public final ModConfigSpec.BooleanValue auraEnabled;
        public final ModConfigSpec.IntValue auraQuality;
        public final ModConfigSpec.BooleanValue trailsEnabled;
        public final ModConfigSpec.BooleanValue beamsEnabled;
        public final ModConfigSpec.BooleanValue vanillaParticles;
        public final ModConfigSpec.BooleanValue externalAssets;

        Vfx(ModConfigSpec.Builder builder) {
            builder.comment("Visual effect tuning. Effects are always cosmetic and client side.").push("vfx");
            intensity = builder.comment("Global multiplier for every custom effect (aurad, beams, impacts).")
                    .defineInRange("intensity", 1.0D, 0.0D, 3.0D);
            auraEnabled = builder.define("auraEnabled", true);
            auraQuality = builder.comment("1 = minimal, 2 = normal, 3 = high detail.")
                    .defineInRange("auraQuality", 2, 1, 3);
            trailsEnabled = builder.define("trailsEnabled", true);
            beamsEnabled = builder.define("beamsEnabled", true);
            vanillaParticles = builder.comment("Also spawn vanilla particles alongside custom effects.")
                    .define("vanillaParticles", true);
            externalAssets = builder.comment("Allow resource packs to replace effects with Blender exported meshes/animations.")
                    .define("externalAssets", true);
            builder.pop();
        }
    }

    /** Camera feedback. */
    public static final class Camera {
        public final ModConfigSpec.BooleanValue screenShakeEnabled;
        public final ModConfigSpec.DoubleValue screenShakeScale;
        public final ModConfigSpec.BooleanValue flashEnabled;
        public final ModConfigSpec.BooleanValue fovEffects;
        public final ModConfigSpec.BooleanValue hitZoom;

        Camera(ModConfigSpec.Builder builder) {
            builder.comment("Camera feedback. Only ever affects the local client.").push("camera");
            screenShakeEnabled = builder.define("screenShakeEnabled", true);
            screenShakeScale = builder.comment("0.0 disables shake amplitude, 2.0 doubles it.")
                    .defineInRange("screenShakeScale", 1.0D, 0.0D, 4.0D);
            flashEnabled = builder.define("flashEnabled", true);
            fovEffects = builder.comment("Speed based FOV changes while flying or dashing.")
                    .define("fovEffects", true);
            hitZoom = builder.comment("Small camera punch when landing a heavy hit.")
                    .define("hitZoom", true);
            builder.pop();
        }
    }

    /** Ki HUD layout. */
    public static final class Hud {
        public final ModConfigSpec.BooleanValue showKiBar;
        public final ModConfigSpec.BooleanValue alwaysShowKiBar;
        public final ModConfigSpec.DoubleValue scale;
        public final ModConfigSpec.IntValue offsetX;
        public final ModConfigSpec.IntValue offsetY;
        public final ModConfigSpec.BooleanValue showTransformation;
        public final ModConfigSpec.BooleanValue showComboCounter;
        public final ModConfigSpec.BooleanValue showAbilityCooldowns;
        public final ModConfigSpec.BooleanValue showBossBar;
        public final ModConfigSpec.DoubleValue lowKiThreshold;
        public final ModConfigSpec.BooleanValue animatedBars;

        Hud(ModConfigSpec.Builder builder) {
            builder.comment("Ki HUD layout, kept compatible with the vanilla HUD.").push("hud");
            showKiBar = builder.define("showKiBar", true);
            alwaysShowKiBar = builder.comment("Show the Ki bar even when full and idle.")
                    .define("alwaysShowKiBar", true);
            scale = builder.defineInRange("scale", 1.0D, 0.5D, 2.0D);
            offsetX = builder.comment("Horizontal offset from the bottom left of the screen.")
                    .defineInRange("offsetX", 0, -2000, 2000);
            offsetY = builder.comment("Vertical offset, positive values move the HUD upwards.")
                    .defineInRange("offsetY", 0, -2000, 2000);
            showTransformation = builder.define("showTransformation", true);
            showComboCounter = builder.define("showComboCounter", true);
            showAbilityCooldowns = builder.define("showAbilityCooldowns", true);
            showBossBar = builder.define("showBossBar", true);
            lowKiThreshold = builder.defineInRange("lowKiThreshold", 0.25D, 0.0D, 1.0D);
            animatedBars = builder.define("animatedBars", true);
            builder.pop();
        }
    }
}
