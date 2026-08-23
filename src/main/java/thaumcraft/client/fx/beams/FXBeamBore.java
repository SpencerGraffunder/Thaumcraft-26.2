package thaumcraft.client.fx.beams;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.ARGB;
import org.joml.Quaternionf;
import thaumcraft.client.fx.particles.ThaumcraftParticle;

/**
 * FXBeamBore - Point-to-point beam effect without entity attachment.
 * Used for arcane bore mining beams, infusion effects, and other
 * static position beams.
 * 
 * Features:
 * - Fixed source position (not attached to entity)
 * - Multiple beam texture options
 * - Scrolling UV animation
 * - Pulse fade in/out effect
 * - Impact flash at target point
 * - Source glow sprite
 * - 3 rotated quads for cylindrical appearance
 * 
 * Ported from 1.12.2
 */
@OnlyIn(Dist.CLIENT)
public class FXBeamBore extends ThaumcraftParticle {
    
    private static final Identifier BEAM_TEXTURE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/beam.png");
    private static final Identifier BEAM1_TEXTURE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/beam1.png");
    private static final Identifier BEAM2_TEXTURE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/beam2.png");
    private static final Identifier BEAM3_TEXTURE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/beam3.png");
    private static final Identifier PARTICLE_TEXTURE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final Identifier NODE_TEXTURE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/auranodes.png");
    
    // Target position
    protected double targetX;
    protected double targetY;
    protected double targetZ;
    protected double prevTargetX;
    protected double prevTargetY;
    protected double prevTargetZ;
    
    // Beam geometry
    protected float length;
    protected float rotYaw;
    protected float rotPitch;
    protected float prevYaw;
    protected float prevPitch;
    
    // Configuration
    protected int beamType = 0;
    protected float endMod = 1.0f;
    protected boolean reverse = false;
    protected boolean pulse = true;
    protected int rotationSpeed = 5;
    
    // State
    protected float prevSize = 0.0f;
    public int impact = 0;
    
    /**
     * Create a beam from a fixed source position to a target point.
     * 
     * @param level The client level
     * @param px Source X coordinate
     * @param py Source Y coordinate
     * @param pz Source Z coordinate
     * @param tx Target X coordinate
     * @param ty Target Y coordinate
     * @param tz Target Z coordinate
     * @param r Red color component (0-1)
     * @param g Green color component (0-1)
     * @param b Blue color component (0-1)
     * @param maxAge Maximum age in ticks
     */
    public FXBeamBore(ClientLevel level, double px, double py, double pz,
                      double tx, double ty, double tz,
                      float r, float g, float b, int maxAge) {
        super(level, px, py, pz, 0, 0, 0);
        
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        this.prevTargetX = tx;
        this.prevTargetY = ty;
        this.prevTargetZ = tz;
        
        // Calculate initial beam orientation
        calculateBeamGeometry();
        this.prevYaw = rotYaw;
        this.prevPitch = rotPitch;
        
        this.lifetime = maxAge;
        
        // Distance-based visibility culling
        Minecraft mc = Minecraft.getInstance();
        int visibleDistance = mc.options.graphicsPreset().get() != GraphicsPreset.FAST ? 64 : 32;
        if (mc.getCameraEntity() != null && mc.getCameraEntity().distanceToSqr(px, py, pz) > visibleDistance * visibleDistance) {
            this.lifetime = 0;
        }
    }
    
    /**
     * Update the beam's source and target positions.
     * Call this to extend the beam's life and change both endpoints.
     */
    public void updateBeam(double sx, double sy, double sz, double tx, double ty, double tz) {
        this.x = sx;
        this.y = sy;
        this.z = sz;
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        
        // Extend lifetime
        while (this.lifetime - this.age < 4) {
            this.lifetime++;
        }
    }
    
    /**
     * Calculate beam length, yaw, and pitch from source to target.
     */
    protected void calculateBeamGeometry() {
        float dx = (float)(x - targetX);
        float dy = (float)(y - targetY);
        float dz = (float)(z - targetZ);
        
        this.length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        double horizontalDist = Mth.sqrt(dx * dx + dz * dz);
        
        this.rotYaw = (float)(Math.atan2(dx, dz) * 180.0 / Math.PI);
        this.rotPitch = (float)(Math.atan2(dy, horizontalDist) * 180.0 / Math.PI);
    }
    
