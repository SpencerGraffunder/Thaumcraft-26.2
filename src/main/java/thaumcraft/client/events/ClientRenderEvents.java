package thaumcraft.client.events;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.ISealDisplayer;
import thaumcraft.client.ThaumometerHUD;
import thaumcraft.client.renderers.SealRenderer;
import thaumcraft.client.renderers.ThaumometerTagRenderer;
import thaumcraft.common.items.tools.ItemThaumometer;

/**
 * ClientRenderEvents - Handles client-side rendering events.
 * 
 * Responsibilities:
 * - Render seals when player holds ISealDisplayer items
 * - Other visual effects (to be added)
 * 
 * Ported from 1.12.2 RenderEventHandler.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class ClientRenderEvents {
    
    /**
     * Called during world rendering to add custom world visuals.
     */
    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        // Check if player is holding an ISealDisplayer item
        if (isHoldingSealDisplayer(player)) {
            SealRenderer.renderSeals(
                event.getPoseStack(),
                event.getSubmitNodeCollector(),
                player
            );
        }

        // Check if player is holding a thaumometer with a scan target: draw the target's aspect
        // tags above it (1.12 RenderEventHandler.thaumTarget + drawTagsOnContainer).
        if (isHoldingThaumometer(player) && ThaumometerHUD.target != null) {
            ThaumometerTagRenderer.renderTags(
                event.getPoseStack(),
                event.getSubmitNodeCollector(),
                player
            );
        }
    }
    
    /**
     * Check if the player is holding a thaumometer (main or off hand).
     */
    private static boolean isHoldingThaumometer(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem() instanceof ItemThaumometer) {
            return true;
        }
        
        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty() && offHand.getItem() instanceof ItemThaumometer) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if the player is holding an item that implements ISealDisplayer.
     * This includes seal items and the golem bell.
     */
    private static boolean isHoldingSealDisplayer(Player player) {
        // Check main hand
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem() instanceof ISealDisplayer) {
            return true;
        }
        
        // Check off hand
        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty() && offHand.getItem() instanceof ISealDisplayer) {
            return true;
        }
        
        return false;
    }
}
