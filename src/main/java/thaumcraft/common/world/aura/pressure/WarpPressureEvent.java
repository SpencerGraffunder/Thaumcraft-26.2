package thaumcraft.common.world.aura.pressure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Saturated flux warps nearby players: 25% chance of 1 normal warp, otherwise
 * 2-5 temporary warp (Thaumaturge WarpPressureEvent equivalent).
 */
public final class WarpPressureEvent extends AbstractFluxPressureEvent {

    private static final int WEIGHT = 5;
    private static final float COST = 20.0F;
    private static final boolean ALLOWED_NEAR_TAINT = true;
    private static final double RANGE = 16.0;
    private static final float NORMAL_WARP_CHANCE = 0.25F;
    private static final int NORMAL_WARP = 1;
    private static final int MIN_TEMPORARY_WARP = 2;
    private static final int TEMPORARY_WARP_SPREAD = 4;

    public WarpPressureEvent() {
        super("warp", WEIGHT, COST, ALLOWED_NEAR_TAINT);
    }

    @Override
    public boolean fire(ServerLevel level, BlockPos origin, FluxPressureState state) {
        List<ServerPlayer> targets = level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(origin).inflate(RANGE));
        if (targets.isEmpty()) {
            return false;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : targets) {
            @Nullable
            IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
            if (warp == null) {
                continue;
            }
            if (random.nextFloat() < NORMAL_WARP_CHANCE) {
                warp.add(IPlayerWarp.EnumWarpType.NORMAL, NORMAL_WARP);
            } else {
                warp.add(IPlayerWarp.EnumWarpType.TEMPORARY,
                        MIN_TEMPORARY_WARP + random.nextInt(TEMPORARY_WARP_SPREAD));
            }
        }
        return true;
    }
}
