package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import thaumcraft.client.fx.beams.FXArc;
import thaumcraft.client.fx.beams.FXBeamBore;
import thaumcraft.client.fx.beams.FXBeamWand;
import thaumcraft.client.fx.beams.FXBolt;
import thaumcraft.client.fx.other.FXBoreStream;
import thaumcraft.client.fx.other.FXEssentiaStream;
import thaumcraft.client.fx.other.FXShieldRunes;
import thaumcraft.client.fx.other.FXVoidStream;
import thaumcraft.client.fx.particles.FXBlockRunes;
import thaumcraft.client.fx.particles.FXBlockWard;
import thaumcraft.client.fx.particles.FXBoreParticles;
import thaumcraft.client.fx.particles.FXBoreSparkle;
import thaumcraft.client.fx.particles.FXBreakingFade;
import thaumcraft.client.fx.particles.FXFireMote;
import thaumcraft.client.fx.particles.FXGeneric;
import thaumcraft.client.fx.particles.FXGenericGui;
import thaumcraft.client.fx.particles.FXGenericP2E;
import thaumcraft.client.fx.particles.FXGenericP2P;
import thaumcraft.client.fx.particles.FXPlane;
import thaumcraft.client.fx.particles.FXSlimyBubble;
import thaumcraft.client.fx.particles.FXSmokeSpiral;
import thaumcraft.client.fx.particles.FXSwarm;
import thaumcraft.client.fx.particles.FXVent;
import thaumcraft.client.fx.particles.FXVent2;
import thaumcraft.client.fx.particles.FXVisSparkle;
import thaumcraft.client.fx.particles.FXWisp;
import thaumcraft.common.tiles.crafting.TileCrucible;
import thaumcraft.init.ModItems;
import thaumcraft.init.ModSounds;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * FXDispatcher - Central particle effect dispatcher for Thaumcraft.
 *
 * Methods are 1.12-faithful ports of the original Thaumcraft 6 FXDispatcher:
 * they spawn the same custom particle classes (FXGeneric, FXBoreParticles,
 * FXBreakingFade, FXVent, FXPlane, beam/stream classes, ...) with the same
 * sprite indices, colors, lifetimes, and motion as 1.12. A small delay queue
 * ({@link #tickDelayed}) reproduces 1.12's {@code ParticleEngine.addEffectWithDelay}.
 */
@OnlyIn(Dist.CLIENT)
public class FXDispatcher {
    
    public static FXDispatcher INSTANCE = new FXDispatcher();
    
    private final Random rand = new Random();
    
    public Level getWorld() {
        return Minecraft.getInstance().level;
    }

    /**
     * Get the client level, required for spawning custom particles
     */
    private ClientLevel getClientLevel() {
        return Minecraft.getInstance().level;
    }

    /**
     * Add a custom particle to the particle engine
     */
    private void addParticle(net.minecraft.client.particle.Particle particle) {
        if (particle != null) {
            Minecraft.getInstance().particleEngine.add(particle);
        }
    }
    
    // ==================== Delayed particles (1.12 ParticleEngine.addEffectWithDelay) ====================
    
    private static final List<DelayedParticle> DELAYED_PARTICLES = new ArrayList<>();
    
    private record DelayedParticle(int ticks, Particle particle) {}
    
    private void addEffectWithDelay(Particle particle, int delay) {
        if (particle == null) return;
        if (delay <= 0) {
            Minecraft.getInstance().particleEngine.add(particle);
        } else {
            DELAYED_PARTICLES.add(new DelayedParticle(delay, particle));
        }
    }
    
    /**
     * Process the delayed-particle queue. Called every client tick (ClientTickHandler).
     */
    public static void tickDelayed() {
        if (DELAYED_PARTICLES.isEmpty()) return;
        for (int i = DELAYED_PARTICLES.size() - 1; i >= 0; i--) {
            DelayedParticle dp = DELAYED_PARTICLES.get(i);
            if (dp.ticks() <= 1) {
                DELAYED_PARTICLES.remove(i);
                Minecraft.getInstance().particleEngine.add(dp.particle());
            } else {
                DELAYED_PARTICLES.set(i, new DelayedParticle(dp.ticks() - 1, dp.particle()));
            }
        }
    }
    
    // ==================== Fire/Alumentum Effects ====================
    
    public void drawFireMote(float x, float y, float z, float vx, float vy, float vz, 
            float r, float g, float b, float alpha, float scale) {
        // 1.12-faithful: half the time a smaller additive mote, half the time a normal one
        boolean bb = rand.nextBoolean();
        FXFireMote particle = new FXFireMote(getClientLevel(), x, y, z, vx, vy, vz, r, g, b, bb ? (scale / 3.0f) : scale, bb ? 1 : 0);
        particle.setAlpha(alpha);
        addParticle(particle);
    }
    
    public void drawAlumentum(float x, float y, float z, float vx, float vy, float vz, 
            float r, float g, float b, float alpha, float scale) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            // Alumentum uses fire motes but with layer 1 (additive blending)
            FXFireMote particle = new FXFireMote(level, x, y, z, vx, vy, vz, r, g, b, scale, 1);
            particle.setAlpha(alpha);
            addParticle(particle);
        }
    }
    
    // ==================== Taint Effects ====================
    
    /**
     * Draw taint corruption particles (1.12-faithful).
     */
    public void drawTaintParticles(float x, float y, float z, float vx, float vy, float vz, float scale) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        FXGeneric fb = new FXGeneric(level, x, y, z, vx, vy, vz);
        fb.setMaxAge(80 + rand.nextInt(20));
        fb.setColor(0.4f + rand.nextFloat() * 0.2f, 0.1f + rand.nextFloat() * 0.3f, 0.5f + rand.nextFloat() * 0.2f);
        fb.setAlphaKeyframes(0.75f, 0.0f);
        fb.setGridSize(16);
        fb.setParticles(57 + rand.nextInt(3), 1, 1);
        // 1.12 scale is in factor units (1.0 = 0.1 block); the port uses block units
        fb.setScaleKeyframes(scale * 0.1f, scale / 4.0f * 0.1f);
        fb.setLayer(1);
        fb.setSlowDown(0.975);
        fb.setGravity(0.2f);
        fb.setRotationSpeedWithStart(rand.nextFloat(), rand.nextBoolean() ? -1.0f : 1.0f);
        addParticle(fb);
    }
    
    // ==================== Lightning/Spark Effects ====================
    
    public void drawLightningFlash(double x, double y, double z, float r, float g, float b, float alpha, float scale) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        FXGeneric fb = new FXGeneric(level, x, y, z, 0.0, 0.0, 0.0);
        fb.setMaxAge(5 + rand.nextInt(5));
        fb.setGridSize(16);
        fb.setColor(r, g, b);
        fb.setAlphaKeyframes(alpha, 0.0f);
        fb.setParticles(108 + rand.nextInt(4), 1, 1);
        fb.setScale(scale * 0.1f);
        fb.setLayer(0);
        fb.setRotationSpeedWithStart(rand.nextFloat(), 0.0f);
        addParticle(fb);
    }
    
    public void spark(double x, double y, double z, float size, float r, float g, float b, float a) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        FXGeneric fb = new FXGeneric(level, x, y, z, 0.0, 0.0, 0.0);
        fb.setMaxAge(5 + rand.nextInt(5));
        fb.setAlphaF(a);
        fb.setColor(r, g, b);
        fb.setGridSize(16);
        fb.setParticles(8 + rand.nextInt(3) * 16, 8, 1);
        fb.setScale(size * 0.1f);
        fb.setFlipped(rand.nextBoolean());
        addParticle(fb);
    }
    
    public void sparkle(float x, float y, float z, float r, float g, float b) {
        if (rand.nextInt(6) < 4) {
            drawGenericParticles(x, y, z, 0.0, 0.0, 0.0, r, g, b, 0.9f, true, 320, 16, 1, 6 + rand.nextInt(4), 0, 0.6f + rand.nextFloat() * 0.2f, 0.0f, 0);
        }
    }
    
    /**
     * Creates a slimy bubble particle effect (for liquid death / flux goo).
     * Animated bubble that rises, inflates, and pops.
     */
    public void slimyBubbleFX(float x, float y, float z, float scale, float r, float g, float b) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXSlimyBubble bubble = new FXSlimyBubble(level, x, y, z, scale, r, g, b);
            addParticle(bubble);
        }
    }
    
    // ==================== Generic Particle Drawing ====================
    
    /**
     * 1.12-faithful generic particle. Note: {@code scale} is in 1.12 factor units
     * (1.0 = 0.1 block) and is converted to the port's block units internally.
     */
    public void drawGenericParticles(double x, double y, double z, double mx, double my, double mz, 
            float r, float g, float b, float alpha, boolean loop, int start, int num, int inc, 
            int age, int delay, float scale, float rot, int layer) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        FXGeneric particle = new FXGeneric(level, x, y, z, mx, my, mz);
        particle.setColor(r, g, b);
        particle.setAlphaF(alpha);
        particle.setLoop(loop);
        particle.setParticles(start, num, inc);
        particle.setMaxAge(age);
        particle.setScale(scale * 0.1f);
        particle.setRotationSpeed(rot);
        particle.setLayer(layer);
        addEffectWithDelay(particle, delay);
    }
    
    /** Same as {@link #drawGenericParticles} but with a 16x16 sprite grid (1.12). */
    public void drawGenericParticles16(double x, double y, double z, double x2, double y2, double z2, 
            float r, float g, float b, float alpha, boolean loop, int start, int num, int inc, 
            int age, int delay, float scale, float rot, int layer) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        FXGeneric particle = new FXGeneric(level, x, y, z, x2, y2, z2);
        particle.setGridSize(16);
        particle.setColor(r, g, b);
        particle.setAlphaF(alpha);
        particle.setLoop(loop);
        particle.setParticles(start, num, inc);
        particle.setMaxAge(age);
        particle.setScale(scale * 0.1f);
        particle.setRotationSpeed(rot);
        particle.setLayer(layer);
        addEffectWithDelay(particle, delay);
    }
    
    /** 1.12-faithful GenPart overload (color range, alpha/scale keyframes, grid, rotstart, slowDown, gravity, delay). */
    public void drawGenericParticles(double x, double y, double z, double mx, double my, double mz, GenPart part) {
        if (part == null) return;
        ClientLevel level = getClientLevel();
        if (level == null) return;
        FXGeneric particle = new FXGeneric(level, x, y, z, mx, my, mz);
        particle.setMaxAge(part.age);
        particle.setColorRange(part.redStart, part.greenStart, part.blueStart, part.redEnd, part.greenEnd, part.blueEnd);
        particle.setAlphaKeyframes(part.alpha);
        particle.setLoop(part.loop);
        particle.setParticles(part.partStart, part.partNum, part.partInc);
        // 1.12 scale keyframes are in factor units; convert to block units
        float[] scale = new float[part.scale.length];
        for (int i = 0; i < scale.length; i++) scale[i] = part.scale[i] * 0.1f;
        particle.setScaleKeyframes(scale);
        particle.setLayer(part.layer);
        particle.setRotationSpeedWithStart(part.rotstart, part.rot);
        particle.setSlowDown(part.slowDown);
        particle.setGravity(part.grav);
        particle.setGridSize(part.grid);
        addEffectWithDelay(particle, part.delay);
    }
    
    // ==================== Crucible Effects ====================
    
    /**
     * Create a colored bubble in the crucible (1.12-faithful: sprite 64, inflate-then-pop 65/66).
     */
    public void crucibleBubble(float x, float y, float z, float cr, float cg, float cb) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        FXGeneric fb = new FXGeneric(level, x, y, z, 0.0, 0.0, 0.0);
        fb.setMaxAge(15 + rand.nextInt(10));
        fb.setScale(rand.nextFloat() * 0.3f + 0.3f);
        fb.setColor(cr, cg, cb);
        fb.setRandomMovementScale(0.002f, 0.002f, 0.002f);
        fb.setGravity(-0.001f);
        fb.setParticle(64);
        fb.setFinalFrames(65, 66, 66);
        addParticle(fb);
    }
    
    /**
     * Create boiling bubbles in the crucible (1.12-faithful).
     */
    public void crucibleBoil(BlockPos pos, TileCrucible tile, int j) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        for (int a = 0; a < 2; ++a) {
            FXGeneric fb = new FXGeneric(level, pos.getX() + 0.2f + rand.nextFloat() * 0.6f, pos.getY() + 0.1f + tile.getFluidHeight(), pos.getZ() + 0.2f + rand.nextFloat() * 0.6f, 0.0, 0.002, 0.0);
            fb.setMaxAge((int) (7.0 + 8.0 / (Math.random() * 0.8 + 0.2)));
            fb.setScale(rand.nextFloat() * 0.3f + 0.2f);
            if (tile.aspects.size() == 0) {
                fb.setColor(1.0f, 1.0f, 1.0f);
            } else {
                int color = tile.aspects.getAspects()[rand.nextInt(tile.aspects.getAspects().length)].getColor();
                Color c = new Color(color);
                fb.setColor(c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f);
            }
            fb.setRandomMovementScale(0.001f, 0.001f, 0.001f);
            fb.setGravity(-0.025f * j);
            fb.setParticle(64);
            fb.setFinalFrames(65, 66);
            addParticle(fb);
        }
    }
    
    /**
     * Create frothy splash particles on crucible surface (1.12-faithful).
     */
    public void crucibleFroth(float x, float y, float z) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        FXGeneric fb = new FXGeneric(level, x, y, z, 0.0, 0.0, 0.0);
        fb.setMaxAge(4 + rand.nextInt(3));
        fb.setScale(rand.nextFloat() * 0.2f + 0.2f);
        fb.setColor(0.5f, 0.5f, 0.7f);
        fb.setRandomMovementScale(0.001f, 0.001f, 0.001f);
        fb.setGravity(0.1f);
        fb.setParticle(64);
        fb.setFinalFrames(65, 66);
        addParticle(fb);
    }
    
    /**
     * Create dripping particles from crucible overflow (1.12-faithful: sprite 73).
     */
    public void crucibleFrothDown(float x, float y, float z) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        FXGeneric fb = new FXGeneric(level, x, y, z, 0.0, 0.0, 0.0);
        fb.setMaxAge(12 + rand.nextInt(12));
        fb.setScale(rand.nextFloat() * 0.2f + 0.4f);
        fb.setColor(0.25f, 0.0f, 0.75f);
        fb.setAlphaF(0.8f);
        fb.setRandomMovementScale(0.001f, 0.001f, 0.001f);
        fb.setGravity(0.05f);
        fb.setNoClip(false);
        fb.setParticle(73);
        fb.setFinalFrames(65, 66);
        fb.setLayer(1);
        addParticle(fb);
    }
    
    // ==================== Bamf/Teleport Effects ====================
    
    public void drawBamf(BlockPos p, boolean sound, boolean flair, Direction side) {
        drawBamf(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, sound, flair, side);
    }
    
    public void drawBamf(BlockPos p, float r, float g, float b, boolean sound, boolean flair, Direction side) {
        drawBamf(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, r, g, b, sound, flair, side);
    }
    
    public void drawBamf(BlockPos p, int color, boolean sound, boolean flair, Direction side) {
        Color c = new Color(color);
        drawBamf(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 
                c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, sound, flair, side);
    }
    
    public void drawBamf(double x, double y, double z, int color, boolean sound, boolean flair, Direction side) {
        Color c = new Color(color);
        drawBamf(x, y, z, c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, sound, flair, side);
    }
    
    public void drawBamf(double x, double y, double z, boolean sound, boolean flair, Direction side) {
        drawBamf(x, y, z, 0.5f, 0.1f, 0.6f, sound, flair, side);
    }
    
    /**
     * Create a "bamf" teleportation poof effect with custom colors.
     */
    public void drawBamf(double x, double y, double z, float r, float g, float b, boolean sound, boolean flair, Direction side) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        if (sound) {
            level.playLocalSound(x, y, z, ModSounds.POOF.get(), SoundSource.BLOCKS, 
                    0.4f, 1.0f + (float) rand.nextGaussian() * 0.05f, false);
        }
        
        // 1.12-faithful: colored smoke burst (texture 123 = smoke sprite row, 5 sprites)
        for (int a = 0; a < 6 + rand.nextInt(3) + 2; a++) {
            double vx = (0.05 + rand.nextFloat() * 0.05) * (rand.nextBoolean() ? -1.0 : 1.0);
            double vy = (0.05 + rand.nextFloat() * 0.05) * (rand.nextBoolean() ? -1.0 : 1.0);
            double vz = (0.05 + rand.nextFloat() * 0.05) * (rand.nextBoolean() ? -1.0 : 1.0);
            if (side != null) {
                vx += side.getStepX() * 0.1;
                vy += side.getStepY() * 0.1;
                vz += side.getStepZ() * 0.1;
            }
            
            FXGeneric smoke = new FXGeneric(level, x + vx * 2.0, y + vy * 2.0, z + vz * 2.0,
                    vx / 2.0, vy / 2.0, vz / 2.0);
            smoke.setMaxAge(20 + rand.nextInt(15));
            smoke.setColor(
                    Mth.clamp(r * (1.0f + (float) rand.nextGaussian() * 0.1f), 0.0f, 1.0f),
                    Mth.clamp(g * (1.0f + (float) rand.nextGaussian() * 0.1f), 0.0f, 1.0f),
                    Mth.clamp(b * (1.0f + (float) rand.nextGaussian() * 0.1f), 0.0f, 1.0f));
            smoke.setAlphaKeyframes(1.0f, 0.1f);
            smoke.setGridSize(16);
            smoke.setParticles(123, 5, 1);
            // Port quad size is block units = 1.12 scale * 0.1
            smoke.setScaleKeyframes(0.3f, 0.4f + rand.nextFloat() * 0.3f);
            smoke.setLayer(1);
            smoke.setSlowDown(0.7);
            smoke.setRotationSpeedWithStart(rand.nextFloat(), rand.nextBoolean() ? -1.0f : 1.0f);
            addParticle(smoke);
        }
        
        if (flair) {
            // 1.12-faithful: wispy motes + white flash
            for (int a = 0; a < 2 + rand.nextInt(3); a++) {
                double vx = (0.025 + rand.nextFloat() * 0.025) * (rand.nextBoolean() ? -1.0 : 1.0);
                double vy = (0.025 + rand.nextFloat() * 0.025) * (rand.nextBoolean() ? -1.0 : 1.0);
                double vz = (0.025 + rand.nextFloat() * 0.025) * (rand.nextBoolean() ? -1.0 : 1.0);
                drawWispyMotes(x + vx * 2.0, y + vy * 2.0, z + vz * 2.0, vx, vy, vz,
                        15 + rand.nextInt(10), -0.01f);
            }
            FXGeneric flash = new FXGeneric(level, x, y, z, 0.0, 0.0, 0.0);
            flash.setMaxAge(10 + rand.nextInt(5));
            flash.setColor(1.0f, 0.9f, 1.0f);
            flash.setAlphaKeyframes(1.0f, 0.0f);
            flash.setGridSize(16);
            flash.setParticles(77, 1, 1);
            // Port quad size is block units = 1.12 scale * 0.1
            flash.setScaleKeyframes(1.0f + rand.nextFloat() * 0.2f, 0.0f);
            flash.setLayer(0);
            flash.setRotationSpeedWithStart(rand.nextFloat(), (float) rand.nextGaussian());
            addParticle(flash);
        }
        
        // 1.12-faithful: curly wisps (always spawn, more when flair)
        for (int a = 0; a < (flair ? 2 : 0) + rand.nextInt(3); a++) {
            drawCurlyWisp(x, y, z, 0.0, 0.0, 0.0, 1.0f,
                    (0.9f + rand.nextFloat() * 0.1f + r) / 2.0f,
                    (0.1f + g) / 2.0f,
                    (0.5f + rand.nextFloat() * 0.1f + b) / 2.0f,
                    0.75f, side, a, 0, 0);
        }
    }
    
    // ==================== Wispy Motes ====================
    /**
     * Create wispy mote particles rising from a block (1.12-faithful).
     */
    public void drawWispyMotesOnBlock(BlockPos pp, int age, float grav) {
        drawWispyMotes(pp.getX() + rand.nextFloat(), pp.getY(), pp.getZ() + rand.nextFloat(),
                0.0, 0.0, 0.0, age,
                0.4f + rand.nextFloat() * 0.6f, 0.6f + rand.nextFloat() * 0.4f, 0.6f + rand.nextFloat() * 0.4f, grav);
    }
    
    /**
     * Create a wispy mote particle with velocity (1.12-faithful random color).
     */
    public void drawWispyMotes(double x, double y, double z, double vx, double vy, double vz, int age, float grav) {
        drawWispyMotes(x, y, z, vx, vy, vz, age,
                0.25f + rand.nextFloat() * 0.75f, 0.25f + rand.nextFloat() * 0.75f, 0.25f + rand.nextFloat() * 0.75f, grav);
    }
    
    /**
     * Create a colored wispy mote particle (1.12-faithful: grid 64, sprites 512-527,
     * fade-in/hold/fade-out alpha, scale 1.0 to 0.5, light wind, random movement).
     */
    public void drawWispyMotes(double x, double y, double z, double vx, double vy, double vz,
            int age, float r, float g, float b, float grav) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXGeneric mote = new FXGeneric(level, x, y, z, vx, vy, vz);
            mote.setMaxAge((int) (age + age / 2 * rand.nextFloat()));
            mote.setColor(r, g, b);
            mote.setAlphaKeyframes(0.0f, 0.6f, 0.6f, 0.0f);
            mote.setGridSize(64);
            mote.setParticles(512, 16, 1);
            // Port quad size is block units = 1.12 scale * 0.1
            mote.setScaleKeyframes(0.1f, 0.05f);
            mote.setLoop(true);
            mote.setWind(0.001);
            mote.setGravity(grav);
            mote.setRandomMovementScale(0.0025f, 0.0f, 0.0025f);
            addParticle(mote);
        }
    }
    
    // ==================== Scan Effects ====================
    
    /**
     * Highlight a block with scan sparkles.
     */
    public void scanHighlight(BlockPos p) {
        Level level = getWorld();
        if (level == null) return;
        
        AABB bb = level.getBlockState(p).getShape(level, p).bounds().move(p);
        scanHighlight(bb);
    }
    
    /**
     * Highlight an entity with scan sparkles.
     */
    public void scanHighlight(Entity e) {
        scanHighlight(e.getBoundingBox());
    }
    
    /**
     * Highlight a bounding box with scan sparkles (1.12-faithful: iterates all 6 faces,
     * 2*num sparkles per face with distance-based random delays).
     */
    public void scanHighlight(AABB bb) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        int num = Mth.ceil((bb.getXsize() + bb.getYsize() + bb.getZsize()) / 3.0f * 2.0);
        double ax = (bb.minX + bb.maxX) / 2.0;
        double ay = (bb.minY + bb.maxY) / 2.0;
        double az = (bb.minZ + bb.maxZ) / 2.0;
        for (Direction face : Direction.values()) {
            double mx = 0.5 + face.getStepX() * 0.51;
            double my = 0.5 + face.getStepY() * 0.51;
            double mz = 0.5 + face.getStepZ() * 0.51;
            for (int a = 0; a < num * 2; ++a) {
                double x = mx;
                double y = my;
                double z = mz;
                x += rand.nextGaussian() * (bb.maxX - bb.minX);
                y += rand.nextGaussian() * (bb.maxY - bb.minY);
                z += rand.nextGaussian() * (bb.maxZ - bb.minZ);
                x = Mth.clamp(x, bb.minX - ax, bb.maxX - ax);
                y = Mth.clamp(y, bb.minY - ay, bb.maxY - ay);
                z = Mth.clamp(z, bb.minZ - az, bb.maxZ - az);
                float r = (16 + rand.nextInt(17)) / 255.0f;
                float g = (132 + rand.nextInt(34)) / 255.0f;
                float b = (223 + rand.nextInt(17)) / 255.0f;
                drawSimpleSparkle(rand, ax + x, ay + y, az + z, 0.0, 0.0, 0.0, 0.4f + (float) rand.nextGaussian() * 0.1f, r, g, b, rand.nextInt(10), 1.0f, 0.0f, 4);
            }
        }
    }
    
    /**
     * Create sparkles flowing from block toward a point (1.12-faithful: per-face,
     * gated on the adjacent block being open, distance-based delays).
     */
    public void drawBlockSparkles(BlockPos p, Vec3 start) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        AABB bs = level.getBlockState(p).getShape(level, p).bounds().move(p).inflate(0.1);
        int num = (int) ((bs.getXsize() + bs.getYsize() + bs.getZsize()) / 3.0f * 20.0);
        for (Direction face : Direction.values()) {
            BlockPos adjPos = p.offset(face.getStepX(), face.getStepY(), face.getStepZ());
            BlockState state = level.getBlockState(adjPos);
            if (!state.isSolidRender() && !state.isFaceSturdy(level, adjPos, face.getOpposite())) {
                boolean rx = face.getStepX() == 0;
                boolean ry = face.getStepY() == 0;
                boolean rz = face.getStepZ() == 0;
                double mx = 0.5 + face.getStepX() * 0.51;
                double my = 0.5 + face.getStepY() * 0.51;
                double mz = 0.5 + face.getStepZ() * 0.51;
                for (int a = 0; a < num * 2; ++a) {
                    double x = mx;
                    double y = my;
                    double z = mz;
                    if (rx) x += rand.nextGaussian() * 0.6;
                    if (ry) y += rand.nextGaussian() * 0.6;
                    if (rz) z += rand.nextGaussian() * 0.6;
                    x = Mth.clamp(x, bs.minX, bs.maxX);
                    y = Mth.clamp(y, bs.minY, bs.maxY);
                    z = Mth.clamp(z, bs.minZ, bs.maxZ);
                    float r = 255.0f / 255.0f;
                    float g = (189 + rand.nextInt(67)) / 255.0f;
                    float b = (64 + rand.nextInt(192)) / 255.0f;
                    Vec3 v1 = new Vec3(p.getX() + x, p.getY() + y, p.getZ() + z);
                    double delay = rand.nextInt(5) + v1.distanceTo(start) * 16.0;
                    drawSimpleSparkle(rand, p.getX() + x, p.getY() + y, p.getZ() + z, 0.0, 0.0025, 0.0, 0.4f + (float) rand.nextGaussian() * 0.1f, r, g, b, (int) delay, 1.0f, 0.01f, 16);
                }
            }
        }
    }
    
    /**
     * Create a simple sparkle particle (1.12-faithful: random flicker alpha keyframes,
     * sprite 320/512, grow-then-shrink scale, wind + random movement, delayed spawn).
     */
    public void drawSimpleSparkle(Random rand, double x, double y, double z, double x2, double y2, double z2, 
            float scale, float r, float g, float b, int delay, float decay, float grav, int baseAge) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        boolean sp = rand.nextFloat() < 0.2;
        FXGeneric fb = new FXGeneric(level, x, y, z, x2, y2, z2);
        int age = baseAge * 4 + rand.nextInt(Math.max(1, baseAge));
        fb.setMaxAge(age);
        fb.setColor(r, g, b);
        float[] alphas = new float[6 + rand.nextInt(Math.max(1, age / 3))];
        for (int a = 1; a < alphas.length - 1; ++a) {
            alphas[a] = rand.nextFloat();
        }
        fb.setAlphaKeyframes(alphas);
        fb.setParticles(sp ? 320 : 512, 16, 1);
        fb.setLoop(true);
        fb.setGravity(grav);
        fb.setScaleKeyframes(scale * 0.1f, scale * 2.0f * 0.1f);
        fb.setLayer(0);
        fb.setSlowDown(decay);
        fb.setRandomMovementScale(5.0E-4f, 0.001f, 5.0E-4f);
        fb.setWind(5.0E-4);
        addEffectWithDelay(fb, delay);
    }
    
    /**
     * Create a line sparkle particle (1.12-faithful: fixed fade-in/out alpha, 3-keyframe scale).
     */
    public void drawLineSparkle(Random rand, double x, double y, double z, double x2, double y2, double z2, 
            float scale, float r, float g, float b, int delay, float decay, float grav, int baseAge) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        boolean sp = rand.nextFloat() < 0.2;
        FXGeneric fb = new FXGeneric(level, x, y, z, x2, y2, z2);
        int age = baseAge * 4 + rand.nextInt(Math.max(1, baseAge));
        fb.setMaxAge(age);
        fb.setColor(r, g, b);
        fb.setAlphaKeyframes(0.0f, 1.0f, 0.0f);
        fb.setParticles(sp ? 320 : 512, 16, 1);
        fb.setLoop(true);
        fb.setGravity(grav);
        fb.setScaleKeyframes(scale * 0.1f, scale * 2.0f * 0.1f, scale * 0.1f);
        fb.setLayer(0);
        fb.setSlowDown(decay);
        fb.setRandomMovementScale(5.0E-5f, 0.0f, 5.0E-5f);
        addEffectWithDelay(fb, delay);
    }
    
    // ==================== Block Mist/Fog ====================
    
    /** 1.12-faithful: 8 mist puffs (sprite 56) with long alpha fade and wind. */
    public void drawBlockMistParticles(BlockPos p, int c) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        AABB bs = level.getBlockState(p).getShape(level, p).bounds().move(p);
        Color color = new Color(c);
        for (int a = 0; a < 8; ++a) {
            double x = p.getX() + bs.minX + rand.nextFloat() * (bs.maxX - bs.minX);
            double y = p.getY() + bs.minY + rand.nextFloat() * (bs.maxY - bs.minY);
            double z = p.getZ() + bs.minZ + rand.nextFloat() * (bs.maxZ - bs.minZ);
            FXGeneric fb = new FXGeneric(level, x, y, z, rand.nextGaussian() * 0.01, rand.nextFloat() * 0.075, rand.nextGaussian() * 0.01);
            fb.setMaxAge(50 + rand.nextInt(25));
            fb.setColor(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f);
            fb.setAlphaKeyframes(0.0f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f, 0.0f);
            fb.setGridSize(16);
            fb.setParticles(56, 1, 1);
            fb.setScaleKeyframes(5.0f * 0.1f, 1.0f * 0.1f);
            fb.setLayer(0);
            fb.setSlowDown(1.0);
            fb.setGravity(0.1f);
            fb.setWind(0.001);
            fb.setRotationSpeedWithStart(rand.nextFloat(), rand.nextBoolean() ? -1.0f : 1.0f);
            addParticle(fb);
        }
    }
    
    /** 1.12-faithful: 6 flat mist puffs (grid 8, sprite 24) drifting along the ground. */
    public void drawBlockMistParticlesFlat(BlockPos p, int c) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        Color color = new Color(c);
        for (int a = 0; a < 6; ++a) {
            double x = p.getX() + rand.nextFloat();
            double y = p.getY() + rand.nextFloat() * 0.125f;
            double z = p.getZ() + rand.nextFloat();
            FXGeneric fb = new FXGeneric(level, x, y, z, (rand.nextFloat() - rand.nextFloat()) * 0.005, 0.005, (rand.nextFloat() - rand.nextFloat()) * 0.005);
            fb.setMaxAge(400 + rand.nextInt(100));
            fb.setColor(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f);
            fb.setAlphaKeyframes(1.0f, 0.0f);
            fb.setGridSize(8);
            fb.setParticles(24, 1, 1);
            fb.setScaleKeyframes(2.0f * 0.1f, 5.0f * 0.1f);
            fb.setLayer(0);
            fb.setSlowDown(1.0);
            fb.setWind(0.001);
            fb.setRotationSpeedWithStart(rand.nextFloat(), rand.nextBoolean() ? -1.0f : 1.0f);
            addParticle(fb);
        }
    }
    
    /** 1.12-faithful: soft colored cloud for wand focus effects. */
    public void drawFocusCloudParticle(double x, double y, double z, double mx, double my, double mz, int c) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        Color color = new Color(c);
        FXGeneric fb = new FXGeneric(level, x, y, z, mx, my, mz);
        fb.setMaxAge(20 + rand.nextInt(10));
        fb.setColor(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f);
        fb.setAlphaKeyframes(0.0f, 0.66f, 0.0f);
        fb.setGridSize(16);
        fb.setParticles(56 + rand.nextInt(4), 1, 1);
        fb.setScaleKeyframes((5.0f + rand.nextFloat()) * 0.1f, (10.0f + rand.nextFloat()) * 0.1f);
        fb.setLayer(0);
        fb.setSlowDown(0.99);
        fb.setWind(0.001);
        fb.setRotationSpeedWithStart(rand.nextFloat(), rand.nextBoolean() ? -0.25f : 0.25f);
        addParticle(fb);
    }
    
    // ==================== Vis/Aura Effects ====================
    
    public void visSparkle(int x, int y, int z, int x2, int y2, int z2, int color) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        Color c = new Color(color);
        FXVisSparkle particle = new FXVisSparkle(level,
                x + rand.nextFloat(), y + rand.nextFloat(), z + rand.nextFloat(),
                x2 + 0.4 + rand.nextFloat() * 0.2f, y2 + 0.4 + rand.nextFloat() * 0.2f, z2 + 0.4 + rand.nextFloat() * 0.2f);
        particle.setColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
        addParticle(particle);
    }
    
    /** 1.12-faithful levitator dust (sprite 56, slow rising, long lifetime). */
    public void drawLevitatorParticles(double x, double y, double z, double x2, double y2, double z2) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXGeneric fb = new FXGeneric(level, x, y, z, x2, y2, z2);
        fb.setMaxAge(200 + rand.nextInt(100));
        fb.setColor(0.5f, 0.5f, 0.2f);
        fb.setAlphaKeyframes(0.3f, 0.0f);
        fb.setGridSize(16);
        fb.setParticles(56, 1, 1);
        fb.setScaleKeyframes(2.0f * 0.1f, 5.0f * 0.1f);
        fb.setLayer(0);
        fb.setSlowDown(1.0);
        fb.setRotationSpeedWithStart(rand.nextFloat(), rand.nextBoolean() ? -1.0f : 1.0f);
        addParticle(fb);
    }
    
    /** 1.12-faithful stabilizer shimmer (sprite 72+, accelerating drift). */
    public void drawStabilizerParticles(double x, double y, double z, double x2, double y2, double z2, int life) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXGeneric fb = new FXGeneric(level, x, y, z, x2, y2, z2);
        fb.setMaxAge(life + rand.nextInt(Math.max(1, life)));
        fb.setColor(0.5f, 0.2f, 0.5f);
        fb.setAlphaKeyframes(0.3f, 0.0f);
        fb.setGridSize(16);
        fb.setParticles(72 + rand.nextInt(4), 1, 1);
        fb.setScaleKeyframes(1.0f * 0.1f, 10.0f * 0.1f);
        fb.setLayer(0);
        fb.setSlowDown(1.01);
        fb.setRotationSpeedWithStart(rand.nextFloat(), rand.nextBoolean() ? -1.0f : 1.0f);
        addParticle(fb);
    }
    
    /** 1.12-faithful golem flight trail (3-keyframe expanding scale, sprite 56). */
    public void drawGolemFlyParticles(double x, double y, double z, double x2, double y2, double z2) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        try {
            FXGeneric fb = new FXGeneric(level, x, y, z, x2, y2, z2);
            fb.setMaxAge(20 + rand.nextInt(5));
            fb.setAlphaKeyframes(0.3f, 0.0f);
            fb.setGridSize(16);
            fb.setParticles(56, 1, 1);
            fb.setScaleKeyframes(1.5f * 0.1f, 3.0f * 0.1f, 8.0f * 0.1f);
            fb.setLayer(0);
            fb.setSlowDown(1.0);
            fb.setWind(0.001);
            fb.setRotationSpeedWithStart(rand.nextFloat(), rand.nextBoolean() ? -1.0f : 1.0f);
            addParticle(fb);
        } catch (Exception ex) {
            // 1.12 wrapped this in a try/catch (world unload races) - keep that
        }
    }
    
    /** 1.12-faithful pollution motes (sprite 56, pink, layer 1). */
    public void drawPollutionParticles(BlockPos p) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        float x = p.getX() + 0.2f + rand.nextFloat() * 0.6f;
        float y = p.getY() + 0.2f + rand.nextFloat() * 0.6f;
        float z = p.getZ() + 0.2f + rand.nextFloat() * 0.6f;
        FXGeneric fb = new FXGeneric(level, x, y, z, (rand.nextFloat() - rand.nextFloat()) * 0.005, 0.02, (rand.nextFloat() - rand.nextFloat()) * 0.005);
        fb.setMaxAge(100 + rand.nextInt(60));
        fb.setColor(1.0f, 0.3f, 0.9f);
        fb.setAlphaKeyframes(0.5f, 0.0f);
        fb.setGridSize(16);
        fb.setParticles(56, 1, 1);
        fb.setScaleKeyframes(2.0f * 0.1f, 5.0f * 0.1f);
        fb.setLayer(1);
        fb.setSlowDown(1.0);
        fb.setWind(0.001);
        fb.setRotationSpeedWithStart(rand.nextFloat(), rand.nextBoolean() ? -1.0f : 1.0f);
        addParticle(fb);
    }
    
    // ==================== Essentia Effects ====================
    
    /**
     * Create an essentia stream flowing from source to target
     * (1.12-faithful: a single FXEssentiaStream particle; {@code count} is the stream's segment count).
     */
    public void essentiaTrailFx(BlockPos p1, BlockPos p2, int count, int color, float scale, int ext) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXEssentiaStream fb = new FXEssentiaStream(level, p1.getX() + 0.5, p1.getY() + 0.5, p1.getZ() + 0.5,
                p2.getX() + 0.5, p2.getY() + 0.5, p2.getZ() + 0.5, count, color, scale, ext, 0.0);
        addParticle(fb);
    }
    
    /**
     * Create a small essentia drip/drop particle.
     */
    /** 1.12-faithful: small colored essentia droplet (sprite 25, layer 1). */
    public void essentiaDropFx(double x, double y, double z, float r, float g, float b, float alpha) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXGeneric fb = new FXGeneric(level, x, y, z, rand.nextGaussian() * 0.005, rand.nextGaussian() * 0.005, rand.nextGaussian() * 0.005);
        fb.setMaxAge(20 + rand.nextInt(10));
        fb.setColor(r, g, b);
        fb.setAlphaF(alpha);
        fb.setLoop(false);
        fb.setParticles(25, 1, 1);
        fb.setScaleKeyframes((0.4f + rand.nextFloat() * 0.2f) * 0.1f, 0.2f * 0.1f);
        fb.setLayer(1);
        fb.setGravity(0.01f);
        fb.setRotationSpeed(0.0f);
        addParticle(fb);
    }
    
    /** 1.12-faithful: FXVent with 0.4 alpha. */
    public void drawVentParticles(double x, double y, double z, double x2, double y2, double z2, int color) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXVent fb = new FXVent(level, x, y, z, x2, y2, z2, color);
        fb.setAlphaF(0.4f);
        addParticle(fb);
    }
    
    /** 1.12-faithful: FXVent with 0.4 alpha + scale. */
    public void drawVentParticles(double x, double y, double z, double x2, double y2, double z2, int color, float scale) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXVent fb = new FXVent(level, x, y, z, x2, y2, z2, color);
        fb.setAlphaF(0.4f);
        fb.setScale(scale);
        addParticle(fb);
    }
    
    /** 1.12-faithful: FXVent2 + 33% chance of a bonus orange spark. */
    public void drawVentParticles2(double x, double y, double z, double x2, double y2, double z2, int color, float scale) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXVent2 fb = new FXVent2(level, x, y, z, x2, y2, z2, color);
        fb.setAlphaF(0.4f);
        fb.setScale(scale);
        addParticle(fb);
        
        if (rand.nextInt(6) < 2) {
            drawGenericParticles(x, y, z, x2 / 2.0, y2 / 2.0, z2 / 2.0, 1.0f, 0.7f, 0.2f, 0.9f, true, 320, 16, 1, 10 + rand.nextInt(4), 0, 0.25f + rand.nextFloat() * 0.1f, 0.0f, 0);
        }
    }
    
    /** 1.12-faithful: dark-green droplet (sprite 73, layer 1, strong gravity). */
    public void jarSplashFx(double x, double y, double z) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXGeneric fb = new FXGeneric(level, x + rand.nextGaussian() * 0.075, y, z + rand.nextGaussian() * 0.075,
                rand.nextGaussian() * 0.015, 0.075f + rand.nextFloat() * 0.05f, rand.nextGaussian() * 0.015);
        fb.setMaxAge(20 + rand.nextInt(10));
        Color c = new Color(2650102);
        fb.setColor(c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f);
        fb.setAlphaF(0.5f);
        fb.setLoop(false);
        fb.setParticles(73, 1, 1);
        fb.setScaleKeyframes((0.4f + rand.nextFloat() * 0.3f) * 0.1f, 0.0f);
        fb.setLayer(1);
        fb.setGravity(0.3f);
        fb.setRotationSpeed(0.0f);
        addParticle(fb);
    }
    
    /** 1.12-faithful: water is the same stream particle, source offset +0.66 on Y, extend 0. */
    public void waterTrailFx(BlockPos p1, BlockPos p2, int count, int color, float scale) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXEssentiaStream fb = new FXEssentiaStream(level, p1.getX() + 0.5, p1.getY() + 0.66, p1.getZ() + 0.5,
                p2.getX() + 0.5, p2.getY() + 0.5, p2.getZ() + 0.5, count, color, scale, 0, 0.2);
        addParticle(fb);
    }
    
    // ==================== Infusion Effects ====================
    
    /**
     * Create infusion particles flowing from an item ingredient to the matrix (1.12-faithful).
     */
    public void drawInfusionParticles1(double x, double y, double z, BlockPos pos, ItemStack stack) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXBoreParticles particle = new FXBoreParticles(level, x, y, z, pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5, rand.nextGaussian() * 0.03, rand.nextGaussian() * 0.03, rand.nextGaussian() * 0.03, stack);
        particle.setAlphaF(0.3f);
        addParticle(particle);
    }
    
    /**
     * Create infusion particles from a problem block (1.12-faithful).
     */
    public void drawInfusionParticles2(double x, double y, double z, BlockPos pos, BlockState state, int md) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXBoreParticles particle = new FXBoreParticles(level, x, y, z, pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5, state, md);
        particle.setAlphaF(0.3f);
        addParticle(particle);
    }
    
    /**
     * Create infusion particles at the center of the matrix (1.12-faithful: purple sparkle).
     */
    public void drawInfusionParticles3(double x, double y, double z, int x2, int y2, int z2) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXBoreSparkle particle = new FXBoreSparkle(level, x, y, z, x2 + 0.5, y2 - 0.5, z2 + 0.5);
        particle.setColor(0.4f + rand.nextFloat() * 0.2f, 0.2f, 0.6f + rand.nextFloat() * 0.3f);
        addParticle(particle);
    }
    
    /**
     * Create infusion particles at a pillar location (1.12-faithful: blue sparkle).
     */
    public void drawInfusionParticles4(double x, double y, double z, int x2, int y2, int z2) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXBoreSparkle particle = new FXBoreSparkle(level, x, y, z, x2 + 0.5, y2 - 0.5, z2 + 0.5);
        particle.setColor(0.2f, 0.6f + rand.nextFloat() * 0.3f, 0.3f);
        addParticle(particle);
    }
    
    // ==================== Arc/Lightning ====================
    
    public void arcLightning(double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float h) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXArc arc = new FXArc(level, x, y, z, tx, ty, tz, r, g, b, h <= 0.0f ? 0.1f : h);
            addParticle(arc);
        }
    }
    
    public void arcBolt(double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float width) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXBolt bolt = new FXBolt(level, x, y, z, tx, ty, tz, r, g, b, width);
            addParticle(bolt);
        }
    }
    
    // ==================== Beam Effects ====================
    
    /**
     * Create or update a continuous beam from a living entity to a target point.
     * Used for wand/gauntlet casting effects.
     * 
     * @param p The source entity (caster)
     * @param tx Target X coordinate
     * @param ty Target Y coordinate
     * @param tz Target Z coordinate
     * @param type Beam texture type (0-3)
     * @param color Beam color as packed RGB int
     * @param reverse Reverse UV scroll direction
     * @param endmod End width modifier
     * @param input Existing beam particle to update, or null to create new
     * @param impact Impact flash counter (triggers flash effect when > 0)
     * @return The beam particle instance for subsequent calls
     */
    public Object beamCont(LivingEntity p, double tx, double ty, double tz, int type, int color, boolean reverse, float endmod, Object input, int impact) {
        ClientLevel level = getClientLevel();
        if (level == null) return null;
        
        FXBeamWand beam = null;
        Color c = new Color(color);
        
        // Check if we have an existing beam to update
        if (input instanceof FXBeamWand existingBeam) {
            beam = existingBeam;
        }
        
        // Create new beam if needed
        if (beam == null || !beam.isAlive()) {
            beam = new FXBeamWand(level, p, tx, ty, tz, 
                    c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, 8);
            beam.setType(type);
            beam.setEndMod(endmod);
            beam.setReverse(reverse);
            addParticle(beam);
        } else {
            // Update existing beam
            beam.updateBeam(tx, ty, tz);
            beam.setEndMod(endmod);
            beam.impact = impact;
        }
        
        return beam;
    }
    
    /**
     * Create or update a point-to-point beam (not attached to entity).
     * Used for arcane bore mining beams, infusion effects, etc.
     * 
     * @param px Source X coordinate
     * @param py Source Y coordinate
     * @param pz Source Z coordinate
     * @param tx Target X coordinate
     * @param ty Target Y coordinate
     * @param tz Target Z coordinate
     * @param type Beam texture type (0-3)
     * @param color Beam color as packed RGB int
     * @param reverse Reverse UV scroll direction
     * @param endmod End width modifier
     * @param input Existing beam particle to update, or null to create new
     * @param impact Impact flash counter (triggers flash effect when > 0)
     * @return The beam particle instance for subsequent calls
     */
    public Object beamBore(double px, double py, double pz, double tx, double ty, double tz, int type, int color, boolean reverse, float endmod, Object input, int impact) {
        ClientLevel level = getClientLevel();
        if (level == null) return null;
        
        FXBeamBore beam = null;
        Color c = new Color(color);
        
        // Check if we have an existing beam to update
        if (input instanceof FXBeamBore existingBeam) {
            beam = existingBeam;
        }
        
        // Create new beam if needed
        if (beam == null || !beam.isAlive()) {
            beam = new FXBeamBore(level, px, py, pz, tx, ty, tz, 
                    c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, 8);
            beam.setType(type);
            beam.setEndMod(endmod);
            beam.setReverse(reverse);
            addParticle(beam);
        } else {
            // Update existing beam
            beam.updateBeam(px, py, pz, tx, ty, tz);
            beam.setEndMod(endmod);
            beam.impact = impact;
        }
        
        return beam;
    }
    
    // ==================== Misc Effects ====================
    
    /** 1.12-faithful burst (sprite 208, 31 particles, age 31) at an arbitrary point. */
    public void burst(double sx, double sy, double sz, float size) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXGeneric fb = new FXGeneric(level, sx, sy, sz, 0.0, 0.0, 0.0);
        fb.setGridSize(16);
        fb.setParticles(208, 31, 1);
        fb.setMaxAge(31);
        fb.setScale(size * 0.1f);
        addParticle(fb);
    }
    
    /** 1.12-faithful: client crack overlay for the given entity. */
    public void excavateFX(BlockPos pos, LivingEntity p, int progress) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            level.destroyBlockProgress(p.getId(), pos, progress);
        }
    }
    
    /** 1.12-faithful: runes centered on the given block (offset +0.5 applied here). */
    public void blockRunes(double x, double y, double z, float r, float g, float b, int dur, float grav) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXBlockRunes runes = new FXBlockRunes(level, x + 0.5, y + 0.5, z + 0.5, r, g, b, dur);
            runes.setGravity(grav);
            addParticle(runes);
        }
    }
    
    /** 1.12-faithful: like blockRunes but with a random scale around 0.5. */
    public void blockRunes2(double x, double y, double z, float r, float g, float b, int dur, float grav) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXBlockRunes runes = new FXBlockRunes(level, x + 0.5, y + 0.5, z + 0.5, r, g, b, dur);
            runes.setGravity(grav);
            runes.setScale(0.5f + (float) rand.nextGaussian() * 0.1f);
            runes.setOffsetX(0);
            addParticle(runes);
        }
    }
    
    /** 1.12-faithful: position-based shield runes above the pedestal (no entity target). */
    public void drawPedestalShield(BlockPos pos) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXShieldRunes fb = new FXShieldRunes(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 8);
            addParticle(fb);
        }
    }
    
    public void blockWard(double x, double y, double z, Direction side, float r, float g, float b) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            // The r, g, b parameters here are actually hit coordinates on the block face (0-1)
            FXBlockWard ward = new FXBlockWard(level, x + 0.5, y + 0.5, z + 0.5, side, r, g, b);
            addParticle(ward);
        }
    }
    
    public void smokeSpiral(double x, double y, double z, float rad, int start, int miny, int color) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            Color c = new Color(color);
            FXSmokeSpiral spiral = new FXSmokeSpiral(level, x, y, z, rad, start, miny);
            spiral.setColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
            addParticle(spiral);
        }
    }
    
    public void drawCurlyWisp(double x, double y, double z, double vx, double vy, double vz,
            float scale, float r, float g, float b, float a, Direction side, int seed, int layer, int delay) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        vx += (0.0025 + rand.nextFloat() * 0.005) * (rand.nextBoolean() ? -1.0 : 1.0);
        vy += (0.0025 + rand.nextFloat() * 0.005) * (rand.nextBoolean() ? -1.0 : 1.0);
        vz += (0.0025 + rand.nextFloat() * 0.005) * (rand.nextBoolean() ? -1.0 : 1.0);
        if (side != null) {
            vx += side.getStepX() * 0.025;
            vy += side.getStepY() * 0.025;
            vz += side.getStepZ() * 0.025;
        }
        FXGeneric wisp = new FXGeneric(level, x + vx * 5.0, y + vy * 5.0, z + vz * 5.0, vx, vy, vz);
        if (seed > 0 && rand.nextBoolean()) {
            wisp.setAngles(90.0f * (float) rand.nextGaussian(), 90.0f * (float) rand.nextGaussian());
        }
        wisp.setMaxAge(25 + rand.nextInt(20 + 20 * seed));
        wisp.setColorRange(r, g, b, 0.1f, 0.0f, 0.1f);
        wisp.setAlphaKeyframes(a, 0.0f);
        wisp.setGridSize(16);
        wisp.setParticles(60 + rand.nextInt(4), 1, 1);
        // Port quad size is block units = 1.12 scale * 0.1
        wisp.setScaleKeyframes(0.5f * scale, (1.0f + rand.nextFloat() * 0.4f) * scale);
        wisp.setLayer(layer);
        wisp.setRotationSpeedWithStart(rand.nextFloat(),
                rand.nextBoolean() ? (-2.0f - rand.nextFloat() * 2.0f) : (2.0f + rand.nextFloat() * 2.0f));
        // delay is 0 at every call site (1.12 addEffectWithDelay had no non-zero uses)
        addParticle(wisp);
    }
    
    /** 1.12-faithful: single FXVoidStream. */
    public void voidStreak(double x, double y, double z, double x2, double y2, double z2, int seed, float scale) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXVoidStream particle = new FXVoidStream(level, x, y, z, x2, y2, z2, seed, scale);
            addParticle(particle);
        }
    }
    
    /** 1.12-faithful: single lava particle pushed out along the facing. */
    public void furnaceLavaFx(int x, int y, int z, int facingX, int facingZ) {
        Level level = getWorld();
        if (level != null) {
            float qx = (facingX == 0) ? (rand.nextFloat() - rand.nextFloat()) * 0.5f : facingX * rand.nextFloat();
            float qz = (facingZ == 0) ? (rand.nextFloat() - rand.nextFloat()) * 0.5f : facingZ * rand.nextFloat();
            level.addParticle(ParticleTypes.LAVA,
                    x + 0.5f + (rand.nextFloat() - rand.nextFloat()) * 0.3f + facingX * 1.0f, y + 0.3f, z + 0.5f + (rand.nextFloat() - rand.nextFloat()) * 0.3f + facingZ * 1.0f,
                    0.15f * qx, 0.2f * rand.nextFloat(), 0.15f * qz);
        }
    }
    
    /** 1.12-faithful: 8 taint-bottle item-crack particles + break sound. */
    public void bottleTaintBreak(double x, double y, double z) {
        Level level = getWorld();
        if (level == null) return;
        
        for (int a = 0; a < 8; a++) {
            level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, ModItems.BOTTLE_TAINT.get()),
                    x, y, z,
                    (float) rand.nextGaussian() * 0.15f, (float) rand.nextDouble() * 0.2f, (float) rand.nextGaussian() * 0.15f);
        }
        level.playLocalSound(x, y, z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL,
                1.0f, rand.nextFloat() * 0.1f + 0.9f, false);
    }
    
    /** 1.12-faithful: white-to-red FXGeneric (sprite 160, 6 frames, layer 1). */
    public void cultistSpawn(double x, double y, double z, double a, double b, double c) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXGeneric fb = new FXGeneric(level, x, y, z, a, b, c);
            fb.setMaxAge(10 + rand.nextInt(10));
            fb.setColorRange(1.0f, 1.0f, 1.0f, 0.6f, 0.0f, 0.0f);
            fb.setAlphaF(0.8f);
            fb.setGridSize(16);
            fb.setParticles(160, 6, 1);
            fb.setScale((3.0f + rand.nextFloat() * 2.0f) * 0.1f);
            fb.setLayer(1);
            addParticle(fb);
        }
    }
    
    /** 1.12-faithful: angled FXGeneric (grid 8, sprite 28) + wispy motes. */
    public void pechsCurseTick(double posX, double posY, double posZ) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXGeneric fb = new FXGeneric(level, posX, posY, posZ, 0.0, 0.0, 0.0);
        fb.setAngles(90.0f * (float) rand.nextGaussian(), 90.0f * (float) rand.nextGaussian());
        fb.setMaxAge(50 + rand.nextInt(50));
        fb.setColorRange(0.9f, 0.1f, 0.5f, 0.1f + rand.nextFloat() * 0.1f, 0.0f, 0.5f + rand.nextFloat() * 0.1f);
        fb.setAlphaKeyframes(0.75f, 0.0f);
        fb.setGridSize(8);
        fb.setParticles(28 + rand.nextInt(4), 1, 1);
        fb.setScaleKeyframes(3.0f * 0.1f, (5.0f + rand.nextFloat() * 2.0f) * 0.1f);
        fb.setLayer(0);
        fb.setRotationSpeedWithStart(rand.nextFloat(), rand.nextBoolean() ? (-3.0f - rand.nextFloat() * 3.0f) : (3.0f + rand.nextFloat() * 3.0f));
        addParticle(fb);
        
        drawWispyMotes(posX, posY, posZ, 0.0, 0.0, 0.0, 10 + rand.nextInt(10), -0.01f);
    }
    
    /** 1.12-faithful: two homing wisps. */
    public void wispFXEG(double posX, double posY, double posZ, Entity target) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        for (int a = 0; a < 2; a++) {
            addParticle(new FXWisp(level, posX, posY, posZ, target));
        }
    }
    
    /** 1.12-faithful: single FXPlane slash between the two points. */
    public void drawSlash(double x, double y, double z, double x2, double y2, double z2, int dur) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXPlane particle = new FXPlane(level, x, y, z, x2, y2, z2, dur);
        addParticle(particle);
    }
    
    /**
     * Create sparkle particles flying from a block to the bore entity.
     */
    public void boreDigFx(int x, int y, int z, Entity e, BlockState bi, int md, int delay) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        float p = 50.0f;
        for (int a = 0; a < p / delay; a++) {
            if (rand.nextInt(4) == 0) {
                // Sparkle particle that homes to the entity
                FXBoreSparkle sparkle = new FXBoreSparkle(level, 
                        x + rand.nextFloat(), y + rand.nextFloat(), z + rand.nextFloat(), e);
                addParticle(sparkle);
            } else {
                // Block debris heading to the entity (1.12-faithful)
                FXBoreParticles fb = new FXBoreParticles(level, 
                        x + rand.nextFloat(), y + rand.nextFloat(), z + rand.nextFloat(),
                        e.getX(), e.getY(), e.getZ(), bi, md);
                fb.setTarget(e);
                addParticle(fb);
            }
        }
    }
    
    public void sonicBoom(double x, double y, double z, Entity source, int duration) {
        Level level = getWorld();
        if (level == null) return;
        
        // Create expanding ring of particles
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI * 2 / 16;
            double px = x + Math.cos(angle) * 0.5;
            double pz = z + Math.sin(angle) * 0.5;
            double vx = Math.cos(angle) * 0.3;
            double vz = Math.sin(angle) * 0.3;
            level.addParticle(ParticleTypes.SONIC_BOOM, px, y + 1, pz, vx, 0, vz);
        }
    }
    
    /** 1.12-faithful: single FXBoreStream from the block toward the entity. */
    public void boreTrailFx(BlockPos p1, Entity e, int count, int color, float scale, int ext) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXBoreStream particle = new FXBoreStream(level, p1.getX() + 0.5, p1.getY() + 0.5, p1.getZ() + 0.5,
                e.getX(), e.getY(), e.getZ(), scale);
        addParticle(particle);
    }
    
    // ==================== Entity Effects ====================
    
    /** 1.12-faithful: slime-ball breaking-fade splash in a random direction. */
    public void splooshFX(Entity e) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        float f = (float) (rand.nextFloat() * Math.PI * 2.0);
        float f2 = 0.5f + rand.nextFloat() * 0.5f;
        float f3 = (float) (Math.sin(f) * f2);
        float f4 = (float) (Math.cos(f) * f2);
        FXBreakingFade fb = new FXBreakingFade(level, e.getX() + f3, e.getY() + rand.nextFloat() * e.getBbHeight(), e.getZ() + f4, Items.SLIME_BALL, 0);
        if (rand.nextBoolean()) {
            fb.setRGB(0.6f, 0.0f, 0.3f);
            fb.setAlphaF(0.4f);
        } else {
            fb.setRGB(0.3f, 0.0f, 0.3f);
            fb.setAlphaF(0.6f);
        }
        fb.setParticleMaxAge((int) (66 / (rand.nextFloat() * 0.9f + 0.1f)));
        addParticle(fb);
    }
    
    /** 1.12-faithful: slime-ball breaking-fade at the entity position. */
    public void taintsplosionFX(Entity e) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXBreakingFade fb = new FXBreakingFade(level, e.getX(), e.getY(), e.getZ(), Items.SLIME_BALL, 0);
        if (rand.nextBoolean()) {
            fb.setRGB(0.6f, 0.0f, 0.3f);
            fb.setAlphaF(0.4f);
        } else {
            fb.setRGB(0.3f, 0.0f, 0.3f);
            fb.setAlphaF(0.6f);
        }
        fb.setParticleMaxAge((int) (66 / (rand.nextFloat() * 0.9f + 0.1f)));
        addParticle(fb);
    }
    
    /** 1.12-faithful: same breaking-fade pattern as taintsplosion. */
    public void tentacleAriseFX(Entity e) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXBreakingFade fb = new FXBreakingFade(level, e.getX(), e.getY(), e.getZ(), Items.SLIME_BALL, 0);
        if (rand.nextBoolean()) {
            fb.setRGB(0.6f, 0.0f, 0.3f);
            fb.setAlphaF(0.4f);
        } else {
            fb.setRGB(0.3f, 0.0f, 0.3f);
            fb.setAlphaF(0.6f);
        }
        fb.setParticleMaxAge((int) (66 / (rand.nextFloat() * 0.9f + 0.1f)));
        addParticle(fb);
    }
    
    /** 1.12-faithful: same breaking-fade pattern as taintsplosion. */
    public void slimeJumpFX(Entity e, int size) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXBreakingFade fb = new FXBreakingFade(level, e.getX(), e.getY(), e.getZ(), Items.SLIME_BALL, 0);
        if (rand.nextBoolean()) {
            fb.setRGB(0.6f, 0.0f, 0.3f);
            fb.setAlphaF(0.4f);
        } else {
            fb.setRGB(0.3f, 0.0f, 0.3f);
            fb.setAlphaF(0.6f);
        }
        fb.setParticleMaxAge((int) (66 / (rand.nextFloat() * 0.9f + 0.1f)));
        addParticle(fb);
    }
    
    /** 1.12-faithful: same breaking-fade pattern as taintsplosion. */
    public void taintLandFX(Entity e) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        FXBreakingFade fb = new FXBreakingFade(level, e.getX(), e.getY() + 0.1, e.getZ(), Items.SLIME_BALL, 0);
        if (rand.nextBoolean()) {
            fb.setRGB(0.6f, 0.0f, 0.3f);
            fb.setAlphaF(0.4f);
        } else {
            fb.setRGB(0.3f, 0.0f, 0.3f);
            fb.setAlphaF(0.6f);
        }
        fb.setParticleMaxAge((int) (66 / (rand.nextFloat() * 0.9f + 0.1f)));
        addParticle(fb);
    }
    
    /**
     * Create a swarm particle that follows the target entity.
     * @param targetedEntity The entity to swarm around
     * @param speed Movement speed
     * @param turnSpeed Turn speed
     * @param pg Gravity
     * @return The created particle
     */
    public Object swarmParticleFX(Entity targetedEntity, float speed, float turnSpeed, float pg) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            // Spawn at random offset around target
            double x = targetedEntity.getX() + (rand.nextFloat() - rand.nextFloat()) * 2.0f;
            double y = targetedEntity.getY() + (rand.nextFloat() - rand.nextFloat()) * 2.0f;
            double z = targetedEntity.getZ() + (rand.nextFloat() - rand.nextFloat()) * 2.0f;
            
            // Purple/pink taint colors
            float r = 0.8f + rand.nextFloat() * 0.2f;
            float g = rand.nextFloat() * 0.4f;
            float b = 1.0f - rand.nextFloat() * 0.2f;
            
            FXSwarm swarm = new FXSwarm(level, x, y, z, targetedEntity, r, g, b, speed, turnSpeed, pg);
            addParticle(swarm);
            return swarm;
        }
        return null;
    }
    
    // ==================== Nitor Effects ====================
    
    /**
     * Draw the white core glow of a Nitor flame (1.12-faithful: 3-keyframe scale pulse).
     */
    public void drawNitorCore(double x, double y, double z, double vx, double vy, double vz) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXGeneric particle = new FXGeneric(level, x, y, z, vx, vy, vz);
            particle.setMaxAge(10);
            particle.setColor(1.0f, 1.0f, 1.0f);
            particle.setAlphaF(1.0f);
            particle.setParticles(457, 1, 1);  // Bright glow particle
            particle.setScaleKeyframes(1.0f * 0.1f, (1.0f + (float) rand.nextGaussian() * 0.1f) * 0.1f, 1.0f * 0.1f);
            particle.setLayer(1);
            particle.setRandomMovementScale(0.0002f, 0.0002f, 0.0002f);
            addParticle(particle);
        }
    }
    
    /**
     * Draw the colored flame particles of a Nitor.
     */
    public void drawNitorFlames(double x, double y, double z, double vx, double vy, double vz, int color, int delay) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            Color c = new Color(color);
            FXGeneric particle = new FXGeneric(level, x, y, z, vx, vy, vz);
            particle.setMaxAge(10 + rand.nextInt(5));
            particle.setColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
            particle.setAlphaF(0.66f);
            particle.setLoop(true);
            particle.setGridSize(64);
            particle.setParticles(264, 8, 1);  // Flame animation
            particle.setScaleKeyframes((3.0f + rand.nextFloat()) * 0.1f, 0.05f * 0.1f);
            particle.setRandomMovementScale(0.0025f, 0.0f, 0.0025f);
            particle.setFlipped(rand.nextBoolean());
            addEffectWithDelay(particle, delay);
        }
    }
    
    // ==================== GUI Effects ====================
    
    /**
     * 1.12-faithful GUI sparkle: an FXGenericGui billboard at z=0 between the two points
     * (used by in-screen effects, e.g. the Thaumonomicon research pages).
     */
    public void drawSimpleSparkleGui(Random rand, double x, double y, double x2, double y2, 
            float scale, float r, float g, float b, int delay, float decay, float grav) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        boolean sp = rand.nextFloat() < 0.2;
        FXGenericGui fb = new FXGenericGui(level, x, y, 0.0, x2, y2, 0.0);
        int age = 32 + rand.nextInt(8);
        fb.setMaxAge(age);
        fb.setColor(r, g, b);
        fb.setAlphaKeyframes(0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f);
        fb.setParticles(sp ? 320 : 512, 16, 1);
        fb.setLoop(true);
        fb.setGravity(grav);
        fb.setScaleKeyframes(scale * 0.1f, scale * 2.0f * 0.1f);
        fb.setNoClip(true); // 1.12 setNoClip(false) = no collision (inverted semantics in the port)
        fb.setLayer(4);
        fb.setSlowDown(decay);
        fb.setRandomMovementScale(0.025f, 0.025f, 0.0f);
        addEffectWithDelay(fb, delay);
    }
    
    /**
     * 1.12-faithful: entity-following wispy motes (sprite 512, 16 frames).
     */
    public void drawWispyMotesEntity(ClientLevel level, double x, double y, double z, Entity entity, float r, float g, float b) {
        if (level == null) return;
        
        FXGenericP2E fb = new FXGenericP2E(level, x, y, z, entity);
        fb.setColor(r, g, b);
        fb.setAlphaF(0.6f);
        fb.setParticles(512, 16, 1);
        fb.setLoop(true);
        fb.setWind(0.001);
        fb.setRandomMovementScale(0.0025f, 0.0f, 0.0025f);
        addParticle(fb);
    }
    
    /**
     * 1.12-faithful: nitor-flame-style wisps (grid 64, sprite 264, 8 frames, looped, delayed).
     */
    public void drawWispParticles(double x, double y, double z, double vx, double vy, double vz, int color, int delay) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        Color c = new Color(color);
        FXGeneric fb = new FXGeneric(level, x, y, z, vx, vy, vz);
        fb.setMaxAge(10 + rand.nextInt(5));
        fb.setColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
        fb.setAlphaF(0.5f);
        fb.setLoop(true);
        fb.setGridSize(64);
        fb.setParticles(264, 8, 1);
        fb.setScaleKeyframes((1.0f + rand.nextFloat() * 0.25f) * 0.1f, 0.05f * 0.1f);
        fb.setWind(0.00025);
        fb.setRandomMovementScale(0.0025f, 0.0f, 0.0025f);
        addEffectWithDelay(fb, delay);
    }
    
    /**
     * GenPart - Generic particle configuration (1.12 field-for-field compatible).
     */
    public static class GenPart {
        public int grid = 64;
        public int age = 0;
        public float redStart = 1.0f, greenStart = 1.0f, blueStart = 1.0f;
        public float redEnd = 1.0f, greenEnd = 1.0f, blueEnd = 1.0f;
        public float[] alpha = new float[]{1.0f};
        public float[] scale = new float[]{1.0f};
        public float rot = 0.0f;
        public float rotstart = 0.0f;
        public boolean loop = false;
        public int partStart = 0, partNum = 1, partInc = 1;
        public int layer = 0;
        public double slowDown = 0.98;
        public float grav = 0.0f;
        public int delay = 0;
    }
}
