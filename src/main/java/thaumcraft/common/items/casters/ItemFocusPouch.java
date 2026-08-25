package thaumcraft.common.items.casters;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import thaumcraft.common.menu.FocusPouchMenu;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Focus Pouch - A portable container for storing foci.
 * Can be worn as a curio (belt slot) for quick access.
 * Holds up to 18 foci.
 */
public class ItemFocusPouch extends Item {

    public static final int INVENTORY_SIZE = 18;

    public ItemFocusPouch() {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            final InteractionHand usedHand = hand;
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.thaumcraft.focus_pouch");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player menuPlayer) {
                    return new FocusPouchMenu(containerId, playerInventory, usedHand);
                }
            }, (buf) -> {
                buf.writeEnum(usedHand);
            });
        }
        
        return InteractionResult.SUCCESS;
    }

    /**
     * Get the inventory contents of this pouch.
     */
    public NonNullList<ItemStack> getInventory(ItemStack item) {
        NonNullList<ItemStack> stackList = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        CompoundTag data = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data != null && data.contains("items")) {
            List<ItemStack> loaded = ItemStack.OPTIONAL_CODEC.listOf()
                    .parse(NbtOps.INSTANCE, data.get("items")).resultOrPartial().orElse(List.of());
            for (int i = 0; i < stackList.size() && i < loaded.size(); i++) {
                stackList.set(i, loaded.get(i));
            }
        }
        return stackList;
    }

    /**
     * Set the inventory contents of this pouch.
     */
    public void setInventory(ItemStack item, NonNullList<ItemStack> stackList) {
        CompoundTag tag = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        List<ItemStack> list = new java.util.ArrayList<>();
        for (ItemStack stack : stackList) {
            list.add(stack);
        }
        tag.put("items", ItemStack.OPTIONAL_CODEC.listOf()
                .encodeStart(NbtOps.INSTANCE, list).resultOrPartial().orElse(new ListTag()));
        CustomData.set(DataComponents.CUSTOM_DATA, item, tag);
    }

    /**
     * Count how many foci are stored in this pouch.
     */
    public int getFociCount(ItemStack item) {
        NonNullList<ItemStack> inv = getInventory(item);
        int count = 0;
        for (ItemStack stack : inv) {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemFocus) {
                count++;
            }
        }
        return count;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        int count = getFociCount(stack);
        if (count > 0) {
            builder.accept(Component.translatable("item.thaumcraft.focus_pouch.contents", count, INVENTORY_SIZE)
                    .withStyle(style -> style.withColor(0x808080)));
        } else {
            builder.accept(Component.translatable("item.thaumcraft.focus_pouch.empty")
                    .withStyle(style -> style.withColor(0x808080)));
        }
        super.appendHoverText(stack, context, display, builder, flag);
    }

    // TODO: Implement Curios integration for belt slot
    // This would allow wearing the pouch and accessing foci quickly
}
