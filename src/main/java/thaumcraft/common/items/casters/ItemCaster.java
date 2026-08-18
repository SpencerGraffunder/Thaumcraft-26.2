package thaumcraft.common.items.casters;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.ICaster;
import thaumcraft.api.casters.IInteractWithCaster;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.common.world.aura.AuraHandler;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Caster Gauntlet - Main item for casting spells in Thaumcraft.
 * Holds a focus and consumes vis from the local aura to cast.
 */
public class ItemCaster extends Item implements ICaster {
    
    private static final DecimalFormat VIS_FORMAT = new DecimalFormat("#######.#");
    
    /** Area of aura this caster can draw from (0=chunk, 1=cross, 2=3x3) */
    private final int auraArea;
    
    public ItemCaster(int area) {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
        this.auraArea = area;
    }
    
    /**
     * Create a basic caster gauntlet (draws from single chunk).
     */
    public static ItemCaster createBasic() {
        return new ItemCaster(0);
    }
    
    /**
     * Create an advanced caster gauntlet (draws from 5 chunks in a cross).
     */
    public static ItemCaster createAdvanced() {
        return new ItemCaster(1);
    }
    
    /**
     * Create a master caster gauntlet (draws from 9 chunks in a 3x3).
     */
    public static ItemCaster createMaster() {
        return new ItemCaster(2);
    }
    
    // ==================== ICaster Implementation ====================
    
    @Override
    public float getConsumptionModifier(ItemStack stack, Player player, boolean crafting) {
        float modifier = 1.0f;
        if (player != null) {
            modifier -= getTotalVisDiscount(player);
        }
        return Math.max(modifier, 0.1f); // Minimum 10% cost
    }
    
    /**
     * Calculate total vis discount from equipped gear.
     */
    private float getTotalVisDiscount(Player player) {
        float discount = 0.0f;
        
        // Check armor slots
        for (ItemStack armor : player.getArmorSlots()) {
            if (!armor.isEmpty() && armor.getItem() instanceof IVisDiscountGear gear) {
                discount += gear.getVisDiscount(armor, player) / 100.0f;
            }
        }
        
        // Check held items (other hand)
        for (ItemStack held : player.getHandSlots()) {
            if (!held.isEmpty() && held.getItem() instanceof IVisDiscountGear gear) {
                discount += gear.getVisDiscount(held, player) / 100.0f;
            }
        }
        
        // Cap at 50%
        return Math.min(discount, 0.5f);
    }
    
    @Override
    public boolean consumeVis(ItemStack stack, Player player, float amount, boolean crafting, boolean simulate) {
        amount *= getConsumptionModifier(stack, player, crafting);
        
        float available = getAuraPool(player);
        if (available < amount) {
            return false;
        }
        
        if (simulate) {
            return true;
        }
        
        // Drain vis from the aura
        return drainFromAura(player, amount);
    }
    
    /**
     * Get total available vis from the aura pool this caster can access.
     */
    private float getAuraPool(Player player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        
        return switch (auraArea) {
            case 1 -> { // Cross pattern (5 chunks)
                float total = AuraHandler.getVis(level, pos);
                for (Direction face : Direction.Plane.HORIZONTAL) {
                    total += AuraHandler.getVis(level, pos.relative(face, 16));
                }
                yield total;
            }
            case 2 -> { // 3x3 grid (9 chunks)
                float total = 0.0f;
                for (int xx = -1; xx <= 1; xx++) {
                    for (int zz = -1; zz <= 1; zz++) {
                        total += AuraHandler.getVis(level, pos.offset(xx * 16, 0, zz * 16));
                    }
                }
                yield total;
            }
            default -> AuraHandler.getVis(level, pos); // Single chunk
        };
    }
    
