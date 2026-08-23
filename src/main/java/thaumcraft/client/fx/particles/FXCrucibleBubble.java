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

/**
 * FXCrucibleBubble - Colored bubble particle for crucible effects.
 * Rises slowly, wobbles, and pops with the color of the current aspect.
 * 
 * Ported from 1.12.2
 */
@OnlyIn(Dist.CLIENT)
public class FXCrucibleBubble extends ThaumcraftParticle {
    
    private float wobblePhase;
    private float wobbleSpeed;
    
    /**
     * Create a crucible bubble particle.
     * 
     * @param level Client level
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @param r Red component (0-1)
     * @param g Green component (0-1)
     * @param b Blue component (0-1)
     */
    public FXCrucibleBubble(ClientLevel level, double x, double y, double z,
                            float r, float g, float b) {
        super(level, x, y, z, 0, 0, 0);
        
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.alpha = 0.7f;
        
        this.quadSize = 0.05f + random.nextFloat() * 0.03f;
        
        // Slow upward drift with slight random horizontal motion
        this.xd = (random.nextFloat() - 0.5f) * 0.01;
        this.yd = 0.01 + random.nextFloat() * 0.015;
        this.zd = (random.nextFloat() - 0.5f) * 0.01;
        
        this.gravity = -0.001f;  // Slight upward buoyancy
        
        this.lifetime = 20 + random.nextInt(20);
        
        this.wobblePhase = random.nextFloat() * (float)Math.PI * 2;
        this.wobbleSpeed = 0.2f + random.nextFloat() * 0.1f;
    }
    
    @Override
    public void tick() {
        this.xo = x;
        this.yo = y;
        this.zo = z;
        
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        
        // Wobble motion
        wobblePhase += wobbleSpeed;
        xd += Mth.sin(wobblePhase) * 0.001;
        zd += Mth.cos(wobblePhase * 0.7f) * 0.001;
        
        // Apply velocity
        x += xd;
        y += yd;
        z += zd;
        
        // Dampen horizontal motion
        xd *= 0.95;
        zd *= 0.95;
        
        // Grow slightly then shrink before popping
        if (age < lifetime * 0.3f) {
            quadSize *= 1.02f;
        } else if (age > lifetime * 0.8f) {
            quadSize *= 0.95f;
            alpha *= 0.9f;
        }
    }
    
    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        Vec3 camPos = camera.position();
        float px = (float)(Mth.lerp(partialTicks, xo, x) - camPos.x());
        float py = (float)(Mth.lerp(partialTicks, yo, y) - camPos.y());
        float pz = (float)(Mth.lerp(partialTicks, zo, z) - camPos.z());

        // Single camera-facing billboard quad (26.2 render-state model)
        Quaternionf rot = camera.rotation();
        int color = ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol);
        int light = 0xF000F0;

        // Bubble sprite (using a circular sprite from the particle sheet)
        // Sprite 160 is a good bubble-like sprite
        int sprite = 160;
        float u0 = (sprite % 64) / 64.0f;
        float u1 = u0 + 0.015625f;
        float v0 = (sprite / 64) / 64.0f;
        float v1 = v0 + 0.015625f;

        state.add(getLayer(), px, py, pz, rot.x, rot.y, rot.z, rot.w, this.quadSize,
                u0, u1, v0, v1, color, light);
    }
}
