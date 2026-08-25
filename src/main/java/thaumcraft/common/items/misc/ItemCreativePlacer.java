package thaumcraft.common.items.misc;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * ItemCreativePlacer - Creative-only item for placing special structures.
 * 
 * This is a creative/debug item that can place special Thaumcraft structures:
 * - Obelisks
 * - Nodes (vis nodes, when implemented)
 * - Caster pedestals
 * 
 * Note: The original 1.12.2 implementation was incomplete.
 * This serves as a placeholder for future structure placement functionality.
 * 
 * Ported to 1.20.1
 */
public class ItemCreativePlacer extends Item {
    
    public ItemCreativePlacer(Properties properties) {
        super(thaumcraft.init.ItemRegistration.id(properties.stacksTo(1).rarity(Rarity.EPIC)));
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        builder.accept(Component.literal("Structure placer tool").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.literal("Creative only").withStyle(ChatFormatting.DARK_PURPLE));
        builder.accept(Component.literal("Not yet implemented").withStyle(ChatFormatting.RED));
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        
        if (player == null) {
            return InteractionResult.FAIL;
        }
        
        BlockState clickedState = level.getBlockState(pos);
        
        // Must click on solid block
        if (!clickedState.isSolidRender()) {
            return InteractionResult.FAIL;
        }
        
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        
        // Get position to place at (offset by the clicked face)
        BlockPos placePos = pos.relative(side);
        
        // Verify player can edit this position
        if (!player.mayUseItemAt(placePos, side, stack)) {
            return InteractionResult.FAIL;
        }
        
        // Check if position is replaceable
        BlockState placeState = level.getBlockState(placePos);
        if (!placeState.canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        
        // TODO: Implement structure placement based on item variant
        // For now this is a placeholder - the original code was also incomplete
        
        // Structures that could be placed:
        // - Eldritch Obelisk (multi-block structure)
        // - Vis Node (when node system is implemented)
        // - Caster Pedestal
        
        player.sendSystemMessage(Component.literal("Structure placement not yet implemented.")
                .withStyle(ChatFormatting.YELLOW));
        
        return InteractionResult.SUCCESS;
    }
}