    /**
     * Drain vis from the aura using this caster's pattern.
     */
    private boolean drainFromAura(Player player, float amount) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        
        switch (auraArea) {
            case 1 -> { // Cross pattern
                float perChunk = amount / 5.0f;
                float remaining = amount;
                remaining -= AuraHandler.drainVis(level, pos, Math.min(perChunk, remaining), false);
                for (Direction face : Direction.Plane.HORIZONTAL) {
                    if (remaining <= 0) break;
                    remaining -= AuraHandler.drainVis(level, pos.relative(face, 16), 
                            Math.min(perChunk, remaining), false);
                }
                return remaining <= 0;
            }
            case 2 -> { // 3x3 grid
                float perChunk = amount / 9.0f;
                float remaining = amount;
                for (int xx = -1; xx <= 1; xx++) {
                    for (int zz = -1; zz <= 1; zz++) {
                        if (remaining <= 0) break;
                        remaining -= AuraHandler.drainVis(level, pos.offset(xx * 16, 0, zz * 16),
                                Math.min(perChunk, remaining), false);
                    }
                }
                return remaining <= 0;
            }
            default -> { // Single chunk
                return AuraHandler.drainVis(level, pos, amount, false) >= amount;
            }
        }
    }
    
    @Override
    @Nullable
    public Item getFocus(ItemStack stack) {
        ItemStack focusStack = getFocusStack(stack);
        if (focusStack != null && !focusStack.isEmpty()) {
            return focusStack.getItem();
        }
        return null;
    }
    
    @Override
    @Nullable
    public ItemStack getFocusStack(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("focus")) {
            CompoundTag focusTag = stack.getTag().getCompound("focus");
            return ItemStack.of(focusTag);
        }
        return null;
    }
    
    @Override
    public void setFocus(ItemStack stack, ItemStack focus) {
        if (focus == null || focus.isEmpty()) {
            if (stack.hasTag()) {
                stack.getTag().remove("focus");
            }
        } else {
            stack.getOrCreateTag().put("focus", focus.save(new CompoundTag()));
        }
    }
    
    @Override
    public ItemStack getPickedBlock(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("picked")) {
            return ItemStack.of(stack.getTag().getCompound("picked"));
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Store a picked block for Equal Trade focus.
     */
    public void storePickedBlock(ItemStack stack, ItemStack pickedBlock) {
        stack.getOrCreateTag().put("picked", pickedBlock.save(new CompoundTag()));
    }
    
    // ==================== Item Behavior ====================
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Direction side = context.getClickedFace();
        InteractionHand hand = context.getHand();
        
        // Check for IInteractWithCaster blocks
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof IInteractWithCaster casterBlock) {
            if (casterBlock.onCasterRightClick(level, stack, player, pos, side, hand)) {
                return InteractionResult.SUCCESS;
            }
        }
        
        // Check tile entities
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof IInteractWithCaster casterTile) {
            if (casterTile.onCasterRightClick(level, stack, player, pos, side, hand)) {
                return InteractionResult.SUCCESS;
            }
        }
        
        // TODO: Add focus-specific block interactions
        
        return InteractionResult.PASS;
    }
    
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack focusStack = getFocusStack(stack);
        
        if (focusStack == null || focusStack.isEmpty()) {
            return InteractionResult.PASS;
        }
        
        if (!(focusStack.getItem() instanceof ItemFocus focus)) {
            return InteractionResult.PASS;
        }
        
        // Calculate vis cost
        float visCost = focus.getVisCost(focusStack);
        
        // Try to consume vis
        if (!consumeVis(stack, player, visCost, false, false)) {
            // Not enough vis
            return InteractionResult.FAIL;
        }
        
        // Cast the spell!
        if (!level.isClientSide()) {
            FocusPackage focusPackage = ItemFocus.getPackage(focusStack);
            if (focusPackage != null) {
                FocusEngine.castFocusPackage(player, focusPackage);
            }
        }
        
        player.swing(hand);
        
        // Apply cooldown
        int cooldown = focus.getActivationTime(focusStack);
        player.getCooldowns().addCooldown(this, cooldown);
        
        return InteractionResult.SUCCESS;
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // TODO: Sync aura information to client when holding caster
    }
    
    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000; // Long duration for channeled spells
    }
    
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
    
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // Only re-animate if focus changed
        if (oldStack.getItem() == this && newStack.getItem() == this) {
            ItemStack oldFocus = getFocusStack(oldStack);
            ItemStack newFocus = getFocusStack(newStack);
            
            if (oldFocus == null && newFocus == null) return false;
            if (oldFocus == null || newFocus == null) return true;
            
            // Compare focus configurations
            if (oldFocus.getItem() instanceof ItemFocus oldF && newFocus.getItem() instanceof ItemFocus newF) {
                String oldSort = oldF.getSortingHelper(oldFocus);
                String newSort = newF.getSortingHelper(newFocus);
                if (oldSort != null && newSort != null) {
                    return !oldSort.equals(newSort);
                }
            }
            return !ItemStack.isSameItemSameTags(oldFocus, newFocus);
        }
        return oldStack.getItem() != newStack.getItem();
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ItemStack focusStack = getFocusStack(stack);
        
        if (focusStack != null && !focusStack.isEmpty() && focusStack.getItem() instanceof ItemFocus focus) {
            // Show vis cost
            float visCost = focus.getVisCost(focusStack);
            if (visCost > 0) {
                tooltip.add(Component.translatable("tc.vis.cost")
                        .append(" ")
                        .append(Component.literal(VIS_FORMAT.format(visCost)))
                        .withStyle(ChatFormatting.ITALIC, ChatFormatting.AQUA));
            }
            
            // Show focus name
            tooltip.add(Component.empty());
            tooltip.add(focusStack.getHoverName()
                    .copy()
                    .withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC, ChatFormatting.GREEN));
            
            // Add focus details
            focus.addFocusInformation(focusStack, level, tooltip, flag);
        } else {
            tooltip.add(Component.translatable("item.thaumcraft.caster.no_focus")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}
