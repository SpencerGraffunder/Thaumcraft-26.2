package thaumcraft.common.tiles.devices;

import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

/**
 * Bellows tile entity - speeds up adjacent furnaces and Thaumcraft devices.
 * Animates on client, accelerates processing on server.
 */
public class TileBellows extends TileThaumcraft {

    // Animation state (client-side)
    public float inflation = 1.0f;
    private boolean direction = false;
    private boolean firstRun = true;
    
    // Tick delay for acceleration
    public int delay = 0;

    public TileBellows(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileBellows(BlockPos pos, BlockState state) {
        this(ModBlockEntities.BELLOWS.get(), pos, state);
    }

    // ==================== Tick ====================

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileBellows tile) {
        if (!isEnabled(state)) return;

        if (tile.firstRun) {
            tile.inflation = 0.35f + level.getRandom().nextFloat() * 0.55f;
            tile.firstRun = false;
        }

        // Deflate
        if (tile.inflation > 0.35f && !tile.direction) {
            tile.inflation -= 0.075f;
        }
        if (tile.inflation <= 0.35f && !tile.direction) {
            tile.direction = true;
        }

        // Inflate
        if (tile.inflation < 1.0f && tile.direction) {
            tile.inflation += 0.025f;
        }
        if (tile.inflation >= 1.0f && tile.direction) {
            tile.direction = false;
            level.playLocalSound(
                    pos.getX(), pos.getY(), pos.getZ(),
                    SoundEvents.GHAST_SHOOT, SoundSource.BLOCKS,
                    0.01f, 0.5f + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2f,
                    false
            );
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileBellows tile) {
        if (!isEnabled(state)) return;

        tile.delay++;
        if (tile.delay >= 2) {
            tile.delay = 0;
            
            Direction facing = getFacing(state);
            if (facing != null) {
                BlockEntity targetTile = level.getBlockEntity(pos.relative(facing));
                
                // Speed up vanilla furnaces
                if (targetTile instanceof FurnaceBlockEntity furnace) {
                    // Advance the vanilla furnace cook progress (see speedUpFurnace)
                    speedUpFurnace(furnace);
                }
                
                // Thaumcraft devices (smelter, etc.) read the bellows bonus themselves via\n            // TileBellows.getBellows(...), so no direct action is needed here.
            }
        }
    }

    // Cached reflection handles for the (private) AbstractFurnaceBlockEntity
    // cook-progress fields. Vanilla hides these, so we access them reflectively
    // with a one-time lookup and graceful fallback (matches the 1.12.2 behavior
    // where Bellows bumped the furnace cook time by 1 every 2 ticks).
    private static volatile Field F_COOKING_TIME;
    private static volatile Field F_COOKING_TOTAL;

    private static void speedUpFurnace(FurnaceBlockEntity furnace) {
        try {
            Class<?> cls = furnace.getClass().getSuperclass(); // AbstractFurnaceBlockEntity
            if (F_COOKING_TIME == null) {
                F_COOKING_TIME = cls.getDeclaredField("cookingTimer");
                F_COOKING_TIME.setAccessible(true);
                F_COOKING_TOTAL = cls.getDeclaredField("cookingTotalTime");
                F_COOKING_TOTAL.setAccessible(true);
            }
            int cur = F_COOKING_TIME.getInt(furnace);
            int total = F_COOKING_TOTAL.getInt(furnace);
            // Only boost while actively cooking and not already complete
            if (cur > 0 && cur < total) {
                F_COOKING_TIME.setInt(furnace, cur + 1);
            }
        } catch (Exception ignored) {
            // Field names/visibility may change between MC versions; fail silently
            // rather than crashing the tick.
        }
    }

    // ==================== State Helpers ====================

    private static boolean isEnabled(BlockState state) {
        // Check if block has ENABLED property and is enabled
        // Or check if block has POWERED property
        if (state.hasProperty(BlockStateProperties.ENABLED)) {
            return state.getValue(BlockStateProperties.ENABLED);
        }
        // Default to enabled if no property exists
        return true;
    }

    public static Direction getFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    // ==================== Static Utility ====================

    /**
     * Count active bellows adjacent to a position in specified directions.
     * Used by smelters and other devices to calculate speed bonuses.
     * 
     * @param level The world
     * @param pos Position to check around
     * @param directions Directions to check for bellows
     * @return Number of active bellows found
     */
    public static int getBellows(Level level, BlockPos pos, Direction[] directions) {
        int bellows = 0;
        for (Direction dir : directions) {
            BlockEntity tile = level.getBlockEntity(pos.relative(dir));
            if (tile instanceof TileBellows) {
                BlockState state = tile.getBlockState();
                Direction bellowsFacing = getFacing(state);
                // Bellows must be facing toward the device and enabled
                if (bellowsFacing == dir.getOpposite() && isEnabled(state)) {
                    bellows++;
                }
            }
        }
        return bellows;
    }

    // ==================== Rendering ====================

    /**
     * Custom render bounding box for rendering.
     * Note: In 1.20.1, this is accessed via TESR if needed.
     */
    public AABB getCustomRenderBoundingBox() {
        return new AABB(
                worldPosition.getX() - 0.3, worldPosition.getY() - 0.3, worldPosition.getZ() - 0.3,
                worldPosition.getX() + 1.3, worldPosition.getY() + 1.3, worldPosition.getZ() + 1.3
        );
    }
}
