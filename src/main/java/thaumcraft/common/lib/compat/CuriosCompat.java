package thaumcraft.common.lib.compat;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import thaumcraft.api.items.IVisDiscountGear;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * CuriosCompat - Safe wrapper for Curios API integration.
 * 
 * Provides methods that safely check if Curios is loaded before
 * attempting to use its API. This allows Thaumcraft to function
 * with or without Curios installed.
 */
public class CuriosCompat {
    
    private static Boolean curiosLoaded = null;
    
    /**
     * Check if Curios mod is loaded.
     */
    public static boolean isCuriosLoaded() {
        if (curiosLoaded == null) {
            curiosLoaded = ModList.get().isLoaded("curios");
        }
        return curiosLoaded;
    }
    
    /**
     * Get total vis discount from Curios-equipped items.
     * 
     * @param player The player to check
     * @return Total vis discount from Curios items
     */
    public static int getVisDiscountFromCurios(Player player) {
        if (!isCuriosLoaded() || player == null) {
            return 0;
        }
        
        try {
            return CuriosHelper.getVisDiscount(player);
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            // Curios API not available or incompatible
            curiosLoaded = false;
            return 0;
        }
    }
    
    /**
     * Iterate over all items in Curios slots.
     * 
     * @param player The player
     * @param consumer Consumer that receives each non-empty ItemStack
     */
    public static void forEachCuriosItem(Player player, Consumer<ItemStack> consumer) {
        if (!isCuriosLoaded() || player == null) {
            return;
        }
        
        try {
            CuriosHelper.forEachItem(player, consumer);
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            curiosLoaded = false;
        }
    }
    
    /**
     * Iterate over all items in Curios slots with slot info.
     * 
     * @param player The player
     * @param consumer Consumer that receives (slotKey, ItemStack)
     */
    public static void forEachCuriosItemWithSlot(Player player, BiConsumer<String, ItemStack> consumer) {
        if (!isCuriosLoaded() || player == null) {
            return;
        }
        
        try {
            CuriosHelper.forEachItemWithSlot(player, consumer);
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            curiosLoaded = false;
        }
    }
    
    /**
     * Find all items in Curios slots matching a predicate.
     * 
     * @param player The player
     * @param predicate The item filter
     * @return List of matching items
     */
    public static List<ItemStack> findCuriosItems(Player player, Predicate<ItemStack> predicate) {
        List<ItemStack> result = new ArrayList<>();
        if (!isCuriosLoaded() || player == null) {
            return result;
        }
        
        try {
            return CuriosHelper.findItems(player, predicate);
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            curiosLoaded = false;
            return result;
        }
    }
    
    /**
     * Inner helper class that actually uses Curios API.
     * Isolated to prevent class loading issues when Curios isn't present.
     */
    private static class CuriosHelper {
        
        static int getVisDiscount(Player player) {
            AtomicInteger total = new AtomicInteger(0);
            
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.getCurios().forEach((slotId, slotHandler) -> {
                    for (int i = 0; i < slotHandler.getSlots(); i++) {
                        ItemStack stack = slotHandler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() instanceof IVisDiscountGear gear) {
                            total.addAndGet(gear.getVisDiscount(stack, player));
                        }
                    }
                });
            });
            
            return total.get();
        }
        
        static void forEachItem(Player player, Consumer<ItemStack> consumer) {
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.getCurios().forEach((slotId, slotHandler) -> {
                    for (int i = 0; i < slotHandler.getSlots(); i++) {
                        ItemStack stack = slotHandler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            consumer.accept(stack);
                        }
                    }
                });
            });
        }
        
        static void forEachItemWithSlot(Player player, BiConsumer<String, ItemStack> consumer) {
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.getCurios().forEach((slotId, slotHandler) -> {
                    for (int i = 0; i < slotHandler.getSlots(); i++) {
                        ItemStack stack = slotHandler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            consumer.accept(slotId + ":" + i, stack);
                        }
                    }
                });
            });
        }
        
        static List<ItemStack> findItems(Player player, Predicate<ItemStack> predicate) {
            List<ItemStack> result = new ArrayList<>();
            
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.getCurios().forEach((slotId, slotHandler) -> {
                    for (int i = 0; i < slotHandler.getSlots(); i++) {
                        ItemStack stack = slotHandler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty() && predicate.test(stack)) {
                            result.add(stack);
                        }
                    }
                });
            });
            
            return result;
        }
    }
}
