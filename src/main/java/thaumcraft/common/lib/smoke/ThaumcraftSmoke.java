package thaumcraft.common.lib.smoke;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import thaumcraft.init.ModRecipeTypes;
import org.slf4j.Logger;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.lib.crafting.CrucibleRecipeType;
import thaumcraft.common.lib.crafting.InfusionRecipeType;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModItems;
import thaumcraft.init.ModSounds;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dev-only in-game assertion battery (the "smoke" layer of the audit pipeline).
 *
 * Static audits (tools/audit_*.py) compare files on disk; this battery boots a
 * REAL dedicated server and asserts the runtime state once every registry,
 * datapack (recipes / research / loot) and runtime registration is final
 * (ServerStartedEvent). Catches what static audits cannot: codec parse
 * failures, serializer mismatches, runtime registration misses, lang holes.
 *
 * Enable: TC_SMOKE=1 (env) or -Dtc.smoke=true (property) when launching the
 * server. Every check logs "[SMOKE] PASS name" / "[SMOKE] FAIL name: detail";
 * the final line is "SMOKE: <n> checks passed" or
 * "SMOKE: <n> passed, <m> FAILED: ..." (grepped by tools/run_smoke.sh and
 * tools/run_all_audits.sh).
 */
public final class ThaumcraftSmoke {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ThaumcraftSmoke() {
    }

    public static boolean enabled() {
        return "1".equals(System.getenv("TC_SMOKE")) || Boolean.getBoolean("tc.smoke");
    }

    private static int passed = 0;
    private static int failed = 0;
    private static final List<String> failures = new ArrayList<>();

    private interface KeyFn {
        Identifier key(Object obj);
    }

    private static void pass(String name) {
        passed++;
        LOGGER.info("[SMOKE] PASS {}", name);
    }

    private static void fail(String name, String detail) {
        failed++;
        failures.add(name + ": " + detail);
        LOGGER.warn("[SMOKE] FAIL {}: {}", name, detail);
    }

    public static void run(MinecraftServer server) {
        passed = 0;
        failed = 0;
        failures.clear();
        try {
            checkHolders("items", ModItems.class,
                    obj -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey((Item) obj));
            checkHolders("blocks", ModBlocks.class,
                    obj -> net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey((Block) obj));
            checkHolders("entities", ModEntities.class,
                    obj -> net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(
                            (net.minecraft.world.entity.EntityType<?>) obj));
            checkHolders("sounds", ModSounds.class,
                    obj -> net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getKey(
                            (SoundEvent) obj));
            checkSoundsJson();
            checkRecipeCounts(server.getRecipeManager());
            checkResearchGates(server.getRecipeManager());
            checkLootModifier();
            checkLootTableResources(server);
            checkEnchantments(server);
            checkAspects();
            checkLangCoverage();
        } catch (Throwable t) {
            fail("smoke-harness", t.toString());
        }
        if (failed == 0) {
            LOGGER.info("SMOKE: {} checks passed", passed);
        } else {
            LOGGER.warn("SMOKE: {} passed, {} FAILED: {}", passed, failed,
                    String.join("; ", failures.subList(0, Math.min(8, failures.size()))));
        }
    }

