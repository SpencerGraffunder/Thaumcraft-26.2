package thaumcraft.client.lib.events;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.ICaster;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.lib.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.items.tools.ItemSanityChecker;
import thaumcraft.common.items.tools.ItemThaumometer;
import thaumcraft.common.world.aura.AuraChunk;

import org.joml.Matrix3x2fStack;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * HudHandler - Renders Thaumcraft HUD overlays.
 *
 * 1:1 port of the 1.12 HudHandler (client/lib/events/HudHandler.java):
 * - Thaumometer aura gauge (vis/flux levels)
 * - Caster gauntlet vis gauge, focus info and the sneaking read-outs
 * - Sanity checker warp gauge
 * - Knowledge-gain icon animation (book in the bottom-right corner)
 *
 * Ported to the 26.3 NeoForge GuiLayer system (RegisterGuiLayersEvent). 1.12's inGame
 * ortho (glOrtho(0, w, h, 0, ...)) has y=0 at the top of the screen with y growing down -
 * the same space as a 26.3 GuiLayer - so every 1.12 offset translates directly.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class HudHandler {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#######.#");

    /** 1.12 HudHandler.HUD = thaumcraft:textures/gui/hud.png (256x256). */
    public static final Identifier HUD_TEXTURE = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/hud.png");
    /** 1.12 HudHandler.BOOK = thaumcraft:textures/items/thaumonomicon.png (the port keeps it at item/thaumonomicon, 32x32). */
    public static final Identifier BOOK_TEXTURE = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/item/thaumonomicon");
    /** 1.12 HudHandler.KNOW_TYPE = knowledge_theory.png / knowledge_observation.png (16x16). */
    public static final Identifier KNOW_THEORY = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/research/knowledge_theory");
    public static final Identifier KNOW_OBSERVATION = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/research/knowledge_observation");
    /** 1.12 ParticleEngine.particleTexture = thaumcraft:textures/misc/particles.png (the port's is 1024x1024). */
    public static final Identifier PARTICLE_TEXTURE = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/particles");

    // Current aura data (updated by packets from server)
    public static AuraChunk currentAura = new AuraChunk(null, (short) 0, 0.0f, 0.0f);

    // 1.12 HudHandler.renderThaumometerHud divides the aura values by 525
    public static final float MAX_VIS = 525.0f;

    /** 1.12 HudHandler.knowledgeGainTrackers - icons animating into the book. */
    public static final LinkedBlockingQueue<KnowledgeGainTracker> knowledgeGainTrackers = new LinkedBlockingQueue<>();

    /** 1.12 HudHandler.kgFade - book fade alpha driver (0..40, 1 per tick while idle). */
    public static float kgFade = 0.0f;

    /** Client-side HUD randomness (1.12 used the player/world RNG for these decorative rolls). */
    private static final Random HUD_RANDOM = new Random();

    /**
     * 1.12 PacketKnowledgeGain.onMessage (client half): enqueue the icon animation.
     * Called from PacketKnowledgeGainClient on the render thread.
     */
    public static void onKnowledgeGain(IPlayerKnowledge.EnumKnowledgeType type, ResearchCategory category) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        knowledgeGainTrackers.add(new KnowledgeGainTracker(type, category, 40 + HUD_RANDOM.nextInt(20), HUD_RANDOM.nextLong()));
    }

    /**
     * 1.12 RenderEventHandler.onRenderTick: while trackers are pending the fade is held at
     * the top (it refills every tick up to 40); once the queue is empty it decays by 1 per
     * tick until the book disappears.
     */
    public static void tickKnowledgeFade() {
        if (knowledgeGainTrackers.isEmpty()) {
            if (kgFade > 0.0f) kgFade--;
        } else {
            kgFade += 10.0f;
            if (kgFade > 40.0f) kgFade = 40.0f;
        }
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        // Register the Thaumcraft HUD overlay
        event.registerAboveAll(Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "thaumcraft_hud"), THAUMCRAFT_HUD);
        Thaumcraft.LOGGER.info("Registered Thaumcraft HUD overlay");
    }

    /**
     * Dev-only verification hook: with {@code ~/.thaumcraft_hud_debug} present the gauge is
     * drawn with a fake aura even when no thaumometer is held, so the draw path can be
     * verified in a headless dev client. Inert in production (no one creates that file).
     */
    private static boolean debugHudForced() {
        try {
            return java.nio.file.Files.exists(java.nio.file.Path.of(System.getProperty("user.home"), ".thaumcraft_hud_debug"));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * The main Thaumcraft HUD layer.
     *
     * 1.12 HudHandler.renderHuds order: knowledge-gain icons first (drawn even with a GUI
     * open, like 1.12's renderHudsInGUI), then the hand gauges - only while the in-game
     * HUD is visible (no screen open), caster dial first (then +33), thaumometer gauge
     * (+80), sanity checker (+75); each HUD is drawn at most once.
     */
    public static final GuiLayer THAUMCRAFT_HUD = (graphics, deltaTracker) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        int ww = mc.getWindow().getGuiScaledWidth();
        int hh = mc.getWindow().getGuiScaledHeight();

        // 1.12 renderHudsInGUI: drawn regardless of open screens
        if (kgFade > 0.0f) {
            renderKnowledgeGains(graphics, player, deltaTracker.getGameTimeDeltaPartialTick(false), ww, hh);
        }

        // 1.12: hand gauges only when inGameHasFocus && isGuiEnabled (no screen open)
        boolean anyThaumometer = false;
        if (mc.gui.screen() == null) {
            boolean drewCaster = false;
            boolean drewThaum = false;
            boolean drewSanity = false;
            int yOffset = 0;

            for (int hand = 0; hand < 2; hand++) {
                ItemStack stack = hand == 0 ? player.getMainHandItem() : player.getOffhandItem();
                if (stack.isEmpty()) continue;

                if (!drewCaster && stack.getItem() instanceof ICaster) {
                    renderCasterHud(graphics, mc, player, stack, yOffset, deltaTracker);
                    drewCaster = true;
                    // 1.12: start += 33 (ModConfig.CONFIG_GRAPHICS.dialBottom is false by default)
                    yOffset += 33;
                } else if (!drewThaum && stack.getItem() instanceof ItemThaumometer) {
                    renderThaumometerHud(graphics, mc, player, yOffset, deltaTracker);
                    drewThaum = true;
                    yOffset += 80;
                } else if (!drewSanity && stack.getItem() instanceof ItemSanityChecker) {
                    renderSanityHud(graphics, player, yOffset);
                    drewSanity = true;
                    yOffset += 75;
                }
            }
            anyThaumometer = drewThaum;
        }

        if (!anyThaumometer && mc.gui.screen() == null && debugHudForced()) {
            renderThaumometerHud(graphics, mc, player, 0, deltaTracker,
                    new AuraChunk(null, (short) 400, 250.0f, 30.0f));
        }

    };

    /**
     * Render the thaumometer aura gauge HUD (reads {@link #currentAura}).
     */
    private static void renderThaumometerHud(GuiGraphicsExtractor graphics, Minecraft mc, Player player,
                                              int yOffset, DeltaTracker deltaTracker) {
        renderThaumometerHud(graphics, mc, player, yOffset, deltaTracker, currentAura);
    }

    /**
     * Render the thaumometer aura gauge HUD from the given aura.
     * 1:1 port of 1.12 HudHandler.renderThaumometerHud.
     */
    private static void renderThaumometerHud(GuiGraphicsExtractor graphics, Minecraft mc, Player player,
                                              int yOffset, DeltaTracker deltaTracker, AuraChunk aura) {

        // 1.12: glTranslated(2.0, shifty, 0.0) wraps every quad below, so x = 2 + <quad x>,
        // y = yOffset + <quad y>.
        final int x = 2;
        final int hudH = 64; // gauge travel is 64px in hud.png
        float count = mc.player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(false);
        float count2 = mc.player.tickCount / 3.0f + deltaTracker.getGameTimeDeltaPartialTick(false);

        float base = Mth.clamp(aura.getBase() / MAX_VIS, 0.0f, 1.0f);
        float vis = Mth.clamp(aura.getVis() / MAX_VIS, 0.0f, 1.0f);
        float flux = Mth.clamp(aura.getFlux() / MAX_VIS, 0.0f, 1.0f);
        if (flux + vis > 1.0f) {
            float m = 1.0f / (flux + vis);
            base *= m;
            vis *= m;
            flux *= m;
        }
        // 1.12 shows the numeric read-out while sneaking (player.isSneaking())
        boolean readout = player.isCrouching();

        // Vis column: glTranslated(5, 10 + (1 - vis) * 64) then glScaled(1, vis) over an 8x64 quad
        float start = 10.0f + (1.0f - vis) * hudH;
        if (vis > 0.0f) {
            int visH = (int) (vis * hudH);
            blitHud(graphics, x + 5, (int) (yOffset + start), 88, 56, 8, hudH, 8, visH, 0xFFB266E5);
            // white shimmer, source row scrolls with the tick counter
            // (1.12 draws this additive 50% white; the GuiLayer pipeline only alpha-blends)
            blitHud(graphics, x + 5, (int) (yOffset + start), 96, (int) (56 + count % 64.0f),
                    8, visH, 8, visH, 0x7FFFFFFF);
            if (readout) {
                drawSmallText(graphics, mc, DECIMAL_FORMAT.format(aura.getVis()),
                        x + 16, (int) (yOffset + start), 0xFFEEAAFF);
            }
        }

        // Flux column sits directly under the vis column
        if (flux > 0.0f) {
            start = 10.0f + (1.0f - flux - vis) * hudH;
            int fluxH = (int) (flux * hudH);
            blitHud(graphics, x + 5, (int) (yOffset + start), 88, 56, 8, hudH, 8, fluxH, 0xFF401A4C);
            blitHud(graphics, x + 5, (int) (yOffset + start), 104, (int) (120 - count2 % 64.0f),
                    8, fluxH, 8, fluxH, 0x7FB266FF);
            if (readout) {
                drawSmallText(graphics, mc, DECIMAL_FORMAT.format(aura.getFlux()),
                        x + 16, (int) (yOffset + start) - 4, 0xFFAA11BB);
            }
        }

        // Gauge frame: quad(1, 1, 72, 48, 16, 80)
        blitHud(graphics, x + 1, yOffset + 1, 72, 48, 16, 80, 16, 80, 0xFFFFFFFF);

        // Base (natural aura) pip: quad(2, 8 + (1 - base) * 64, 117, 61, 14, 5)
        int baseStart = 8 + (int) ((1.0f - base) * hudH);
        blitHud(graphics, x + 2, yOffset + baseStart, 117, 61, 14, 5, 14, 5, 0xFFFFFFFF);
    }

    /**
     * Render the caster gauntlet HUD - 1:1 port of 1.12 HudHandler.renderCastingWandHud.
     *
     * 1.12 transforms, in order: translate(0, shifty), the 64x64 dial quad scaled 0.5,
     * translate(16, 16) (dial centre), then translate(16, -10) + scale 0.5 for the vis gauge,
     * the full-size 16x42 gauge frame at the untranslated (16, -10) origin, and the sneaking
     * read-outs rotated -90 around (32, 6).
     */
    private static void renderCasterHud(GuiGraphicsExtractor graphics, Minecraft mc, Player player,
                                         ItemStack casterStack, int yOffset, DeltaTracker deltaTracker) {

        ICaster caster = (ICaster) casterStack.getItem();

        int max = currentAura.getBase();
        int amt = (int) currentAura.getVis();
        ItemFocus focus = (ItemFocus) caster.getFocus(casterStack);
        ItemStack focusStack = caster.getFocusStack(casterStack);

        // Dial: glScaled(0.5) over quad(0, 0, 0, 0, 64, 64) -> 32x32 at the top-left corner
        blitHud(graphics, 0, yOffset, 0, 0, 64, 64, 32, 32, 0xFFFFFFFF);

        // Vis gauge: 1.12 translate(16,16) then translate(16,-10) -> (32, 6), then glScaled(0.5)
        final int gx = 32;
        final int gy = 6;
        int loc = max > 0 ? (int) (30.0f * amt / max) : 0;
        loc = Mth.clamp(loc, 0, 30);

        if (loc > 0) {
            Color ac = new Color(Aspect.ENERGY.getColor());
            int color = (0xCC << 24) | (ac.getRed() << 16) | (ac.getGreen() << 8) | ac.getBlue();
            // quad(-4, 35 - loc, 104, 0, 8, loc) at half scale -> (30, 6 + (35-loc)/2), 4 x loc/2
            blitHud(graphics, gx - 2, yOffset + gy + (35 - loc) / 2, 104, 0, 8, loc, 4, loc / 2, color);
        }
        // Gauge frame: quad(-8, -3, 72, 0, 16, 42) at the unscaled (32, 6) origin -> (24, 3), 16x42
        blitHud(graphics, gx - 8, yOffset + gy - 3, 72, 0, 16, 42, 16, 42, 0xFFFFFFFF);

        // 1.12: read-outs only while sneaking (player.isSneaking())
        if (player.isCrouching()) {
            Matrix3x2fStack pose = graphics.pose();
            // glRotatef(-90, 0, 0, 1) around (32, 6): the vis read-out runs upwards
            pose.pushMatrix();
            pose.translate((float) gx, (float) (yOffset + gy));
            // graphics.pose() is a JOML Matrix3x2fStack, so rotate() takes radians
            pose.rotate(-Mth.HALF_PI);
            graphics.text(mc.font, DECIMAL_FORMAT.format(amt), -32, -4, 0xFFFFFFFF, false);

            if (focus != null && focus.getVisCost(focusStack) > 0.0f) {
                // 1.12: still in the rotated space, at (-32 - width/2, 32) -> runs upwards to
                // the right of the dial
                String msg = DECIMAL_FORMAT.format(focus.getVisCost(focusStack)
                        * caster.getConsumptionModifier(casterStack, player, false));
                graphics.text(mc.font, msg, -32 - mc.font.width(msg) / 2, 32, 0xFFFFFFFF, false);
            }
            pose.popMatrix();
        }

        if (focus != null && !focusStack.isEmpty()) {
            // translate(16,16) then translate(-24,-24) then renderItemAndEffectIntoGUI(.., 16, 16)
            // -> the 16x16 focus icon at (8, 8) relative to the dial
            graphics.item(focusStack, 8, yOffset + 8);
        }
    }

    /**
     * Render the sanity checker warp gauge - 1:1 port of 1.12 HudHandler.renderSanityHud.
     * Drawn at (0, shifty): 20x76 background, three coloured warp fills (temporary / normal /
     * permanent) inside the 20x76 frame, and a jittering warning icon at 100+ warp.
     */
    private static void renderSanityHud(GuiGraphicsExtractor graphics, Player player, int yOffset) {
        // 1.12 background: quad(1, 1, 152, 0, 20, 76)
        blitHud(graphics, 1, yOffset + 1, 152, 0, 20, 76, 20, 76, 0xFFFFFFFF);

        thaumcraft.api.capabilities.IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player).orElse(null);
        int p = warp != null ? warp.get(thaumcraft.api.capabilities.IPlayerWarp.EnumWarpType.PERMANENT) : 0;
        int s = warp != null ? warp.get(thaumcraft.api.capabilities.IPlayerWarp.EnumWarpType.NORMAL) : 0;
        int t = warp != null ? warp.get(thaumcraft.api.capabilities.IPlayerWarp.EnumWarpType.TEMPORARY) : 0;

        float tw = (float) (p + s + t);
        float mod = 1.0f;
        if (tw > 100.0f) {
            mod = 100.0f / tw;
            tw = 100.0f;
        }
        int gap = (int) ((100.0f - tw) / 100.0f * 48.0f);
        int wt = (int) (t / 100.0f * 48.0f * mod);
        int ws = (int) (s / 100.0f * 48.0f * mod);

        if (t > 0) {
            // quad(7, 21 + gap, 200, gap, 8, wt + gap)
            blitHud(graphics, 7, yOffset + 21 + gap, 200, gap, 8, wt + gap, 8, wt + gap, 0xFFFF80FF);
        }
        if (s > 0) {
            // quad(7, 21 + wt + gap, 200, wt + gap, 8, wt + ws + gap)
            blitHud(graphics, 7, yOffset + 21 + wt + gap, 200, wt + gap, 8, wt + ws + gap, 8, wt + ws + gap, 0xFFBF00BF);
        }
        if (p > 0) {
            // quad(7, 21 + wt + ws + gap, 200, wt + ws + gap, 8, 48)
            blitHud(graphics, 7, yOffset + 21 + wt + ws + gap, 200, wt + ws + gap, 8, 48, 8, 48, 0xFF800080);
        }

        // 1.12 frame: quad(1, 1, 176, 0, 20, 76)
        blitHud(graphics, 1, yOffset + 1, 176, 0, 20, 76, 20, 76, 0xFFFFFFFF);

        if (tw >= 100.0f) {
            // glScaled(0.75) + glTranslated(rand01, rand01) over quad(3, 3, 216, 0, 20, 16)
            int jx = HUD_RANDOM.nextInt(2);
            int jy = HUD_RANDOM.nextInt(2);
            blitHud(graphics, 2 + jx, yOffset + 2 + jy, 216, 0, 20, 16, 15, 12, 0xFFFFFFFF);
        }
    }

    /**
     * Render the knowledge-gain animation - 1:1 port of 1.12 HudHandler.renderKnowledgeGains.
     * The book fades in the bottom-right corner while knowledge icons pop in, drift towards
     * it, shrink into it and flash.
     */
    private static void renderKnowledgeGains(GuiGraphicsExtractor graphics, Player player, float partialTicks,
                                              int ww, int hh) {
        Matrix3x2fStack pose = graphics.pose();
        int bookAlpha = (int) (Mth.clamp(kgFade / 40.0f, 0.0f, 1.0f) * 255.0f);

        // 1.12: full-texture book quad at (ww - 17, hh - 17) with alpha kgFade/40
        // (the port's icon is 32x32, blitted down to the 1.12 16x16 footprint)
        pose.pushMatrix();
        graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_TEXTURE, ww - 17, hh - 17, 0, 0, 16, 16, 32, 32,
                (bookAlpha << 24) | 0xFFFFFF);

        LinkedBlockingQueue<KnowledgeGainTracker> temp = new LinkedBlockingQueue<>();
        int a = 0;
        while (!knowledgeGainTrackers.isEmpty()) {
            KnowledgeGainTracker current = knowledgeGainTrackers.poll();
            if (current == null) break;

            Identifier typeTex = current.type == IPlayerKnowledge.EnumKnowledgeType.THEORY
                    ? KNOW_THEORY : KNOW_OBSERVATION;
            Random rand = new Random(current.seed);

            pose.pushMatrix();
            float s = 16.0f;
            float x = ww / 4f + rand.nextInt(32);
            float y = hh / 3f + rand.nextInt(32);
            float wot = 0.0f;
            if (current.progress < current.max * 0.66f) {
                float q = (current.progress - partialTicks) / (current.max * 0.66f);
                s *= q;
                float m = (float) (Math.sin(q * Math.PI - Mth.HALF_PI) * 0.5f + 0.5f);
                y *= m;
                float d = (float) Math.sin(m * Math.PI * 0.5f);
                x *= d;
            } else {
                wot = current.max - current.progress + partialTicks;
                float wot2 = wot / (current.max * 0.33f);
                float m = (float) (Math.sin(wot2 * Math.PI * 2.0f - Mth.HALF_PI) * 0.5f + 1.5f);
                if (wot2 < 0.5f) {
                    s *= wot2 * 2.0f;
                }
                s *= m;
            }
            float xx = ww - 12 + rand.nextInt(8) - x;
            float yy = hh - 12 + rand.nextInt(8) - y;

            // 1.12 GUI sparkles; current.sparks is never set true in 1.12 (kept 1:1)
            if (current.sparks && HUD_RANDOM.nextInt((int) (1.0f + current.progress / (float) current.max * 10.0f)) == 0) {
                float r = 255.0f / 255.0f;
                float g = (189 + HUD_RANDOM.nextInt(67)) / 255.0f;
                float b = (64 + HUD_RANDOM.nextInt(192)) / 255.0f;
                FXDispatcher.INSTANCE.drawSimpleSparkleGui(HUD_RANDOM,
                        xx + HUD_RANDOM.nextGaussian() * 5.0, yy + HUD_RANDOM.nextGaussian() * 5.0,
                        HUD_RANDOM.nextGaussian(), HUD_RANDOM.nextGaussian(),
                        24.0f, r, g, b, HUD_RANDOM.nextInt(5), 0.9f, -1.0f);
            }

            // glTranslatef(xx, yy, -80 + a) + glRotatef(84 + rand12, 0, 0, -1) + the centered
            // 16x16 quad scaled to s (blit x/y are whole pixels, so scale the pose instead)
            pose.translate(xx, yy);
            pose.rotate(-((84 + rand.nextInt(12)) * (float) Math.PI / 180.0f));
            pose.scale(s / 16.0f, s / 16.0f);
            graphics.blit(RenderPipelines.GUI_TEXTURED, typeTex, -8, -8, 0, 0, 16, 16, 16, 16, 0xFFFFFFFF);
            if (current.category != null && current.category.icon != null) {
                // 1.12: category icon at 0.75 scale, one unit closer to the camera
                pose.scale(0.75f, 0.75f);
                graphics.blit(RenderPipelines.GUI_TEXTURED, current.category.icon, -8, -8, 0, 0, 16, 16, 16, 16, 0xFFFFFFFF);
            }
            if (current.progress > current.max * 0.9f) {
                float wot3 = wot / (current.max * 0.1f);
                float m2 = (float) (Math.sin(wot3 * Math.PI * 2.0f - Mth.HALF_PI) * 0.25f + 0.25f);
                drawParticlePop(graphics, pose, rand, 64.0f * m2);
            }
            if (current.progress < current.max * 0.1f) {
                float wot3 = 1.0f - (current.progress - partialTicks) / (current.max * 0.1f);
                float m2 = (float) (Math.sin(wot3 * Math.PI * 2.0f - Mth.HALF_PI) * 0.25f + 0.25f);
                drawParticlePop(graphics, pose, rand, 32.0f * m2);
            }
            temp.offer(current);
            pose.popMatrix();
            a++;
        }
        while (!temp.isEmpty()) {
            knowledgeGainTrackers.offer(temp.poll());
        }
        pose.popMatrix();
    }

    /**
     * 1.12's final "pop": a random particle sprite (64x64 grid, row 5) from the particle
     * texture, rotated and stretched to the pop size (additive fullbright in 1.12). The
     * port's particle texture is 1024x1024 (16px grid cells), so the same grid cell maps to
     * a doubled pixel region.
     */
    private static void drawParticlePop(GuiGraphicsExtractor graphics, Matrix3x2fStack pose,
                                        Random rand, float size) {
        if (size <= 0.0f) return;
        pose.pushMatrix();
        pose.rotate(rand.nextInt(360) * (float) Math.PI / 180.0f);
        pose.scale(size / 16.0f, size / 16.0f);
        int frame = 320 + rand.nextInt(16);
        int xm = (frame % 64) * (1024 / 64);
        int ym = (frame / 64) * (1024 / 64);
        graphics.blit(RenderPipelines.GUI_TEXTURED, PARTICLE_TEXTURE, -8, -8, xm, ym, 16, 16, 1024, 1024, 0xFFFFFFFF);
        pose.popMatrix();
    }

    /**
     * Blit a rectangle out of the 256x256 hud.png, 1.12 UtilsFX.drawTexturedQuad style:
     * the source region (su,sv,sw,sh) is stretched to the drawn size (dw,dh).
     */
    private static void blitHud(GuiGraphicsExtractor graphics, int x, int y, int su, int sv,
                                int sw, int sh, int dw, int dh, int color) {
        if (dw <= 0 || dh <= 0) return;
        graphics.blit(RenderPipelines.GUI_TEXTURED, HUD_TEXTURE, x, y, su, sv, dw, dh, 256, 256, color);
    }

    /** 1.12 drew the sneaking read-out with glScaled(0.5, 0.5, 0.5). */
    private static void drawSmallText(GuiGraphicsExtractor graphics, Minecraft mc, String msg,
                                      int x, int y, int color) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.scale(0.5f, 0.5f);
        graphics.text(mc.font, msg, 0, 0, color, false);
        pose.popMatrix();
    }

    /**
     * Update the current aura data (called from packet handler).
     */
    public static void updateAura(AuraChunk aura) {
        currentAura = aura;
    }

    /**
     * Update aura values directly.
     */
    public static void updateAura(float base, float vis, float flux) {
        if (currentAura == null) {
            currentAura = new AuraChunk(null, (short) 0, vis, flux);
        }
        currentAura.setBase((short) base);
        currentAura.setVis(vis);
        currentAura.setFlux(flux);
    }

    /**
     * 1.12 HudHandler.KnowledgeGainTracker, 1:1.
     */
    public static class KnowledgeGainTracker {
        public final IPlayerKnowledge.EnumKnowledgeType type;
        public final ResearchCategory category;
        public int progress;
        public final int max;
        public final long seed;
        public boolean sparks;

        public KnowledgeGainTracker(IPlayerKnowledge.EnumKnowledgeType type, ResearchCategory category,
                                     int progress, long seed) {
            sparks = false;
            this.type = type;
            this.category = category;
            if (type == IPlayerKnowledge.EnumKnowledgeType.THEORY) {
                progress += 10;
            }
            this.progress = progress;
            max = progress;
            this.seed = seed;
        }
    }
}
