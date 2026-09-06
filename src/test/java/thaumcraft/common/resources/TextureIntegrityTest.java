package thaumcraft.common.resources;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Resource-integrity tests — catch the purple/black item rendering bug class
 * WITHOUT a Minecraft runtime.
 *
 * Two checks:
 *  1. {@link #all_model_texture_references_resolve()} — every texture referenced by
 *     a Thaumcraft model (item or block, directly in its {@code textures} map) must
 *     resolve to an existing PNG. A missing file = the item renders as the
 *     missing-texture checkerboard.
 *  2. {@link #no_missing_textured_placeholder_textures()} — no Thaumcraft texture
 *     may be the magenta/black "missing texture" checkerboard (the placeholder that
 *     several 1.20.1-era assets shipped with). A texture that IS the checkerboard
 *     renders as broken, even though the file exists.
 *
 * Both checks fail with a full list of offenders (not just the first), so a single
 * run is a complete audit of the texture set.
 *
 * Runs in a plain JVM (reads PNGs via {@link ImageIO}, JSON via Gson) — no
 * NeoForge client/server required.
 */
public class TextureIntegrityTest {

    private static final Gson GSON = new Gson();

    /** Root of the Thaumcraft asset tree on the test classpath (…/assets/thaumcraft). */
    private static Path RES;

    @BeforeClass
    public static void locateResources() throws Exception {
        URL url = TextureIntegrityTest.class.getResource("/assets/thaumcraft");
        assertNotNull("assets/thaumcraft is not on the test classpath", url);
        RES = Paths.get(url.toURI());
        assertTrue("expected a directory asset root, got " + RES, Files.isDirectory(RES));
    }

    // ------------------------------------------------------------------ check 1

    @Test
    public void all_model_texture_references_resolve() throws Exception {
        List<String> missing = new ArrayList<>();
        Path models = RES.resolve("models");
        if (Files.isDirectory(models)) {
            try (Stream<Path> s = Files.walk(models)) {
                s.filter(p -> p.toString().endsWith(".json")).forEach(p ->
                        checkModel(p, models, missing));
            }
        }
        assertTrue("Model texture references that do not resolve to an existing texture:\n  "
                + String.join("\n  ", missing), missing.isEmpty());
    }

    private void checkModel(Path modelFile, Path modelsRoot, List<String> missing) {
        JsonObject m;
        String json;
        try {
            json = Files.readString(modelFile);
        } catch (IOException e) {
            missing.add(rel(modelFile) + " (unreadable: " + e.getMessage() + ")");
            return;
        }
        try {
            m = GSON.fromJson(json, JsonObject.class);
        } catch (Exception e) {
            missing.add(rel(modelFile) + " (unparseable: " + e.getMessage() + ")");
            return;
        }
        if (m == null) return;

        // Own texture map: every thaumcraft: ref must resolve to a PNG.
        if (m.has("textures") && m.get("textures").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : m.getAsJsonObject("textures").entrySet()) {
                String ref = e.getValue().getAsString();
                if (ref.startsWith("thaumcraft:")) {
                    Path tex = RES.resolve("textures")
                            .resolve(ref.substring("thaumcraft:".length()) + ".png");
                    if (!Files.isRegularFile(tex)) {
                        missing.add(rel(modelFile) + " -> " + ref + "  [texture file missing]");
                    }
                }
            }
        }

        // A TC parent must itself be a model that exists (vanilla parents are fine).
        if (m.has("parent") && m.get("parent").isJsonPrimitive()) {
            String parent = m.get("parent").getAsString();
            if (parent.startsWith("thaumcraft:") && !resolveModel(parent, modelsRoot)) {
                missing.add(rel(modelFile) + " -> parent " + parent + "  [model file missing]");
            }
        }
    }

    private static boolean resolveModel(String modelId, Path modelsRoot) {
        // modelId like "thaumcraft:item/foo" -> models/item/foo.json
        String path = modelId.substring("thaumcraft:".length());
        return Files.isRegularFile(modelsRoot.resolve(path + ".json"));
    }

    // ------------------------------------------------------------------ check 2

    @Test
    public void no_missing_textured_placeholder_textures() throws Exception {
        List<String> placeholders = new ArrayList<>();
        Path textures = RES.resolve("textures");
        if (Files.isDirectory(textures)) {
            try (Stream<Path> s = Files.walk(textures)) {
                s.filter(p -> p.toString().endsWith(".png")).forEach(p -> {
                    try {
                        if (isMissingTexturePattern(ImageIO.read(p.toFile()))) {
                            placeholders.add(rel(p));
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException("failed to decode " + p, e);
                    }
                });
            }
        }
        assertTrue("Textures that are the magenta/black 'missing texture' checkerboard "
                + "(render as broken):\n  " + String.join("\n  ", placeholders),
                placeholders.isEmpty());
    }

    /**
     * The Minecraft "missing texture" / debug pattern: exactly two colours —
     * magenta (255,0,255) and black (0,0,0) — in a checkerboard. A real texture
     * essentially never has this exact 2-colour signature.
     */
    private static boolean isMissingTexturePattern(BufferedImage img) {
        Set<Integer> colors = new HashSet<>();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y) & 0x00FFFFFF; // ignore alpha
                if (colors.size() > 2) return false;     // not the 2-colour pattern
                colors.add(rgb);
            }
        }
        return colors.size() == 2
                && colors.contains(0xFF00FF)   // magenta
                && colors.contains(0x000000);  // black
    }

    // ------------------------------------------------------------------ helper

    private static String rel(Path p) {
        return RES.relativize(p).toString().replace('\\', '/');
    }
}
