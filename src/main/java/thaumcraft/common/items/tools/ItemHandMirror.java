package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import thaumcraft.common.menu.HandMirrorMenu;
import thaumcraft.common.tiles.devices.TileMirror;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModSounds;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Hand Mirror - Links to a magic mirror block for remote inventory access.
 * Right-click on a magic mirror to link.
 * Right-click in air to access linked mirror's inventory.
 */
public class ItemHandMirror extends Item {

    public ItemHandMirror() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("linkX");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) {
            return InteractionResult.FAIL;
        }

        // Check if clicking on a magic mirror
        if (!level.getBlockState(pos).is(ModBlocks.MIRROR_ITEM.get())) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            player.swing(context.getHand());
            return InteractionResult.SUCCESS;
        }

        // Link to the mirror
        BlockEntity te = level.getBlockEntity(pos);
        if (te != null) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putInt("linkX", pos.getX());
            tag.putInt("linkY", pos.getY());
            tag.putInt("linkZ", pos.getZ());
            tag.putString("linkDim", level.dimension().location().toString());

            player.playSound(ModSounds.JAR.get(), 1.0f, 1.0f);
            player.sendSystemMessage(Component.translatable("tc.handmirrorlinked"));
            player.inventoryMenu.broadcastChanges();
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("linkX")) {
                int lx = tag.getInt("linkX");
                int ly = tag.getInt("linkY");
                int lz = tag.getInt("linkZ");
                String dimKey = tag.getString("linkDim");

                // Try to find the target dimension
                ServerLevel targetLevel = null;
                if (level.getServer() != null) {
                    for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
                        if (serverLevel.dimension().location().toString().equals(dimKey)) {
                            targetLevel = serverLevel;
                            break;
                        }
                    }
                }

                if (targetLevel == null) {
                    return InteractionResult.PASS;
                }

                BlockPos targetPos = new BlockPos(lx, ly, lz);
                BlockEntity te = targetLevel.getBlockEntity(targetPos);

                // Check if the mirror still exists
                if (te == null || !targetLevel.getBlockState(targetPos).is(ModBlocks.MIRROR_ITEM.get())) {
                    // Mirror is gone, clear link
                    stack.setTag(null);
                    player.playSound(ModSounds.ZAP.get(), 1.0f, 1.0f);
                    player.sendSystemMessage(Component.translatable("tc.handmirrorerror"));
                    return InteractionResult.FAIL;
                }

                // Open mirror GUI
                if (player instanceof ServerPlayer serverPlayer) {
                    final InteractionHand usedHand = hand;
                    NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                        @Override
                        public Component getDisplayName() {
                            return Component.translatable("container.thaumcraft.hand_mirror");
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player menuPlayer) {
                            return new HandMirrorMenu(containerId, playerInventory, usedHand);
                        }
                    }, (FriendlyByteBuf buf) -> {
                        buf.writeEnum(usedHand);
                    });
                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * Transport an item through a linked mirror.
     * 
     * @param mirror The hand mirror item with link data
     * @param items The item to transport
     * @param player The player using the mirror
     * @param level The current level
     * @return true if transport was successful
     */
    public static boolean transport(ItemStack mirror, ItemStack items, Player player, Level level) {
        if (!mirror.hasTag()) {
            return false;
        }

        CompoundTag tag = mirror.getTag();
        if (tag == null || !tag.contains("linkX")) {
            return false;
        }

        int lx = tag.getInt("linkX");
        int ly = tag.getInt("linkY");
        int lz = tag.getInt("linkZ");
        String dimKey = tag.getString("linkDim");

        // Find target dimension
        ServerLevel targetLevel = null;
        if (level.getServer() != null) {
            for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
                if (serverLevel.dimension().location().toString().equals(dimKey)) {
                    targetLevel = serverLevel;
                    break;
                }
            }
        }

        if (targetLevel == null) {
            return false;
        }

        BlockPos targetPos = new BlockPos(lx, ly, lz);
        BlockEntity te = targetLevel.getBlockEntity(targetPos);

        if (te == null || !(te instanceof TileMirror tileMirror)) {
            // Mirror is gone, clear link
            mirror.setTag(null);
            player.playSound(ModSounds.ZAP.get(), 1.0f, 0.8f);
            player.sendSystemMessage(Component.translatable("tc.handmirrorerror"));
            return false;
        }

        // Try to transport the item
        if (tileMirror.transportDirect(items)) {
            items.setCount(0);
            player.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.1f, 1.0f);
            return true;
        }

        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("linkX")) {
                int lx = tag.getInt("linkX");
                int ly = tag.getInt("linkY");
                int lz = tag.getInt("linkZ");
                String dim = tag.getString("linkDim");
                tooltip.add(Component.translatable("tc.handmirrorlinkedto",
                        lx + "," + ly + "," + lz + " in " + dim)
                        .withStyle(style -> style.withColor(0x808080)));
            }
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
