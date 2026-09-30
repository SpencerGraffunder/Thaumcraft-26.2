package thaumcraft.client.fx.particles;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

/**
 * FXGeneric - The most commonly used particle type in Thaumcraft.
 * Supports sprite animation, scale/alpha keyframes, color interpolation,
 * rotation, wind effects, and custom physics.
 */
@OnlyIn(Dist.CLIENT)
public class FXGeneric extends ThaumcraftParticle {

    // Animation state
    protected boolean doneFrames = false;
    protected boolean flipped = false;

    // Wind
    protected double windX = 0;
    protected double windZ = 0;

    // Render layer (0 = translucent, 1 = lit/additive)
    protected int layer = 0;

    // Color interpolation
    protected float startR, startG, startB;
    protected float endR, endG, endB;

    // Sprite animation
    protected boolean loop = false;
    protected int startParticle = 0;
    protected int numParticles = 1;
    protected int particleInc = 1;
    protected int[] finalFrames = null;

    // Scale keyframes
    protected float[] scaleKeys = new float[]{1.0f};
    protected float[] scaleFrames = new float[]{0.0f};

    // Alpha keyframes
    protected float[] alphaKeys = new float[]{1.0f};
    protected float[] alphaFrames = new float[]{0.0f};

    // Physics
    protected double slowDown = 0.98;
    protected float randomX = 0, randomY = 0, randomZ = 0;

    // Rotation
    protected float rotationSpeed = 0.0f;

    // Angle mode (for directional particles)
    protected boolean angled = false;
    protected float angleYaw = 0;
    protected float anglePitch = 0;

    // Grid size for sprite sheet (64x64 default)
    protected int gridSize = 64;

    // Sprite index tracking
    protected int spriteIndexX = 0;
    protected int spriteIndexY = 0;

    // SpriteSet for vanilla sprite provider integration
    protected SpriteSet sprites;

    public FXGeneric(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        init();
    }

    public FXGeneric(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        init();
    }

    private void init() {
        this.setSize(0.1f, 0.1f);
        this.startR = this.rCol;
        this.startG = this.gCol;
        this.startB = this.bCol;
        this.endR = this.rCol;
        this.endG = this.gCol;
        this.endB = this.bCol;
    }

    /**
     * Pre-calculate all frame values for smooth interpolation
     */
    protected void calculateFrames() {
        doneFrames = true;

        // Calculate alpha frames
        if (alphaKeys == null) {
            alphaKeys = new float[]{1.0f};
        }
        alphaFrames = new float[this.lifetime + 1];
        float inc = (alphaKeys.length - 1) / (float) this.lifetime;
        float is = 0.0f;
        for (int a = 0; a <= this.lifetime; ++a) {
            int isF = Mth.floor(is);
            float diff = (isF < alphaKeys.length - 1) ? (alphaKeys[isF + 1] - alphaKeys[isF]) : 0.0f;
            float pa = is - isF;
            alphaFrames[a] = alphaKeys[isF] + diff * pa;
            is += inc;
        }

        // Calculate scale frames
        if (scaleKeys == null) {
            scaleKeys = new float[]{1.0f};
        }
        scaleFrames = new float[this.lifetime + 1];
        inc = (scaleKeys.length - 1) / (float) this.lifetime;
        is = 0.0f;
        for (int a = 0; a <= this.lifetime; ++a) {
            int isF = Mth.floor(is);
            float diff = (isF < scaleKeys.length - 1) ? (scaleKeys[isF + 1] - scaleKeys[isF]) : 0.0f;
            float pa = is - isF;
            scaleFrames[a] = scaleKeys[isF] + diff * pa;
            is += inc;
        }
    }

    @Override
    public void tick() {
        if (!doneFrames) {
            calculateFrames();
        }

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // Update rotation (1.12: particleAngle += 2π * rotationSpeed)
        this.oRoll = this.roll;
        this.roll += this.rotationSpeed * 6.2831855f;

        // Apply gravity
        this.yd -= 0.04 * this.gravity;

        // Move particle
        this.move(this.xd, this.yd, this.zd);

        // Apply slowdown/friction
        this.xd *= this.slowDown;
        this.yd *= this.slowDown;
        this.zd *= this.slowDown;

        // Apply random movement
        if (randomX != 0 || randomY != 0 || randomZ != 0) {
            this.xd += this.random.nextGaussian() * randomX;
            this.yd += this.random.nextGaussian() * randomY;
            this.zd += this.random.nextGaussian() * randomZ;
        }

        // Apply wind
        this.xd += this.windX;
        this.zd += this.windZ;

        // Ground friction
        if (this.onGround && slowDown != 1.0) {
            this.xd *= 0.7;
            this.zd *= 0.7;
        }

        // Update alpha from keyframes
        if (alphaFrames != null && alphaFrames.length > 0) {
            this.alpha = alphaFrames[Math.min(this.age, alphaFrames.length - 1)];
        }

        // Update scale from keyframes
        if (scaleFrames != null && scaleFrames.length > 0) {
            this.quadSize = scaleFrames[Math.min(this.age, scaleFrames.length - 1)];
        }

        // Update color interpolation
        float progress = (float) this.age / (float) this.lifetime;
        this.rCol = Mth.lerp(progress, this.startR, this.endR);
        this.gCol = Mth.lerp(progress, this.startG, this.endG);
        this.bCol = Mth.lerp(progress, this.startB, this.endB);

        // Update sprite animation
        updateSpriteIndex();
    }

