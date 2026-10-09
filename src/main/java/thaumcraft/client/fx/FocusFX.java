package thaumcraft.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.client.fx.particles.FXGeneric;
import thaumcraft.common.items.casters.foci.FocusEffectAir;
import thaumcraft.common.items.casters.foci.FocusEffectBreak;
import thaumcraft.common.items.casters.foci.FocusEffectCurse;
import thaumcraft.common.items.casters.foci.FocusEffectEarth;
import thaumcraft.common.items.casters.foci.FocusEffectExchange;
import thaumcraft.common.items.casters.foci.FocusEffectFire;
import thaumcraft.common.items.casters.foci.FocusEffectFlux;
import thaumcraft.common.items.casters.foci.FocusEffectFrost;
import thaumcraft.common.items.casters.foci.FocusEffectHeal;
import thaumcraft.common.items.casters.foci.FocusEffectRift;

/**
 * Client-side particle rendering for focus effects.
 *
 * 1.12 put each effect's particle code in the focus class itself
 * ({@code FocusEffect.renderParticleFX}); the 1.12/Forge 1.12 loader shipped
 * client classes on the server classpath, so common code could reference them.
 * NeoForge 26.3 does not (and @OnlyIn no longer strips members), so the
 * per-effect FX bodies live here instead, keyed by the concrete effect class.
 */
@OnlyIn(Dist.CLIENT)
public final class FocusFX {

    private FocusFX() {
    }

    public static void render(FocusEffect effect, Level level, double posX, double posY, double posZ,
                              double motionX, double motionY, double motionZ) {
        if (!(level instanceof ClientLevel clientLevel)) {
            return;
        }
        RandomSource random = clientLevel.getRandom();
        if (effect instanceof FocusEffectAir) {
            renderAir(random, clientLevel, posX, posY, posZ, motionX, motionY, motionZ);
        } else if (effect instanceof FocusEffectFire) {
            renderFire(random, clientLevel, posX, posY, posZ, motionX, motionY, motionZ);
        } else if (effect instanceof FocusEffectFrost) {
            renderFrost(random, clientLevel, posX, posY, posZ, motionX, motionY, motionZ);
        } else if (effect instanceof FocusEffectEarth) {
            renderEarth(random, clientLevel, posX, posY, posZ, motionX, motionY, motionZ);
        } else if (effect instanceof FocusEffectFlux) {
            renderFlux(random, clientLevel, posX, posY, posZ, motionX, motionY, motionZ);
        } else if (effect instanceof FocusEffectBreak) {
            renderBreak(random, clientLevel, posX, posY, posZ, motionX, motionY, motionZ);
        } else if (effect instanceof FocusEffectHeal) {
            renderHeal(random, clientLevel, posX, posY, posZ, motionX, motionY, motionZ);
        } else if (effect instanceof FocusEffectExchange) {
            renderExchange(random, clientLevel, posX, posY, posZ, motionX, motionY, motionZ);
        } else if (effect instanceof FocusEffectCurse) {
            renderCurse(random, clientLevel, posX, posY, posZ, motionX, motionY, motionZ);
        } else if (effect instanceof FocusEffectRift) {
            renderRift(random, clientLevel, posX, posY, posZ, motionX, motionY, motionZ);
        }
    }

    private static void renderAir(RandomSource random, ClientLevel level, double x, double y, double z,
                                  double mx, double my, double mz) {
        // 1.12-faithful: FXDispatcher.GenPart wind mote (grid 32, sprites 337..341)
        FXDispatcher.GenPart pp = new FXDispatcher.GenPart();
        pp.grav = -0.1F;
        pp.age = 20 + random.nextInt(10);
        pp.alpha = new float[] { 0.5F, 0.0F };
        pp.grid = 32;
        pp.partStart = 337;
        pp.partInc = 1;
        pp.partNum = 5;
        pp.slowDown = 0.75;
        pp.rot = (float) random.nextGaussian() / 2.0F;
        float s = (float) (2.0 + random.nextGaussian() * 0.5);
        pp.scale = new float[] { s, s * 2.0F };
        FXDispatcher.INSTANCE.drawGenericParticles(x, y, z, mx, my, mz, pp);
    }

