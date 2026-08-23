package thaumcraft.client.lib.events;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
 * GUI render-state rewrite; see TODO below.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class HudHandler {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#######.#");
    
    // Current aura data (updated by packets from server)
    public static AuraChunk currentAura = new AuraChunk(null, (short) 0, 0.0f, 0.0f);
    
    // Max vis for gauge scaling
    private static final float MAX_VIS = 500.0f;
    
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
        
        int x = 2;
        int y = yOffset + 2;
        
        // Get aura values
        float base = currentAura != null ? currentAura.getBase() : 100;
        float vis = currentAura != null ? currentAura.getVis() : 50;
        float flux = currentAura != null ? currentAura.getFlux() : 10;
        
        // Normalize to 0-1 range
        float visNorm = Mth.clamp(vis / MAX_VIS, 0, 1);
        float fluxNorm = Mth.clamp(flux / MAX_VIS, 0, 1);
        float baseNorm = Mth.clamp(base / MAX_VIS, 0, 1);
        
        // Scale if total exceeds 1
        if (visNorm + fluxNorm > 1) {
            float scale = 1.0f / (visNorm + fluxNorm);
            visNorm *= scale;
            fluxNorm *= scale;
        }
        
        int gaugeHeight = 64;
        int gaugeWidth = 8;
        
        // TODO(26.2): restore textured HUD frame rendering via RenderPipelines.GUI_TEXTURED blit
        
        // Draw vis bar (purple)
        if (visNorm > 0) {
            int visHeight = (int) (gaugeHeight * visNorm);
            int visY = y + 10 + (gaugeHeight - visHeight);
            graphics.fill(x + 5, visY, x + 5 + gaugeWidth, visY + visHeight, 0xB0664499);
        }
        
        // Draw flux bar (dark purple) below vis
        if (fluxNorm > 0) {
            int fluxHeight = (int) (gaugeHeight * fluxNorm);
            int fluxY = y + 10 + (int)(gaugeHeight * (1 - visNorm - fluxNorm));
            graphics.fill(x + 5, fluxY, x + 5 + gaugeWidth, fluxY + fluxHeight, 0xB0331144);
        }
        
        // Draw base marker line
        int baseY = y + 8 + (int)((1 - baseNorm) * gaugeHeight);
        graphics.fill(x + 2, baseY, x + 16, baseY + 2, 0xFFFFFFFF);
        
        // Draw values if sneaking
        if (player.isShiftKeyDown()) {
            Font font = mc.font;
            int textX = x + 18;
            int visTextY = y + 20;
            int fluxTextY = y + 40;
            
            graphics.text(font, DECIMAL_FORMAT.format(vis), textX, visTextY, 0xEE99FF, false);
            graphics.text(font, DECIMAL_FORMAT.format(flux), textX, fluxTextY, 0xAA33BB, false);
        }
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
        
        // TODO(26.2): restore dial/focus rendering via the new GUI render-state API
        
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
            graphics.text(mc.font, visStr, barX - 8, barY + 22, 0xFFFFFF, false);
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