    /**
     * Update sprite index based on animation settings
     */
    protected void updateSpriteIndex() {
        int index;
        if (loop) {
            index = startParticle + (this.age / particleInc) % numParticles;
        } else {
            float fs = this.age / (float) this.lifetime;
            index = (int) (startParticle + Math.min(numParticles * fs, numParticles - 1));
        }

        // Handle final frames (death animation)
        if (finalFrames != null && finalFrames.length > 0 && this.age > this.lifetime - finalFrames.length) {
            int frame = this.lifetime - this.age;
            if (frame < 0) frame = 0;
            if (frame < finalFrames.length) {
                index = finalFrames[frame];
            }
        }

        setParticleTextureIndex(index);
    }

    /**
     * Set sprite index from a linear index in the grid
     */
    public void setParticleTextureIndex(int index) {
        if (index < 0) index = 0;
        this.spriteIndexX = index % gridSize;
        this.spriteIndexY = index / gridSize;
    }

    @Override
    public Layer getLayer() {
        // Raw grid-fraction UVs sample Thaumcraft's 1024x1024 particles.png atlas.
        // 1.12 layer 0 (normal) and 1 (additive) both map to translucent blending:
        // 26.3 has no additive particle pipeline and OPAQUE would render
        // semi-transparent smoke as solid.
        return TC_PARTICLES_LAYER_TRANSLUCENT;
    }

    @Override
    protected float getU0() {
        float u0 = (float) spriteIndexX / (float) gridSize;      // left edge of sprite
        float u1 = ((float) spriteIndexX + 1.0f) / (float) gridSize; // right edge of sprite
        // 1.12 default (flipped=false) renders X-mirrored: the -x corner gets the RIGHT
        // edge (tx2) and the +x corner the LEFT edge (tx1). 26.3's renderRotatedQuad maps
        // -x -> getU0() and +x -> getU1(), so the compensation is: unflipped returns the
        // mirrored pair; setFlipped(true) restores the vanilla (unmirrored) orientation.
        return flipped ? u0 : u1;
    }

    @Override
    protected float getU1() {
        float u0 = (float) spriteIndexX / (float) gridSize;
        float u1 = ((float) spriteIndexX + 1.0f) / (float) gridSize;
        return flipped ? u1 : u0;
    }

