package thaumcraft.common.lib.events;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.blocks.world.ore.BlockCrystalTC;
import thaumcraft.common.items.resources.ItemVisCrystal;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModItems;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * PlayerEvents - Handles all player-related events for Thaumcraft.
 * 
 * Ported from 1.12.2. Key functionality:
 * - Attach capabilities to players
 * - Sync knowledge/warp when player joins
 * - Clone capabilities on death/dimension change
 * - Track research progress triggers (crystal pickup, thaumonomicon, etc.)
 * 
 * Key API changes:
 * - AttachCapabilitiesEvent<Entity> same
 * - EntityJoinWorldEvent -> EntityJoinLevelEvent
 * - PlayerEvent.Clone same
 * - EntityPlayer -> Player
 * - EntityPlayerMP -> ServerPlayer
 * - world.isRemote -> level.isClientSide()
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class PlayerEvents {
    
    // Players that need their knowledge synced
    public static final Set<String> syncList = new HashSet<>();
    
    // ==================== Capability Events ====================
    
    // NOTE: Capability attachment is handled by ThaumcraftCapabilities class
    // to avoid duplicate registration errors.
    
    /**
     * Clone capabilities when player respawns or changes dimension
     */
    @SubscribeEvent
    public static void cloneCapabilitiesEvent(PlayerEvent.Clone event) {
        try {
            // Clone knowledge
            IPlayerKnowledge oldKnowledge = ThaumcraftCapabilities.getKnowledge(event.getOriginal());
            IPlayerKnowledge newKnowledge = ThaumcraftCapabilities.getKnowledge(event.getEntity());
            if (oldKnowledge != null && newKnowledge != null) {
                CompoundTag nbtKnowledge = oldKnowledge.serializeNBT();
                newKnowledge.deserializeNBT(nbtKnowledge);
            }
            
            // Clone warp
            IPlayerWarp oldWarp = ThaumcraftCapabilities.getWarp(event.getOriginal());
            IPlayerWarp newWarp = ThaumcraftCapabilities.getWarp(event.getEntity());
            if (oldWarp != null && newWarp != null) {
                CompoundTag nbtWarp = oldWarp.serializeNBT();
                newWarp.deserializeNBT(nbtWarp);
            }
        } catch (Exception e) {
            Thaumcraft.LOGGER.error("Could not clone player [{}] knowledge when respawning/changing dimensions", 
                event.getOriginal().getName().getString(), e);
        }
    }
    
    // ==================== Join/Login Events ====================
    
    /**
     * Sync capabilities when player joins world
     */
    @SubscribeEvent
    public static void playerJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
            
            if (knowledge != null) {
                knowledge.sync(player);
            }
            if (warp != null) {
                warp.sync(player);
            }
            
            Thaumcraft.LOGGER.debug("Synced Thaumcraft data for player: {}", player.getName().getString());
        }
    }
    
    // ==================== Tick Events ====================
    
    /**
     * Player tick event - handles periodic sync and warp effects
     */
    @SubscribeEvent
    public static void livingTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Player player) {
            if (!player.level().isClientSide()) {
                // Periodic knowledge sync (every 20 ticks)
                if (player.tickCount % 20 == 0 && player instanceof ServerPlayer serverPlayer) {
                    String playerName = player.getName().getString();
                    if (syncList.remove(playerName)) {
                        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
                        if (knowledge != null) {
                            knowledge.sync(serverPlayer);
                        }
                    }
                }
                
                // TODO: Periodic research checks (every 200 ticks)
                // if (player.tickCount % 200 == 0) {
                //     ConfigResearch.checkPeriodicStuff(player);
                // }
                
                // Warp effects (every 2000 ticks = ~100 seconds)
                if (player.tickCount % 2000 == 0) {
                    WarpEvents.checkWarpEvent(player);
                }
                
                // Death Gaze effect check (every 20 ticks)
                if (player.tickCount % 20 == 5) {
                    WarpEvents.checkDeathGaze(player);
                }
            }
        }
    }
    
    // ==================== Sleep Events ====================
    
    /**
     * Handle player waking up - triggers the dream that starts Thaumcraft progression.
     * When a player sleeps for the first time after picking up a crystal, they receive
     * the "gotdream" research flag which allows them to craft a Thaumonomicon.
     */
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        // Check if player has picked up a crystal but hasn't had the dream yet
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge == null) return;
        
        boolean hasPickedCrystal = knowledge.isResearchKnown("!gotcrystal");
        boolean hasDream = knowledge.isResearchKnown("!gotdream");
        
        if (hasPickedCrystal && !hasDream) {
            // Grant the dream research
            ThaumcraftApi.internalMethods.completeResearch(player, "!gotdream");
            
            // Show dream message to player
            player.sendSystemMessage(
                Component.translatable("tc.dream.1"));
            player.sendSystemMessage(
                Component.translatable("tc.dream.2"));
            
            Thaumcraft.LOGGER.info("Player {} received the Thaumcraft dream", player.getName().getString());
        }
    }
    
    // ==================== Item Events ====================
    
    /**
     * Handle item pickup - triggers research progression when picking up crystals.
     * This is essential for starting Thaumcraft progression.
     */
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide()) {
            return;
        }
        
        ItemStack stack = event.getOriginalStack();
        if (stack.isEmpty()) return;
        
        Item item = stack.getItem();
        
        // Check if it's a crystal item (vis crystal item or crystal block)
        boolean isCrystal = false;
        
        // Check for vis crystal items (6 primal types)
        if (item instanceof ItemVisCrystal) {
            isCrystal = true;
        }
        // Check for crystal block items
        else if (item == ModBlocks.CRYSTAL_AIR.get().asItem() ||
                 item == ModBlocks.CRYSTAL_FIRE.get().asItem() ||
                 item == ModBlocks.CRYSTAL_WATER.get().asItem() ||
                 item == ModBlocks.CRYSTAL_EARTH.get().asItem() ||
                 item == ModBlocks.CRYSTAL_ORDER.get().asItem() ||
                 item == ModBlocks.CRYSTAL_ENTROPY.get().asItem() ||
                 item == ModBlocks.CRYSTAL_FLUX.get().asItem()) {
            isCrystal = true;
        }
        
        if (isCrystal) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            if (knowledge != null && !knowledge.isResearchKnown("!gotcrystal")) {
                // Grant crystal pickup research
                ThaumcraftApi.internalMethods.completeResearch(player, "!gotcrystal");
                
                // Show hint message
                player.sendSystemMessage(
                    Component.translatable("tc.crystal.pickup"));
                
                Thaumcraft.LOGGER.info("Player {} picked up first crystal - sleep to receive dream!", 
                    player.getName().getString());
            }
        }
        
        // Check for thaumonomicon pickup
        if (item == ModItems.THAUMONOMICON.get()) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            if (knowledge != null && !knowledge.isResearchKnown("!gotthaumonomicon")) {
                ThaumcraftApi.internalMethods.completeResearch(player, "!gotthaumonomicon");
                Thaumcraft.LOGGER.info("Player {} picked up Thaumonomicon - research unlocked!", 
                    player.getName().getString());
            }
        }
    }
    
    /**
     * Track who threw an item (for item-specific mechanics)
     */
    @SubscribeEvent
    public static void droppedItem(ItemTossEvent event) {
        ItemEntity itemEntity = event.getEntity();
        CompoundTag itemData = itemEntity.getPersistentData();
        itemData.putString("thrower", event.getPlayer().getName().getString());
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Mark a player for knowledge sync on next tick
     */
    public static void markForSync(Player player) {
        syncList.add(player.getName().getString());
    }
    
    /**
     * Get runic charge from an item
     */
    public static int getRunicCharge(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty() || !stack.has(DataComponents.CUSTOM_DATA)) {
            return 0;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag != null && tag.contains("TC.RUNIC")) {
            return tag.getByteOr("TC.RUNIC", (byte)0);
        }
        return 0;
    }
    
    /**
     * Get warp value from an item
     */
    public static int getFinalWarp(net.minecraft.world.item.ItemStack stack, Player player) {
        if (stack.isEmpty()) {
            return 0;
        }
        int warp = 0;
        
        // Check for IWarpingGear interface
        if (stack.getItem() instanceof thaumcraft.api.items.IWarpingGear warpingGear) {
            warp += warpingGear.getWarp(stack, player);
        }
        
        // Check for NBT warp tag
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag != null && tag.contains("TC.WARP")) {
                warp += tag.getByteOr("TC.WARP", (byte)0);
            }
        }
        
        return warp;
    }
    
    /**
     * Get vis discount from an item
     */
    public static int getFinalDiscount(net.minecraft.world.item.ItemStack stack, Player player) {
        if (stack.isEmpty() || !(stack.getItem() instanceof thaumcraft.api.items.IVisDiscountGear gear)) {
            return 0;
        }
        return gear.getVisDiscount(stack, player);
    }
}
