package thaumcraft.client.models.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.entity.state.TurretCrossbowRenderState;

/**
 * CrossbowModel - Model for the basic crossbow turret.
 * 
 * Based on the original ModelCrossbow from 1.12.2.
 * Features a tripod base with 4 legs and a rotating crossbow mechanism.
 */
@OnlyIn(Dist.CLIENT)
public class CrossbowModel extends EntityModel<TurretCrossbowRenderState> {
    
    public static final ModelLayerLocation LAYER_LOCATION = 
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "turret_crossbow"), "main");
    
    // Main parts
    private final ModelPart crossbow;
    private final ModelPart tripod;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart leg4;
    
    // Crossbow sub-parts (children of crossbow)
    private final ModelPart crossl1;
    private final ModelPart crossl2;
    private final ModelPart crossl3;
    private final ModelPart crossr1;
    private final ModelPart crossr2;
    private final ModelPart crossr3;
    private final ModelPart loadbarcross;
    private final ModelPart loadbarl;
    private final ModelPart loadbarr;
    
    // Animation values
    private float loadProgress = 0.0f;
    
    public CrossbowModel(ModelPart root) {
        super(root);
        this.crossbow = root.getChild("crossbow");
        this.tripod = root.getChild("tripod");
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.leg4 = root.getChild("leg4");
        
        // Get crossbow children
        this.crossl1 = crossbow.getChild("crossl1");
        this.crossl2 = crossbow.getChild("crossl2");
        this.crossl3 = crossbow.getChild("crossl3");
        this.crossr1 = crossbow.getChild("crossr1");
        this.crossr2 = crossbow.getChild("crossr2");
        this.crossr3 = crossbow.getChild("crossr3");
        this.loadbarcross = crossbow.getChild("loadbarcross");
        this.loadbarl = crossbow.getChild("loadbarl");
        this.loadbarr = crossbow.getChild("loadbarr");
    }
    
    /**
     * Create the layer definition for the crossbow turret model.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        
        // Tripod center
        partdefinition.addOrReplaceChild("tripod",
            CubeListBuilder.create()
                .texOffs(13, 0)
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 12.0F, 0.0F));
        
        // Legs (4 legs on tripod)
        partdefinition.addOrReplaceChild("leg1",
            CubeListBuilder.create()
                .texOffs(20, 10)
                .addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5236F, 0.0F, 0.0F));
        
        partdefinition.addOrReplaceChild("leg2",
            CubeListBuilder.create()
                .texOffs(20, 10)
                .addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5236F, 1.5708F, 0.0F));
        
        partdefinition.addOrReplaceChild("leg3",
            CubeListBuilder.create()
                .texOffs(20, 10)
                .addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5236F, 3.1416F, 0.0F));
        
        partdefinition.addOrReplaceChild("leg4",
            CubeListBuilder.create()
                .texOffs(20, 10)
                .addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5236F, 4.7124F, 0.0F));
        
        // Main crossbow body
        PartDefinition crossbowPart = partdefinition.addOrReplaceChild("crossbow",
            CubeListBuilder.create()
                .texOffs(28, 14)
                .addBox(-2.0F, 0.0F, -7.0F, 4.0F, 2.0F, 14.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 10.0F, 0.0F));
        
        // Ammo box on top
        crossbowPart.addOrReplaceChild("ammobox",
            CubeListBuilder.create()
                .texOffs(38, 0)
                .addBox(-2.0F, -5.0F, -6.0F, 4.0F, 5.0F, 9.0F, CubeDeformation.NONE),
            PartPose.ZERO);
        
        // Barrel
        crossbowPart.addOrReplaceChild("barrel",
            CubeListBuilder.create()
                .texOffs(20, 28)
                .addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE),
            PartPose.ZERO);
        
        // Base bar cross
        crossbowPart.addOrReplaceChild("basebarcross",
            CubeListBuilder.create()
                .texOffs(0, 13)
                .addBox(-2.0F, 0.5F, 10.0F, 4.0F, 1.0F, 1.0F, CubeDeformation.NONE),
            PartPose.ZERO);
        
        // Base bars
        crossbowPart.addOrReplaceChild("basebarr",
            CubeListBuilder.create()
                .texOffs(40, 23)
                .addBox(-1.0F, 0.0F, 7.0F, 1.0F, 2.0F, 5.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.1396F, 0.0F));
        
        crossbowPart.addOrReplaceChild("basebarl",
            CubeListBuilder.create()
                .texOffs(40, 23)
                .addBox(0.0F, 0.0F, 7.0F, 1.0F, 2.0F, 5.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.1396F, 0.0F));
        
        // Loading mechanism
        crossbowPart.addOrReplaceChild("loadbarcross",
            CubeListBuilder.create()
                .texOffs(0, 13)
                .addBox(-2.0F, -8.5F, -0.5F, 4.0F, 1.0F, 1.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5585F, 0.0F, 0.0F));
        
        crossbowPart.addOrReplaceChild("loadbarl",
            CubeListBuilder.create()
                .texOffs(0, 15)
                .addBox(2.0F, -9.0F, -1.0F, 1.0F, 11.0F, 2.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5585F, 0.0F, 0.0F));
        
        crossbowPart.addOrReplaceChild("loadbarr",
            CubeListBuilder.create()
                .texOffs(0, 15)
                .addBox(-3.0F, -9.0F, -1.0F, 1.0F, 11.0F, 2.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5585F, 0.0F, 0.0F));
        
        // Cross bow arms (left side)
        crossbowPart.addOrReplaceChild("crossl1",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(0.0F, 0.0F, -6.0F, 5.0F, 2.0F, 1.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.2443F, 0.0F));
        
        crossbowPart.addOrReplaceChild("crossl2",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(4.0F, 0.0F, -5.0F, 3.0F, 2.0F, 1.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.2443F, 0.0F));
        
        crossbowPart.addOrReplaceChild("crossl3",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(6.0F, 0.0F, -4.0F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.2443F, 0.0F));
        
        // Cross bow arms (right side)
        crossbowPart.addOrReplaceChild("crossr1",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-5.0F, 0.0F, -6.0F, 5.0F, 2.0F, 1.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.2443F, 0.0F));
        
        crossbowPart.addOrReplaceChild("crossr2",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-7.0F, 0.0F, -5.0F, 3.0F, 2.0F, 1.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.2443F, 0.0F));
        
        crossbowPart.addOrReplaceChild("crossr3",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-8.0F, 0.0F, -4.0F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.2443F, 0.0F));
        
        return LayerDefinition.create(meshdefinition, 64, 32);
    }
    
    @Override
    public void setupAnim(TurretCrossbowRenderState state) {
        // Store load progress for animation
        this.loadProgress = state.loadProgress;
        
        // Head rotation (crossbow aims)
        this.crossbow.yRot = state.yRot * ((float)Math.PI / 180F);
        this.crossbow.xRot = state.xRot * ((float)Math.PI / 180F);
        
        // Bow arm animation on firing
        float swingProgress = state.attackAnim;
        if (swingProgress > 0) {
            float bowAnim = Mth.sin(Mth.sqrt(swingProgress) * (float)Math.PI * 2.0F) * 0.2F;
            this.crossl1.yRot = -0.2443F + bowAnim;
            this.crossl2.yRot = -0.2443F + bowAnim;
            this.crossl3.yRot = -0.2443F + bowAnim;
            this.crossr1.yRot = 0.2443F - bowAnim;
            this.crossr2.yRot = 0.2443F - bowAnim;
            this.crossr3.yRot = 0.2443F - bowAnim;
        } else {
            // Reset to default
            this.crossl1.yRot = -0.2443F;
            this.crossl2.yRot = -0.2443F;
            this.crossl3.yRot = -0.2443F;
            this.crossr1.yRot = 0.2443F;
            this.crossr2.yRot = 0.2443F;
            this.crossr3.yRot = 0.2443F;
        }
        
        // Loading bar animation
        float loadAnim = -0.5585F + Mth.sin(Mth.sqrt(loadProgress) * (float)Math.PI * 2.0F) * 0.5F;
        this.loadbarcross.xRot = loadAnim;
        this.loadbarl.xRot = loadAnim;
        this.loadbarr.xRot = loadAnim;
        
        // Adjust legs if riding a minecart
        if (state.passenger) {
            this.leg1.y = 11.5F;
            this.leg2.y = 11.5F;
            this.leg3.y = 11.5F;
            this.leg4.y = 11.5F;
            this.leg1.xRot = 0.1F;
            this.leg2.xRot = 0.1F;
            this.leg3.xRot = 0.1F;
            this.leg4.xRot = 0.1F;
        } else {
            this.leg1.y = 12.0F;
            this.leg2.y = 12.0F;
            this.leg3.y = 12.0F;
            this.leg4.y = 12.0F;
            this.leg1.xRot = 0.5236F;
            this.leg2.xRot = 0.5236F;
            this.leg3.xRot = 0.5236F;
            this.leg4.xRot = 0.5236F;
        }
    }
}
