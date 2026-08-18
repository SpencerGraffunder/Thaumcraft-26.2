package thaumcraft.client.models.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;

/**
 * Model for the Bellows block entity.
 * Features animated accordion-style bag that compresses/expands.
 */
@OnlyIn(Dist.CLIENT)
public class BellowsModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = 
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "bellows"), "main");

    private final ModelPart bottomPlank;
    private final ModelPart middlePlank;
    private final ModelPart topPlank;
    private final ModelPart bag;
    private final ModelPart nozzle;

    public BellowsModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.bottomPlank = root.getChild("bottom_plank");
        this.middlePlank = root.getChild("middle_plank");
        this.topPlank = root.getChild("top_plank");
        this.bag = root.getChild("bag");
        this.nozzle = root.getChild("nozzle");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Bottom plank: 12x2x12 at y=22 (near bottom of block)
        partdefinition.addOrReplaceChild("bottom_plank", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-6.0f, 0.0f, -6.0f, 12.0f, 2.0f, 12.0f),
                PartPose.offset(0.0f, 22.0f, 0.0f));

        // Middle plank: 12x2x12 at y=16 (middle of block)
        partdefinition.addOrReplaceChild("middle_plank", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-6.0f, -1.0f, -6.0f, 12.0f, 2.0f, 12.0f),
                PartPose.offset(0.0f, 16.0f, 0.0f));

        // Top plank: 12x2x12 at y=8 (near top of block)
        partdefinition.addOrReplaceChild("top_plank", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-6.0f, 0.0f, -6.0f, 12.0f, 2.0f, 12.0f),
                PartPose.offset(0.0f, 8.0f, 0.0f));

        // Bag (accordion): 20x24x20 centered at y=16
        partdefinition.addOrReplaceChild("bag", CubeListBuilder.create()
                .texOffs(48, 0)
                .addBox(-10.0f, -12.0f, -10.0f, 20.0f, 24.0f, 20.0f),
                PartPose.offset(0.0f, 16.0f, 0.0f));

        // Nozzle: 4x4x2 at front
        partdefinition.addOrReplaceChild("nozzle", CubeListBuilder.create()
                .texOffs(0, 36)
                .addBox(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 2.0f),
                PartPose.offset(0.0f, 16.0f, 6.0f));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, 
                               int packedOverlay, float red, float green, float blue, float alpha) {
        bottomPlank.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        middlePlank.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        topPlank.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        bag.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        nozzle.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    /**
     * Set the inflation/compression state of the bellows.
     * @param inflation 0.0 = fully compressed, 1.0 = fully expanded
     */
    public void setInflation(float inflation) {
        // Scale the bag vertically based on inflation
        // The bag's Y scale changes with inflation
        float scale = 0.125f + inflation * 0.875f;
        bag.yScale = scale;
        
        // Move planks based on inflation
        // Top plank moves down as bellows compress
        float topOffset = (1.0f - inflation) * 6.0f; // Moves down up to 6 units
        topPlank.y = 8.0f + topOffset;
        
        // Bottom plank moves up as bellows compress  
        float bottomOffset = (1.0f - inflation) * 6.0f;
        bottomPlank.y = 22.0f - bottomOffset;
    }

    /**
     * Get the top plank for custom rendering.
     */
    public ModelPart getTopPlank() {
        return topPlank;
    }

    /**
     * Get the bottom plank for custom rendering.
     */
    public ModelPart getBottomPlank() {
        return bottomPlank;
    }

    /**
     * Get the bag for custom rendering.
     */
    public ModelPart getBag() {
        return bag;
    }
}