    @Override
    public void tick() {
        // Store previous positions
        this.xo = x;
        this.yo = y;
        this.zo = z;
        
        this.prevTargetX = targetX;
        this.prevTargetY = targetY;
        this.prevTargetZ = targetZ;
        
        this.prevYaw = rotYaw;
        this.prevPitch = rotPitch;
        
        // Recalculate geometry
        calculateBeamGeometry();
        
        // Impact countdown
        if (impact > 0) {
            impact--;
        }
        
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }
    
    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        // Beams are rendered as a chain of camera-facing billboard quads along the
        // source->target axis (26.2 render-state particle model).
        float size = 1.0f;
        if (pulse) {
            size = Math.min(age / 4.0f, 1.0f);
            size = prevSize + (size - prevSize) * partialTicks;
        }

        float opacity = 0.4f;
        if (pulse && lifetime - age <= 4) {
            opacity = 0.4f - (4 - (lifetime - age)) * 0.1f;
        }

        // Interpolated source and target positions (world space)
        float sx = (float) Mth.lerp(partialTicks, xo, x);
        float sy = (float) Mth.lerp(partialTicks, yo, y);
        float sz = (float) Mth.lerp(partialTicks, zo, z);
        float tx = (float) Mth.lerp(partialTicks, prevTargetX, targetX);
        float ty = (float) Mth.lerp(partialTicks, prevTargetY, targetY);
        float tz = (float) Mth.lerp(partialTicks, prevTargetZ, targetZ);

        Vec3 camPos = camera.position();
        float cxp = (float) camPos.x();
        float cyp = (float) camPos.y();
        float czp = (float) camPos.z();

        int color = ARGB.colorFromFloat(opacity, rCol, gCol, bCol);
        int light = 0xF000F0; // Full brightness
        Quaternionf rot = camera.rotation();

        float beamWidth = 0.15f * size;
        float beamWidthEnd = beamWidth * endMod;
        float beamLength = this.length * size;

        // Draw the beam as overlapping billboard quads along the axis
        int segments = Math.max(2, (int) (beamLength * 4.0f));
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float px = sx + (tx - sx) * t - cxp;
            float py = sy + (ty - sy) * t - cyp;
            float pz = sz + (tz - sz) * t - czp;
            float w = beamWidth + (beamWidthEnd - beamWidth) * t;
            state.add(getLayer(), px, py, pz, rot.x, rot.y, rot.z, rot.w, w,
                    0.0f, 1.0f, 0.0f, 1.0f, color, light);
        }

        // Source glow sprite at the beam origin
        float glowSize = 0.33f * size;
        int glowColor = ARGB.colorFromFloat(Math.min(1.0f, opacity * 2.0f), rCol, gCol, bCol);
        state.add(getLayer(), sx - cxp, sy - cyp, sz - czp, rot.x, rot.y, rot.z, rot.w, glowSize,
                0.0f, 1.0f, 0.0f, 1.0f, glowColor, light);

        // Impact flash at the target point
        if (impact > 0) {
            float impactSize = endMod / 2.0f / Math.max(1.0f, 6 - impact);
            state.add(getLayer(), tx - cxp, ty - cyp, tz - czp, rot.x, rot.y, rot.z, rot.w, impactSize,
                    0.0f, 1.0f, 0.0f, 1.0f, glowColor, light);
        }

        prevSize = size;
    }

    // ==================== Configuration Methods ====================
    
    public void setRGB(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }
    
    public void setType(int type) {
        this.beamType = type;
    }
    
    public void setEndMod(float endMod) {
        this.endMod = endMod;
    }
    
    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }
    
    public void setPulse(boolean pulse) {
        this.pulse = pulse;
    }
    
    public void setRotationSpeed(int rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }
    
    public float getLength() {
        return length;
    }
}
