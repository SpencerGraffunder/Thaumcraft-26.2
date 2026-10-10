package thaumcraft.common.world.aura.pressure;

/**
 * Base for flux pressure events, carrying the immutable traits (Thaumaturge
 * AbstractFluxPressureEvent equivalent).
 */
public abstract class AbstractFluxPressureEvent implements FluxPressureEvent {

    private final String name;
    private final int weight;
    private final float cost;
    private final boolean allowedNearTaint;

    protected AbstractFluxPressureEvent(String name, int weight, float cost, boolean allowedNearTaint) {
        this.name = name;
        this.weight = weight;
        this.cost = cost;
        this.allowedNearTaint = allowedNearTaint;
    }

    @Override
    public final String name() {
        return this.name;
    }

    @Override
    public final int weight() {
        return this.weight;
    }

    @Override
    public final float cost() {
        return this.cost;
    }

    @Override
    public final boolean allowedNearTaint() {
        return this.allowedNearTaint;
    }
}
