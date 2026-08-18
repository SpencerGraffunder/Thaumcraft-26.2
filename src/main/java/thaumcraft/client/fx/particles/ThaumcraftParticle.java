package thaumcraft.client.fx.particles;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Base class for custom Thaumcraft particles.
 * Ported to MC 26.2 (SingleQuadParticle + render-state model).
 * Supports color interpolation, scale animation, rotation, and custom rendering.
 */
@OnlyIn(Dist.CLIENT)
public class ThaumcraftParticle extends SingleQuadParticle {

    // Color interpolation
    protected float startR, startG, startB;
    protected float endR, endG, endB;

    // Scale animation
    protected float startScale;
    protected float endScale;

    // Rotation
    protected float rotationSpeed;

    // Physics
    protected double slowDown = 0.98;
    protected float windX, windZ;
    protected boolean noClip = false;

    // Sprite animation
    protected int spriteStart = 0;
    protected int spriteCount = 1;
    protected int spriteIncrement = 1;
    protected boolean spriteLoop = false;
    protected int gridSize = 64;

    // Render layer (0 = normal, 1 = additive) -> maps to TRANSLUCENT/OPAQUE
    protected int layer = 0;

    // Random source (Particle base no longer exposes one)
    protected final RandomSource random = RandomSource.create();

    public ThaumcraftParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, defaultSprite());
        this.startR = this.rCol;
        this.startG = this.gCol;
        this.startB = this.bCol;
        this.endR = this.rCol;
        this.endG = this.gCol;
        this.endB = this.bCol;
        this.startScale = this.quadSize;
        this.endScale = this.quadSize;
    }

    public ThaumcraftParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz, defaultSprite());
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.startR = this.rCol;
        this.startG = this.gCol;
        this.startB = this.bCol;
        this.endR = this.rCol;
        this.endG = this.gCol;
        this.endB = this.bCol;
        this.startScale = this.quadSize;
        this.endScale = this.quadSize;
    }

    /** Fetch a default particle sprite from the particle atlas (safe before atlas load). */
    protected static TextureAtlasSprite defaultSprite() {
        try {
            return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).missingSprite();
        } catch (Exception e) {
            return null;
        }
    }

    protected void setSpriteFromIdentifier(Identifier id) {
        TextureAtlasSprite spr = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(id);
        if (spr != null) this.setSprite(spr);
    }

    protected void pickSprite(SpriteSet sprites) {
        this.setSprite(sprites.get(this.getRandom()));
    }

    @Override
    public Layer getLayer() {
        return layer == 0 ? Layer.TRANSLUCENT : Layer.OPAQUE;
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

        // Update rotation
        this.oRoll = this.roll;
        this.roll += this.rotationSpeed;

        // Apply gravity
        this.yd -= 0.04 * this.gravity;

        // Move
        if (!noClip) {
            this.move(this.xd, this.yd, this.zd);
        } else {
            this.x += this.xd;
            this.y += this.yd;
            this.z += this.zd;
        }

        // Apply friction/slowdown
        this.xd *= this.slowDown;
        this.yd *= this.slowDown;
        this.zd *= this.slowDown;

        // Apply wind
        this.xd += this.windX;
        this.zd += this.windZ;

        // Ground friction
        if (this.onGround && slowDown != 1.0) {
            this.xd *= 0.7;
            this.zd *= 0.7;
        }

        // Update color interpolation
        float progress = (float) this.age / (float) this.lifetime;
        this.rCol = Mth.lerp(progress, this.startR, this.endR);
        this.gCol = Mth.lerp(progress, this.startG, this.endG);
        this.bCol = Mth.lerp(progress, this.startB, this.endB);

        // Update scale interpolation
        this.quadSize = Mth.lerp(progress, this.startScale, this.endScale);
    }

    // ==================== Configuration Methods ====================

    public ThaumcraftParticle setTCColor(float r, float g, float b) {
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

    public ThaumcraftParticle setColor(float r1, float g1, float b1, float r2, float g2, float b2) {
        this.rCol = r1;
        this.gCol = g1;
        this.bCol = b1;
        this.startR = r1;
        this.startG = g1;
        this.startB = b1;
        this.endR = r2;
        this.endG = g2;
        this.endB = b2;
        return this;
    }

    public ThaumcraftParticle setTCAlpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public ThaumcraftParticle setTCAlphaRange(float startAlpha, float endAlpha) {
        this.alpha = startAlpha;
        return this;
    }

    public ThaumcraftParticle setTCScale(float scale) {
        this.quadSize = scale;
        this.startScale = scale;
        this.endScale = scale;
        return this;
    }

    public ThaumcraftParticle setTCScaleRange(float startScale, float endScale) {
        this.quadSize = startScale;
        this.startScale = startScale;
        this.endScale = endScale;
        return this;
    }

    public ThaumcraftParticle setTCLifetime(int lifetime) {
        this.lifetime = lifetime;
        return this;
    }

    public ThaumcraftParticle setTCGravity(float gravity) {
        this.gravity = gravity;
        return this;
    }

    public ThaumcraftParticle setTCRotationSpeed(float speed) {
        this.rotationSpeed = speed;
        return this;
    }

    public ThaumcraftParticle setTCSlowDown(double slowDown) {
        this.slowDown = slowDown;
        return this;
    }

    public ThaumcraftParticle setTCNoClip(boolean noClip) {
        this.noClip = noClip;
        return this;
    }

    public ThaumcraftParticle setTCLayer(int layer) {
        this.layer = layer;
        return this;
    }

    public ThaumcraftParticle setTCWind(float windX, float windZ) {
        this.windX = windX;
        this.windZ = windZ;
        return this;
    }
}
