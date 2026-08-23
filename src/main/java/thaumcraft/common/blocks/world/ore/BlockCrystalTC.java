package thaumcraft.common.blocks.world.ore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.init.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Crystal cluster blocks that grow on stone surfaces.
 * They interact with the aura - growing when vis is high, shrinking when low.
 */
public class BlockCrystalTC extends Block {

    public static final IntegerProperty SIZE = IntegerProperty.create("size", 0, 3);
    public static final IntegerProperty GENERATION = IntegerProperty.create("generation", 1, 4);

    private static final VoxelShape SHAPE_FULL = Shapes.block();
    private static final VoxelShape SHAPE_UP = Block.box(0.0, 8.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape SHAPE_DOWN = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    private static final VoxelShape SHAPE_NORTH = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 8.0);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0.0, 0.0, 8.0, 16.0, 16.0, 16.0);
    private static final VoxelShape SHAPE_EAST = Block.box(8.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape SHAPE_WEST = Block.box(0.0, 0.0, 0.0, 8.0, 16.0, 16.0);

    private final Aspect aspect;

    public BlockCrystalTC(Aspect aspect) {
        super(BlockBehaviour.Properties.of()
                .mapColor(getColorForAspect(aspect))
                .strength(0.25f)
                .sound(SoundType.GLASS)
                .lightLevel(state -> 1)
                .noOcclusion()
                .noCollision()
                .randomTicks());
        this.aspect = aspect;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(SIZE, 0)
                .setValue(GENERATION, 1));
    }

    private static MapColor getColorForAspect(Aspect aspect) {
        if (aspect == Aspect.AIR) return MapColor.COLOR_YELLOW;
        if (aspect == Aspect.FIRE) return MapColor.COLOR_RED;
        if (aspect == Aspect.WATER) return MapColor.COLOR_BLUE;
        if (aspect == Aspect.EARTH) return MapColor.COLOR_GREEN;
        if (aspect == Aspect.ORDER) return MapColor.QUARTZ;
        if (aspect == Aspect.ENTROPY) return MapColor.COLOR_GRAY;
        if (aspect == Aspect.FLUX) return MapColor.COLOR_PURPLE;
        return MapColor.QUARTZ;
    }

    public Aspect getAspect() {
        return aspect;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIZE, GENERATION);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Determine shape based on attached surface
        int attachedCount = 0;
        Direction attachedDir = null;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (isValidSupport(neighborState, level, neighborPos, dir.getOpposite())) {
                attachedCount++;
                attachedDir = dir;
            }
        }

        if (attachedCount > 1 || attachedDir == null) {
            return SHAPE_FULL;
        }

        return switch (attachedDir) {
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
        };
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isTouchingStone(level, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                   BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int generation = state.getValue(GENERATION);
        if (random.nextInt(3 + generation) != 0) {
            return;
        }

        int threshold = 10;
        int growth = state.getValue(SIZE);

        if (aspect != Aspect.FLUX) {
            // Normal crystal - interacts with vis
            float vis = AuraHelper.getVis(level, pos);
            int auraBase = AuraHelper.getAuraBase(level, pos);

            if (vis <= threshold) {
                // Low vis - crystal shrinks
                if (growth > 0) {
                    level.setBlock(pos, state.setValue(SIZE, growth - 1), 3);
                    AuraHelper.addVis(level, pos, threshold);
                } else if (isTouchingOtherCrystal(level, pos)) {
                    level.removeBlock(pos, false);
                    AuraHelper.addVis(level, pos, threshold);
                }
            } else if (vis > auraBase + threshold) {
                // High vis - crystal grows or spreads
                long maxGrowth = 5 - generation + (pos.asLong() % 3);
                if (growth < 3 && growth < maxGrowth) {
                    if (AuraHelper.drainVis(level, pos, threshold, false) > 0) {
                        level.setBlock(pos, state.setValue(SIZE, growth + 1), 3);
                    }
                } else if (generation < 4) {
                    BlockPos spreadPos = findSpreadPosition(level, pos, random);
                    if (spreadPos != null && AuraHelper.drainVis(level, pos, threshold, false) > 0) {
                        int newGen = random.nextInt(6) == 0 ? generation : generation + 1;
                        level.setBlock(spreadPos, this.defaultBlockState().setValue(GENERATION, Math.min(4, newGen)), 3);
                    }
                }
            }
        } else {
            // Flux crystal - interacts with flux
            float flux = AuraHelper.getFlux(level, pos);
            int auraBase = AuraHelper.getAuraBase(level, pos);

            if (flux <= threshold) {
                if (growth > 0) {
                    level.setBlock(pos, state.setValue(SIZE, growth - 1), 3);
                    AuraHelper.polluteAura(level, pos, threshold, false);
                } else if (isTouchingOtherCrystal(level, pos)) {
                    level.removeBlock(pos, false);
                    AuraHelper.polluteAura(level, pos, threshold, false);
                }
            } else if (flux > auraBase + threshold) {
                long maxGrowth = 5 - generation + (pos.asLong() % 3);
                if (growth < 3 && growth < maxGrowth) {
                    if (AuraHelper.drainFlux(level, pos, threshold, false) > 0) {
                        level.setBlock(pos, state.setValue(SIZE, growth + 1), 3);
                    }
                } else if (generation < 4) {
                    BlockPos spreadPos = findSpreadPosition(level, pos, random);
                    if (spreadPos != null && AuraHelper.drainFlux(level, pos, threshold, false) > 0) {
                        int newGen = random.nextInt(6) == 0 ? generation : generation + 1;
                        level.setBlock(spreadPos, this.defaultBlockState().setValue(GENERATION, Math.min(4, newGen)), 3);
                    }
                }
            }
        }
    }

    private boolean isTouchingStone(LevelReader level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (isValidSupport(neighborState, level, neighborPos, dir.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidSupport(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        // Check if the block is a solid stone-like block
        return state.isFaceSturdy(level, pos, face) && 
               (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || 
                state.is(Blocks.GRANITE) || state.is(Blocks.DIORITE) || 
                state.is(Blocks.ANDESITE) || state.is(Blocks.TUFF) ||
                state.is(Blocks.COBBLESTONE) || state.is(Blocks.MOSSY_COBBLESTONE));
    }

    private boolean isTouchingOtherCrystal(LevelReader level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState neighborState = level.getBlockState(pos.relative(dir));
            if (neighborState.getBlock() == this) {
                return true;
            }
        }
        return false;
    }

    private BlockPos findSpreadPosition(Level level, BlockPos pos, RandomSource random) {
        int x = pos.getX() + random.nextInt(3) - 1;
        int y = pos.getY() + random.nextInt(3) - 1;
        int z = pos.getZ() + random.nextInt(3) - 1;
        BlockPos targetPos = new BlockPos(x, y, z);

        if (targetPos.equals(pos)) {
            return null;
        }

        BlockState targetState = level.getBlockState(targetPos);
        if ((level.isEmptyBlock(targetPos) || targetState.canBeReplaced()) &&
            random.nextInt(16) == 0 &&
            isTouchingStone(level, targetPos)) {
            return targetPos;
        }

        return null;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>();
        
        ItemInstance tool = builder.getOptionalParameter(LootContextParams.TOOL);
        boolean silkTouch = tool != null && getEnchantmentLevel(builder.getLevel(), Enchantments.SILK_TOUCH, tool) > 0;
        
        if (silkTouch) {
            // Silk touch - drop the block itself
            drops.add(new ItemStack(this));
        } else {
            // Normal drop - vis crystals based on block's aspect
            int fortune = tool != null ? getEnchantmentLevel(builder.getLevel(), Enchantments.FORTUNE, tool) : 0;
            int baseCount = 1 + state.getValue(SIZE);
            int count = baseCount + (fortune > 0 ? builder.getLevel().getRandom().nextInt(fortune + 1) : 0);
            
            // Get the correct vis crystal item for this aspect
            Item crystalItem = getCrystalItemForAspect(aspect);
            if (crystalItem != null) {
                drops.add(new ItemStack(crystalItem, count));
            }
        }
        
        return drops;
    }
    
    /**
     * Get the vis crystal item that corresponds to this block's aspect.
     */
    private static Item getCrystalItemForAspect(Aspect aspect) {
        if (aspect == Aspect.AIR) return ModItems.VIS_CRYSTAL_AIR.get();
        if (aspect == Aspect.FIRE) return ModItems.VIS_CRYSTAL_FIRE.get();
        if (aspect == Aspect.WATER) return ModItems.VIS_CRYSTAL_WATER.get();
        if (aspect == Aspect.EARTH) return ModItems.VIS_CRYSTAL_EARTH.get();
        if (aspect == Aspect.ORDER) return ModItems.VIS_CRYSTAL_ORDER.get();
        if (aspect == Aspect.ENTROPY) return ModItems.VIS_CRYSTAL_ENTROPY.get();
        // Flux crystals don't drop vis crystals - they might drop flux_crystal item
        if (aspect == Aspect.FLUX) return ModItems.FLUX_CRYSTAL.get();
        return null;
    }

    /**
     * Get the gameplay level of an enchantment on a tool.
     */
    private static int getEnchantmentLevel(net.minecraft.server.level.ServerLevel level,
                                           net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantment,
                                           ItemInstance tool) {
        net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> holder =
                level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(enchantment);
        return tool.getEnchantmentLevel(holder);
    }
}
