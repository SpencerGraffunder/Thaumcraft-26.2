package thaumcraft.common.lib.research.theorycraft;

/**
 * Data-driven research card entry.
 * Provides a one-time research progress bonus; registered in ConfigResearch.initTheorycraft().
 */
public class ResearchCard {

    private final String key;
    private final String name;
    private final int researchProgress;

    public ResearchCard(String key, String name, int researchProgress) {
        this.key = key;
        this.name = name;
        this.researchProgress = researchProgress;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public int getResearchProgress() {
        return researchProgress;
    }
}
