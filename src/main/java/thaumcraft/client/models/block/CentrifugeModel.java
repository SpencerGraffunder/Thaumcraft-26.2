package thaumcraft.client.models.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.tile.state.CentrifugeRenderState;

/**
 * Model for the Centrifuge block entity.
 * Has a static outer frame (top/bottom boxes) and a spinning inner mechanism.
 */
@OnlyIn(Dist.CLIENT)
public class CentrifugeModel extends Model<CentrifugeRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = 
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "centrifuge"), "main");

    private final ModelPart top;
    private final ModelPart bottom;
    private final ModelPart spinnyBit; // Contains crossbar, core, and dinguses

    public CentrifugeModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.top = root.getChild("top");
        this.bottom = root.getChild("bottom");
        this.spinnyBit = root.getChild("spinny_bit");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Static top box: 8x4x8 at y=-8 (relative to center)
        partdefinition.addOrReplaceChild("top", CubeListBuilder.create()
                .texOffs(20, 16)
                .addBox(-4.0f, -8.0f, -4.0f, 8.0f, 4.0f, 8.0f),
                PartPose.ZERO);

        // Static bottom box: 8x4x8 at y=4 (relative to center)
        partdefinition.addOrReplaceChild("bottom", CubeListBuilder.create()
                .texOffs(20, 16)
                .addBox(-4.0f, 4.0f, -4.0f, 8.0f, 4.0f, 8.0f),
                PartPose.ZERO);

        // Spinning mechanism group
        PartDefinition spinnyBit = partdefinition.addOrReplaceChild("spinny_bit", CubeListBuilder.create(),
                PartPose.ZERO);

        // Crossbar: 8x2x2 centered
        spinnyBit.addOrReplaceChild("crossbar", CubeListBuilder.create()
                .texOffs(16, 0)
                .addBox(-4.0f, -1.0f, -1.0f, 8.0f, 2.0f, 2.0f),
                PartPose.ZERO);

        // Core: 3x8x3 centered
        spinnyBit.addOrReplaceChild("core", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-1.5f, -4.0f, -1.5f, 3.0f, 8.0f, 3.0f),
                PartPose.ZERO);

        // Dingus1 (right): 4x6x4 at x=4
        spinnyBit.addOrReplaceChild("dingus1", CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(4.0f, -3.0f, -2.0f, 4.0f, 6.0f, 4.0f),
                PartPose.ZERO);

        // Dingus2 (left): 4x6x4 at x=-8
        spinnyBit.addOrReplaceChild("dingus2", CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-8.0f, -3.0f, -2.0f, 4.0f, 6.0f, 4.0f),
                PartPose.ZERO);

        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    @Override
    public void setupAnim(CentrifugeRenderState state) {
        super.setupAnim(state);
        spinnyBit.yRot = state.rotation;
    }

    /**
     * Set the Y rotation of the spinning mechanism.
     */
    public void setSpinRotation(float rotationY) {
        spinnyBit.yRot = rotationY;
    }
}
