package thaumcraft.client.fx.particles;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

/**
 * Breaking item particle with fade effect.
 * Shows item fragments that fade out over time.
 */
@OnlyIn(Dist.CLIENT)
public class FXBreakingFade extends ThaumcraftParticle {
    
    private final ItemStack itemStack;
    
    public FXBreakingFade(ClientLevel level, double x, double y, double z, Item item) {
        this(level, x, y, z, item, 0);
    }
    
    public FXBreakingFade(ClientLevel level, double x, double y, double z, Item item, int meta) {
        this(level, x, y, z, 0, 0, 0, item, meta);
    }
    
    public FXBreakingFade(ClientLevel level, double x, double y, double z, 
                          double vx, double vy, double vz, Item item, int meta) {
        super(level, x, y, z, vx, vy, vz);
        
        this.itemStack = new ItemStack(item);
        
        // Set sprite from item
        this.setSprite(itemParticleSprite(itemStack, level));
        
        this.gravity = 1.0f;
        this.quadSize = 0.1f;
        this.lifetime = 20;
        this.alpha = 1.0f;
        
        // Random initial velocity
        this.xd = vx + (this.random.nextFloat() - 0.5f) * 0.2f;
        this.yd = vy + this.random.nextFloat() * 0.2f;
        this.zd = vz + (this.random.nextFloat() - 0.5f) * 0.2f;
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

    public FXBreakingFade setParticleMaxAge(int age) {
        this.lifetime = age;
        return this;
    }
    
    public FXBreakingFade setParticleGravity(float g) {
        this.gravity = g;
        return this;
    }
    
    public FXBreakingFade setSpeed(double vx, double vy, double vz) {
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        return this;
    }
    
    public void boom() {
        float f = (float)(this.random.nextFloat() + this.random.nextFloat() + 1.0) * 0.15f;
        float len = Mth.sqrt((float)(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd));
        this.xd = this.xd / len * f * 0.964f;
        this.yd = this.yd / len * f * 0.964f + 0.1f;
        this.zd = this.zd / len * f * 0.964f;
    }
    
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        
        // Apply gravity
        this.yd -= 0.04 * this.gravity;
        
        // Move
        this.move(this.xd, this.yd, this.zd);
        
        // Friction
        this.xd *= 0.98;
        this.yd *= 0.98;
        this.zd *= 0.98;
        
        if (this.onGround) {
            this.xd *= 0.7;
            this.zd *= 0.7;
        }
    }
    
    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        // Calculate fade
        float fade = 1.0f - (float)this.age / (float)this.lifetime;
        
        Vec3 cameraPos = camera.position();
        float x = (float)(Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float)(Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float)(Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());
        
        Quaternionf quaternion = camera.rotation();
        float size = this.quadSize;
        
        // Sprite UVs (item particle icon from the particle atlas)
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightCoords(partialTicks);
        
        int color = ARGB.colorFromFloat(this.alpha * fade, this.rCol, this.gCol, this.bCol);
        
        state.add(getLayer(), x, y, z, quaternion.x, quaternion.y, quaternion.z, quaternion.w,
                size, u0, u1, v0, v1, color, light);
    }
}
