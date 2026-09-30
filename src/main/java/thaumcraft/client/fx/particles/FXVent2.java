package thaumcraft.client.fx.particles;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.awt.Color;

/**
 * Vent particle effect - expanding smoke/steam-like particle.
 * Used for vents, steam effects, and similar atmospheric effects.
 */
@OnlyIn(Dist.CLIENT)
public class FXVent2 extends ThaumcraftParticle {
    
    private float maxScale;
    private float grav;
    
    public FXVent2(ClientLevel level, double x, double y, double z, 
                   double vx, double vy, double vz, int color) {
        super(level, x, y, z, vx, vy, vz);
        
        this.setSize(0.02f, 0.02f);
        this.quadSize = this.random.nextFloat() * 0.1f + 0.05f;
        this.maxScale = this.quadSize;
        this.startScale = 0.01f;
        this.endScale = this.maxScale;
        
        // Parse color
        Color c = new Color(color);
        this.rCol = (float) Mth.clamp(c.getRed() / 255.0f + this.random.nextGaussian() * 0.05, 0.0, 1.0);
        this.gCol = (float) Mth.clamp(c.getGreen() / 255.0f + this.random.nextGaussian() * 0.05, 0.0, 1.0);
        this.bCol = (float) Mth.clamp(c.getBlue() / 255.0f + this.random.nextGaussian() * 0.05, 0.0, 1.0);
        this.startR = this.rCol;
        this.startG = this.gCol;
        this.startB = this.bCol;
        this.endR = this.rCol;
        this.endG = this.gCol;
        this.endB = this.bCol;
        
        this.alpha = 0.33f;
        this.grav = (float) (this.random.nextGaussian() * 0.0075);
        this.gravity = 0;
        this.slowDown = 0.85;
        this.lifetime = 40;
        this.noClip = false;
    }
    
    private float baseAlpha = 0.33f;
    
    public void setAlphaF(float alpha) {
        this.baseAlpha = alpha;
        this.alpha = alpha;
    }
    
    public FXVent2 setScale(float f) {
        this.quadSize *= f;
        this.maxScale *= f;
        this.startScale *= f;
        this.endScale = this.maxScale;
        return this;
    }
    
    public FXVent2 setHeading(double vx, double vy, double vz, float speed, float spread) {
        float len = Mth.sqrt((float)(vx * vx + vy * vy + vz * vz));
        vx /= len;
        vy /= len;
        vz /= len;
        vx += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075 * spread;
        vy += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075 * spread;
        vz += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075 * spread;
        this.xd = vx * speed;
        this.yd = vy * speed;
        this.zd = vz * speed;
        return this;
    }
    
    @Override
    public Layer getLayer() {
        return TC_PARTICLES_LAYER_TRANSLUCENT;
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.position();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());

        float size = 0.3f * this.quadSize;
        int color = ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol);
        int light = this.getLightCoords(partialTicks);

        Quaternionf rot = camera.rotation();

        // 1.12: part = 1 + growth*4, cell = (part%16, part/64); X-mirrored (pass u1, u0)
        int part = (int) (1.0f + (this.maxScale > 0.0f ? this.quadSize / this.maxScale : 0.0f) * 4.0f);
        float uL = (part % 16) / 64.0f;
        float uR = uL + 0.015625f;
        float vT = (part / 64) / 64.0f;
        float vB = vT + 0.015625f;

        state.add(getLayer(), x, y, z, rot.x, rot.y, rot.z, rot.w, size,
                uR, uL, vT, vB, color, light);
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
        
        // Add gravity variation
        this.yd += this.grav;
        
        // Move
        this.move(this.xd, this.yd, this.zd);
        
        // Slow down
        this.xd *= this.slowDown;
        this.yd *= this.slowDown;
        this.zd *= this.slowDown;
        
        // Scale grows over time
        float progress = (float) this.age / (float) this.lifetime;
        this.quadSize = Mth.lerp(progress, this.startScale, this.endScale);
        
        // Fade out alpha as scale reaches max
        this.alpha = baseAlpha * (1.0f - progress);
        
        if (this.onGround) {
            this.xd *= 0.7;
            this.zd *= 0.7;
        }
    }
    
    public FXVent2 setRGB(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.startR = r;
        this.startG = g;
        this.startB = b;
        this.endR = r;
        this.endG = g;
        this.endB = b;
        return this;
    }
}
