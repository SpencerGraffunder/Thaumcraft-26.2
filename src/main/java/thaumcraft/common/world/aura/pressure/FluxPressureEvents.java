package thaumcraft.common.world.aura.pressure;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Hub for saturated-flux pressure events (Thaumaturge FluxPressureEvents
 * equivalent). A saturated chunk queues a trigger; each server tick the
 * pending trigger resolves one weighted-random event at a random surface
 * position in that chunk, draining flux to pay its cost.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class FluxPressureEvents {

    private static final double STACK_SUPPRESSION_RANGE_SQ = 32.0 * 32.0;
    private static final float COST_EPSILON = 0.001F;
    private static final int CHUNK_WIDTH = 16;

    private static final Map<ResourceKey<Level>, FluxPressureState> STATES = new ConcurrentHashMap<>();

    private FluxPressureEvents() {}

    public static boolean isEnabled() {
        return !ModConfig.wussMode;
    }

    private static FluxPressureState stateOf(ServerLevel level) {
        return STATES.computeIfAbsent(level.dimension(), dim -> new FluxPressureState());
    }

    /** Queue a pressure-event trigger for the chunk containing this position. */
    public static void queue(ServerLevel level, ChunkPos chunkPos) {
        if (isEnabled()) {
            stateOf(level).queue(new BlockPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ()));
        }
    }

    /**
     * Fire a specific event at the origin: suppresses near taint/rain for
     * non-taint-safe events, pays the flux cost, then fires.
     *
     * @return true if the event fired and paid its cost.
     */
    public static boolean trigger(ServerLevel level, BlockPos origin, FluxPressureEvent event) {
        if (!isEnabled()) {
            return false;
        }
        FluxPressureState state = stateOf(level);
        if (!event.allowedNearTaint()
                && (TaintHelper.isNearTaintSeed(level, origin)
                    || state.hasRainNear(origin, STACK_SUPPRESSION_RANGE_SQ))) {
            return false;
        }
        if (AuraHandler.drainFlux(level, origin, event.cost(), true) + COST_EPSILON < event.cost()) {
            return false;
        }
        if (!event.fire(level, origin, state)) {
            return false;
        }
        AuraHandler.drainFlux(level, origin, event.cost(), false);
        return true;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!isEnabled()) {
            return;
        }
        FluxPressureState state = STATES.get(level.dimension());
        if (state == null) {
            return;
        }
        @Nullable
        BlockPos pending = state.pollPending();
        if (pending != null) {
            RandomSource random = level.getRandom();
            BlockPos origin = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                    pending.offset(random.nextInt(CHUNK_WIDTH), 0, random.nextInt(CHUNK_WIDTH)));
            trigger(level, origin, FluxPressureEventTypes.choose(random));
        }
        state.tick(level);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            STATES.remove(level.dimension());
        }
    }
}
