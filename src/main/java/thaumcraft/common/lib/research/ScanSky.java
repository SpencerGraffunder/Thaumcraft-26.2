package thaumcraft.common.lib.research;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.IScanThing;
import thaumcraft.init.ModItems;

/**
 * A scanner that checks for celestial observations.
 * Players can observe the sun, moon, and stars when looking at the sky.
 */
public class ScanSky implements IScanThing {
    
    @Override
    public boolean checkThing(Player player, Object obj) {
        // Only works when looking at nothing (sky)
        if (obj != null) return false;
        
        // Must be looking up
        if (player.getXRot() > 0.0f) return false;
        
        // Must be able to see sky
        if (!player.level().canSeeSky(player.blockPosition().above())) return false;
        
        // Must be in overworld
        if (!player.level().dimension().equals(Level.OVERWORLD)) return false;
        
        // Must know celestial scanning research
        if (!ThaumcraftCapabilities.knowsResearchStrict(player, "CELESTIALSCANNING")) return false;
        
        return true;
    }
    
    @Override
    public void onSuccess(Player player, Object object) {
        if (object != null || player.getXRot() > 0.0f) return;
        if (!player.level().canSeeSky(player.blockPosition().above())) return;
        if (!ThaumcraftCapabilities.knowsResearchStrict(player, "CELESTIALSCANNING")) return;
        
        Level level = player.level();
        
        // Calculate celestial positions
        int yaw = (int)(player.getYRot() + 90.0f) % 360;
        int pitch = (int) Math.abs(player.getXRot());
        int celestialAngle = (int)((level.getOverworldClockTime() / 24000.0 + 0.25) * 360.0) % 360;
        boolean isNight = celestialAngle > 180;
        
        if (isNight) {
            celestialAngle -= 180;
        }
        
        boolean inRangeYaw;
        boolean inRangePitch;
        
        if (celestialAngle > 90) {
            inRangeYaw = Math.abs(Math.abs(yaw) - 180) < 10;
            inRangePitch = Math.abs(180 - celestialAngle - pitch) < 7;
        } else {
            inRangeYaw = Math.abs(yaw) < 10;
            inRangePitch = Math.abs(celestialAngle - pitch) < 7;
        }
        
        long worldDay = level.getOverworldClockTime() / 24000L;
        String dayPrefix = "CEL_" + worldDay + "_";
        
        if (inRangeYaw && inRangePitch) {
            // Looking at sun or moon
            int moonPhase = (int)(level.getOverworldClockTime() / 24000L % 8L);
            String key = dayPrefix + (isNight ? ("Moon" + moonPhase) : "Sun");
            
            if (ThaumcraftCapabilities.isResearchKnown(player, key)) {
                player.sendSystemMessage(Component.translatable("tc.celestial.fail.1"));
                return;
            }
            
            // Check for scribing tools and paper
            if (!hasScribingTools(player)) {
                player.sendSystemMessage(Component.translatable("tc.celestial.fail.2"));
                return;
            }
            
            // Consume scribing tools and paper
            consumeScribingTools(player);
            
            // Grant celestial notes reward (1.12 granted celestialNotes on success)
            grantCelestialNotes(player, isNight ? "moon" : "sun", isNight ? moonPhase : 0);
            
            ThaumcraftApi.internalMethods.progressResearch(player, key);
            cleanResearch(player, dayPrefix);
            
        } else if (isNight) {
            // Looking at stars at night
            int starDirection = getStarDirection(player);
            String key = dayPrefix + "Star" + starDirection;
            
            if (ThaumcraftCapabilities.isResearchKnown(player, key)) {
                player.sendSystemMessage(Component.translatable("tc.celestial.fail.1"));
                return;
            }
            
            // Check for scribing tools and paper (1.12 requires these for stars too)
            if (!hasScribingTools(player)) {
                player.sendSystemMessage(Component.translatable("tc.celestial.fail.2"));
                return;
            }
            
            // Consume scribing tools and paper
            consumeScribingTools(player);
            
            // Grant celestial notes reward (1.12 granted celestialNotes on success)
            grantCelestialNotes(player, "star", starDirection);
            
            ThaumcraftApi.internalMethods.progressResearch(player, key);
            cleanResearch(player, dayPrefix);
        }
    }
    
