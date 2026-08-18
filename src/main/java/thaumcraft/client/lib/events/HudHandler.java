package thaumcraft.client.lib.events;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterGuiOverlaysEvent;
import net.neoforged.neoforge.client.gui.overlay.IGuiOverlay;
import net.neoforged.neoforge.client.gui.overlay.IGuiOverlay;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import org.joml.Matrix4f;
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
 * Ported from 1.12.2 to use 1.20.1 GUI overlay system.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class HudHandler {

    private static final Identifier HUD_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/hud.png");
    
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#######.#");
    
    // Current aura data (updated by packets from server)
    public static AuraChunk currentAura = new AuraChunk(null, (short) 0, 0.0f, 0.0f);
    
    // Max vis for gauge scaling
    private static final float MAX_VIS = 500.0f;
    
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        // Register the Thaumcraft HUD overlay
        event.registerAboveAll("thaumcraft_hud", THAUMCRAFT_HUD);
        Thaumcraft.LOGGER.info("Registered Thaumcraft HUD overlay");
    }
    
    /**
     * The main Thaumcraft HUD overlay.
     */
    public static final IGuiOverlay THAUMCRAFT_HUD = (gui, graphics, partialTick, screenWidth, screenHeight) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null || mc.options.hideGui) return;
        
        int yOffset = 0;
        
        // Check main hand and off hand for Thaumcraft items
        for (int hand = 0; hand < 2; hand++) {
            ItemStack stack = hand == 0 ? player.getMainHandItem() : player.getOffhandItem();
            
            if (stack.isEmpty()) continue;
            
            if (stack.getItem() instanceof ICaster) {
                renderCasterHud(graphics, mc, player, stack, yOffset, partialTick);
                yOffset += 36;
            } else if (stack.getItem() instanceof ItemThaumometer) {
                renderThaumometerHud(graphics, mc, player, yOffset, partialTick);
                yOffset += 80;
            }
        }
    };
    
    /**
     * Render the thaumometer aura gauge HUD.
     */
    private static void renderThaumometerHud(GuiGraphics graphics, Minecraft mc, Player player, 
                                              int yOffset, float partialTicks) {
        
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
        
        // Draw background frame
        RenderSystem.setShaderTexture(0, HUD_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Draw the gauge frame (from texture)
        graphics.blit(HUD_TEXTURE, x, y, 72, 48, 16, 80);
        
        // Draw vis bar (purple)
        if (visNorm > 0) {
            int visHeight = (int) (gaugeHeight * visNorm);
            int visY = y + 10 + (gaugeHeight - visHeight);
            drawColoredRect(graphics, x + 5, visY, gaugeWidth, visHeight, 0xB0664499);
            
            // Animated shimmer effect
            float shimmer = (player.tickCount + partialTicks) % 64;
            drawColoredRect(graphics, x + 5, visY, gaugeWidth, visHeight, 
                    0x40FFFFFF, shimmer / 64f);
        }
        
        // Draw flux bar (dark purple) below vis
        if (fluxNorm > 0) {
            int fluxHeight = (int) (gaugeHeight * fluxNorm);
            int fluxY = y + 10 + (int)(gaugeHeight * (1 - visNorm - fluxNorm));
            drawColoredRect(graphics, x + 5, fluxY, gaugeWidth, fluxHeight, 0xB0331144);
        }
        
        // Draw base marker line
        int baseY = y + 8 + (int)((1 - baseNorm) * gaugeHeight);
        drawColoredRect(graphics, x + 2, baseY, 14, 2, 0xFFFFFFFF);
        
        // Draw values if sneaking
        if (player.isShiftKeyDown()) {
            Font font = mc.font;
            graphics.pose().pushPose();
            graphics.pose().scale(0.5f, 0.5f, 1.0f);
            
            int textX = (x + 18) * 2;
            int visTextY = (y + 20) * 2;
            int fluxTextY = (y + 40) * 2;
            
            graphics.drawString(font, DECIMAL_FORMAT.format(vis), textX, visTextY, 0xEE99FF, false);
            graphics.drawString(font, DECIMAL_FORMAT.format(flux), textX, fluxTextY, 0xAA33BB, false);
            
            graphics.pose().popPose();
        }
        
        RenderSystem.disableBlend();
    }
    
    /**
     * Render the caster gauntlet HUD.
     */
    private static void renderCasterHud(GuiGraphics graphics, Minecraft mc, Player player,
                                         ItemStack casterStack, int yOffset, float partialTicks) {
        
        ICaster caster = (ICaster) casterStack.getItem();
        
        int x = 2;
        int y = yOffset + 2;
        
        // Get aura vis for the gauge
        float maxVis = currentAura != null ? currentAura.getBase() : 100;
        float currentVis = currentAura != null ? currentAura.getVis() : 50;
        
        RenderSystem.setShaderTexture(0, HUD_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Draw dial background (scaled to 0.5)
        graphics.pose().pushPose();
        graphics.pose().scale(0.5f, 0.5f, 1.0f);
        graphics.blit(HUD_TEXTURE, x * 2, y * 2, 0, 0, 64, 64);
        graphics.pose().popPose();
        
        // Draw vis gauge
        int gaugeHeight = 30;
        float visRatio = Mth.clamp(currentVis / Math.max(maxVis, 1), 0, 1);
        int filledHeight = (int) (gaugeHeight * visRatio);
        
        // Vis bar position (to the right of the dial)
        int barX = x + 34;
        int barY = y + 2;
        
        // Draw gauge background
        graphics.pose().pushPose();
        graphics.pose().scale(0.5f, 0.5f, 1.0f);
        graphics.blit(HUD_TEXTURE, barX * 2, barY * 2, 72, 0, 16, 42);
        graphics.pose().popPose();
        
        // Draw vis fill with aspect color
        Color visColor = new Color(Aspect.ENERGY.getColor());
        int fillY = barY + 3 + (int)((1 - visRatio) * 15);
        drawColoredRect(graphics, barX + 2, fillY, 4, (int)(15 * visRatio), 
                (visColor.getRed() << 16) | (visColor.getGreen() << 8) | visColor.getBlue() | 0xCC000000);
        
        // Draw focus if equipped
        ItemStack focusStack = caster.getFocusStack(casterStack);
        if (focusStack != null && !focusStack.isEmpty() && focusStack.getItem() instanceof ItemFocus focus) {
            // Render focus item
            graphics.renderItem(focusStack, x + 4, y + 4);
            
            // Show vis cost if sneaking
            if (player.isShiftKeyDown()) {
                float visCost = focus.getVisCost(focusStack);
                if (visCost > 0) {
                    float mod = caster.getConsumptionModifier(casterStack, player, false);
                    String costStr = DECIMAL_FORMAT.format(visCost * mod);
                    
                    graphics.pose().pushPose();
                    graphics.pose().scale(0.5f, 0.5f, 1.0f);
                    int textX = (x + 24) * 2;
                    int textY = (y + 24) * 2;
                    graphics.drawString(mc.font, costStr, textX, textY, 0xFFFFFF, false);
                    graphics.pose().popPose();
                }
            }
        }
        
        // Show current vis amount if sneaking
        if (player.isShiftKeyDown()) {
            graphics.pose().pushPose();
            graphics.pose().scale(0.5f, 0.5f, 1.0f);
            String visStr = DECIMAL_FORMAT.format(currentVis);
            graphics.drawString(mc.font, visStr, (barX - 8) * 2, (barY + 22) * 2, 0xFFFFFF, false);
            graphics.pose().popPose();
        }
        
        RenderSystem.disableBlend();
    }
    
    /**
     * Draw a colored rectangle.
     */
    private static void drawColoredRect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, color);
    }
    
    /**
     * Draw a colored rectangle with alpha based on animation.
     */
    private static void drawColoredRect(GuiGraphics graphics, int x, int y, int width, int height, 
                                         int color, float alpha) {
        int a = (int)(((color >> 24) & 0xFF) * alpha);
        int finalColor = (a << 24) | (color & 0x00FFFFFF);
        graphics.fill(x, y, x + width, y + height, finalColor);
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