    /**
     * 1.12-faithful angled rendering: when {@link #setAngles} has been called, the quad is
     * rendered with a fixed world-space orientation (yaw/pitch) plus spin instead of the
     * camera-facing billboard. 1.12 did this with GL fixed-function transforms
     * (glRotatef(-yaw+90, Y) then glRotatef(pitch+90, X) then glRotated(roll, Z)); the 26.3
     * render-state pipeline stores a per-particle quaternion, so we build the equivalent
     * quaternion here and feed the quad through the same {@code state.add} path.
     */
    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
        if (!angled) {
            super.extract(state, camera, partialTick);
            return;
        }
        // joml 1.10.x: rotateX/Y/Z take radians and post-multiply (same order as GL's matrix stack)
        // 1.12 applied these rotations in VIEW space (modelview = camera view matrix).
        // The 26.3 render state rotates in WORLD space, so start from the camera's
        // camera->world rotation and post-multiply: q_world = R_cam * Ry * Rx * Rz.
        Quaternionf q = new Quaternionf(camera.rotation());
        q.rotateY((float) Math.toRadians(-angleYaw + 90.0f));
        q.rotateX((float) Math.toRadians(anglePitch + 90.0f));
        float rollAngle = Mth.lerp(partialTick, oRoll, roll);
        if (rollAngle != 0.0f) {
            q.rotateZ(rollAngle);
        }
        Vec3 cam = camera.position();
        float px = (float) (Mth.lerp(partialTick, xo, x) - cam.x());
        float py = (float) (Mth.lerp(partialTick, yo, y) - cam.y());
        float pz = (float) (Mth.lerp(partialTick, zo, z) - cam.z());
        state.add(getLayer(), px, py, pz, q.x(), q.y(), q.z(), q.w(),
                getQuadSize(partialTick), getU0(), getU1(), getV0(), getV1(),
                ARGB.colorFromFloat(alpha, rCol, gCol, bCol), getLightCoords(partialTick));
    }

    @Override
    protected float getV0() {
        return (float) spriteIndexY / (float) gridSize;
    }

    @Override
    protected float getV1() {
        return ((float) spriteIndexY + 1.0f) / (float) gridSize;
    }



    // ==================== Configuration Methods ====================
    // Note: Using void returns to avoid conflicts with parent class methods

    @Override
    public void setColor(float r, float g, float b) {
        // Handle colors > 1 as 0-255 range
        if (r > 1.0f) r /= 255.0f;
        if (g > 1.0f) g /= 255.0f;
        if (b > 1.0f) b /= 255.0f;

        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.startR = r;
        this.startG = g;
        this.startB = b;
        this.endR = r;
        this.endG = g;
        this.endB = b;
    }

    public void setColorRange(float r1, float g1, float b1, float r2, float g2, float b2) {
        // Handle colors > 1 as 0-255 range
        if (r1 > 1.0f) r1 /= 255.0f;
        if (g1 > 1.0f) g1 /= 255.0f;
        if (b1 > 1.0f) b1 /= 255.0f;
        if (r2 > 1.0f) r2 /= 255.0f;
        if (g2 > 1.0f) g2 /= 255.0f;
        if (b2 > 1.0f) b2 /= 255.0f;

        this.rCol = r1;
        this.gCol = g1;
        this.bCol = b1;
        this.startR = r1;
        this.startG = g1;
        this.startB = b1;
        this.endR = r2;
        this.endG = g2;
        this.endB = b2;
    }

    public void setAlphaF(float alpha) {
        this.alpha = alpha;
        this.alphaKeys = new float[]{alpha};
    }

    public void setAlphaKeyframes(float... alphaKeyframes) {
        this.alpha = alphaKeyframes[0];
        this.alphaKeys = alphaKeyframes;
    }

    public void setScale(float scale) {
        this.quadSize = scale;
        this.scaleKeys = new float[]{scale};
    }

    public void setScaleKeyframes(float... scaleKeyframes) {
        this.quadSize = scaleKeyframes[0];
        this.scaleKeys = scaleKeyframes;
    }

    public void setMaxAge(int maxAge) {
        this.lifetime = maxAge;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public void setParticles(int startParticle, int numParticles, int particleInc) {
        this.startParticle = startParticle;
        this.numParticles = numParticles;
        this.particleInc = particleInc;
        setParticleTextureIndex(startParticle);
    }

    public void setParticle(int particleIndex) {
        this.startParticle = particleIndex;
        this.numParticles = 1;
        this.particleInc = 1;
        setParticleTextureIndex(particleIndex);
    }

    public void setRotationSpeed(float speed) {
        this.rotationSpeed = (float) (speed * 0.017453292519943); // Convert degrees to radians
    }

    public void setRotationSpeedWithStart(float startAngle, float speed) {
        this.roll = (float) (startAngle * Math.PI * 2.0);
        this.rotationSpeed = (float) (speed * 0.017453292519943);
    }

    public void setSlowDown(double slowDown) {
        this.slowDown = slowDown;
    }

    public void setRandomMovementScale(float x, float y, float z) {
        this.randomX = x;
        this.randomY = y;
        this.randomZ = z;
    }

    /**
     * 1.12-faithful {@code setWind(d)}: wind vector has magnitude 0.1 (per-tick displacement
     * {@code 0.1 * d}) and its direction rotates with the moon phase.
     */
    public void setWind(double d) {
        int m = (int) ((this.level.getOverworldClockTime() / 24000L) % 8);
        double angle = m * (40 + this.random.nextInt(10)) / 180.0f * Math.PI;
        this.windX = 0.1 * Math.cos(angle) * d;
        this.windZ = -0.1 * Math.sin(angle) * d;
    }

    public void setWind(double windX, double windZ) {
        this.windX = windX;
        this.windZ = windZ;
    }

    public void setFinalFrames(int... frames) {
        this.finalFrames = frames;
    }

    public void setAngles(float yaw, float pitch) {
        this.angleYaw = yaw;
        this.anglePitch = pitch;
        this.angled = true;
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
    }

    public void setGridSize(int size) {
        this.gridSize = size;
    }

    public void setNoClip(boolean noClip) {
        this.hasPhysics = !noClip;
    }

    public void setSprites(SpriteSet sprites) {
        this.sprites = sprites;
    }

    public boolean isFlipped() {
        return flipped;
    }
}
