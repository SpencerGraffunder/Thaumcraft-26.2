package thaumcraft.client.fx.particles;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.GraphicsPreset;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

/**
 * Bore particle effect - block fragments that fly towards a target (like arcane bore).
 * Particles attract to target position and shrink as they get close.
 */
@OnlyIn(Dist.CLIENT)
public class FXBoreParticles extends ThaumcraftParticle {
    
    private final BlockState blockState;
    private final ItemStack itemStack;
    private Entity target;
    private double targetX;
    private double targetY;
    private double targetZ;
    
    public FXBoreParticles(ClientLevel level, double x, double y, double z, 
                           double tx, double ty, double tz, 
                           BlockState state, int side) {
        super(level, x, y, z);
        
        this.blockState = state;
        this.itemStack = null;
        
        // Set sprite from block
        try {
            this.setSprite(Minecraft.getInstance().getModelManager()
                    .getBlockStateModelSet().getParticleMaterial(state).sprite());
        } catch (Exception e) {
            // Fallback if texture fails
            this.remove();
            return;
        }
        
        this.rCol = 0.6f;
        this.gCol = 0.6f;
        this.bCol = 0.6f;
        this.quadSize = this.random.nextFloat() * 0.3f + 0.4f;
        
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        
        // Calculate lifetime based on distance
        double dx = tx - x;
        double dy = ty - y;
        double dz = tz - z;
        int base = (int)(Mth.sqrt((float)(dx * dx + dy * dy + dz * dz)) * 10.0f);
        if (base < 1) base = 1;
        this.lifetime = base / 2 + this.random.nextInt(base);
        
        // Small random initial motion
        float f3 = 0.01f;
        this.xd = this.random.nextGaussian() * f3;
        this.yd = this.random.nextGaussian() * f3;
        this.zd = this.random.nextGaussian() * f3;
        this.gravity = 0.01f;
    }
    
    public FXBoreParticles(ClientLevel level, double x, double y, double z,
                           double tx, double ty, double tz,
                           double sx, double sy, double sz,
                           ItemStack item) {
        super(level, x, y, z, sx, sy, sz);
        
        this.blockState = null;
        this.itemStack = item;
        
        // Set sprite from item
        this.setSprite(itemParticleSprite(item, level));
        
        this.rCol = 0.6f;
        this.gCol = 0.6f;
        this.bCol = 0.6f;
        this.quadSize = this.random.nextFloat() * 0.3f + 0.4f;
        
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        
        // Calculate lifetime based on distance
        double dx = tx - x;
        double dy = ty - y;
        double dz = tz - z;
        int base = (int)(Mth.sqrt((float)(dx * dx + dy * dy + dz * dz)) * 10.0f);
        if (base < 1) base = 1;
        this.lifetime = base / 2 + this.random.nextInt(base);
        
        this.xd = sx + this.random.nextGaussian() * 0.01f;
        this.yd = sy + this.random.nextGaussian() * 0.01f;
        this.zd = sz + this.random.nextGaussian() * 0.01f;
        this.gravity = 0.01f;
        
        // Distance culling
        Entity renderEntity = Minecraft.getInstance().getCameraEntity();
        int visibleDistance = Minecraft.getInstance().options.graphicsPreset().get() != GraphicsPreset.FAST ? 64 : 32;
        if (renderEntity != null && renderEntity.distanceToSqr(x, y, z) > visibleDistance * visibleDistance) {
            this.lifetime = 0;
        }
    }
    
    private net.minecraft.client.renderer.texture.TextureAtlasSprite itemParticleSprite(ItemStack item, ClientLevel level) {
        try {
            ItemStackRenderState scratch = new ItemStackRenderState();
            Minecraft.getInstance().getItemModelResolver()
                    .updateForTopItem(scratch, item, ItemDisplayContext.GROUND, level, null, 0);
            Material.Baked material = scratch.pickParticleMaterial(this.random);
            if (material != null) return material.sprite();
        } catch (Exception ignored) {}
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.ITEMS).missingSprite();
    }

    public FXBoreParticles setTarget(Entity target) {
        this.target = target;
        return this;
    }
    
    public FXBoreParticles getObjectColor(BlockPos pos) {
        if (this.blockState != null) {
            try {
                BlockTintSource tintSource = Minecraft.getInstance().getBlockColors().getTintSource(this.blockState, 0);
                if (tintSource != null) {
                    int color = tintSource.colorAsTerrainParticle(this.blockState, this.level, pos);
                    this.rCol *= (color >> 16 & 0xFF) / 255.0f;
                    this.gCol *= (color >> 8 & 0xFF) / 255.0f;
                    this.bCol *= (color & 0xFF) / 255.0f;
                }
            } catch (Exception ignored) {}
        } else if (this.itemStack != null) {
            // TODO(26.2): item particle tint moved to ItemStackRenderState.LayerRenderState.tintLayers()
            // (private, no public accessor); item particles are left untinted like vanilla BreakingItemParticle.
        }
        return this;
    }
    
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        
        // Update target position if tracking entity
        if (this.target != null) {
            this.targetX = this.target.getX();
            this.targetY = this.target.getY() + this.target.getEyeHeight();
            this.targetZ = this.target.getZ();
        }
        
        // Check if reached target or expired
        if (this.age++ >= this.lifetime || 
            (Mth.floor(this.x) == Mth.floor(this.targetX) && 
             Mth.floor(this.y) == Mth.floor(this.targetY) && 
             Mth.floor(this.z) == Mth.floor(this.targetZ))) {
            this.remove();
            return;
        }
        
        // Move
        this.move(this.xd, this.yd, this.zd);
        
        // Friction
        this.xd *= 0.985;
        this.yd *= 0.95;
        this.zd *= 0.985;
        
        // Attract towards target
        double dx = this.targetX - this.x;
        double dy = this.targetY - this.y;
        double dz = this.targetZ - this.z;
        double dist = Mth.sqrt((float)(dx * dx + dy * dy + dz * dz));
        double clamp = Math.min(0.25, dist / 15.0);
        
        // Shrink when close
        if (dist < 2.0) {
            this.quadSize *= 0.9f;
        }
        
        // Normalize and apply attraction
        dx /= dist;
        dy /= dist;
        dz /= dist;
        
        this.xd += dx * clamp;
        this.yd += dy * clamp;
        this.zd += dz * clamp;
        
        // Clamp velocity
        this.xd = Mth.clamp(this.xd, -clamp, clamp);
        this.yd = Mth.clamp(this.yd, -clamp, clamp);
        this.zd = Mth.clamp(this.zd, -clamp, clamp);
        
        // Add some randomness
        this.xd += this.random.nextGaussian() * 0.005;
        this.yd += this.random.nextGaussian() * 0.005;
        this.zd += this.random.nextGaussian() * 0.005;
    }
    
    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.position();
        float x = (float)(Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float)(Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float)(Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());
        
        Quaternionf quaternion = camera.rotation();
        float size = 0.1f * this.quadSize;
        
        // Sprite UVs (block/item particle icon from the particle atlas)
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightCoords(partialTicks);
        
        int color = ARGB.colorFromFloat(1.0f, this.rCol, this.gCol, this.bCol);
        
        state.add(getLayer(), x, y, z, quaternion.x, quaternion.y, quaternion.z, quaternion.w,
                size, u0, u1, v0, v1, color, light);
    }
}