    private int getStarDirection(Player player) {
        float yaw = player.getYRot();
        // Normalize to 0-360
        while (yaw < 0) yaw += 360;
        while (yaw >= 360) yaw -= 360;
        
        // Map to 4 cardinal directions (0-3)
        if (yaw < 45 || yaw >= 315) return 0; // South
        if (yaw < 135) return 1; // West  
        if (yaw < 225) return 2; // North
        return 3; // East
    }
    
    private void cleanResearch(Player player, String dayPrefix) {
        // Remove old celestial research from previous days
        var knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge == null) return;
        
        var toRemove = new java.util.ArrayList<String>();
        for (String key : knowledge.getResearchList()) {
            if (key.startsWith("CEL_") && !key.startsWith(dayPrefix)) {
                toRemove.add(key);
            }
        }
        
        for (String key : toRemove) {
            knowledge.removeResearch(key);
        }
        
        ResearchManager.syncList.put(player.getName().getString(), true);
    }
    
    /**
     * Check if the player has scribing tools (1.12: ItemsTC.scribingTools) + 1 paper.
     */
    private boolean hasScribingTools(Player player) {
        return player.getInventory().contains(new ItemStack(ModItems.SCRIBING_TOOLS.get()))
            && player.getInventory().contains(new ItemStack(Items.PAPER));
    }
    
    /**
     * Consume scribing tools and 1 paper from inventory (1.12 ScanSky).
     */
    private void consumeScribingTools(Player player) {
        for (ItemStack stack : player.getInventory()) {
            if (stack.getItem() == ModItems.SCRIBING_TOOLS.get() && stack.getCount() > 0) {
                stack.shrink(1);
                break;
            }
        }
        for (ItemStack paper : player.getInventory()) {
            if (paper.getItem() == Items.PAPER && paper.getCount() > 0) {
                paper.shrink(1);
                break;
            }
        }
    }
    
    /**
     * Grant the celestial notes item on successful scan (1.12 granted celestialNotes).
     * 26.2 uses separate items: sun, stars_1-4, moon_1-8.
     */
    private void grantCelestialNotes(Player player, String type, int index) {
        Item item;
        if (type.equals("sun")) {
            item = ModItems.CELESTIAL_NOTES_SUN.get();
        } else if (type.equals("star")) {
            switch (index) {
                case 0: item = ModItems.CELESTIAL_NOTES_STARS_1.get(); break;
                case 1: item = ModItems.CELESTIAL_NOTES_STARS_2.get(); break;
                case 2: item = ModItems.CELESTIAL_NOTES_STARS_3.get(); break;
                default: item = ModItems.CELESTIAL_NOTES_STARS_4.get(); break;
            }
        } else {
            switch (index) {
                case 0: item = ModItems.CELESTIAL_NOTES_MOON_1.get(); break;
                case 1: item = ModItems.CELESTIAL_NOTES_MOON_2.get(); break;
                case 2: item = ModItems.CELESTIAL_NOTES_MOON_3.get(); break;
                case 3: item = ModItems.CELESTIAL_NOTES_MOON_4.get(); break;
                case 4: item = ModItems.CELESTIAL_NOTES_MOON_5.get(); break;
                case 5: item = ModItems.CELESTIAL_NOTES_MOON_6.get(); break;
                case 6: item = ModItems.CELESTIAL_NOTES_MOON_7.get(); break;
                default: item = ModItems.CELESTIAL_NOTES_MOON_8.get(); break;
            }
        }
        ItemStack reward = new ItemStack(item);
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
    }
    
    @Override
    public String getResearchKey(Player player, Object object) {
        return ""; // Key is determined dynamically in onSuccess
    }
}
