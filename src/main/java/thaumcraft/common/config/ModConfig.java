package thaumcraft.common.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Thaumcraft configuration values.
 *
 * Ported from 1.12.2 Thaumcraft.java config section.
 *
 * NOTE: This is a placeholder config class. When the full Forge config system
 * is available, these values should be loaded from a .cfg file.
 * For now, they use hardcoded default values matching the 1.12.2 defaults.
 */
public class ModConfig {

    // ==================== Config Values ====================

    // World generation
    public static boolean generateThaumium = true;
    public static boolean generateInfusedOres = true;
    public static boolean generateSilverwoodTrees = true;
    public static boolean generateGreatwoodTrees = true;
    public static boolean generateRuins = true;
    public static boolean generateTaint = true;
    public static boolean generateWardedBlocks = true;

    // Aura
    public static boolean auraEnabled = true;
    public static int auraRadius = 20;
    public static double auraVisGain = 1.0;

    // Crafting
    public static boolean wussMode = false; // If true, no warp from crafting

    // Research
    public static boolean researchEnabled = true;
    public static boolean automaticResearch = false;

    // Seals
    public static boolean sealsEnabled = true;
    public static int sealVisCost = 25;

    // Golems
    public static boolean golemsEnabled = true;
    public static int golemMaxCount = 8;

    // Warp
    public static int maxWarp = 100;
    public static boolean warpEnabled = true;

    // ==================== Dimension Whitelist/Blacklist ====================

    /**
     * Dimensions where Thaumcraft features are enabled.
     * If empty, features are enabled in all dimensions.
     * Format: "namespace:dimension_id"
     */
    public static final List<String> dimensionWhitelist = new ArrayList<>();

    /**
     * Dimensions where Thaumcraft features are disabled.
     * Format: "namespace:dimension_id"
     */
    public static final List<String> dimensionBlacklist = new ArrayList<>(Arrays.asList(
        "minecraft:the_end"
    ));

    /**
     * Check if a dimension allows Thaumcraft features.
     */
    public static boolean isDimensionAllowed(String dimensionId) {
        // Blacklist takes priority
        if (dimensionBlacklist.contains(dimensionId)) {
            return false;
        }
        // If whitelist is empty, allow all (except blacklisted)
        if (dimensionWhitelist.isEmpty()) {
            return true;
        }
        // Check whitelist
        return dimensionWhitelist.contains(dimensionId);
    }

    // ==================== Item Blacklists ====================

    /**
     * Items that cannot be used in certain contexts.
     * Format: "namespace:item_id"
     */
    public static final Set<String> portableHoleBlacklist = new HashSet<>();

    /**
     * Items that cannot be used in golem inventories.
     */
    public static final Set<String> golemItemBlacklist = new HashSet<>();

    /**
     * Check if wuss mode is enabled.
     */
    public static boolean isWussMode() {
        return wussMode;
    }

    /**
     * Initialize the config with default values.
     */
    private static CommentedFileConfig config;

    public static void init(File configDir) {
        // Create config directory if it doesn't exist
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        // Create config file
        File configFile = new File(configDir, "thaumcraft-common.toml");
        config = CommentedFileConfig.builder(configFile)
            .sync()
            .separator(".")
            .format(TomlFormat.standard())
            .build();

        // Load config
        try {
            config.load();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Apply config values
        if (config.containsKey("wussmode")) {
            wussMode = config.getBoolean("wussmode");
        }

        // Save config
        config.save();
    }
}
