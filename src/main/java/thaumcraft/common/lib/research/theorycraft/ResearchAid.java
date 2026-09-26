package thaumcraft.common.lib.research.theorycraft;

/**
 * Data-driven research aid entry.
 * Provides a global research-speed bonus; registered in ConfigResearch.initTheorycraft().
 */
public class ResearchAid {

    private final String key;
    private final String name;
    private final float speedMultiplier;

    public ResearchAid(String key, String name, float speedMultiplier) {
        this.key = key;
        this.name = name;
        this.speedMultiplier = speedMultiplier;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public float getSpeedMultiplier() {
        return speedMultiplier;
    }
}
