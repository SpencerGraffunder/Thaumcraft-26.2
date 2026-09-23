package thaumcraft.common.blocks.essentia;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.Aspect;
import net.minecraft.world.item.ItemStack;

import net.minecraft.core.Direction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.tiles.essentia.TileEssentiaReservoir;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;
import thaumcraft.init.BlockRegistration;

/**
 * Essentia Reservoir - A large tank for storing essentia.
 * 
 * Stores 500 units of essentia (double a normal jar).
 * Can connect to tubes from any side.
 * Adjacent reservoirs will balance their contents.
 */
public class BlockEssentiaReservoir extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    public BlockEssentiaReservoir() {
        super(BlockRegistration.id(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0f)
                .sound(SoundType.METAL)
                .noOcclusion()));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                  BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // Phial interaction: right-click with an empty essentia container to extract 1 essence
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof IEssentiaContainerItem container) {
            AspectList heldAspects = container.getAspects(held);
            if (heldAspects == null || heldAspects.size() == 0) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof thaumcraft.common.tiles.essentia.TileEssentiaReservoir tile) {
                    Direction side = hit.getDirection();
                    Aspect aspect = tile.getEssentiaType(side);
                    int amt = tile.getEssentiaAmount(side);
                    if (aspect != null && amt > 0) {
                        if (tile.takeFromContainer(aspect, 1)) {
                            AspectList cur = container.getAspects(held);
                            if (cur == null) cur = new AspectList();
                            cur.add(aspect, 1);
                            container.setAspects(held, cur);
                                                        return InteractionResult.CONSUME;
                        }
                    }
                }
            }
        }


        // Fill from phial to reservoir
        ItemStack held2 = player.getMainHandItem();
        if (held2.getItem() instanceof IEssentiaContainerItem container2) {
            AspectList heldAspects2 = container2.getAspects(held2);
            if (heldAspects2 != null && heldAspects2.size() > 0) {
                BlockEntity be2 = level.getBlockEntity(pos);
                if (be2 instanceof TileEssentiaReservoir tile2) {
                    Aspect aspect2 = heldAspects2.getAspects()[0];
                    if (tile2.addToContainer(aspect2, 1) > 0) {
                        AspectList cur2 = container2.getAspects(held2);
                        cur2.add(aspect2, -1);
                        container2.setAspects(held2, cur2);
                        return InteractionResult.CONSUME;
                    }
                }
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            // 1.12: breaking a reservoir pollutes the aura with the lost essentia
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TileEssentiaReservoir res) {
                int lost = res.getAmount();
                if (lost > 0) {
                    AuraHelper.polluteAura(level, pos, lost / 10f, true);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileEssentiaReservoir reservoir) {
            return (int) (reservoir.getFillPercent() * 15);
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEssentiaReservoir(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return type == ModBlockEntities.ESSENTIA_RESERVOIR.get() ?
                (lvl, pos, st, te) -> TileEssentiaReservoir.serverTick(lvl, pos, st, (TileEssentiaReservoir) te) : null;
    }
}
