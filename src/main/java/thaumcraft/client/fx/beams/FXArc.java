package thaumcraft.client.fx.beams;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import thaumcraft.client.fx.particles.ThaumcraftParticle;

import java.util.ArrayList;
import java.util.List;

/**
 * FXArc - Lightning arc effect between two points.
 * Creates a jagged beam with spark particles along its path.
 * Used for shock focus, lightning effects, and electrical discharges.
 */
@OnlyIn(Dist.CLIENT)
public class FXArc extends ThaumcraftParticle {

    protected static final Identifier BEAM_TEXTURE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/beamh.png");

    protected List<Vec3> points = new ArrayList<>();
    protected double targetX;
    protected double targetY;
    protected double targetZ;
    protected float length;

    public FXArc(ClientLevel level, double x, double y, double z,
                 double targetX, double targetY, double targetZ,
                 float r, float g, float b, double heightGravity) {
        super(level, x, y, z, 0, 0, 0);

        this.rCol = r;
        this.gCol = g;
        this.bCol = b;

        this.setSize(0.02f, 0.02f);
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;

        // Store relative target position
        this.targetX = targetX - x;
        this.targetY = targetY - y;
        this.targetZ = targetZ - z;

        this.lifetime = 3;

        // Calculate arc path
        calculateArcPoints(heightGravity);
    }

    /**
     * Calculate the points along the arc path with noise for jagged effect
     */
    protected void calculateArcPoints(double heightGravity) {
        Vec3 start = Vec3.ZERO;
        Vec3 end = new Vec3(this.targetX, this.targetY, this.targetZ);

        this.length = (float) end.length();

        // Calculate velocity needed to reach target with gravity
        double gravity = 0.115;
        double noise = 0.25;

        Vec3 velocity = calculateVelocity(start, end, heightGravity, gravity);
        double stepLengthSq = velocity.lengthSqr();

        Vec3 current = start;
        points.add(start);

        // Generate points along the arc
        for (int i = 0; i < 50 && current.distanceToSqr(end) > stepLengthSq; i++) {
            Vec3 next = current.add(velocity);
            current = next;

            // Add noise for jagged appearance
            Vec3 noisyPoint = next.add(
                    (this.random.nextDouble() - this.random.nextDouble()) * noise,
                    (this.random.nextDouble() - this.random.nextDouble()) * noise,
                    (this.random.nextDouble() - this.random.nextDouble()) * noise
            );
            points.add(noisyPoint);

            // Apply gravity to velocity
            velocity = velocity.subtract(0, gravity / 1.9, 0);
        }

        points.add(end);
    }

    /**
     * Calculate initial velocity to reach target with given gravity
     */
    protected Vec3 calculateVelocity(Vec3 start, Vec3 end, double height, double gravity) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;

        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // Time to reach target
        double time = horizontalDist / 0.5;
        if (time < 1) time = 1;

        // Initial velocity
        double vx = dx / time;
        double vz = dz / time;
        double vy = (dy + 0.5 * gravity * time * time) / time + height;

        return new Vec3(vx, vy, vz);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        // Arc uses custom rendering with its own texture.
        // Render as a chain of camera-facing billboard quads along the arc points.
        if (points.size() < 2) return;

        Vec3 cameraPos = camera.position();
        double px = Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x();
        double py = Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y();
        double pz = Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z();

        float alpha = 1.0f - (this.age + partialTicks) / this.lifetime;
        float size = 0.125f;

        int light = 0xF000F0; // Full brightness
        int color = ARGB.colorFromFloat(alpha, this.rCol, this.gCol, this.bCol);
        Quaternionf rot = camera.rotation();

        // Render line segments between points
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 p1 = points.get(i);
            Vec3 p2 = points.get(i + 1);

            // Midpoint for the quad
            float mx = (float) (px + (p1.x + p2.x) / 2);
            float my = (float) (py + (p1.y + p2.y) / 2);
            float mz = (float) (pz + (p1.z + p2.z) / 2);

            state.add(getLayer(), mx, my, mz, rot.x, rot.y, rot.z, rot.w, size,
                    0.0f, 1.0f, 0.0f, 1.0f, color, light);
        }
    }

    @Override
    public int getLightCoords(float partialTick) {
        return 0xF000F0; // Full brightness for lightning
    }

    // ==================== Configuration Methods ====================

    public void setRGB(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    public List<Vec3> getPoints() {
        return points;
    }

    public float getLength() {
        return length;
    }
}
