package thaumcraft.common.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.golems.ISealDisplayer;
import thaumcraft.api.golems.seals.ISeal;

import thaumcraft.init.ModItems;

import javax.annotation.Nullable;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * ItemSealPlacer - Item used to place golem command seals on blocks.
 * 
 * Each seal type has its own item variant. When used on a block face,
 * places a seal entity that golems can interact with.
 * 
 * Implements ISealDisplayer so that existing seals are visible while holding this item.
 */
public class ItemSealPlacer extends Item implements ISealDisplayer {

    private final String sealKey;

    public ItemSealPlacer(String sealKey) {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties()
                .stacksTo(64)));
        this.sealKey = sealKey;
    }

    /**
     * Create a blank seal (can't be placed).
     */
    public static ItemSealPlacer createBlank() {
        return new ItemSealPlacer("blank");
    }

    /**
     * Create a seal placer for a specific seal type.
     */
    public static ItemSealPlacer create(String sealKey) {
        return new ItemSealPlacer(sealKey);
    }

    public String getSealKey() {
        return sealKey;
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

        // Blank seals can't be placed
        if (sealKey.equals("blank")) {
            return InteractionResult.PASS;
        }

        // Sneaking bypasses placement (for block interaction)
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Check if player can edit at this position
        if (!player.mayUseItemAt(pos, side, stack)) {
            return InteractionResult.FAIL;
        }

        // Get the seal type
        ISeal sealTemplate = SealHandler.getSeal(sealKey);
        if (sealTemplate == null) {
            return InteractionResult.FAIL;
        }

        // Check if seal can be placed at this position
        if (!sealTemplate.canPlaceAt(level, pos, side)) {
            return InteractionResult.FAIL;
        }

        // Create a new seal instance
        ISeal seal;
        try {
            seal = sealTemplate.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return InteractionResult.FAIL;
        }

        // Place the seal
        if (SealHandler.addSealEntity(level, pos, side, seal, player)) {
            // Consume item if not in creative
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        if (!sealKey.equals("blank")) {
            ISeal seal = SealHandler.getSeal(sealKey);
            if (seal != null) {
                // Show seal description
                builder.accept(Component.translatable("seal." + sealKey.replace(":", ".") + ".desc")
                        .withStyle(style -> style.withColor(0x808080)));
            }
        }
        super.appendHoverText(stack, context, display, builder, flag);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (sealKey.equals("blank")) {
            return Component.translatable("item.thaumcraft.seal_blank");
        }
        return Component.translatable("item.thaumcraft.seal." + sealKey.replace(":", "."));
    }

    /**
     * Get an ItemStack for a specific seal type.
     * Maps seal keys to their registered items.
     */
    public static ItemStack getSealStack(String sealKey) {
        return switch (sealKey) {
            case "thaumcraft:pickup" -> new ItemStack(ModItems.SEAL_PICKUP.get());
            case "thaumcraft:empty" -> new ItemStack(ModItems.SEAL_EMPTY.get());
            case "thaumcraft:fill" -> new ItemStack(ModItems.SEAL_FILL.get());
            case "thaumcraft:guard" -> new ItemStack(ModItems.SEAL_GUARD.get());
            case "thaumcraft:butcher" -> new ItemStack(ModItems.SEAL_BUTCHER.get());
            case "thaumcraft:harvest" -> new ItemStack(ModItems.SEAL_HARVEST.get());
            case "thaumcraft:lumber" -> new ItemStack(ModItems.SEAL_LUMBER.get());
            case "thaumcraft:breaker" -> new ItemStack(ModItems.SEAL_BREAKER.get());
            case "thaumcraft:provide" -> new ItemStack(ModItems.SEAL_PROVIDE.get());
            case "blank" -> new ItemStack(ModItems.SEAL_BLANK.get());
            default -> ItemStack.EMPTY;
        };
    }
}
