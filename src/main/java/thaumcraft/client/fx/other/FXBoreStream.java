package thaumcraft.client.fx.other;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import thaumcraft.client.fx.particles.ThaumcraftParticle;

/**
 * Bore stream particle effect - creates a beam-like stream for the arcane bore.
 * Shows the direction of mining/energy flow.
 */
@OnlyIn(Dist.CLIENT)
public class FXBoreStream extends ThaumcraftParticle {
    
    private final double targetX, targetY, targetZ;
    private float beamLength;
    private float rotationAngle;
    
    public FXBoreStream(ClientLevel level, double x, double y, double z,
                        double tx, double ty, double tz, float scale) {
        super(level, x, y, z);
        
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        
        // Calculate beam length
        double dx = tx - x;
        double dy = ty - y;
        double dz = tz - z;
        this.beamLength = Mth.sqrt((float)(dx * dx + dy * dy + dz * dz));
        
        this.rCol = 0.5f;
        this.gCol = 0.3f;
        this.bCol = 0.8f;
        this.alpha = 0.8f;
        
        this.quadSize = scale;
        this.lifetime = 10;
        this.gravity = 0;
        this.rotationAngle = this.random.nextFloat() * 360.0f;
        this.noClip = true;
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
        
        // Rotate slightly each tick
        this.rotationAngle += 5.0f;
        
        // Fade out
        float progress = (float) this.age / (float) this.lifetime;
        this.alpha = 0.8f * (1.0f - progress);
    }
    
    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.position();
        
        float x1 = (float)(this.x - cameraPos.x());
        float y1 = (float)(this.y - cameraPos.y());
        float z1 = (float)(this.z - cameraPos.z());
        
        float x2 = (float)(this.targetX - cameraPos.x());
        float y2 = (float)(this.targetY - cameraPos.y());
        float z2 = (float)(this.targetZ - cameraPos.z());
        
        // Calculate beam length
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.001f) return;
        
        // Render the beam as a chain of overlapping camera-facing billboard quads
        // along the source->target line (render-state particle model).
        int segments = Math.max(2, Math.min(8, (int)(length * 4.0f)));
        float width = this.quadSize * 0.05f;
        
        Quaternionf rot = camera.rotation();
        int light = 0xF000F0; // Full brightness
        
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float px = x1 + dx * t;
            float py = y1 + dy * t;
            float pz = z1 + dz * t;
            
            // Fade alpha towards the target end (matches the old per-vertex fade)
            int color = ARGB.colorFromFloat(this.alpha * (1.0f - t * 0.5f), this.rCol, this.gCol, this.bCol);
            
            state.add(getLayer(), px, py, pz, rot.x, rot.y, rot.z, rot.w, width,
                    0.0f, 1.0f, 0.0f, 1.0f, color, light);
        }
    }
}
