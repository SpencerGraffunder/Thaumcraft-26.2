package thaumcraft.common.lib.research;

/**
 * Stat-threshold based discovery entry.
 * Grants research once the player's movement stat crosses the threshold.
 */
public class StatDiscovery {

    private final String key;
    private final String name;
    private final int statThreshold;

    public StatDiscovery(String key, String name, int statThreshold) {
        this.key = key;
        this.name = name;
        this.statThreshold = statThreshold;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public int getStatThreshold() {
        return statThreshold;
    }
}
