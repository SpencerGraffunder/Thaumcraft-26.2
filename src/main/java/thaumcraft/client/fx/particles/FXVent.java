package thaumcraft.client.fx.particles;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.awt.Color;

/**
 * FXVent - Smoke/steam particle for essentia vents and similar effects.
 * Grows in size then fades out, with slight upward drift.
 */
@OnlyIn(Dist.CLIENT)
public class FXVent extends ThaumcraftParticle {

    protected float targetScale;
    protected static final int GRID_SIZE = 64;

    public FXVent(ClientLevel level, double x, double y, double z, 
                  double vx, double vy, double vz, int color) {
        super(level, x, y, z, vx, vy, vz);

        this.targetScale = 1.0f;
        this.setSize(0.02f, 0.02f);
        this.quadSize = this.random.nextFloat() * 0.1f + 0.05f;

        this.xd = vx;
        this.yd = vy;
        this.zd = vz;

        // Extract color from int
        Color c = new Color(color);
        this.rCol = c.getRed() / 255.0f;
        this.gCol = c.getGreen() / 255.0f;
        this.bCol = c.getBlue() / 255.0f;

        // Set heading with some randomness
        setHeading(vx, vy, vz, 0.125f, 5.0f);

        // Check distance for visibility culling
        Entity viewEntity = Minecraft.getInstance().getCameraEntity();
        int visibleDistance = Minecraft.getInstance().options.graphicsPreset().get() != GraphicsPreset.FAST ? 50 : 25;
        if (viewEntity != null && viewEntity.distanceToSqr(x, y, z) > visibleDistance * visibleDistance) {
            this.lifetime = 0;
        }

        this.alpha = 1.0f;
    }

    /**
     * Set heading direction with spread
     */
    public void setHeading(double vx, double vy, double vz, float speed, float spread) {
        double length = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (length > 0) {
            vx /= length;
            vy /= length;
            vz /= length;
        }

        // Add randomness to direction
        vx += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075 * spread;
        vy += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075 * spread;
        vz += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075 * spread;

        this.xd = vx * speed;
        this.yd = vy * speed;
        this.zd = vz * speed;
    }

    public void setScale(float scale) {
        this.quadSize *= scale;
        this.targetScale *= scale;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.age++;

        // Die when reached target scale
        if (this.quadSize >= this.targetScale) {
            this.remove();
            return;
        }

        // Slight upward drift
        this.yd += 0.0025;

        // Move
        this.move(this.xd, this.yd, this.zd);

        // Friction
        this.xd *= 0.85;
        this.yd *= 0.85;
        this.zd *= 0.85;

        // Grow
        if (this.quadSize < this.targetScale) {
            this.quadSize *= 1.15f;
        }
        if (this.quadSize > this.targetScale) {
            this.quadSize = this.targetScale;
        }

        // Ground friction
        if (this.onGround) {
            this.xd *= 0.7;
            this.zd *= 0.7;
        }
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.position();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());

        float size = 0.3f * this.quadSize;

        // Alpha fades as it grows
        float displayAlpha = this.alpha * ((this.targetScale - this.quadSize) / this.targetScale);

        int color = ARGB.colorFromFloat(displayAlpha, this.rCol, this.gCol, this.bCol);
        int light = this.getLightCoords(partialTicks);

        Quaternionf rot = camera.rotation();

        state.add(getLayer(), x, y, z, rot.x, rot.y, rot.z, rot.w, size,
                0.0f, 1.0f, 0.0f, 1.0f, color, light);
    }

    // ==================== Configuration Methods ====================

    public void setRGB(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }
}
