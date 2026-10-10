package thaumcraft.common.world.aura.pressure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.init.ModBlocks;

/**
 * Flux goo placement helper for pressure events (Thaumaturge PhysicalFlux
 * equivalent). Our goo block's default state is a full-level (7) pool.
 */
public final class FluxGooUtil {

    private FluxGooUtil() {}

    /**
     * Place a full flux goo pool at the position if it is air/replaceable.
     */
    public static boolean placeGoo(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        BlockState at = level.getBlockState(pos);
        if (!at.isAir() && !at.canBeReplaced()) {
            return false;
        }
        level.setBlockAndUpdate(pos, ModBlocks.FLUX_GOO.get().defaultBlockState());
        return true;
    }
}
