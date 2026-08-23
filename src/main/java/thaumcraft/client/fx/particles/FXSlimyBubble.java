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
 * FXSlimyBubble - Animated bubble effect for flux goo/liquid death.
 * Shows a bubble rising from slime, inflating then popping.
 * Uses animated sprite frames from the particle sheet.
 * 
 * Animation sequence:
 * - Frames 144-146: Bubble forming
 * - Frames 147-148: Bubble floating (loops)
 * - Frames 148-150: Bubble popping
 * 
 * Ported from 1.12.2
 */
@OnlyIn(Dist.CLIENT)
public class FXSlimyBubble extends ThaumcraftParticle {
    
    private int particle = 144;
    
    /**
     * Create a slimy bubble particle.
     * 
     * @param level The client level
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @param scale Particle scale
     */
    public FXSlimyBubble(ClientLevel level, double x, double y, double z, float scale) {
        super(level, x, y, z, 0, 0, 0);
        
        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;
        
        this.gravity = 0.0f;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        
        this.quadSize = scale;
        this.lifetime = 15 + random.nextInt(5);
        this.setSize(0.01f, 0.01f);
    }
    
    /**
     * Create a colored slimy bubble.
     */
    public FXSlimyBubble(ClientLevel level, double x, double y, double z, float scale, float r, float g, float b) {
        this(level, x, y, z, scale);
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
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
        
        // Animation sequence
        if (age - 1 < 6) {
            // Forming phase - bubble grows
            particle = 144 + age / 2;
            if (age == 5) {
                // Pop up slightly when fully formed
                y += 0.1;
            }
        } else if (age < lifetime - 4) {
            // Floating phase - bubble drifts up, wobbles
            yd += 0.005;
            particle = 147 + (age % 4) / 2;
        } else {
            // Popping phase - slow down and pop
            yd /= 2.0;
            particle = 150 - (lifetime - age) / 2;
        }
        
        y += yd;
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

        // Calculate UV from particle index (64x64 grid)
        float u0 = (particle % 64) / 64.0f;
        float u1 = u0 + 0.015625f;
        float v0 = (particle / 64) / 64.0f;
        float v1 = v0 + 0.015625f;

        state.add(getLayer(), px, py, pz, rot.x, rot.y, rot.z, rot.w, this.quadSize,
                u0, u1, v0, v1, color, light);
    }
}
