package com.animeki.client.render;

import com.animeki.AnimeKi;
import com.animeki.boss.VoidTitanEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Model of the Void Titan: a deliberately oversized humanoid, built in code so the mod ships without
 * binary model files.
 *
 * <p>The mesh is defined instead of loaded, which keeps every vertex documented and lets a resource
 * pack swap in a Blender exported model by only replacing this class' layer definition (or the
 * {@code VoidTitanRenderer} itself).</p>
 */
public class VoidTitanModel extends HumanoidModel<VoidTitanEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(AnimeKi.id("void_titan"), "main");

    public VoidTitanModel(ModelPart root) {
        super(root);
    }

    /** Builds the layer definition consumed by the layer registry. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation armor = new CubeDeformation(0.35F);

        // Head sits high on a broad torso; the hat part is required by HumanoidModel.
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5.0F, -11.0F, -5.0F, 10.0F, 10.0F, 10.0F, CubeDeformation.NONE)
                .addBox(-1.0F, -13.0F, -1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, -4.0F, 0.0F));
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-5.0F, -11.0F, -5.0F, 10.0F, 10.0F, 10.0F, armor), PartPose.offset(0.0F, -4.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-7.0F, -12.0F, -4.0F, 14.0F, 14.0F, 8.0F, CubeDeformation.NONE)
                .addBox(-8.0F, -12.0F, -4.5F, 16.0F, 4.0F, 9.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 6.0F, 0.0F));
        // Arms: long, angled slightly outward, with heavy shoulder plating.
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16)
                .addBox(-3.0F, -2.0F, -2.5F, 5.0F, 16.0F, 5.0F, CubeDeformation.NONE), PartPose.offset(-9.0F, -4.5F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror()
                .addBox(-2.0F, -2.0F, -2.5F, 5.0F, 16.0F, 5.0F, CubeDeformation.NONE), PartPose.offset(9.0F, -4.5F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-3.0F, 0.0F, -3.0F, 6.0F, 18.0F, 6.0F, CubeDeformation.NONE), PartPose.offset(-3.5F, 6.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror()
                .addBox(-3.0F, 0.0F, -3.0F, 6.0F, 18.0F, 6.0F, CubeDeformation.NONE), PartPose.offset(3.5F, 6.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
