package thaumcraft.common.world.aura.pressure;

import java.util.List;

import net.minecraft.util.RandomSource;

/**
 * The catalog of flux pressure event types and the weighted pick
 * (Thaumaturge FluxPressureEventTypes equivalent).
 */
public final class FluxPressureEventTypes {

    public static final List<FluxPressureEvent> ALL = List.of(
            new WispPressureEvent(),
            new LightningPressureEvent(),
            new RainPressureEvent(),
            new WarpPressureEvent(),
            new ExhaustPressureEvent());

    private FluxPressureEventTypes() {}

    public static FluxPressureEvent choose(RandomSource random) {
        int total = 0;
        for (FluxPressureEvent event : ALL) {
            total += event.weight();
        }
        int roll = random.nextInt(total);
        for (FluxPressureEvent event : ALL) {
            roll -= event.weight();
            if (roll < 0) {
                return event;
            }
        }
        return ALL.get(0);
    }
}
