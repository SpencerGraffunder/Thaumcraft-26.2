package thaumcraft.common.items.casters;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import thaumcraft.api.casters.FocusPackage;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Base class for all focus items.
 * A focus is installed into a caster gauntlet to provide spell effects.
 */
public class ItemFocus extends Item {
    
    private static final DecimalFormat VIS_FORMAT = new DecimalFormat("#######.#");
    
    /** Maximum complexity this focus can handle */
    private final int maxComplexity;
    
    public ItemFocus(int complexity) {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE));
        this.maxComplexity = complexity;
    }
    
    /**
     * Get the color of this focus based on its effects.
     */
    public int getFocusColor(ItemStack focusStack) {
        if (focusStack.isEmpty() || !focusStack.has(DataComponents.CUSTOM_DATA)) {
            return 0xFFFFFF; // White default
        }
        
        CompoundTag tag = focusStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag != null && tag.contains("color")) {
            return tag.getIntOr("color", 0);
        }
        
        // Calculate color from effects and cache it
        FocusPackage core = getPackage(focusStack);
        if (core != null) {
            // Default purple-ish color for magic
            int color = 0x9933FF;
            tag = focusStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putInt("color", color);
            CustomData.set(DataComponents.CUSTOM_DATA, focusStack, tag);
            return color;
        }
        
        return 0xFFFFFF;
    }
    
    /**
     * Get a string for sorting/comparing focus configurations.
     */
    public String getSortingHelper(ItemStack focusStack) {
        if (focusStack.isEmpty() || !focusStack.has(DataComponents.CUSTOM_DATA)) {
            return null;
        }
        
        CompoundTag tag = focusStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int sh = tag != null ? tag.getIntOr("srt", 0) : 0;
        
        if (sh == 0) {
            FocusPackage pack = getPackage(focusStack);
            if (pack != null) {
                sh = pack.getSortingHelper();
                CompoundTag srtTag = focusStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                srtTag.putInt("srt", sh);
                CustomData.set(DataComponents.CUSTOM_DATA, focusStack, srtTag);
            }
        }
        
        return focusStack.getHoverName().getString() + sh;
    }
    
    /**
     * Store a FocusPackage configuration in this focus.
     */
    public static void setPackage(ItemStack focusStack, FocusPackage core) {
        CompoundTag tag = core.serialize();
        CompoundTag stackTag = focusStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        stackTag.put("package", tag);
        CustomData.set(DataComponents.CUSTOM_DATA, focusStack, stackTag);
    }
    
    /**
     * Get the FocusPackage configuration from this focus.
     */
    @Nullable
    public static FocusPackage getPackage(ItemStack focusStack) {
        if (focusStack.isEmpty()) {
            return null;
        }
        
        CompoundTag data = focusStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag tag = data != null ? data.getCompound("package").orElse(null) : null;
        if (tag != null) {
            FocusPackage pack = new FocusPackage();
            pack.deserialize(tag);
            return pack;
        }
        
        return null;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        List<Component> tooltip = new java.util.ArrayList<>();
        addFocusInformation(stack, null, tooltip, flag);
        tooltip.forEach(builder);
    }
    
    /**
     * Add focus-specific tooltip information.
     */
    public void addFocusInformation(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        FocusPackage pack = getPackage(stack);
        if (pack != null) {
            float visCost = getVisCost(stack);
            String amount = VIS_FORMAT.format(visCost);
            tooltip.add(Component.translatable("item.thaumcraft.focus.cost", amount)
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.AQUA));
            
            // TODO: Add focus element descriptions
        }
    }
    
    /**
     * Get the vis cost for casting with this focus.
     */
    public float getVisCost(ItemStack focusStack) {
        FocusPackage pack = getPackage(focusStack);
        if (pack == null) {
            return 0.0f;
        }
        return pack.getComplexity() / 5.0f;
    }
    
    /**
     * Get the cooldown time after casting with this focus.
     */
    public int getActivationTime(ItemStack focusStack) {
        FocusPackage pack = getPackage(focusStack);
        if (pack == null) {
            return 0;
        }
        int complexity = pack.getComplexity();
        return Math.max(5, (complexity / 5) * (complexity / 4));
    }
    
    /**
     * Get the maximum complexity this focus can handle.
     */
    public int getMaxComplexity() {
        return maxComplexity;
    }
    
    /**
     * Create a focus with default blank configuration.
     */
    public static ItemFocus createBlank() {
        return new ItemFocus(25); // Standard complexity cap
    }
    
    /**
     * Create an advanced focus with higher complexity cap.
     */
    public static ItemFocus createAdvanced() {
        return new ItemFocus(50); // Advanced complexity cap
    }
}
