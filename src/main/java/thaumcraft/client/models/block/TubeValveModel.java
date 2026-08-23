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
import net.minecraft.util.Unit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;

/**
 * Model for essentia tube valve components.
 * Used by TileTubeValveRenderer, TileTubeBufferRenderer, and TileTubeOnewayRenderer.
 */
@OnlyIn(Dist.CLIENT)
public class TubeValveModel extends Model<Unit> {

    public static final ModelLayerLocation LAYER_LOCATION = 
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "tube_valve"), "main");

    private final ModelPart valveRod;
    private final ModelPart valveRing;

    public TubeValveModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.valveRod = root.getChild("valve_rod");
        this.valveRing = root.getChild("valve_ring");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // ValveRod: 2x2x2 at y=2
        partdefinition.addOrReplaceChild("valve_rod", CubeListBuilder.create()
                .texOffs(0, 10)
                .addBox(-1.0f, 2.0f, -1.0f, 2.0f, 2.0f, 2.0f),
                PartPose.ZERO);

        // ValveRing: 4x1x4 at y=4
        partdefinition.addOrReplaceChild("valve_ring", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0f, 4.0f, -2.0f, 4.0f, 1.0f, 4.0f),
                PartPose.ZERO);

        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    /**
     * Render only the rod part.
     */
    public void renderRod(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        valveRod.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    /**
     * Render only the ring part.
     */
    public void renderRing(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        valveRing.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    /**
     * Get the rod part.
     */
    public ModelPart getRod() {
        return valveRod;
    }

    /**
     * Get the ring part.
     */
    public ModelPart getRing() {
        return valveRing;
    }
}
