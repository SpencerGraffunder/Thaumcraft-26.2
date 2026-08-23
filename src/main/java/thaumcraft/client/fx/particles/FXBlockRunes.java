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
 * FXBlockRunes - Magical rune particles that appear on blocks.
 * Used for ward effects, magical barriers, and enchantment visuals.
 */
@OnlyIn(Dist.CLIENT)
public class FXBlockRunes extends ThaumcraftParticle {

    protected double offsetX;
    protected double offsetY;
    protected float rotation;
    protected int runeIndex;

    // Sprite tracking (64x64 grid)
    protected static final int GRID_SIZE = 64;

    public FXBlockRunes(ClientLevel level, double x, double y, double z, 
                        float r, float g, float b, int duration) {
        super(level, x, y, z, 0, 0, 0);

        this.offsetX = 0.0;
        this.offsetY = 0.0;
        this.rotation = 0.0f;
        this.runeIndex = 0;

        // Ensure non-zero color
        if (r == 0.0f) r = 1.0f;

        // Random 90-degree rotation
        this.rotation = this.random.nextInt(4) * 90.0f;

        this.rCol = r;
        this.gCol = g;
        this.bCol = b;

        this.gravity = 0.0f;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;

        this.lifetime = 3 * duration;
        this.setSize(0.01f, 0.01f);

        // Random rune index (224-240 range in sprite sheet)
        this.runeIndex = (int) (this.random.nextFloat() * 16.0 + 224.0);

        // Random offsets for position variation
        this.offsetX = this.random.nextFloat() * 0.2;
        this.offsetY = -0.3 + this.random.nextFloat() * 0.6;

        this.quadSize = (float) (1.0 + this.random.nextGaussian() * 0.1);
        this.alpha = 0.0f;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        // Calculate alpha based on age
        float threshold = this.lifetime / 5.0f;
        if (this.age <= threshold) {
            this.alpha = this.age / threshold;
        } else {
            this.alpha = (this.lifetime - this.age) / (float) this.lifetime;
        }

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // Apply gravity and movement
        this.yd -= 0.04 * this.gravity;
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.position();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());

        // Rune orientation: rotated around Y, then tilted flat (same as old render)
        Quaternionf quaternion = new Quaternionf();
        quaternion.rotateY((float) Math.toRadians(rotation));
        quaternion.rotateZ((float) Math.toRadians(90.0f));

        // Old quad spanned +/-0.5*size; new model scale is the half-extent
        float size = 0.3f * this.quadSize;
        float displayAlpha = this.alpha / 2.0f;

        int color = ARGB.colorFromFloat(displayAlpha, this.rCol, this.gCol, this.bCol);
        int light = 0xF000F0; // Full brightness for runes

        float offsetXf = (float) this.offsetX;
        float offsetYf = (float) this.offsetY;

        state.add(getLayer(), x + offsetXf, y + offsetYf, z - 0.51f,
                quaternion.x, quaternion.y, quaternion.z, quaternion.w,
                size / 2.0f, 0.0f, 1.0f, 0.0f, 1.0f, color, light);
    }

    @Override
    public int getLightCoords(float partialTick) {
        return 0xF000F0; // Full brightness
    }

    // ==================== Configuration Methods ====================

    public void setScale(float scale) {
        this.quadSize = scale;
    }

    public void setOffsetX(double offset) {
        this.offsetX = offset;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }
}
