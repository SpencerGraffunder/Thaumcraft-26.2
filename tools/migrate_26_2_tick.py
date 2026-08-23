#!/usr/bin/env python3
"""
Thaumcraft 26.2 port — tick event migration (4 files).

NeoForge split the Forge `TickEvent` into per-context Pre/Post events:
  ClientTickEvent.Pre / ClientTickEvent.Post
  RenderTickEvent.Post
  LevelTickEvent.Pre / LevelTickEvent.Post
and removed `event.side` / `event.level` / `event.phase` (use getLevel()).
"""
import pathlib
import re

SRC = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java"


def replace_method(text: str, start_marker: str, new_body: str) -> str:
    """Replace the method beginning at start_marker up to its matching close brace."""
    idx = text.index(start_marker)
    brace_idx = text.index("{", idx)
    depth = 0
    i = brace_idx
    while i < len(text):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                break
        i += 1
    return text[:idx] + new_body + text[i + 1:]


def edit(path: str, fn) -> None:
    p = SRC / path
    t = p.read_text(encoding="utf-8")
    new = fn(t)
    if new != t:
        p.write_text(new, encoding="utf-8")
        print("  edited", path)


# ---------- ClientEvents ----------
def fix_client_events(t: str) -> str:
    t = t.replace(
        "import net.minecraftforge.event.TickEvent;",
        "import net.neoforged.neoforge.client.event.ClientTickEvent;\n"
        "import net.neoforged.neoforge.client.event.RenderTickEvent;",
    )
    t = t.replace(
        "public static void onClientTick(TickEvent.ClientTickEvent event)",
        "public static void onClientTick(ClientTickEvent.Pre event)",
    )
    t = t.replace(
        "public static void onRenderTick(TickEvent.RenderTickEvent event)",
        "public static void onRenderTick(RenderTickEvent.Post event)",
    )
    t = t.replace("Bus.FORGE", "Bus.GAME")
    return t


edit("thaumcraft/client/lib/events/ClientEvents.java", fix_client_events)


# ---------- KeyHandler ----------
def fix_key_handler(t: str) -> str:
    t = t.replace(
        "import net.minecraftforge.event.TickEvent;",
        "import net.neoforged.neoforge.client.event.ClientTickEvent;",
    )
    t = t.replace(
        "public static void onClientTick(TickEvent.ClientTickEvent event) {",
        "public static void onClientTick(ClientTickEvent.Pre event) {",
    )
    # remove the START-phase guard (Pre only fires at start)
    t = re.sub(
        r"\n[ \t]*if \(event\.phase != TickEvent\.Phase\.START\) \{\n[ \t]*return;\n[ \t]*\}\n",
        "\n",
        t,
    )
    return t


edit("thaumcraft/client/lib/events/KeyHandler.java", fix_key_handler)


# ---------- AuraScheduler ----------
def fix_aura_scheduler(t: str) -> str:
    t = t.replace(
        "import net.minecraftforge.event.TickEvent;",
        "import net.neoforged.neoforge.event.tick.LevelTickEvent;",
    )
    t = t.replace(
        "public static void onLevelTick(TickEvent.LevelTickEvent event) {",
        "public static void onLevelTick(LevelTickEvent.Post event) {",
    )
    t = t.replace(
        "if (event.side.isClient() || event.phase == TickEvent.Phase.START) return;",
        "if (event.getLevel().isClientSide()) return;",
    )
    t = t.replace(
        "if (event.level instanceof ServerLevel level) {",
        "if (event.getLevel() instanceof ServerLevel level) {",
    )
    return t


edit("thaumcraft/common/world/aura/AuraScheduler.java", fix_aura_scheduler)


# ---------- ServerEvents ----------
def fix_server_events(t: str) -> str:
    t = t.replace(
        "import net.minecraftforge.event.TickEvent;",
        "import net.neoforged.neoforge.event.tick.LevelTickEvent;\n"
        "import net.neoforged.neoforge.client.event.ClientTickEvent;",
    )
    t = t.replace("Bus.FORGE", "Bus.GAME")

    marker = "public static void onLevelTick(TickEvent.LevelTickEvent event)"
    new_body = """public static void onLevelTick(LevelTickEvent.Pre event) {
        // Only run on ServerLevel
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // Start aura thread if not already running
        if (!AuraThreadManager.hasThread(level.dimension()) && AuraHandler.getAuraWorld(level.dimension()) != null) {
            AuraThreadManager.startThread(level.dimension());
        }
    }

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        // Only run on ServerLevel
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        String dimKey = level.dimension().location().toString();

        if (!serverTicks.containsKey(dimKey)) {
            serverTicks.put(dimKey, 0);
        }

        // Process delayed runnables
        processRunnables(dimKey);

        // Process block swaps and breaks
        tickBlockSwap(level);
        tickBlockBreak(level);

        int ticks = serverTicks.get(dimKey);

        // Periodic cleanup (every 20 ticks = 1 second)
        if (ticks % 20 == 0) {
            // Clean up suspended or expired golem tasks
            TaskHandler.clearSuspendedOrExpiredTasks(level);

            // Mark dirty aura chunks for saving
            ResourceKey<Level> dimension = level.dimension();
            CopyOnWriteArrayList<ChunkPos> dirtyChunks = AuraHandler.dirtyChunks.get(dimension);
            if (dirtyChunks != null && !dirtyChunks.isEmpty()) {
                for (ChunkPos pos : dirtyChunks) {
                    // Mark the chunk as needing to be saved
                    level.getChunkSource().getChunk(pos.x(), pos.z(), false);
                    // The chunk will be marked dirty automatically when aura data is saved
                }
                dirtyChunks.clear();
            }

            // Handle flux rift triggers (if not in wuss mode)
            if (AuraHandler.riftTrigger.containsKey(dimension)) {
                if (!ModConfig.wussMode) {
                    BlockPos riftPos = AuraHandler.riftTrigger.get(dimension);
                    EntityFluxRift.createRift(level, riftPos);
                }
                AuraHandler.riftTrigger.remove(dimension);
            }
        }

        // Tick all seals in this dimension (every tick)
        SealHandler.tickSealEntities(level);

        // Increment tick counter
        serverTicks.put(dimKey, ticks + 1);
    }"""
    t = replace_method(t, marker, new_body)

    # onClientTick -> Post, drop the END-phase guard
    t = re.sub(
        r"public static void onClientTick\(TickEvent\.ClientTickEvent event\) \{\s*"
        r"if \(event\.phase != TickEvent\.Phase\.END\) \{\s*return;\s*\}",
        "public static void onClientTick(ClientTickEvent.Post event) {",
        t,
    )
    return t


edit("thaumcraft/common/lib/events/ServerEvents.java", fix_server_events)

print("done")
