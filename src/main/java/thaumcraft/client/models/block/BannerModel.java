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
import thaumcraft.client.renderers.tile.state.BannerRenderState;

/**
 * BannerModel - Model for Thaumcraft banners.
 * 
 * Components:
 * - Pole: Vertical support for standing banners
 * - Beam: Horizontal bar at top
 * - B1/B2: Decorative tabs at ends of beam
 * - Banner: The main banner cloth
 * 
 * Ported from 1.12.2 ModelBanner.
 */
@OnlyIn(Dist.CLIENT)
public class BannerModel extends Model<BannerRenderState> {
    
    public static final ModelLayerLocation LAYER_LOCATION = 
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "banner"), "main");
    
    private final ModelPart pole;
    private final ModelPart beam;
    private final ModelPart b1;
    private final ModelPart b2;
    private final ModelPart banner;
    
    public BannerModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.pole = root.getChild("pole");
        this.beam = root.getChild("beam");
        this.b1 = root.getChild("b1");
        this.b2 = root.getChild("b2");
        this.banner = root.getChild("banner");
    }
    
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        
        // Pole: vertical support (width 2, height 31, depth 2)
        partdefinition.addOrReplaceChild("pole", CubeListBuilder.create()
                .texOffs(62, 0)
                .addBox(-1.0F, -7.0F, -3.0F, 2, 31, 2),
                PartPose.ZERO);
        
        // Beam: horizontal bar at top (width 14, height 2, depth 2)
        partdefinition.addOrReplaceChild("beam", CubeListBuilder.create()
                .texOffs(30, 0)
                .addBox(-7.0F, -7.0F, -1.0F, 14, 2, 2),
                PartPose.ZERO);
        
        // B1: left decorative tab (width 2, height 3, depth 3)
        partdefinition.addOrReplaceChild("b1", CubeListBuilder.create()
                .texOffs(0, 29)
                .addBox(-5.0F, -7.5F, -1.5F, 2, 3, 3),
                PartPose.ZERO);
        
        // B2: right decorative tab (width 2, height 3, depth 3)
        partdefinition.addOrReplaceChild("b2", CubeListBuilder.create()
                .texOffs(0, 29)
                .addBox(3.0F, -7.5F, -1.5F, 2, 3, 3),
                PartPose.ZERO);
        
        // Banner: main cloth (width 14, height 28, depth 1)
        partdefinition.addOrReplaceChild("banner", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-7.0F, 0.0F, -0.5F, 14, 28, 1),
                PartPose.offset(0.0F, -5.0F, 0.0F));
        
        return LayerDefinition.create(meshdefinition, 128, 64);
    }
    
    @Override
    public void setupAnim(BannerRenderState state) {
        super.setupAnim(state);
        banner.xRot = state.wind;
    }
    
    /**
     * Set the banner's rotation angle for wind animation.
     */
    public void setBannerRotation(float xRot) {
        banner.xRot = xRot;
    }
}
