package thaumcraft.api.capabilities;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ThaumcraftCapabilities - Access point for Thaumcraft's player capabilities.
 * 
 * @author Azanor
 * Ported to 1.20.1
 */
public class ThaumcraftCapabilities {

    // ==================== Player Knowledge ====================

    /**
     * The capability object for IPlayerKnowledge
     */
    public static final Capability<IPlayerKnowledge> KNOWLEDGE = CapabilityManager.get(new CapabilityToken<>() {});

    /**
     * Retrieves the knowledge capability handler for the supplied player.
     * @param player The player to get knowledge for
     * @return The knowledge capability, or null if not present
     */
    @Nullable
    public static IPlayerKnowledge getKnowledge(@Nonnull Player player) {
        return player.getCapability(KNOWLEDGE).orElse(null);
    }

    /**
     * Shortcut method to check if player knows the passed research entries.
     * All must be true. Research does not need to be complete, just 'in progress'.
     * 
     * Individual entries can contain && for 'and' check, e.g. "basicgolemancy&&infusion"
     * Individual entries can contain || for 'or' check, e.g. "basicgolemancy||infusion"
     * Queries should NOT contain both && and || - shennanigans will occur.
     * 
     * @param player The player to check
     * @param research The research keys to check
     * @return true if all research is known
     */
    public static boolean knowsResearch(@Nonnull Player player, @Nonnull String... research) {
        IPlayerKnowledge knowledge = getKnowledge(player);
        if (knowledge == null) return false;
        
        for (String r : research) {
            if (r.contains("&&")) {
                String[] rr = r.split("&&");
                if (!knowsResearch(player, rr)) return false;
            } else if (r.contains("||")) {
                String[] rr = r.split("\\|\\|");
                boolean anyTrue = false;
                for (String str : rr) {
                    if (knowsResearch(player, str)) {
                        anyTrue = true;
                        break;
                    }
                }
                if (!anyTrue) return false;
            } else {
                if (!knowledge.isResearchKnown(r)) return false;
            }
        }
        return true;
    }

    /**
     * Shortcut method to check if player knows all the passed research entries.
     * Research needs to be complete and 'in progress' research will only count 
     * if a stage is passed in the research parameter (using @, eg. "FOCUSFIRE@2")
     * 
     * @param player The player to check
     * @param research The research keys to check
     * @return true if all research is complete
     */
    public static boolean knowsResearchStrict(@Nonnull Player player, @Nonnull String... research) {
        IPlayerKnowledge knowledge = getKnowledge(player);
        if (knowledge == null) return false;
        
        for (String r : research) {
            if (r.contains("&&")) {
                String[] rr = r.split("&&");
                if (!knowsResearchStrict(player, rr)) return false;
            } else if (r.contains("||")) {
                String[] rr = r.split("\\|\\|");
                boolean anyTrue = false;
                for (String str : rr) {
                    if (knowsResearchStrict(player, str)) {
                        anyTrue = true;
                        break;
                    }
                }
                if (!anyTrue) return false;
            } else if (r.contains("@")) {
                if (!knowledge.isResearchKnown(r)) return false;
            } else {
                if (!knowledge.isResearchComplete(r)) return false;
            }
        }
        return true;
    }

    /**
     * Simple check if player knows a specific research.
     * @param player The player to check
     * @param researchKey The research key to check
     * @return true if the research is known
     */
    public static boolean isResearchKnown(@Nonnull Player player, @Nonnull String researchKey) {
        IPlayerKnowledge knowledge = getKnowledge(player);
        return knowledge != null && knowledge.isResearchKnown(researchKey);
    }

    // ==================== Player Warp ====================

    /**
     * The capability object for IPlayerWarp
     */
    public static final Capability<IPlayerWarp> WARP = CapabilityManager.get(new CapabilityToken<>() {});

    /**
     * Retrieves the warp capability handler for the supplied player.
     * @param player The player to get warp for
     * @return The warp capability, or null if not present
     */
    @Nullable
    public static IPlayerWarp getWarp(@Nonnull Player player) {
        return player.getCapability(WARP).orElse(null);
    }
}
