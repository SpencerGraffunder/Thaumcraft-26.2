package thaumcraft.client.models.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import thaumcraft.client.renderers.entity.state.EldritchGolemRenderState;

/**
 * EldritchGolemModel - Model for the Eldritch Golem boss.
 * 
 * A massive armored construct with:
 * - Robed body with collar and cloak
 * - Optional head (can be headless)
 * - Large shoulder pauldrons
 * - Segmented waist armor
 * - Attack animations for arm swings
 */
@OnlyIn(Dist.CLIENT)
public class EldritchGolemModel extends EntityModel<EldritchGolemRenderState> {
    
    public static final ModelLayerLocation LAYER_LOCATION = 
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "eldritch_golem"), "main");
    
    private final ModelPart head;
    private final ModelPart headStump; // Shown when headless
    private final ModelPart torso;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart cloak;
    private final ModelPart frontCloth;
    
    public EldritchGolemModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.headStump = root.getChild("head_stump");
        this.torso = root.getChild("torso");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.cloak = root.getChild("cloak");
        this.frontCloth = root.getChild("front_cloth");
    }
    
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        
        // Head - main head with hood
        partdefinition.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(47, 12)
                .addBox(-3.5F, -6.0F, -2.5F, 7.0F, 7.0F, 5.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 4.5F, -3.8F, -0.1047F, 0.0F, 0.0F));
        
        // Head stump - shown when headless
        partdefinition.addOrReplaceChild("head_stump",
            CubeListBuilder.create()
                .texOffs(26, 16)
                .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, -0.1047F, 0.0F, 0.0F));
        
        // Torso with collar
        PartDefinition torso = partdefinition.addOrReplaceChild("torso",
            CubeListBuilder.create()
                .texOffs(34, 45)
                .addBox(-5.0F, 2.5F, -3.0F, 10.0F, 10.0F, 6.0F, CubeDeformation.NONE)
                // Collar pieces
                .texOffs(75, 50)
                .addBox(3.5F, -0.5F, -4.5F, 1.0F, 4.0F, 10.0F, CubeDeformation.NONE) // CollarL
                .texOffs(67, 50)
                .addBox(-4.5F, -0.5F, -4.5F, 1.0F, 4.0F, 10.0F, CubeDeformation.NONE) // CollarR
                .texOffs(77, 59)
                .addBox(-3.5F, -0.5F, 4.5F, 7.0F, 4.0F, 1.0F, CubeDeformation.NONE) // CollarB
                .texOffs(77, 59)
                .addBox(-3.5F, -0.5F, -5.5F, 7.0F, 4.0F, 1.0F, CubeDeformation.NONE), // CollarF
            PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.1745F, 0.0F, 0.0F));
        
        // Right arm with shoulder pauldron
        PartDefinition rightArm = partdefinition.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(78, 32)
                .addBox(-3.5F, 1.5F, -2.0F, 4.0F, 13.0F, 5.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(-5.0F, 3.0F, -2.0F, 0.0F, 0.0F, 0.1047F));
        
        // Right shoulder pauldron
        rightArm.addOrReplaceChild("right_shoulder",
            CubeListBuilder.create()
                .texOffs(56, 31)
                .addBox(-4.5F, -1.5F, -2.5F, 5.0F, 6.0F, 6.0F, CubeDeformation.NONE)
                .texOffs(0, 0)
                .addBox(-4.3F, -1.0F, -3.0F, 4.0F, 5.0F, 7.0F, CubeDeformation.NONE),
            PartPose.ZERO);
        
        // Left arm with shoulder pauldron
        PartDefinition leftArm = partdefinition.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
                .texOffs(78, 32).mirror()
                .addBox(-0.5F, 1.5F, -2.0F, 4.0F, 13.0F, 5.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(5.0F, 3.0F, -2.0F, 0.0F, 0.0F, -0.1047F));
        
        // Left shoulder pauldron
        leftArm.addOrReplaceChild("left_shoulder",
            CubeListBuilder.create()
                .texOffs(56, 31).mirror()
                .addBox(-0.5F, -1.5F, -2.5F, 5.0F, 6.0F, 6.0F, CubeDeformation.NONE)
                .texOffs(0, 0).mirror()
                .addBox(0.3F, -1.0F, -3.0F, 4.0F, 5.0F, 7.0F, CubeDeformation.NONE),
            PartPose.ZERO);
        
        // Right leg with waist armor
        partdefinition.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(79, 19)
                .addBox(-2.5F, 2.5F, -2.0F, 4.0F, 9.0F, 4.0F, CubeDeformation.NONE)
                // Waist armor
                .texOffs(96, 14)
                .addBox(-3.0F, -0.5F, -2.5F, 5.0F, 3.0F, 5.0F, CubeDeformation.NONE),
            PartPose.offset(-2.0F, 12.5F, 0.0F));
        
        // Left leg with waist armor
        partdefinition.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
                .texOffs(79, 19).mirror()
                .addBox(-1.5F, 2.5F, -2.0F, 4.0F, 9.0F, 4.0F, CubeDeformation.NONE)
                // Waist armor
                .texOffs(96, 14).mirror()
                .addBox(-2.0F, -0.5F, -2.5F, 5.0F, 3.0F, 5.0F, CubeDeformation.NONE),
            PartPose.offset(2.0F, 12.5F, 0.0F));
        
        // Back cloak
        partdefinition.addOrReplaceChild("cloak",
            CubeListBuilder.create()
                .texOffs(0, 47)
                .addBox(-5.0F, 1.5F, 4.0F, 10.0F, 12.0F, 1.0F, CubeDeformation.NONE)
                .texOffs(0, 59)
                .addBox(-5.0F, 13.5F, 1.7F, 10.0F, 4.0F, 1.0F, CubeDeformation.NONE)
                .texOffs(0, 37)
                .addBox(-5.0F, 17.5F, -0.8F, 10.0F, 4.0F, 1.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.1396F, 0.0F, 0.0F));
        
        // Front cloth
        partdefinition.addOrReplaceChild("front_cloth",
            CubeListBuilder.create()
                .texOffs(114, 52)
                .addBox(-3.0F, 3.2F, -3.5F, 6.0F, 10.0F, 1.0F, CubeDeformation.NONE)
                .texOffs(114, 39)
                .addBox(-3.0F, 13.5F, -3.5F, 6.0F, 6.0F, 1.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.1745F, 0.0F, 0.0F));
        
        return LayerDefinition.create(meshdefinition, 128, 64);
    }
    
    @Override
    public void setupAnim(EldritchGolemRenderState state) {
        // Head rotation (unless spawning or headless)
        if (state.spawnTimer > 0) {
            this.head.xRot = state.spawnTimer / 2.0F / 57.3F;
            this.head.yRot = 0.0F;
        } else {
            this.head.yRot = state.yRot / 4.0F * ((float)Math.PI / 180F);
            this.head.xRot = state.xRot / 2.0F * ((float)Math.PI / 180F) - 0.1047F;
            this.headStump.yRot = state.yRot * ((float)Math.PI / 180F);
            this.headStump.xRot = state.xRot * ((float)Math.PI / 180F) - 0.1047F;
        }
        
        // Head visibility based on headless state
        this.head.visible = !state.headless;
        this.headStump.visible = state.headless;
        
        // Leg animation
        this.rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.4662F) * 1.4F * state.walkAnimationSpeed;
        this.leftLeg.xRot = Mth.cos(state.walkAnimationPos * 0.4662F + (float)Math.PI) * 1.4F * state.walkAnimationSpeed;
        
        // Arm animation
        int attackTimer = state.attackTimer;
        if (attackTimer > 0) {
            // Attack animation - arms swing down
            float attackProgress = doAbs(attackTimer - (state.ageInTicks - (int)state.ageInTicks), 10.0F);
            this.rightArm.xRot = -2.0F + 1.5F * attackProgress;
            this.leftArm.xRot = -2.0F + 1.5F * attackProgress;
        } else {
            // Walking arm swing
            this.rightArm.xRot = Mth.cos(state.walkAnimationPos * 0.4F + (float)Math.PI) * 2.0F * state.walkAnimationSpeed * 0.5F;
            this.leftArm.xRot = Mth.cos(state.walkAnimationPos * 0.4F) * 2.0F * state.walkAnimationSpeed * 0.5F;
        }
        
        // Cloak animation - sways with movement
        float cloakSway = Math.min(Mth.cos(state.walkAnimationPos * 0.44F) * 1.4F * state.walkAnimationSpeed,
                Mth.cos(state.walkAnimationPos * 0.44F + (float)Math.PI) * 1.4F * state.walkAnimationSpeed);
        this.cloak.xRot = -cloakSway / 3.0F + 0.1396F;
        this.frontCloth.xRot = cloakSway + 0.1745F;
    }
    
    private float doAbs(float value, float range) {
        return (Math.abs(value % range - range * 0.5F) - range * 0.25F) / (range * 0.25F);
    }
}
