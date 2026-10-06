package thaumcraft.client.lib.events;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.ICaster;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.items.tools.ItemThaumometer;
import thaumcraft.common.world.aura.AuraChunk;

import java.awt.Color;
import java.text.DecimalFormat;

/**
 * HudHandler - Renders Thaumcraft HUD overlays.
 * 
 * Displays:
 * - Thaumometer aura gauge (vis/flux levels)
 * - Caster gauntlet vis gauge and focus info
 * - Sanity checker warp levels
 * 
 * Ported to the 26.3 NeoForge GuiLayer system (RegisterGuiLayersEvent); every quad is the
 * 1.12 hud.png quad, same source rectangle and same drawn size.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class HudHandler {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#######.#");
    
    /** 1.12 HudHandler.HUD = thaumcraft:textures/gui/hud.png (byte-identical asset in this port). */
    public static final Identifier HUD_TEXTURE = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/hud.png");
    
    // Current aura data (updated by packets from server)
    public static AuraChunk currentAura = new AuraChunk(null, (short) 0, 0.0f, 0.0f);
    
    // 1.12 HudHandler.renderThaumometerHud divides the aura values by 525
    public static final float MAX_VIS = 525.0f;
    
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        // Register the Thaumcraft HUD overlay
        event.registerAboveAll(Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "thaumcraft_hud"), THAUMCRAFT_HUD);
        Thaumcraft.LOGGER.info("Registered Thaumcraft HUD overlay");
    }
    
    /** Throttled [TC-DIAG] render counter (only the 1st/101st/... render logs). */
    private static int hudRenderCount = 0;

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
     * 1.12 HudHandler.renderHud: the gauges stack from the top-left corner, caster dial
     * first (then +33), thaumometer gauge at the running offset. Same here.
     */
    public static final GuiLayer THAUMCRAFT_HUD = (graphics, deltaTracker) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        int yOffset = 0;
        boolean drewThaumometer = false;

        // Check main hand and off hand for Thaumcraft items (1.12 iterates both hands)
        for (int hand = 0; hand < 2; hand++) {
            ItemStack stack = hand == 0 ? player.getMainHandItem() : player.getOffhandItem();

            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof ICaster) {
                renderCasterHud(graphics, mc, player, stack, yOffset, deltaTracker);
                // 1.12: start += 33 (ModConfig.CONFIG_GRAPHICS.dialBottom is false by default)
                yOffset += 33;
            } else if (stack.getItem() instanceof ItemThaumometer) {
                renderThaumometerHud(graphics, mc, player, yOffset, deltaTracker);
                yOffset += 80;
                drewThaumometer = true;
            }
        }

        if (!drewThaumometer && debugHudForced()) {
            renderThaumometerHud(graphics, mc, player, yOffset, deltaTracker,
                    new AuraChunk(null, (short) 400, 250.0f, 30.0f));
        }

        hudRenderCount++;
        if (hudRenderCount % 100 == 1) {
            Thaumcraft.LOGGER.info("[TC-DIAG] hud layer rendered#{} holdingThaumometer={} auraBase={} auraVis={} auraFlux={}",
                    hudRenderCount, drewThaumometer, currentAura.getBase(),
                    (int) currentAura.getVis(), (int) currentAura.getFlux());
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
     */
    private static void renderThaumometerHud(GuiGraphicsExtractor graphics, Minecraft mc, Player player,
                                              int yOffset, DeltaTracker deltaTracker, AuraChunk aura) {

        // ---- 1.12 HudHandler.renderThaumometerHud, 1:1 ----
        // 1.12 inGame ortho is top-left origin (glOrtho(0, w, h, 0, ...): y=0 at the top of
        // the screen, y growing down), exactly like this GuiLayer's space, so the 1.12
        // offsets translate directly: glTranslated(2.0, shifty, 0.0) -> x = 2 + <quad x>,
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
        // 1.12 shows the numeric read-out in creative mode (player.isCreative())
        boolean readout = player.isCreative();

        // Vis column: glTranslated(5, 10 + (1 - vis) * 64) then glScaled(1, vis) over an 8x64 quad
        float start = 10.0f + (1.0f - vis) * hudH;
        if (vis > 0.0f) {
            int visH = (int) (vis * hudH);
            blitHud(graphics, x + 5, (int) (yOffset + start), 88, 56, 8, hudH, 8, visH, 0xFFB266E5);
            // additive white shimmer, source row scrolls with the tick counter
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
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.scale(0.5f, 0.5f);
        graphics.text(mc.font, msg, 0, 0, color, false);
        pose.popMatrix();
    }
    
    /**
     * Render the caster gauntlet HUD - 1.12 HudHandler.renderCastingWandHud, 1:1.
     *
     * 1.12 transforms, in order: translate(0, shifty), the 64x64 dial quad scaled 0.5,
     * translate(16, 16) (dial centre), then translate(16, -10) + scale 0.5 for the vis gauge,
     * and translate(-24, -24) for the 16x16 focus item. The absolute positions below are those
     * transforms folded out.
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

        // Vis gauge group: translate(16, 16) then translate(16, -10) then glScaled(0.5)
        final int gx = 32;
        final int gy = 6;
        int loc = max > 0 ? (int) (30.0f * amt / max) : 0;
        loc = Mth.clamp(loc, 0, 30);

        if (loc > 0) {
            Color ac = new Color(Aspect.ENERGY.getColor());
            int color = (0xCC << 24) | (ac.getRed() << 16) | (ac.getGreen() << 8) | ac.getBlue();
            // quad(-4, 35 - loc, 104, 0, 8, loc) at half scale
            blitHud(graphics, gx - 2, gy + (35 - loc) / 2, 104, 0, 8, loc, 4, loc / 2, color);
        }
        // Gauge frame: quad(-8, -3, 72, 0, 16, 42) at half scale
        blitHud(graphics, gx - 4, gy - 1, 72, 0, 16, 42, 8, 21, 0xFFFFFFFF);

        if (player.isCreative()) {
            var pose = graphics.pose();
            // 1.12: glRotatef(-90, 0, 0, 1) then drawString(-32, -4) - the read-out runs upwards
            pose.pushMatrix();
            pose.translate((float) gx, (float) gy);
            // graphics.pose() is a JOML Matrix3x2fStack, so rotate() takes radians
            pose.rotate(-Mth.HALF_PI);
            graphics.text(mc.font, DECIMAL_FORMAT.format(amt), -32, -4, 0xFFFFFFFF, false);
            pose.popMatrix();

            if (focus != null && focus.getVisCost(focusStack) > 0.0f) {
                String msg = DECIMAL_FORMAT.format(focus.getVisCost(focusStack)
                        * caster.getConsumptionModifier(casterStack, player, false));
                graphics.text(mc.font, msg, gx - 32 - mc.font.width(msg) / 2, gy + 32, 0xFFFFFFFF, false);
            }
        }

        if (focus != null && !focusStack.isEmpty()) {
            // translate(16,16) then translate(-24,-24) then renderItemAndEffectIntoGUI(.., 16, 16)
            graphics.item(focusStack, 8, yOffset + 8);
        }
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
}
