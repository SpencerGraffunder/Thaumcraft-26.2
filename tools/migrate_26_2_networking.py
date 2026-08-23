#!/usr/bin/env python3
"""Migrate Thaumcraft networking packets from Forge SimpleChannel/NetworkEvent
to NeoForge CustomPacketPayload/IPayloadContext/StreamCodec. (v2)"""
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NET = os.path.join(ROOT, "src/main/java/thaumcraft/common/lib/network")

S2C = [
    "misc/PacketSealToClient.java", "playerdata/PacketSyncKnowledge.java",
    "playerdata/PacketSyncWarp.java", "playerdata/PacketWarpMessage.java",
    "misc/PacketAuraToClient.java", "tiles/PacketTileToClient.java",
    "fx/PacketFXSlash.java", "fx/PacketFXBlockArc.java", "fx/PacketFXBlockBamf.java",
    "fx/PacketFXZap.java", "fx/PacketFXEssentiaSource.java", "fx/PacketFXShield.java",
    "fx/PacketFXWispZap.java", "fx/PacketFXFocusEffect.java", "fx/PacketFXFocusPartImpact.java",
    "fx/PacketFXFocusPartImpactBurst.java", "fx/PacketFXInfusionSource.java", "fx/PacketFXPollute.java",
    "fx/PacketFXBoreDig.java", "fx/PacketFXScanSource.java", "fx/PacketFXSonic.java",
    "fx/PacketFXBlockMist.java", "misc/PacketMiscEvent.java", "misc/PacketKnowledgeGain.java",
    "misc/PacketSealFilterToClient.java", "misc/PacketBiomeChange.java",
]
C2S = [
    "tiles/PacketTileToServer.java", "playerdata/PacketSyncProgressToServer.java",
    "playerdata/PacketSyncResearchFlagsToServer.java", "misc/PacketFocusChangeToServer.java",
    "misc/PacketItemKeyToServer.java", "playerdata/PacketFocusNodesToServer.java",
    "playerdata/PacketPlayerFlagToServer.java", "misc/PacketLogisticsRequestToServer.java",
    "misc/PacketMiscStringToServer.java", "misc/PacketStartTheoryToServer.java",
    "misc/PacketSelectThaumotoriumRecipeToServer.java", "playerdata/PacketFocusNameToServer.java",
]

NEW_IMPORTS = (
    "import net.minecraft.network.protocol.common.custom.CustomPacketPayload;\n"
    "import net.minecraft.network.codec.StreamCodec;\n"
    "import net.minecraft.network.RegistryFriendlyByteBuf;\n"
    "import net.neoforged.neoforge.network.handling.IPayloadContext;\n"
)


def strip_import(c, imp):
    return re.sub(re.escape(imp) + r"\s*\n", "", c)


def has_import(c, imp):
    return (imp + "\n").lstrip(" \n") in c


def extract_body(c, start_idx):
    i = c.index("{", start_idx)
    n = 0
    while i < len(c):
        ch = c[i]
        if ch == "{":
            n += 1
        elif ch == "}":
            n -= 1
            if n == 0:
                return i
        i += 1
    raise RuntimeError("unbalanced braces")


def transform(path):
    with open(path, "r", encoding="utf-8") as f:
        c = f.read()
    name = os.path.splitext(os.path.basename(path))[0]

    mdecl = re.search(r"^public class " + re.escape(name) + r"\s*\{", c, re.M)
    if not mdecl:
        print("WARN no class decl", path); return
    c = c[:mdecl.start()] + "public class " + name + " implements CustomPacketPayload {" + c[mdecl.end():]

    mh = re.search(
        r"public static void handle\(" + re.escape(name) + r" \w+,\s*Supplier<NetworkEvent\.Context>\s*\w+\s*\)",
        c)
    if not mh:
        print("WARN no handle sig", path); return
    body_end = extract_body(c, mh.start())
    oldbody = c[mh.start():body_end + 1]

    first_nl = oldbody.index("{") + 1
    inner = oldbody[first_nl:body_end].lstrip("\n")
    inner = inner.replace("NetworkEvent.Context ctx = ctxSupplier.get();", "")
    inner = inner.replace("ctxSupplier.get().", "ctx.")
    inner = inner.replace("ctx.get().getSender()", "ctx.player()")
    inner = inner.replace("ctx.get()", "ctx")
    inner = inner.replace("ctxSupplier.get()", "ctx")
    inner = re.sub(r"^[ \t]*ctx\.setPacketHandled\([^)]*\);\s*$", "", inner, flags=re.M)
    inner = inner.replace("\n\n\n", "\n\n").rstrip() + "\n    "

    newmethod = (
        "public static void handle(" + name + " msg, IPayloadContext ctx) {\n"
        + "        " + inner + "\n    }"
    )
    c = c[:mh.start()] + newmethod + c[body_end + 1:]

    c = strip_import(c, "import net.neoforged.neoforge.network.event.NetworkEvent;")
    if not re.search(r"\bSupplier\b", c):
        c = strip_import(c, "import java.util.function.Supplier;")
    if not has_import(c, "import net.minecraft.network.protocol.common.custom.CustomPacketPayload;"):
        c = c.replace("import net.minecraft.network.", NEW_IMPORTS + "import net.minecraft.network.", 1)

    mdecl2 = re.search(r"^(public class " + re.escape(name) + r" implements CustomPacketPayload\s*\{)", c, re.M)
    fields = (
        "\n"
        "    public static final CustomPacketPayload.Type<" + name + "> TYPE =\n"
        "        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(\"thaumcraft\", \""
        + name.lower() + "\"));\n"
        "\n"
        "    public static final StreamCodec<RegistryFriendlyByteBuf, " + name + "> STREAM_CODEC =\n"
        "        StreamCodec.of(" + name + "::encode, " + name + "::decode);\n"
        "\n"
        "    @Override\n"
        "    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {\n"
        "        return TYPE;\n"
        "    }\n"
    )
    c = c[:mdecl2.end()] + fields + c[mdecl2.end():]

    with open(path, "w", encoding="utf-8") as f:
        f.write(c)
    print("OK  ", name)


def main():
    for rel in S2C + C2S:
        transform(os.path.join(NET, rel))
    print("TOTAL", len(S2C) + len(C2S))


if __name__ == "__main__":
    main()
