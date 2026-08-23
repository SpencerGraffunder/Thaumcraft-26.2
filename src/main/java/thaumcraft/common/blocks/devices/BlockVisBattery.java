package thaumcraft.common.blocks.devices;

import net.minecraft.core.Direction;
import net.minecraft.world.level.redstone.Orientation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Vis Battery - stores vis from the aura and releases it when powered by redstone.
 * 
 * When powered by redstone:
 *   - Releases stored vis back into the aura (1 vis per tick, up to 10 stored)
 * 
 * When not powered:
 *   - Absorbs vis from the aura when aura is above 90% of base
 *   - Releases vis to aura when aura drops below 75% of base
 * 
 * Provides comparator output based on charge level (0-10).
 * Light level scales with charge.
 */
public class BlockVisBattery extends Block {
    
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 10);
    
    public BlockVisBattery() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(0.5f)
                .sound(SoundType.STONE)
                .randomTicks()
                .lightLevel(state -> state.getValue(CHARGE)));
        
        registerDefaultState(stateDefinition.any().setValue(CHARGE, 0));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }
    
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int charge = state.getValue(CHARGE);
        
        if (level.hasNeighborSignal(pos)) {
            // Powered by redstone - release vis
            if (charge > 0) {
                AuraHandler.addVis(level, pos, 1.0f);
                level.setBlockAndUpdate(pos, state.setValue(CHARGE, charge - 1));
                level.scheduleTick(pos, this, 5);
            }
        } else {
            // Not powered - regulate aura
            float aura = AuraHelper.getVis(level, pos);
            int base = AuraHelper.getAuraBase(level, pos);
            
            if (charge < 10 && aura > base * 0.9 && aura > 1.0f) {
                // Aura is high - absorb some
                AuraHandler.drainVis(level, pos, 1.0f, false);
                level.setBlockAndUpdate(pos, state.setValue(CHARGE, charge + 1));
                level.scheduleTick(pos, this, 100 + random.nextInt(100));
            } else if (charge > 0 && aura < base * 0.75) {
                // Aura is low - release some
                AuraHandler.addVis(level, pos, 1.0f);
                level.setBlockAndUpdate(pos, state.setValue(CHARGE, charge - 1));
                level.scheduleTick(pos, this, 20 + random.nextInt(20));
            }
        }
    }
    
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean isMoving) {
        if (level.hasNeighborSignal(pos)) {
            level.scheduleTick(pos, this, 1);
        }
    }
    
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
    
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(CHARGE);
    }
    
    // Factory method for registration
    public static BlockVisBattery create() {
        return new BlockVisBattery();
    }
}
