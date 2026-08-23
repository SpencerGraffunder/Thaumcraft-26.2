package thaumcraft.client.fx.beams;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.ARGB;
import org.joml.Quaternionf;
import thaumcraft.client.fx.particles.ThaumcraftParticle;

/**
 * FXBeamWand - Continuous beam effect from a living entity to a target point.
 * Used for wand/gauntlet casting effects, creating a smooth beam that follows
 * the caster's position and points toward the target.
 * 
 * Features:
 * - Entity-attached source (follows caster)
 * - Multiple beam texture options
 * - Scrolling UV animation
 * - Pulse fade in/out effect
 * - Impact flash at target point
 * - 3 rotated quads for cylindrical appearance
 * 
 * Ported from 1.12.2
 */
@OnlyIn(Dist.CLIENT)
public class FXBeamWand extends ThaumcraftParticle {
    
    private static final Identifier BEAM_TEXTURE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/beam.png");
    private static final Identifier BEAM1_TEXTURE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/beam1.png");
    private static final Identifier BEAM2_TEXTURE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/beam2.png");
    private static final Identifier BEAM3_TEXTURE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/beam3.png");
    private static final Identifier PARTICLE_TEXTURE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    
    // Source entity
    protected LivingEntity sourceEntity;
    protected double offset;
    
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
     * Create a beam from a living entity to a target point.
     * 
     * @param level The client level
     * @param source The source entity (caster)
     * @param tx Target X coordinate
     * @param ty Target Y coordinate
     * @param tz Target Z coordinate
     * @param r Red color component (0-1)
     * @param g Green color component (0-1)
     * @param b Blue color component (0-1)
     * @param maxAge Maximum age in ticks
     */
    public FXBeamWand(ClientLevel level, LivingEntity source, 
                      double tx, double ty, double tz,
                      float r, float g, float b, int maxAge) {
        super(level, source.getX(), source.getY(), source.getZ(), 0, 0, 0);
        
        this.sourceEntity = source;
        this.offset = source.getBbHeight() / 2.0f + 0.25;
        
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
        if (mc.getCameraEntity() != null) {
            int visibleDistance = mc.options.graphicsPreset().get() != GraphicsPreset.FAST ? 50 : 25;
            if (mc.getCameraEntity().distanceTo(source) > visibleDistance) {
                this.lifetime = 0;
            }
        }
    }
    
    /**
     * Update the beam's target position.
     * Call this to extend the beam's life and change the endpoint.
     */
    public void updateBeam(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        
        // Extend lifetime
        while (this.lifetime - this.age < 4) {
            this.lifetime++;
        }
    }
    
    /**
     * Calculate beam length, yaw, and pitch from source to target.
     */
    protected void calculateBeamGeometry() {
        double sx = sourceEntity.getX();
        double sy = sourceEntity.getY() + offset;
        double sz = sourceEntity.getZ();
        
        float dx = (float)(sx - targetX);
        float dy = (float)(sy - targetY);
        float dz = (float)(sz - targetZ);
        
        this.length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        double horizontalDist = Mth.sqrt(dx * dx + dz * dz);
        
        this.rotYaw = (float)(Math.atan2(dx, dz) * 180.0 / Math.PI);
        this.rotPitch = (float)(Math.atan2(dy, horizontalDist) * 180.0 / Math.PI);
    }
    
    @Override
    public void tick() {
        // Store previous positions
        this.xo = sourceEntity.getX();
        this.yo = sourceEntity.getY() + offset;
        this.zo = sourceEntity.getZ();
        
        this.prevTargetX = targetX;
        this.prevTargetY = targetY;
        this.prevTargetZ = targetZ;
        
        this.prevYaw = rotYaw;
        this.prevPitch = rotPitch;
        
        // Recalculate geometry
        calculateBeamGeometry();
        
        // Normalize rotation changes
        while (rotPitch - prevPitch < -180.0f) prevPitch -= 360.0f;
        while (rotPitch - prevPitch >= 180.0f) prevPitch += 360.0f;
        while (rotYaw - prevYaw < -180.0f) prevYaw -= 360.0f;
        while (rotYaw - prevYaw >= 180.0f) prevYaw += 360.0f;
        
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
        // Beam attached to the casting entity: render as billboard quads along the axis.
        float size = 1.0f;
        if (pulse) {
            size = Math.min(age / 4.0f, 1.0f);
            size = prevSize + (size - prevSize) * partialTicks;
        }

        float opacity = 0.4f;
        if (pulse && lifetime - age <= 4) {
            opacity = 0.4f - (4 - (lifetime - age)) * 0.1f;
        }

        // Source position follows the casting entity's hand
        double prevSX = sourceEntity.xo;
        double prevSY = sourceEntity.yo + offset;
        double prevSZ = sourceEntity.zo;
        double currSX = sourceEntity.getX();
        double currSY = sourceEntity.getY() + offset;
        double currSZ = sourceEntity.getZ();

        float yawRad = sourceEntity.getYRot() * ((float) Math.PI / 180.0f);
        float cosYaw = Mth.cos(yawRad);
        float sinYaw = Mth.sin(yawRad);

        prevSX -= cosYaw * 0.066f;
        prevSY -= 0.06;
        prevSZ -= sinYaw * 0.04f;
        currSX -= cosYaw * 0.066f;
        currSY -= 0.06;
        currSZ -= sinYaw * 0.04f;

        Vec3 look = sourceEntity.getLookAngle();
        prevSX += look.x * 0.3;
        prevSY += look.y * 0.3;
        prevSZ += look.z * 0.3;
        currSX += look.x * 0.3;
        currSY += look.y * 0.3;
        currSZ += look.z * 0.3;

        float sx = (float) Mth.lerp(partialTicks, prevSX, currSX);
        float sy = (float) Mth.lerp(partialTicks, prevSY, currSY);
        float sz = (float) Mth.lerp(partialTicks, prevSZ, currSZ);
        float tx = (float) Mth.lerp(partialTicks, prevTargetX, targetX);
        float ty = (float) Mth.lerp(partialTicks, prevTargetY, targetY);
        float tz = (float) Mth.lerp(partialTicks, prevTargetZ, targetZ);

        Vec3 camPos = camera.position();
        float cxp = (float) camPos.x();
        float cyp = (float) camPos.y();
        float czp = (float) camPos.z();

        int color = ARGB.colorFromFloat(opacity, rCol, gCol, bCol);
        int light = 0xF000F0;
        Quaternionf rot = camera.rotation();

        float beamWidth = 0.15f * size;
        float beamWidthEnd = beamWidth * endMod;
        float beamLength = this.length * size;

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

        // Impact flash at the target point
        if (impact > 0) {
            float impactSize = endMod / 2.0f / Math.max(1.0f, 6 - impact);
            state.add(getLayer(), tx - cxp, ty - cyp, tz - czp, rot.x, rot.y, rot.z, rot.w, impactSize,
                    0.0f, 1.0f, 0.0f, 1.0f, color, light);
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
    
    public LivingEntity getSourceEntity() {
        return sourceEntity;
    }
    
    public float getLength() {
        return length;
    }
}
