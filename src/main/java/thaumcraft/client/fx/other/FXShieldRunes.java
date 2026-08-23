package thaumcraft.client.fx.other;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import thaumcraft.client.fx.particles.ThaumcraftParticle;

/**
 * Shield runes particle effect - creates magical runes that orbit around a shielded entity.
 * Used for shield visualization and magical protection effects.
 */
@OnlyIn(Dist.CLIENT)
public class FXShieldRunes extends ThaumcraftParticle {
    
    private final Entity target;
    private float orbitAngle;
    private float orbitHeight;
    private float orbitSpeed;
    private float orbitRadius;
    private int runeIndex;
    
    public FXShieldRunes(ClientLevel level, Entity target, float r, float g, float b, int lifetime) {
        super(level, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ());
        
        this.target = target;
        
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.alpha = 0.8f;
        
        this.orbitAngle = this.random.nextFloat() * 360.0f;
        this.orbitHeight = this.random.nextFloat() * target.getBbHeight();
        this.orbitSpeed = 3.0f + this.random.nextFloat() * 2.0f;
        this.orbitRadius = target.getBbWidth() * 0.8f + 0.3f;
        this.runeIndex = this.random.nextInt(16);
        
        this.quadSize = 0.15f + this.random.nextFloat() * 0.1f;
        this.lifetime = lifetime;
        this.gravity = 0;
        this.noClip = true;
    }
    
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        
        if (this.age++ >= this.lifetime || target == null || !target.isAlive()) {
            this.remove();
            return;
        }
        
        // Update orbit
        this.orbitAngle += this.orbitSpeed;
        
        // Calculate position on orbit
        float rad = (float) Math.toRadians(this.orbitAngle);
        this.x = this.target.getX() + Mth.cos(rad) * this.orbitRadius;
        this.y = this.target.getY() + this.orbitHeight;
        this.z = this.target.getZ() + Mth.sin(rad) * this.orbitRadius;
        
        // Fade in/out
        float progress = (float) this.age / (float) this.lifetime;
        if (progress < 0.1f) {
            this.alpha = progress * 10.0f * 0.8f;
        } else if (progress > 0.9f) {
            this.alpha = (1.0f - progress) * 10.0f * 0.8f;
        } else {
            this.alpha = 0.8f;
        }
    }
    
    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.position();
        float x = (float)(Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float)(Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float)(Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());
        
        Quaternionf quaternion = camera.rotation();
        
        // Texture coords for rune from sprite sheet
        float u0 = this.runeIndex / 64.0f;
        float u1 = u0 + 1.0f / 64.0f;
        float v0 = 6.0f / 64.0f; // Rune row
        float v1 = v0 + 1.0f / 64.0f;
        
        int color = ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol);
        int light = 0xF000F0; // Full bright for magic particles
        
        state.add(getLayer(), x, y, z, quaternion.x, quaternion.y, quaternion.z, quaternion.w,
                this.quadSize, u0, u1, v0, v1, color, light);
    }
}
