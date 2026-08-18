package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.IPlantable;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.world.aura.AuraHandler;
import thaumcraft.init.ModBlocks;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TaintHelper - Manages taint spread and purification mechanics.
 * 
 * Taint spreads from taint seeds (EntityTaintSeed) and converts blocks
 * into tainted variants. Flux saturation increases spread rate.
 */
public class TaintHelper {
    
    // Track taint seeds per dimension
    private static final ConcurrentHashMap<ResourceKey<Level>, ArrayList<BlockPos>> taintSeeds = new ConcurrentHashMap<>();
    
    /**
     * Register a taint seed location.
     */
    public static void addTaintSeed(Level level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        ArrayList<BlockPos> locs = taintSeeds.computeIfAbsent(dim, k -> new ArrayList<>());
        if (!locs.contains(pos)) {
            locs.add(pos);
        }
    }
    
    /**
     * Remove a taint seed location.
     */
    public static void removeTaintSeed(Level level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        ArrayList<BlockPos> locs = taintSeeds.get(dim);
        if (locs != null && !locs.isEmpty()) {
            locs.remove(pos);
        }
    }
    
    /**
     * Check if a position is within range of a taint seed.
     */
    public static boolean isNearTaintSeed(Level level, BlockPos pos) {
        double area = ModConfig.taintSpreadArea * ModConfig.taintSpreadArea;
        ResourceKey<Level> dim = level.dimension();
        ArrayList<BlockPos> locs = taintSeeds.get(dim);
        
        if (locs != null && !locs.isEmpty()) {
            for (BlockPos p : new ArrayList<>(locs)) { // Copy to avoid CME
                if (p.distSqr(pos) <= area) {
                    // Verify the seed entity still exists
                    // TODO: Check for EntityTaintSeed when ported
                    // For now, just return true if within range
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if a position is at the edge of a taint seed's influence.
     */
    public static boolean isAtTaintSeedEdge(Level level, BlockPos pos) {
        double area = ModConfig.taintSpreadArea * ModConfig.taintSpreadArea;
        double fringe = ModConfig.taintSpreadArea * 0.8 * (ModConfig.taintSpreadArea * 0.8);
        ResourceKey<Level> dim = level.dimension();
        ArrayList<BlockPos> locs = taintSeeds.get(dim);
        
        if (locs != null && !locs.isEmpty()) {
            for (BlockPos p : new ArrayList<>(locs)) {
                double d = p.distSqr(pos);
                if (d < area && d > fringe) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Spread taint fibres from a position.
     */
    public static void spreadFibres(Level level, BlockPos pos) {
        spreadFibres(level, pos, false);
    }
    
    /**
     * Spread taint fibres from a position.
     * @param ignore If true, ignores wuss mode and rate checks
     */
    public static void spreadFibres(Level level, BlockPos pos, boolean ignore) {
        if (level.isClientSide) return;
        
        // Check wuss mode
        if (!ignore && ModConfig.wussMode) {
            return;
        }
        
        // Rate limiting based on flux saturation
        float mod = 0.001f + AuraHandler.getFluxSaturation(level, pos) * 2.0f;
        if (!ignore && level.getRandom().nextFloat() > ModConfig.taintSpreadRate / 100.0f * mod) {
            return;
        }
        
        if (!isNearTaintSeed(level, pos)) {
            return;
        }
        
        // Pick a random adjacent block
        int xx = pos.getX() + level.getRandom().nextInt(3) - 1;
        int yy = pos.getY() + level.getRandom().nextInt(3) - 1;
        int zz = pos.getZ() + level.getRandom().nextInt(3) - 1;
        BlockPos target = new BlockPos(xx, yy, zz);
        
        if (target.equals(pos)) {
            return;
        }
        
        BlockState bs = level.getBlockState(target);
        Block block = bs.getBlock();
        float hardness = bs.getDestroySpeed(level, target);
        
        // Don't convert unbreakable or very hard blocks
        if (hardness < 0.0f || hardness > 10.0f) {
            return;
        }
        
        MapColor material = bs.getMapColor(level, target);
        
        // Check if block can be converted to taint fibre
        if (!block.defaultBlockState().canOcclude() && 
            !material.equals(MapColor.WATER) &&
            (level.isEmptyBlock(target) || 
             bs.canBeReplaced() || 
             block instanceof BushBlock || 
             block instanceof IPlantable) &&
            BlockUtils.isAdjacentToSolidBlock(level, target) &&
            !BlockTaintFibre.isOnlyAdjacentToTaint(level, target)) {
            
            // Convert to taint fibre
            if (ModBlocks.TAINT_FIBRE != null) {
                level.setBlockAndUpdate(target, ModBlocks.TAINT_FIBRE.get().defaultBlockState());
                level.blockEvent(target, ModBlocks.TAINT_FIBRE.get(), 1, 0);
                AuraHelper.drainFlux(level, target, 0.01f, false);
            }
            return;
        }
        
        // Convert leaves - check if adjacent to taint log or fibre
        if (bs.is(net.minecraft.tags.BlockTags.LEAVES)) {
            Direction face = BlockUtils.getFaceBlockTouching(level, target, ModBlocks.TAINT_FIBRE.get());
            if (face == null && ModBlocks.TAINT_LOG != null) {
                face = BlockUtils.getFaceBlockTouching(level, target, ModBlocks.TAINT_LOG.get());
            }
            if (level.getRandom().nextFloat() < 0.6 && face != null && ModBlocks.TAINT_FEATURE != null) {
                // Convert to taint feature with facing
                level.setBlockAndUpdate(target, ModBlocks.TAINT_FEATURE.get().defaultBlockState()
                        .setValue(BlockTaintFeature.FACING, face));
                level.blockEvent(target, ModBlocks.TAINT_FEATURE.get(), 1, 0);
            } else if (ModBlocks.TAINT_FIBRE != null) {
                level.setBlockAndUpdate(target, ModBlocks.TAINT_FIBRE.get().defaultBlockState());
                level.blockEvent(target, ModBlocks.TAINT_FIBRE.get(), 1, 0);
                AuraHelper.drainFlux(level, target, 0.01f, false);
            }
            return;
        }
        
        // Convert blocks that are surrounded by taint
        if (BlockTaintFibre.isHemmedByTaint(level, target) && hardness < 5.0f) {
            // Convert logs to taint log
            if (bs.is(net.minecraft.tags.BlockTags.LOGS)) {
                if (ModBlocks.TAINT_LOG != null) {
                    // Preserve axis from original log if possible
                    Direction.Axis axis = Direction.Axis.Y;
                    if (bs.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)) {
                        axis = bs.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS);
                    }
                    level.setBlockAndUpdate(target, ModBlocks.TAINT_LOG.get().defaultBlockState()
                            .setValue(BlockTaintLog.AXIS, axis));
                    level.blockEvent(target, ModBlocks.TAINT_LOG.get(), 1, 0);
                    AuraHelper.drainFlux(level, target, 0.01f, false);
                }
                return;
            }
            
            // Convert mushroom blocks, gourds, cacti, coral, sponge, wood to taint crust
            if (block == Blocks.RED_MUSHROOM_BLOCK || block == Blocks.BROWN_MUSHROOM_BLOCK ||
                material == MapColor.COLOR_GREEN || // Cactus-like
                material == MapColor.PLANT ||       // Plant materials
                material == MapColor.WOOD) {
                if (ModBlocks.TAINT_CRUST != null) {
                    level.setBlockAndUpdate(target, ModBlocks.TAINT_CRUST.get().defaultBlockState());
                    level.blockEvent(target, ModBlocks.TAINT_CRUST.get(), 1, 0);
                    AuraHelper.drainFlux(level, target, 0.01f, false);
                }
                return;
            }
            
            // Convert sand, dirt, grass, clay to taint soil
            if (material == MapColor.SAND || material == MapColor.DIRT || 
                material == MapColor.GRASS || material == MapColor.CLAY) {
                if (ModBlocks.TAINT_SOIL != null) {
                    level.setBlockAndUpdate(target, ModBlocks.TAINT_SOIL.get().defaultBlockState());
                    level.blockEvent(target, ModBlocks.TAINT_SOIL.get(), 1, 0);
                    AuraHelper.drainFlux(level, target, 0.01f, false);
                }
                return;
            }
            
            // Convert stone to taint rock
            if (material == MapColor.STONE) {
                if (ModBlocks.TAINT_ROCK != null) {
                    level.setBlockAndUpdate(target, ModBlocks.TAINT_ROCK.get().defaultBlockState());
                    level.blockEvent(target, ModBlocks.TAINT_ROCK.get(), 1, 0);
                    AuraHelper.drainFlux(level, target, 0.01f, false);
                }
                return;
            }
        }
        
        // Spawn new taint seeds at the edge of influence when flux is high
        // TODO: Implement when EntityTaintSeed is ported
    }
    
    /**
     * Clear all taint seed tracking data.
     * Called on dimension unload.
     */
    public static void clearDimension(ResourceKey<Level> dimension) {
        taintSeeds.remove(dimension);
    }
    
    /**
     * Clear all taint seed data.
     * Called on server shutdown.
     */
    public static void clearAll() {
        taintSeeds.clear();
    }
}
