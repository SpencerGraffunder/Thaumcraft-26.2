#!/usr/bin/env python3
"""
Thaumcraft 26.2 port — pass 10: migrate network packets to NeoForge payloads.

For each packet class under .../lib/network/{misc,fx,playerdata,tiles}/ :
  1. class X -> class X implements CustomPacketPayload
  2. insert TYPE + STREAM_CODEC + type() overrides after the class body open
  3. handle(X msg, Supplier<NetworkEvent.Context> ctx) -> handle(X msg, IPayloadContext ctx)
     with ctx.get() -> ctx,  ctx.getSender() -> ctx.player(),  drop setPacketHandled
  4. strip NetworkEvent / Supplier imports; add payload imports

PacketHandler is rewritten separately; send sites + registration wired after.
Idempotent (skips files already implementing CustomPacketPayload).
"""
import pathlib
import re

NET = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java" / "thaumcraft" / "common" / "lib" / "network"

ADDS = {
    "net.minecraft.network.protocol.common.custom.CustomPacketPayload",
    "net.minecraft.network.RegistryFriendlyByteBuf",
    "net.minecraft.network.codec.StreamCodec",
    "net.minecraft.resources.Identifier",
    "net.neoforged.neoforge.network.handling.IPayloadContext",
}
DROP = {
    "net.neoforged.neoforge.network.event.NetworkEvent",
    "java.util.function.Supplier",
}

def slug(name: str) -> str:
    s = re.sub(r"(?<!^)(?=[A-Z])", "-", name).lower()
    return s.replace("-", "")

changed = 0
skipped = 0
for p in sorted(NET.glob("*.java")) + [f for sub in ("misc", "fx", "playerdata", "tiles") for f in sorted((NET / sub).glob("*.java"))]:
    if not p.exists():
        continue
    t = p.read_text(encoding="utf-8")
    if "implements CustomPacketPayload" in t or "implements net.minecraft.network.protocol.common.custom.CustomPacketPayload" in t:
        skipped += 1
        continue
    name = p.stem
    orig = t

    # 3) handle signature + body
    before = t
    # ctx.getSender() -> ctx.player()   (before ctx.get() strip, so getSender handled explicitly)
    t = re.sub(r"\bctx\.getSender\(\)", "ctx.player()", t)
    # ctx.get() -> ctx
    t = re.sub(r"\bctx\.get\(\)", "ctx", t)
    # remove setPacketHandled lines
    t = re.sub(r"[ \t]*ctx\.(get\(\)\.)?setPacketHandled\(true\);\s*\n", "", t)
    # handle signature
    t = re.sub(
        r"(public static void handle\(\s*\w+\s*\w+\s*,\s*)Supplier<NetworkEvent\.Context>(\s*\w+\s*\))",
        r"\1IPayloadContext\2",
        t)
    t = re.sub(
        r"(public static void handle\(\s*\w+\s*\w+\s*,\s*)NetworkEvent\.Context(\s*\w+\s*\))",
        r"\1IPayloadContext\2",
        t)

    # 1) class decl implements (replace the brace too so re-find works)
    m = re.search(r"(class\s+" + re.escape(name) + r")\s*(\{)", t)
    if not m:
        print("  NO CLASS HEAD:", name); continue
    t = t[:m.start()] + f"class {name} implements CustomPacketPayload " + m.group(2) + t[m.end():]

    # 2) insert TYPE/STREAM_CODEC/type() right after the class body '{'
    m2 = re.search(r"class\s+" + re.escape(name) + r"\s+implements CustomPacketPayload\s*\{", t)
    idx = m2.end()
    add = (
        f"\n    public static final CustomPacketPayload.Type<{name}> TYPE =\n"
        f"        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(\"thaumcraft\", \"{slug(name)}\"));\n"
        f"\n"
        f"    public static final StreamCodec<RegistryFriendlyByteBuf, {name}> STREAM_CODEC =\n"
        f"        StreamCodec.of({name}::encode, {name}::decode);\n"
        f"\n"
        f"    @Override\n"
        f"    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {{\n"
        f"        return this.TYPE;\n"
        f"    }}\n"
    )
    t = t[:idx] + add + t[idx:]

    # 4) imports: drop obsolete, add needed
    lines = t.splitlines(keepends=True)
    keep = [ln for ln in lines if not any(ln.strip() == f"import {x};" for x in DROP)]
    present = {ln.strip() for ln in keep}
    to_add = [f"import {x};" for x in ADDS if f"import {x};" not in present]
    # insert after package line (each on its own line)
    out = []
    inserted = False
    for ln in keep:
        out.append(ln)
        if not inserted and ln.startswith("package "):
            for a in to_add:
                out.append(a + "\n")
            inserted = True
    if not inserted:
        out = [a + "\n" for a in to_add] + out
    t = "".join(out)

    if t != orig:
        p.write_text(t, encoding="utf-8")
        changed += 1

print(f"pass10 network packets: changed={changed} skipped={skipped}")
