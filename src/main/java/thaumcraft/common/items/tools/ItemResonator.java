package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.IEssentiaTransport;

/**
 * Resonator - A diagnostic tool for essentia transport systems.
 * Right-click on essentia pipes/containers to see their contents and suction.
 */
public class ItemResonator extends Item {

    public ItemResonator() {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack theStack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        Direction side = context.getClickedFace();

        if (player == null) {
            return InteractionResult.FAIL;
        }

        BlockEntity tile = level.getBlockEntity(pos);
        if (tile == null || !(tile instanceof IEssentiaTransport)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            player.swingAndResetAttackStrength(context.getHand(), net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
            return InteractionResult.SUCCESS;
        }

        IEssentiaTransport transport = (IEssentiaTransport) tile;

        // Display essentia type and amount (1.12: normal containers show the
        // single type; tube buffers list every aspect they hold, sorted by name)
        if (!(tile instanceof thaumcraft.common.tiles.essentia.TileTubeBuffer) && transport.getEssentiaType(side) != null) {
            player.sendSystemMessage(Component.translatable("tc.resonator1",
                    String.valueOf(transport.getEssentiaAmount(side)),
                    transport.getEssentiaType(side).getName()));
        } else if (tile instanceof thaumcraft.common.tiles.essentia.TileTubeBuffer && ((thaumcraft.api.aspects.IAspectContainer) tile).getAspects().size() > 0) {
            for (thaumcraft.api.aspects.Aspect aspect : ((thaumcraft.api.aspects.IAspectContainer) tile).getAspects().getAspectsSortedByName()) {
                player.sendSystemMessage(Component.translatable("tc.resonator1",
                        String.valueOf(((thaumcraft.api.aspects.IAspectContainer) tile).getAspects().getAmount(aspect)),
                        aspect.getName()));
            }
        }

        // Display suction info
        String suctionType = "tc.resonator3"; // "None"
        if (transport.getSuctionType(side) != null) {
            suctionType = transport.getSuctionType(side).getName();
        }
        player.sendSystemMessage(Component.translatable("tc.resonator2",
                String.valueOf(transport.getSuctionAmount(side)),
                suctionType));

        // Play sound
        level.playSound(null, pos, SoundEvents.SHIELD_BLOCK.value(), SoundSource.BLOCKS,
                0.5f, 1.9f + level.getRandom().nextFloat() * 0.1f);

        // Condenser: show its cost and interval (1.12 tc.condenser1/2)
        if (tile instanceof thaumcraft.common.tiles.devices.TileCondenser condenser) {
            player.sendSystemMessage(Component.translatable("tc.condenser1", String.valueOf(condenser.cost)));
            player.sendSystemMessage(Component.translatable("tc.condenser2",
                    String.valueOf(condenser.interval), String.valueOf(condenser.interval / 20)));
        }

        return InteractionResult.SUCCESS;
    }
}