    private static void renderFire(RandomSource random, ClientLevel level, double x, double y, double z,
                                   double mx, double my, double mz) {
        // 1.12-faithful: FXDispatcher.GenPart flame mote (sprites 640..649)
        FXDispatcher.GenPart pp = new FXDispatcher.GenPart();
        pp.grav = -0.2F;
        pp.age = 10;
        pp.alpha = new float[] { 0.7F };
        pp.partStart = 640;
        pp.partInc = 1;
        pp.partNum = 10;
        pp.slowDown = 0.75;
        pp.scale = new float[] { (float) (1.5 + random.nextGaussian() * 0.2F) };
        FXDispatcher.INSTANCE.drawGenericParticles(x, y, z, mx, my, mz, pp);
    }

    private static void renderFrost(RandomSource random, ClientLevel level, double x, double y, double z,
                                    double mx, double my, double mz) {
        // 1.12-faithful: FXGeneric frost sprite (sprite 8)
        FXGeneric fb = new FXGeneric(level, x, y, z, mx, my, mz);
        fb.setMaxAge(40 + random.nextInt(40));
        fb.setAlphaKeyframes(1.0F, 0.0F);
        fb.setParticles(8, 1, 1);
        fb.setGravity(0.033F);
        fb.setSlowDown(0.8);
        fb.setRandomMovementScale(0.0025F, 1.0E-4F, 0.0025F);
        fb.setScale((float) (0.7F + random.nextGaussian() * 0.3F) * 0.1F);
        fb.setRotationSpeedWithStart(random.nextFloat() * 3.0F, (float) random.nextGaussian() / 4.0F);
        FXDispatcher.INSTANCE.addEffectWithDelay(fb, 0);
    }

    private static void renderEarth(RandomSource random, ClientLevel level, double x, double y, double z,
                                    double mx, double my, double mz) {
        // 1.12-faithful: FXDispatcher.GenPart dirt mote (sprites 75..78)
        FXDispatcher.GenPart pp = new FXDispatcher.GenPart();
        pp.grav = 0.4F;
        pp.layer = 1;
        pp.age = 20 + random.nextInt(10);
        pp.alpha = new float[] { 1.0F, 0.0F };
        pp.partStart = 75 + random.nextInt(4);
        pp.partInc = 1;
        pp.partNum = 1;
        pp.slowDown = 0.9;
        pp.rot = (float) random.nextGaussian();
        float s = (float) (1.0 + random.nextGaussian() * 0.2F);
        pp.scale = new float[] { s, s / 2.0F };
        FXDispatcher.INSTANCE.drawGenericParticles(x, y, z, mx, my, mz, pp);
    }

    private static void renderFlux(RandomSource random, ClientLevel level, double x, double y, double z,
                                   double mx, double my, double mz) {
        // 1.12-faithful: FXGeneric flux mote (sprite 128, 14 frames, looping)
        FXGeneric fb = new FXGeneric(level, x, y, z,
                mx + random.nextGaussian() * 0.01,
                my + random.nextGaussian() * 0.01,
                mz + random.nextGaussian() * 0.01);
        fb.setMaxAge((int) (15.0F + 10.0F * random.nextFloat()));
        fb.setColor(0.25F + random.nextFloat() * 0.25F, 0.0F, 0.25F + random.nextFloat() * 0.25F);
        fb.setAlphaKeyframes(0.0F, 1.0F, 1.0F, 0.0F);
        fb.setGridSize(64);
        fb.setParticles(128, 14, 1);
        fb.setScaleKeyframes((2.0F + random.nextFloat()) * 0.1F, (0.25F + random.nextFloat() * 0.25F) * 0.1F);
        fb.setLoop(true);
        fb.setSlowDown(0.9);
        fb.setGravity((float) (random.nextGaussian() * 0.1F));
        fb.setRandomMovementScale(0.0125F, 0.0125F, 0.0125F);
        fb.setRotationSpeed((float) random.nextGaussian());
        FXDispatcher.INSTANCE.addEffectWithDelay(fb, random.nextInt(4));
    }

    private static void renderBreak(RandomSource random, ClientLevel level, double x, double y, double z,
                                    double mx, double my, double mz) {
        // 1.12-faithful: FXGeneric breaking sprite (sprites 704..713)
        FXGeneric fb = new FXGeneric(level, x, y, z, mx, my, mz);
        fb.setMaxAge(6 + random.nextInt(6));
        int q = random.nextInt(4);
        fb.setParticles(704 + q * 3, 3, 1);
        fb.setSlowDown(0.8);
        fb.setScale((float) (1.7F + random.nextGaussian() * 0.3F) * 0.1F);
        FXDispatcher.INSTANCE.addEffect(fb);
    }