    // ---------------------------------------------------------------- holders
    /**
     * Reflect over every public static final DeferredHolder field of a
     * registration class and assert each resolves to a registry-known object.
     * Self-maintaining: a newly added registration is covered automatically.
     */
    private static void checkHolders(String label, Class<?> regClass, KeyFn keyFn) {
        int total = 0;
        List<String> bad = new ArrayList<>();
        for (Field f : regClass.getFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || !Modifier.isFinal(f.getModifiers())
                    || !f.getType().getName().contains("DeferredHolder")) {
                continue;
            }
            total++;
            try {
                Object holder = f.get(null);
                Object obj = holder.getClass().getMethod("get").invoke(holder);
                Identifier id = obj == null ? null : keyFn.key(obj);
                if (obj == null || id == null || !Thaumcraft.MODID.equals(id.getNamespace())) {
                    bad.add(f.getName());
                }
            } catch (Throwable t) {
                bad.add(f.getName() + "(" + t.getClass().getSimpleName() + ")");
            }
        }
        if (bad.isEmpty() && total > 0) {
            pass(label + " (" + total + " holders, all registered)");
        } else {
            fail(label, total + " holders, " + bad.size() + " unresolved: "
                    + bad.subList(0, Math.min(8, bad.size())));
        }
    }

    // ---------------------------------------------------------------- sounds
    private static void checkSoundsJson() {
        try (InputStream in = ThaumcraftSmoke.class.getResourceAsStream("/assets/thaumcraft/sounds.json")) {
            if (in == null) {
                fail("sounds.json", "resource not on classpath");
                return;
            }
            JsonObject root = com.google.gson.JsonParser.parseString(
                    new String(in.readAllBytes())).getAsJsonObject();
            List<String> missing = new ArrayList<>();
            for (Map.Entry<String, com.google.gson.JsonElement> e : root.entrySet()) {
                if (net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT
                        .getValue(Identifier.fromNamespaceAndPath(Thaumcraft.MODID, e.getKey())) == null) {
                    missing.add(e.getKey());
                }
            }
            if (missing.isEmpty() && root.size() > 0) {
                pass("sounds (" + root.size() + " ids, all registered)");
            } else {
                fail("sounds", missing.size() + " unregistered: " + missing);
            }
        } catch (Exception e) {
            fail("sounds.json", e.toString());
        }
    }

    // ---------------------------------------------------------------- recipes
    private static void checkRecipeCounts(RecipeManager rm) {
        List<String> problems = new ArrayList<>();
        int total = 0;

        // arcane workbench (shaped + shapeless share one type and data dir)
        int arcaneParsed = 0;
        for (RecipeHolder<?> h : rm.recipeMap().byType(ModRecipeTypes.ARCANE_WORKBENCH.get())) {
            arcaneParsed++;
            if (!(h.value() instanceof thaumcraft.api.crafting.IArcaneRecipe)) {
                problems.add("arcane deserialized as " + h.value().getClass().getSimpleName());
            }
        }
        total += arcaneParsed;
        Integer arcaneDisk = countFiles("data/thaumcraft/recipe/arcane_workbench", null);
        if (arcaneDisk != null && arcaneDisk != arcaneParsed) {
            problems.add("arcane on-disk=" + arcaneDisk + " parsed=" + arcaneParsed);
        }

        int crucibleParsed = 0;
        for (RecipeHolder<?> h : rm.recipeMap().byType(ModRecipeTypes.CRUCIBLE.get())) {
            if (h.value() instanceof CrucibleRecipeType) {
                crucibleParsed++;
            } else {
                problems.add("crucible deserialized as " + h.value().getClass().getSimpleName());
            }
        }
        total += crucibleParsed;
        Integer crucibleDisk = countFiles("data/thaumcraft/recipe/crucible", "thaumcraft:crucible");
        if (crucibleDisk != null && crucibleDisk != crucibleParsed) {
            problems.add("crucible on-disk=" + crucibleDisk + " parsed=" + crucibleParsed);
        }

        int infusionParsed = 0;
        for (RecipeHolder<?> h : rm.recipeMap().byType(ModRecipeTypes.INFUSION.get())) {
            if (h.value() instanceof InfusionRecipeType) {
                infusionParsed++;
            } else {
                problems.add("infusion deserialized as " + h.value().getClass().getSimpleName());
            }
        }
        total += infusionParsed;
        // IE recipes extend InfusionRecipeType (same RecipeType), so both dirs
        // parse into the INFUSION type list.
        Integer infusionDisk = countFiles("data/thaumcraft/recipe/infusion", null);
        Integer ieDisk = countFiles("data/thaumcraft/recipe/infusion_enchantment", null);
        int diskTotal = (infusionDisk == null || ieDisk == null) ? -1 : infusionDisk + ieDisk;
        if (diskTotal >= 0 && diskTotal != infusionParsed) {
            problems.add("infusion on-disk=" + diskTotal + " parsed=" + infusionParsed);
        }

        if (problems.isEmpty() && total > 0) {
            pass("recipes (" + total + " TC recipes parsed, all match on-disk counts)");
        } else {
            fail("recipes", total + " parsed; " + String.join("; ", problems));
        }
    }

    /**
     * Count recipe JSONs under a classpath dir (dev runs have a file dir).
     * {@code typeFilter} = exact "type" value to count, or null = every .json.
     * Returns null when the comparison cannot be made (packaged jar).
     */
    private static Integer countFiles(String classpathDir, String typeFilter) {
        try {
            var url = ThaumcraftSmoke.class.getResource("/" + classpathDir);
            if (url == null || !"file".equals(url.getProtocol())) {
                return null;
            }
            File dir = new File(url.toURI());
            if (!dir.isDirectory()) {
                return null;
            }
            int count = 0;
            for (File f : dir.listFiles()) {
                if (f == null || !f.getName().endsWith(".json")) {
                    continue;
                }
                if (typeFilter == null) {
                    count++;
                } else {
                    JsonObject d = com.google.gson.JsonParser.parseString(
                            new String(Files.readAllBytes(f.toPath()))).getAsJsonObject();
                    if (typeFilter.equals(d.has("type") ? d.get("type").getAsString() : "")) {
                        count++;
                    }
                }
            }
            return count;
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------- research
    private static void checkResearchGates(RecipeManager rm) {
        List<String> missing = new ArrayList<>();
        int gated = 0;
        List<RecipeHolder<?>> all = new ArrayList<>();
        for (RecipeHolder<?> h : rm.recipeMap().byType(ModRecipeTypes.ARCANE_WORKBENCH.get())) {
            all.add(h);
        }
        for (RecipeHolder<?> h : rm.recipeMap().byType(ModRecipeTypes.CRUCIBLE.get())) {
            all.add(h);
        }
        for (RecipeHolder<?> h : rm.recipeMap().byType(ModRecipeTypes.INFUSION.get())) {
            all.add(h);
        }
        for (RecipeHolder<?> h : all) {
            Object r = h.value();
            String gate;
            try {
                gate = (String) r.getClass().getMethod("getResearch").invoke(r);
            } catch (Exception e) {
                continue;
            }
            if (gate == null || gate.isEmpty()) {
                continue;
            }
            gated++;
            // gates may be compound ("A&&B") and stage-suffixed ("A@2") —
            // every part must resolve to a registered research key
            for (String part : gate.split("&&")) {
                String key = part.contains("@") ? part.substring(0, part.indexOf('@')) : part;
                if (ResearchCategories.getResearch(key) == null) {
                    missing.add(gate + " (" + h.value().getClass().getSimpleName() + ")");
                    break;
                }
            }
        }
        if (missing.isEmpty() && gated > 0) {
            pass("research gates (" + gated + " gated recipes, all keys resolve)");
        } else {
            fail("research gates", gated + " gated; " + missing.size() + " unresolved: "
                    + missing.subList(0, Math.min(8, missing.size())));
        }
    }

    // ----------------------------------------------------------------- loot
    private static void checkLootModifier() {
        var id = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "refining_mining");
        if (NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS.containsKey(id)) {
            pass("loot modifier (thaumcraft:refining_mining registered)");
        } else {
            fail("loot modifier", "thaumcraft:refining_mining serializer not registered");
        }
    }

    private static void checkLootTableResources(MinecraftServer server) {
        List<String> problems = new ArrayList<>();
        for (String block : List.of("cinnabar_ore", "deepslate_cinnabar_ore")) {
            var id = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "loot_table/blocks/" + block + ".json");
            boolean ok;
            try {
                ok = server.getResourceManager().getResource(id).isPresent();
            } catch (Exception e) {
                ok = false;
            }
            if (!ok) {
                problems.add(id.toString());
            }
        }
        if (problems.isEmpty()) {
            pass("loot tables (TC ore block loot resources present)");
        } else {
            fail("loot tables", "missing: " + problems);
        }
    }

    // ----------------------------------------------------------- enchantments
    private static void checkEnchantments(MinecraftServer server) {
        List<String> tc = new ArrayList<>();
        var registry = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (var enchantment : registry) {
            var id = registry.getKey(enchantment);
            if (id != null && Thaumcraft.MODID.equals(id.getNamespace())) {
                tc.add(id.getPath());
            }
        }
        if (!tc.isEmpty()) {
            pass("enchantments (" + tc.size() + " TC enchantments: " + tc + ")");
        } else {
            fail("enchantments", "no thaumcraft: enchantments registered");
        }
    }

    // --------------------------------------------------------------- aspects
    private static void checkAspects() {
        int objects = AspectHelper.objectTagCount();
        int entities = AspectHelper.entityTagCount();
        List<String> problems = new ArrayList<>();
        if (objects < 100) {
            problems.add("only " + objects + " object tags registered (expected >100)");
        }
        if (entities < 10) {
            problems.add("only " + entities + " entity tags registered (expected >10)");
        }
        for (Object[] spot : new Object[][]{
                { ModBlocks.ARCANE_STONE.get().asItem(), "arcane stone" },
                { net.minecraft.world.level.block.Blocks.DIRT.asItem(), "dirt" },
                { ModBlocks.CINNABAR_ORE.get().asItem(), "cinnabar ore" } }) {
            var list = AspectHelper.getObjectAspects(new ItemStack((Item) spot[0]));
            if (list == null || list.getAspects().length == 0) {
                problems.add("no aspects for " + spot[1]);
            }
        }
        if (problems.isEmpty()) {
            pass("aspects (" + objects + " object tags, " + entities + " entity tags, spot-checks ok)");
        } else {
            fail("aspects", String.join("; ", problems));
        }
    }

    // ------------------------------------------------------------------ lang
    private static void checkLangCoverage() {
        try (InputStream in = ThaumcraftSmoke.class.getResourceAsStream("/assets/thaumcraft/lang/en_us.json")) {
            if (in == null) {
                fail("lang", "en_us.json not on classpath");
                return;
            }
            JsonObject lang = com.google.gson.JsonParser.parseString(
                    new String(in.readAllBytes())).getAsJsonObject();
            List<String> missing = new ArrayList<>();
            for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
                if (id == null || !Thaumcraft.MODID.equals(id.getNamespace())) {
                    continue;
                }
                // block items inherit their name from block.<ns>.<id>
                if (item instanceof BlockItem) {
                    continue;
                }
                if (!lang.has("item." + Thaumcraft.MODID + "." + id.getPath())) {
                    missing.add("item." + id.getPath());
                }
            }
            for (Block block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
                var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
                if (id == null || !Thaumcraft.MODID.equals(id.getNamespace())) {
                    continue;
                }
                if (!lang.has("block." + Thaumcraft.MODID + "." + id.getPath())) {
                    missing.add("block." + id.getPath());
                }
            }
            if (missing.isEmpty()) {
                pass("lang (every TC item/block has an en_us entry)");
            } else {
                fail("lang", missing.size() + " missing: " + missing.subList(0, Math.min(8, missing.size())));
            }
        } catch (Exception e) {
            fail("lang", e.toString());
        }
    }
}
