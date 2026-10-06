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
 * Ported to the 26.2 NeoForge GuiLayer system (RegisterGuiLayersEvent).
 * NOTE: texture-based rendering was stubbed to plain colored bars for the 26.2
 * GUI render-state rewrite.
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
    
    /**
     * The main Thaumcraft HUD layer.
     */
    public static final GuiLayer THAUMCRAFT_HUD = (graphics, deltaTracker) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null) return;
        
        int yOffset = 0;
        
        // Check main hand and off hand for Thaumcraft items
        for (int hand = 0; hand < 2; hand++) {
            ItemStack stack = hand == 0 ? player.getMainHandItem() : player.getOffhandItem();
            
            if (stack.isEmpty()) continue;
            
            if (stack.getItem() instanceof ICaster) {
                renderCasterHud(graphics, mc, player, stack, yOffset, deltaTracker);
                yOffset += 36;
            } else if (stack.getItem() instanceof ItemThaumometer) {
                renderThaumometerHud(graphics, mc, player, yOffset, deltaTracker);
                yOffset += 80;
            }
        }
    };
    
    /**
     * Render the thaumometer aura gauge HUD.
     */
    private static void renderThaumometerHud(GuiGraphicsExtractor graphics, Minecraft mc, Player player, 
                                              int yOffset, DeltaTracker deltaTracker) {
        
        // ---- 1.12 HudHandler.renderThaumometerHud, 1:1 ----
        // GL11.glTranslated(2.0, shifty, 0.0) wraps every quad below, so x = 2 + <quad x>.
        final int x = 2;
        final int hudH = 64; // gauge travel is 64px in hud.png
        float count = mc.player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(false);
        float count2 = mc.player.tickCount / 3.0f + deltaTracker.getGameTimeDeltaPartialTick(false);

        float base = Mth.clamp(currentAura.getBase() / MAX_VIS, 0.0f, 1.0f);
        float vis = Mth.clamp(currentAura.getVis() / MAX_VIS, 0.0f, 1.0f);
        float flux = Mth.clamp(currentAura.getFlux() / MAX_VIS, 0.0f, 1.0f);
        if (flux + vis > 1.0f) {
            float m = 1.0f / (flux + vis);
            base *= m;
            vis *= m;
            flux *= m;
        }

        // Vis column: glTranslated(5, 10 + (1 - vis) * 64) then glScaled(1, vis) over an 8x64 quad
        float start = 10.0f + (1.0f - vis) * hudH;
        if (vis > 0.0f) {
            int visH = (int) (vis * hudH);
            blitHud(graphics, x + 5, (int) (yOffset + start), 88, 56, 8, hudH, 8, visH, 0xFFB266E5);
            // additive white shimmer, source row scrolls with the tick counter
            blitHud(graphics, x + 5, (int) (yOffset + start), 96, (int) (56 + count % 64.0f),
                    8, visH, 8, visH, 0x7FFFFFFF);
            if (player.isShiftKeyDown()) {
                drawSmallText(graphics, mc, DECIMAL_FORMAT.format(currentAura.getVis()),
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
            if (player.isShiftKeyDown()) {
                drawSmallText(graphics, mc, DECIMAL_FORMAT.format(currentAura.getFlux()),
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
     * Render the caster gauntlet HUD.
     */
    private static void renderCasterHud(GuiGraphicsExtractor graphics, Minecraft mc, Player player,
                                         ItemStack casterStack, int yOffset, DeltaTracker deltaTracker) {
        
        ICaster caster = (ICaster) casterStack.getItem();
        
        int x = 2;
        int y = yOffset + 2;
        
        // Get aura vis for the gauge
        float maxVis = currentAura != null ? currentAura.getBase() : 100;
        float currentVis = currentAura != null ? currentAura.getVis() : 50;
        
        // Dial/focus rendering — see HUD note above: textured dial blit is skipped
        // pending the 26.2 RenderPipeline texture wiring.
        
        // Draw vis gauge
        int gaugeHeight = 30;
        float visRatio = Mth.clamp(currentVis / Math.max(maxVis, 1), 0, 1);
        int filledHeight = (int) (gaugeHeight * visRatio);
        
        // Vis bar position (to the right of the dial)
        int barX = x + 34;
        int barY = y + 2;
        
        // Draw gauge background
        graphics.fill(barX, barY, barX + 8, barY + 42, 0x40000000);
        
        // Draw vis fill with aspect color
        Color visColor = new Color(Aspect.ENERGY.getColor());
        int fillY = barY + 3 + (int)((1 - visRatio) * 15);
        graphics.fill(barX + 2, fillY, barX + 6, fillY + (int)(15 * visRatio), 
                (visColor.getRed() << 16) | (visColor.getGreen() << 8) | visColor.getBlue() | 0xCC000000);
        
        // Show current vis amount if sneaking
        if (player.isShiftKeyDown()) {
            String visStr = DECIMAL_FORMAT.format(currentVis);
            graphics.text(mc.font, visStr, barX - 8, barY + 22, 0xFFFFFFFF, false);
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
