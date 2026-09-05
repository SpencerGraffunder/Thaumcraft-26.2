package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import thaumcraft.common.menu.VoidSiphonMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.crafting.TileVoidSiphon;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;
import thaumcraft.init.BlockRegistration;

/**
 * BlockVoidSiphon - Harvests void seeds from flux rifts.
 * 
 * The void siphon draws energy from nearby flux rifts to generate
 * void seeds, a powerful crafting component.
 * Requires line-of-sight to a flux rift to operate.
 */
public class BlockVoidSiphon extends Block implements EntityBlock {
    
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    
    // Complex shape matching the original (3 parts: base, pillar, orb)
    private static final VoxelShape SHAPE_BASE = Block.box(3.0, 0.0, 3.0, 13.0, 2.0, 13.0);
    private static final VoxelShape SHAPE_TOP = Block.box(4.0, 2.0, 4.0, 12.0, 11.0, 12.0);
    private static final VoxelShape SHAPE_ORB = Block.box(5.0, 12.0, 5.0, 10.0, 16.0, 10.0);
    private static final VoxelShape SHAPE = Shapes.or(SHAPE_BASE, SHAPE_TOP, SHAPE_ORB);
    
    public BlockVoidSiphon() {
        super(BlockRegistration.id(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(3.0f)
                .sound(SoundType.METAL)
                .noOcclusion()));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ENABLED, true));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ENABLED);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                  BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileVoidSiphon siphon) {
            // Right-click with a hand to collect any void seeds
            ItemStack seeds = siphon.getItem(0);
            if (!seeds.isEmpty() && player.getMainHandItem().isEmpty()) {
                if (!player.getInventory().add(seeds)) {
                    player.drop(seeds, false);
                }
                siphon.setItem(0, ItemStack.EMPTY);
                return InteractionResult.SUCCESS;
            }
            // Otherwise open the void siphon GUI
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("container.thaumcraft.void_siphon");
                    }
                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player pl) {
                        return new VoidSiphonMenu(id, inv, siphon);
                    }
                }, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.CONSUME;
        }
        
        return InteractionResult.CONSUME;
    }
    
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TileVoidSiphon siphon) {
                ItemStack stack = siphon.getItem(0);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileVoidSiphon(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (type == ModBlockEntities.VOID_SIPHON.get()) {
            if (level.isClientSide()) {
                return (lvl, pos, st, be) -> TileVoidSiphon.clientTick(lvl, pos, st, (TileVoidSiphon) be);
            } else {
                return (lvl, pos, st, be) -> TileVoidSiphon.serverTick(lvl, pos, st, (TileVoidSiphon) be);
            }
        }
        return null;
    }
}
