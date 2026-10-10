package thaumcraft.common.world.aura.pressure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.entities.monster.EntityWisp;
import thaumcraft.init.ModEntities;

/**
 * Saturated flux spawns a wisp above the surface, 1/3 of the time carrying
 * the vitium (FLUX) aspect (Thaumaturge WispPressureEvent equivalent).
 */
public final class WispPressureEvent extends AbstractFluxPressureEvent {

    private static final int WEIGHT = 25;
    private static final float COST = 5.0F;
    private static final boolean ALLOWED_NEAR_TAINT = true;
    private static final int HEIGHT_ABOVE_SURFACE = 5;
    private static final int VITIUM_CHANCE = 3;

    public WispPressureEvent() {
        super("wisp", WEIGHT, COST, ALLOWED_NEAR_TAINT);
    }

    @Override
    public boolean fire(ServerLevel level, BlockPos origin, FluxPressureState state) {
        BlockPos spawn = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, origin).above(HEIGHT_ABOVE_SURFACE);
        if (spawn.getY() >= level.getMaxY() || !level.hasChunkAt(spawn)) {
            return false;
        }
        EntityWisp wisp = ModEntities.WISP.get().create(level, EntitySpawnReason.EVENT);
        if (wisp == null) {
            return false;
        }
        wisp.setPos(spawn.getX() + 0.5, spawn.getY() + 0.5, spawn.getZ() + 0.5);
        wisp.setYRot(0.0f);
        wisp.setXRot(0.0f);
        if (level.getRandom().nextInt(VITIUM_CHANCE) == 0) {
            wisp.setWispType(Aspect.FLUX.getTag());
        }
        if (!level.noCollision(wisp)) {
            wisp.discard();
            return false;
        }
        level.addFreshEntity(wisp);
        return true;
    }
}
