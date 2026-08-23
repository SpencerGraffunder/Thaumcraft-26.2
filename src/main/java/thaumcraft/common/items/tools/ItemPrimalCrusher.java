package thaumcraft.common.items.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.init.ModItems;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.core.registries.Registries;

/**
 * Primal Crusher - A multi-tool that works as both pickaxe and shovel.
 * Made from void metal, it self-repairs and causes warp.
 * Has built-in Destructive and Refining infusion enchantments.
 * Ported to MC 26.2: pickaxe property + tool component based.
 */
public class ItemPrimalCrusher extends Item implements IWarpingGear {

    /**
     * Custom tier for the Primal Crusher - based on void metal but enhanced.
     * Durability: 500, Speed: 8, Damage: 4, Enchantability: 20
     */
    public static final ToolMaterial PRIMAL_VOID_TIER = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 500, 8.0f, 4.0f, 20,
            ThaumcraftItemTag("void_metal_ingot"));

    private static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> ThaumcraftItemTag(String name) {
        return net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("thaumcraft", name));
    }

    public ItemPrimalCrusher() {
        super(new Item.Properties()
                .rarity(Rarity.EPIC)
                .pickaxe(PRIMAL_VOID_TIER, 3.5f, -2.8f));
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        // Works on both pickaxe and shovel blocks
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // Fast on all pickaxe and shovel blocks
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return 8.0f;
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.VOID_METAL_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }

    /**
     * Self-repair mechanic - repairs 1 durability every 20 ticks.
     */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);

        if (stack.isDamaged() && entity != null && entity.tickCount % 20 == 0 && entity instanceof LivingEntity living) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 2;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("enchantment.thaumcraft.destructive").withStyle(style -> style.withColor(0x8B4513)));
        builder.accept(Component.translatable("enchantment.thaumcraft.refining").withStyle(style -> style.withColor(0xFFD700)));
        builder.accept(Component.translatable("item.thaumcraft.primal_crusher.desc").withStyle(style -> style.withColor(0x808080)));
        builder.accept(Component.translatable("item.thaumcraft.self_repair").withStyle(style -> style.withColor(0x9400D3)));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
