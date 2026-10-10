package com.omnitrix.mod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.omnitrix.mod.OmnitrixMod;
import com.omnitrix.mod.alien.AlienForm;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class AlienModels {

    public static final ResourceLocation TEXTURE_HEATBLAST = ResourceLocation.fromNamespaceAndPath(OmnitrixMod.MODID, "textures/entity/alien/heatblast.png");
    public static final ResourceLocation TEXTURE_XLR8 = ResourceLocation.fromNamespaceAndPath(OmnitrixMod.MODID, "textures/entity/alien/xlr8.png");
    public static final ResourceLocation TEXTURE_FOUR_ARMS = ResourceLocation.fromNamespaceAndPath(OmnitrixMod.MODID, "textures/entity/alien/four_arms.png");
    public static final ResourceLocation TEXTURE_DIAMONDHEAD = ResourceLocation.fromNamespaceAndPath(OmnitrixMod.MODID, "textures/entity/alien/diamondhead.png");
    public static final ResourceLocation TEXTURE_CANNONBOLT = ResourceLocation.fromNamespaceAndPath(OmnitrixMod.MODID, "textures/entity/alien/cannonbolt.png");
    public static final ResourceLocation TEXTURE_UPGRADE = ResourceLocation.fromNamespaceAndPath(OmnitrixMod.MODID, "textures/entity/alien/upgrade.png");
    public static final ResourceLocation TEXTURE_STINKFLY = ResourceLocation.fromNamespaceAndPath(OmnitrixMod.MODID, "textures/entity/alien/stinkfly.png");
    public static final ResourceLocation TEXTURE_WILDMUTT = ResourceLocation.fromNamespaceAndPath(OmnitrixMod.MODID, "textures/entity/alien/wildmutt.png");

    private static final Map<AlienForm, ModelPart> alienModels = new HashMap<>();

    static {
        // Build the procedural geometry for all 8 aliens!
        alienModels.put(AlienForm.HEATBLAST, createHeatblastMesh().bakeRoot());
        alienModels.put(AlienForm.XLR8, createXLR8Mesh().bakeRoot());
        alienModels.put(AlienForm.FOUR_ARMS, createFourArmsMesh().bakeRoot());
        alienModels.put(AlienForm.DIAMONDHEAD, createDiamondheadMesh().bakeRoot());
        alienModels.put(AlienForm.CANNONBOLT, createCannonboltMesh().bakeRoot());
        alienModels.put(AlienForm.UPGRADE, createUpgradeMesh().bakeRoot());
        alienModels.put(AlienForm.STINKFLY, createStinkflyMesh().bakeRoot());
        alienModels.put(AlienForm.WILDMUTT, createWildmuttMesh().bakeRoot());
    }

    public static ResourceLocation getTexture(AlienForm form) {
        return switch (form) {
            case HEATBLAST -> TEXTURE_HEATBLAST;
            case XLR8 -> TEXTURE_XLR8;
            case FOUR_ARMS -> TEXTURE_FOUR_ARMS;
            case DIAMONDHEAD -> TEXTURE_DIAMONDHEAD;
            case CANNONBOLT -> TEXTURE_CANNONBOLT;
            case UPGRADE -> TEXTURE_UPGRADE;
            case STINKFLY -> TEXTURE_STINKFLY;
            case WILDMUTT -> TEXTURE_WILDMUTT;
            default -> TEXTURE_HEATBLAST;
        };
    }

    // ==========================================
    // 1. HEATBLAST: Magma cracks, fiery collar, fiery head corona
    // ==========================================
    private static LayerDefinition createHeatblastMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head with flaming crown
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                        // Flame crest
                        .texOffs(32, 0).addBox(-3.0F, -11.0F, -3.0F, 6.0F, 3.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Torso with magma collar
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)
                        .texOffs(16, 32).addBox(-5.0F, -1.0F, -3.0F, 10.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Arms (thick magma hands)
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        // Legs
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    // ==========================================
    // 2. XLR8: Sleek aerodynamic helmet cone, velociraptor tail, wheeled feet
    // ==========================================
    private static LayerDefinition createXLR8Mesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Sleek helmet head with pointed cone at back
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F)
                        // Aerodynamic fin / cone
                        .texOffs(32, 0).addBox(-2.0F, -6.0F, 3.5F, 4.0F, 5.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Slender athletic torso + tail
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-3.5F, 0.0F, -2.0F, 7.0F, 12.0F, 4.0F)
                        // Dynamic tail
                        .texOffs(24, 32).addBox(-1.5F, 9.0F, 2.0F, 3.0F, 3.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Scissor claw arms
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -2.0F, -1.5F, 3.0F, 12.0F, 3.0F)
                        .texOffs(52, 16).addBox(-1.5F, 10.0F, -3.5F, 2.0F, 2.0F, 3.0F),
                PartPose.offset(-4.5F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -1.5F, 3.0F, 12.0F, 3.0F)
                        .texOffs(52, 16).mirror().addBox(-0.5F, 10.0F, -3.5F, 2.0F, 2.0F, 3.0F),
                PartPose.offset(4.5F, 2.0F, 0.0F));

        // Legs with spheres / roller balls at feet
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F)
                        .texOffs(0, 32).addBox(-2.0F, 9.5F, -2.0F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F)
                        .texOffs(0, 32).mirror().addBox(-2.0F, 9.5F, -2.0F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    // ==========================================
    // 3. FOUR ARMS: Massive Tetramand with 4 fully articulated arms
    // ==========================================
    private static LayerDefinition createFourArmsMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Fierce quad-eyed head
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 9.0F, 9.0F),
                PartPose.offset(0.0F, -2.0F, 0.0F));

        // Massive muscular torso
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 20).addBox(-6.0F, -2.0F, -3.5F, 12.0F, 14.0F, 7.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Upper Arms
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 0).addBox(-4.0F, -2.0F, -2.5F, 5.0F, 12.0F, 5.0F),
                PartPose.offset(-7.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-1.0F, -2.0F, -2.5F, 5.0F, 12.0F, 5.0F),
                PartPose.offset(7.0F, 0.0F, 0.0F));

        // Lower Arms (secondary pair!)
        root.addOrReplaceChild("right_arm_lower",
                CubeListBuilder.create().texOffs(40, 20).addBox(-3.5F, -1.0F, -2.0F, 4.0F, 11.0F, 4.0F),
                PartPose.offset(-6.5F, 5.0F, 0.0F));

        root.addOrReplaceChild("left_arm_lower",
                CubeListBuilder.create().texOffs(40, 20).mirror().addBox(-0.5F, -1.0F, -2.0F, 4.0F, 11.0F, 4.0F),
                PartPose.offset(6.5F, 5.0F, 0.0F));

        // Powerful thick legs
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 20).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F),
                PartPose.offset(-2.8F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 20).mirror().addBox(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F),
                PartPose.offset(2.8F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    // ==========================================
    // 4. DIAMONDHEAD: Petrosapien crystalline spikes on back and pointed crown
    // ==========================================
    private static LayerDefinition createDiamondheadMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Crystalline jaw & crown
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                        // Back horn
                        .texOffs(32, 0).addBox(-1.5F, -11.0F, 0.0F, 3.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Torso with 2 giant crystalline back spikes
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4.5F, 0.0F, -2.5F, 9.0F, 12.0F, 5.0F)
                        // Left crystal back shard
                        .texOffs(0, 32).addBox(-3.5F, -5.0F, 2.5F, 3.0F, 9.0F, 3.0F)
                        // Right crystal back shard
                        .texOffs(0, 32).addBox(0.5F, -5.0F, 2.5F, 3.0F, 9.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Crystal blade arms
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F)
                        .texOffs(40, 36).addBox(-2.5F, 6.0F, -4.0F, 3.0F, 7.0F, 2.0F),
                PartPose.offset(-5.5F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F)
                        .texOffs(40, 36).mirror().addBox(-0.5F, 6.0F, -4.0F, 3.0F, 7.0F, 2.0F),
                PartPose.offset(5.5F, 2.0F, 0.0F));

        // Legs
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-2.0F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(2.0F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    // ==========================================
    // 5. CANNONBOLT: Arburian Pelarota armored hemisphere shell
    // ==========================================
    private static LayerDefinition createCannonboltMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Wide head embedded in shoulders
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -5.0F, -4.0F, 8.0F, 6.0F, 8.0F),
                PartPose.offset(0.0F, 2.0F, -2.0F));

        // Massive armored shell sphere body
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 16).addBox(-7.0F, 0.0F, -5.0F, 14.0F, 14.0F, 12.0F)
                        // Back armor plates
                        .texOffs(0, 42).addBox(-6.0F, 1.0F, 7.0F, 12.0F, 12.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Heavy paws
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 0).addBox(-4.0F, -2.0F, -2.5F, 5.0F, 11.0F, 5.0F),
                PartPose.offset(-7.5F, 3.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-1.0F, -2.0F, -2.5F, 5.0F, 11.0F, 5.0F),
                PartPose.offset(7.5F, 3.0F, 0.0F));

        // Stubby armored legs
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(40, 20).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 10.0F, 5.0F),
                PartPose.offset(-3.5F, 14.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(40, 20).mirror().addBox(-2.5F, 0.0F, -2.5F, 5.0F, 10.0F, 5.0F),
                PartPose.offset(3.5F, 14.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    // ==========================================
    // 6. UPGRADE: Galvanic Mechamorph sleek tech body with glowing ocular lens
    // ==========================================
    private static LayerDefinition createUpgradeMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Domed head with central eye
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                        // Cyclops optical ring
                        .texOffs(32, 0).addBox(-2.0F, -5.0F, -4.5F, 4.0F, 4.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Smooth circuit-laced torso
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Smooth morphing arms
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        // Legs
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    // ==========================================
    // 7. STINKFLY: Insectoid head with eye stalks, 4 wings, tail stinger
    // ==========================================
    private static LayerDefinition createStinkflyMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Insectoid head + 4 eye stalks
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 7.0F, 6.0F)
                        // 4 eye stalks
                        .texOffs(32, 0).addBox(-6.0F, -9.0F, -4.0F, 3.0F, 3.0F, 3.0F)
                        .texOffs(32, 0).addBox(3.0F, -9.0F, -4.0F, 3.0F, 3.0F, 3.0F)
                        .texOffs(32, 0).addBox(-6.0F, -5.0F, -4.0F, 3.0F, 3.0F, 3.0F)
                        .texOffs(32, 0).addBox(3.0F, -5.0F, -4.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Thorax + 4 Wings + Tail
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-3.5F, 0.0F, -2.5F, 7.0F, 12.0F, 5.0F)
                        // Stinger tail
                        .texOffs(24, 34).addBox(-1.0F, 8.0F, 2.5F, 2.0F, 2.0F, 10.0F)
                        .texOffs(24, 46).addBox(-0.5F, 5.0F, 11.5F, 1.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Animated Wings
        root.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(0, 36).addBox(0.0F, -2.0F, 2.0F, 12.0F, 8.0F, 0.1F),
                PartPose.offset(2.0F, 2.0F, 2.0F));

        root.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(0, 36).mirror().addBox(-12.0F, -2.0F, 2.0F, 12.0F, 8.0F, 0.1F),
                PartPose.offset(-2.0F, 2.0F, 2.0F));

        // Spindly arms
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -2.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(-4.5F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(4.5F, 2.0F, 0.0F));

        // Spindly legs
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    // ==========================================
    // 8. WILDMUTT: Quadrupedal Vulpimancer with quill spine & gill jowls
    // ==========================================
    private static LayerDefinition createWildmuttMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Eyeless head with sharp muzzle & sensory gills
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -8.0F, 8.0F, 7.0F, 8.0F)
                        // Sensory gills
                        .texOffs(32, 0).addBox(-5.5F, -2.0F, -5.0F, 11.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, 9.0F, -6.0F));

        // Horizontal muscular quadruped body
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 16).addBox(-5.0F, -3.0F, -8.0F, 10.0F, 8.0F, 16.0F)
                        // Spine quill ridge
                        .texOffs(0, 40).addBox(-1.0F, -5.0F, -7.0F, 2.0F, 2.0F, 14.0F),
                PartPose.offset(0.0F, 10.0F, 0.0F));

        // Front Left / Right Legs
        root.addOrReplaceChild("right_arm", // used as front right leg
                CubeListBuilder.create().texOffs(36, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
                PartPose.offset(-4.5F, 14.0F, -5.0F));

        root.addOrReplaceChild("left_arm", // used as front left leg
                CubeListBuilder.create().texOffs(36, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
                PartPose.offset(4.5F, 14.0F, -5.0F));

        // Back Left / Right Legs
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(36, 30).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
                PartPose.offset(-4.5F, 14.0F, 6.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(36, 30).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
                PartPose.offset(4.5F, 14.0F, 6.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static void renderAlien(AlienForm form, Player player, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTick) {
        ModelPart root = alienModels.get(form);
        if (root == null) return;

        ResourceLocation texture = getTexture(form);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));

        // Calculate animation parameters
        float limbSwing = player.walkAnimation.position(partialTick);
        float limbSwingAmount = player.walkAnimation.speed(partialTick);
        float ageInTicks = player.tickCount + partialTick;
        float netHeadYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.getYHeadRot()) - Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float headPitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());

        // Apply animations based on alien structure
        animateAlien(form, root, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        root.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
    }

    private static void animateAlien(AlienForm form, ModelPart root, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.resetPose();

        ModelPart head = root.getChild("head");
        ModelPart body = root.getChild("body");
        ModelPart rightArm = root.getChild("right_arm");
        ModelPart leftArm = root.getChild("left_arm");
        ModelPart rightLeg = root.getChild("right_leg");
        ModelPart leftLeg = root.getChild("left_leg");

        // Universal head rotation
        head.yRot = netHeadYaw * ((float)Math.PI / 180F);
        head.xRot = headPitch * ((float)Math.PI / 180F);

        // Walking / Running limbs
        rightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
        leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;

        // Alien-specific animations
        if (form == AlienForm.FOUR_ARMS) {
            ModelPart rightArmLower = root.getChild("right_arm_lower");
            ModelPart leftArmLower = root.getChild("left_arm_lower");
            rightArmLower.xRot = leftArm.xRot * 0.8F;
            leftArmLower.xRot = rightArm.xRot * 0.8F;
        } else if (form == AlienForm.STINKFLY) {
            ModelPart leftWing = root.getChild("left_wing");
            ModelPart rightWing = root.getChild("right_wing");
            // High speed wing flapping
            float flap = Mth.sin(ageInTicks * 1.3F) * 0.7F;
            leftWing.yRot = flap;
            rightWing.yRot = -flap;
        } else if (form == AlienForm.XLR8) {
            // XLR8 sprints with aggressive forward lean
            if (limbSwingAmount > 0.1F) {
                body.xRot = 0.4F;
                head.xRot += 0.3F;
            }
        }
    }

    public static void renderFirstPersonArm(AlienForm form, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ModelPart root = alienModels.get(form);
        if (root == null) return;

        ResourceLocation texture = getTexture(form);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));

        ModelPart armPart = (arm == HumanoidArm.RIGHT) ? root.getChild("right_arm") : root.getChild("left_arm");
        armPart.resetPose();
        armPart.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
    }
}
