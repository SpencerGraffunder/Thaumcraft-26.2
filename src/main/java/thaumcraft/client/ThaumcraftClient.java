package thaumcraft.client;

import net.minecraft.resources.Identifier;

/**
 * 26.2 client entry point (mod client class).
 *
 * The 26.2 GUI/render-state rewrite (new RenderPipeline-based blit API) is a
 * later phase: texture fields are exposed as identifiers for now; consumers
 * (e.g. HudHandler) null-guard them and skip textured rendering until the
 * atlas-pipeline wiring lands.
 */
public class ThaumcraftClient {

    /** HUD gauge frame texture (gui/hud.png). Null until the texture pipeline is wired. */
    public static Identifier HUD_TEXTURE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/hud.png");

    /** Caster dial texture (gui/dial.png). Null until the texture pipeline is wired. */
    public static Identifier DIAL_TEXTURE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/dial.png");

    private ThaumcraftClient() {
    }
}