    private static void renderHeal(RandomSource random, ClientLevel level, double x, double y, double z,
                                   double mx, double my, double mz) {
        // 1.12-faithful: FXGeneric heal mote (white, alpha 0->0.7->0.7->0)
        FXGeneric fb = new FXGeneric(level, x, y, z,
                mx + random.nextGaussian() * 0.01,
                my + random.nextGaussian() * 0.01,
                mz + random.nextGaussian() * 0.01);
        fb.setMaxAge((int) (10.0F + 10.0F * random.nextFloat()));
        fb.setColor(1.0F, 1.0F, 1.0F);
        fb.setAlphaKeyframes(0.0F, 0.7F, 0.7F, 0.0F);
        fb.setGridSize(64);
        fb.setParticles(0, 1, 1);
        fb.setScaleKeyframes(random.nextFloat() * 2.0F * 0.1F, random.nextFloat() * 0.1F);
        fb.setSlowDown(0.8);
        fb.setGravity((float) (random.nextGaussian() * 0.1F));
        fb.setRandomMovementScale(0.0125F, 0.0125F, 0.0125F);
        fb.setRotationSpeed((float) random.nextGaussian());
        FXDispatcher.INSTANCE.addEffectWithDelay(fb, random.nextInt(4));
    }

    private static void renderExchange(RandomSource random, ClientLevel level, double x, double y, double z,
                                       double mx, double my, double mz) {
        // 1.12-faithful: FXGeneric exchange sprite (sprite 448, 9 frames)
        FXGeneric fb = new FXGeneric(level, x, y, z,
                mx + random.nextGaussian() * 0.01,
                my + random.nextGaussian() * 0.01,
                mz + random.nextGaussian() * 0.01);
        fb.setMaxAge(9);
        fb.setColor(0.25F + random.nextFloat() * 0.25F, 0.25F + random.nextFloat() * 0.25F,
                0.25F + random.nextFloat() * 0.25F);
        fb.setAlphaKeyframes(0.0F, 0.6F, 0.6F, 0.0F);
        fb.setGridSize(64);
        fb.setParticles(448, 9, 1);
        fb.setScaleKeyframes(0.5F * 0.1F, 0.25F * 0.1F);
        fb.setGravity((float) (random.nextGaussian() * 0.01F));
        fb.setRandomMovementScale(0.0025F, 0.0025F, 0.0025F);
        FXDispatcher.INSTANCE.addEffect(fb);
    }

    private static void renderCurse(RandomSource random, ClientLevel level, double x, double y, double z,
                                    double mx, double my, double mz) {
        // 1.12-faithful: FXGeneric curse sprite (sprites 72..75)
        FXGeneric fb = new FXGeneric(level, x, y, z, mx, my, mz);
        fb.setMaxAge(8);
        fb.setColor(0.41F + random.nextFloat() * 0.2F, 0.0F, 0.019F + random.nextFloat() * 0.2F);
        fb.setAlphaKeyframes(0.0F, random.nextFloat(), random.nextFloat(), random.nextFloat(), 0.0F);
        fb.setGridSize(16);
        fb.setParticles(72 + random.nextInt(4), 1, 1);
        fb.setScale((2.0F + random.nextFloat() * 4.0F) * 0.1F);
        fb.setLoop(false);
        fb.setSlowDown(0.9);
        fb.setGravity(0.0F);
        fb.setRotationSpeedWithStart(random.nextFloat(), 0.0F);
        FXDispatcher.INSTANCE.addEffectWithDelay(fb, random.nextInt(4));
    }

    private static void renderRift(RandomSource random, ClientLevel level, double x, double y, double z,
                                   double mx, double my, double mz) {
        // 1.12-faithful: FXGeneric rift sprite (sprite 0, 384+ frames)
        FXGeneric fb = new FXGeneric(level, x, y, z, mx, my, mz);
        fb.setMaxAge(16 + random.nextInt(16));
        fb.setParticles(384 + random.nextInt(16), 1, 1);
        fb.setSlowDown(0.75);
        fb.setAlphaKeyframes(1.0F, 0.0F);
        fb.setScale((float) (0.7F + random.nextGaussian() * 0.3F) * 0.1F);
        fb.setColor(0.25F, 0.25F, 1.0F);
        fb.setRandomMovementScale(0.01F, 0.01F, 0.01F);
        FXDispatcher.INSTANCE.addEffectWithDelay(fb, 0);
    }
}
