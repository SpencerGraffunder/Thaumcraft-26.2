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
 * CrossbowAdvancedModel - Model for the advanced crossbow turret.
 * 
 * Based on the original crossbow_advanced.obj from 1.12.2.
 * Features additional parts: shield, box (ammunition storage), brain module, and loader mechanism.
 */
@OnlyIn(Dist.CLIENT)
public class CrossbowAdvancedModel extends EntityModel<TurretCrossbowRenderState> {
    
    public static final ModelLayerLocation LAYER_LOCATION = 
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "turret_crossbow_advanced"), "main");
    
    // Base/legs
    private final ModelPart legs;
    
    // Main mechanism (rotates for aiming)
    private final ModelPart mech;
    private final ModelPart box;
    private final ModelPart shield;
    private final ModelPart brain;
    private final ModelPart loader;
    private final ModelPart bow1;
    private final ModelPart bow2;
    
    // Animation values
    private float loadProgress = 0.0f;
    
    public CrossbowAdvancedModel(ModelPart root) {
        super(root);
        this.legs = root.getChild("legs");
        this.mech = root.getChild("mech");
        this.box = mech.getChild("box");
        this.shield = mech.getChild("shield");
        this.brain = mech.getChild("brain");
        this.loader = mech.getChild("loader");
        this.bow1 = mech.getChild("bow1");
        this.bow2 = mech.getChild("bow2");
    }
    
    /**
     * Create the layer definition for the advanced crossbow turret model.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        
        // Legs - tripod base with 4 legs
        PartDefinition legsPart = partdefinition.addOrReplaceChild("legs",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 9.0F, 0.0F));
        
        // Individual legs angled outward
        legsPart.addOrReplaceChild("leg1",
            CubeListBuilder.create()
                .texOffs(0, 8)
                .addBox(-1.0F, 0.0F, 0.0F, 2.0F, 15.0F, 2.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 2.0F, -2.0F, 0.5236F, 0.0F, 0.0F));
        
        legsPart.addOrReplaceChild("leg2",
            CubeListBuilder.create()
                .texOffs(0, 8)
                .addBox(-1.0F, 0.0F, -2.0F, 2.0F, 15.0F, 2.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 2.0F, 2.0F, -0.5236F, 0.0F, 0.0F));
        
        legsPart.addOrReplaceChild("leg3",
            CubeListBuilder.create()
                .texOffs(0, 8)
                .addBox(0.0F, 0.0F, -1.0F, 2.0F, 15.0F, 2.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(-2.0F, 2.0F, 0.0F, 0.0F, 0.0F, -0.5236F));
        
        legsPart.addOrReplaceChild("leg4",
            CubeListBuilder.create()
                .texOffs(0, 8)
                .addBox(-2.0F, 0.0F, -1.0F, 2.0F, 15.0F, 2.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(2.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.5236F));
        
        // Main mechanism body (rotates with head)
        PartDefinition mechPart = partdefinition.addOrReplaceChild("mech",
            CubeListBuilder.create()
                .texOffs(16, 0)
                .addBox(-3.0F, -2.0F, -6.0F, 6.0F, 4.0F, 12.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 6.0F, 0.0F));
        
        // Ammunition box on top
        mechPart.addOrReplaceChild("box",
            CubeListBuilder.create()
                .texOffs(40, 16)
                .addBox(-2.5F, -6.0F, -4.0F, 5.0F, 4.0F, 8.0F, CubeDeformation.NONE),
            PartPose.ZERO);
        
        // Shield on front
        mechPart.addOrReplaceChild("shield",
            CubeListBuilder.create()
                .texOffs(40, 0)
                .addBox(-4.0F, -3.0F, -8.0F, 8.0F, 6.0F, 2.0F, CubeDeformation.NONE),
            PartPose.ZERO);
        
        // Brain module (thaumic targeting)
        mechPart.addOrReplaceChild("brain",
            CubeListBuilder.create()
                .texOffs(52, 8)
                .addBox(-1.5F, -4.0F, 4.0F, 3.0F, 3.0F, 3.0F, CubeDeformation.NONE),
            PartPose.ZERO);
        
        // Loader mechanism (slides back during reload)
        mechPart.addOrReplaceChild("loader",
            CubeListBuilder.create()
                .texOffs(16, 16)
                .addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 6.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 0.0F, 2.0F));
        
        // Bow arm 1 (left)
        mechPart.addOrReplaceChild("bow1",
            CubeListBuilder.create()
                .texOffs(0, 26)
                .addBox(1.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F, CubeDeformation.NONE)
                .texOffs(16, 26)
                .addBox(6.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 0.0F, -6.0F));
        
        // Bow arm 2 (right)
        mechPart.addOrReplaceChild("bow2",
            CubeListBuilder.create()
                .texOffs(0, 26)
                .addBox(-7.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F, CubeDeformation.NONE)
                .texOffs(16, 26)
                .addBox(-9.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 0.0F, -6.0F));
        
        return LayerDefinition.create(meshdefinition, 64, 32);
    }
    
    @Override
    public void setupAnim(TurretCrossbowRenderState state) {
        // Store load progress for animation
        this.loadProgress = state.loadProgress;
        
        // Mech rotation (aiming)
        this.mech.yRot = state.yRot * ((float)Math.PI / 180F);
        this.mech.xRot = state.xRot * ((float)Math.PI / 180F);
        
        // Loader slide animation during reload
        float loaderZ = Mth.sin(Mth.sqrt(loadProgress) * (float)Math.PI * 2.0F) / 12.0F * 16.0F;
        this.loader.z = 2.0F + loaderZ;
        
        // Bow arm animation on firing
        float swingProgress = state.attackAnim;
        if (swingProgress > 0) {
            float bowAnim = Mth.sin(Mth.sqrt(swingProgress) * (float)Math.PI * 2.0F) * 0.35F;
            this.bow1.yRot = bowAnim;
            this.bow2.yRot = -bowAnim;
        } else {
            this.bow1.yRot = 0.0F;
            this.bow2.yRot = 0.0F;
        }
    }
}
