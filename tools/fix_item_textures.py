#!/usr/bin/env python3
"""Fix item-model texture references that point at non-existent textures.

Two classes of problem in the 26.2 port:
  A. Naming mismatch - the model references a texture name that doesn't exist,
     but the actual texture exists under a corrected name. Fix: repoint the
     model's layer0 to the existing texture.
  B. Genuinely missing - no texture exists anywhere for the item. Fix: generate
     a simple, distinct 16x16 placeholder texture so the item renders instead of
     the purple/black missing-texture pattern.

Usage:
  python3 tools/fix_item_textures.py <assets_dir>
"""
import json
import sys
from pathlib import Path

# A. repoint these models to textures that actually exist
REMAPPING = {
    "blank_seal": "thaumcraft:item/seals/seal_blank",
    "brain_curious": "thaumcraft:item/mind_clockwork",
    "caster_master": "thaumcraft:item/caster_basic_model",
    "curiosity": "thaumcraft:item/curiosity_band",
    "golem_module_aggression": "thaumcraft:item/enchanted_placeholder",
    "golem_module_vision": "thaumcraft:item/enchanted_placeholder",
    "grapple_gun_spool": "thaumcraft:item/enchanted_placeholder",
    "grapple_gun_tip": "thaumcraft:item/enchanted_placeholder",
    "label_blank": "thaumcraft:item/label",
    "label_filled": "thaumcraft:item/label",
    "phial_empty": "thaumcraft:item/phial",
    "phial_filled": "thaumcraft:item/phial",
    "seal_provide": "thaumcraft:item/seals/seal_provider",
}
for i in range(1, 9):
    REMAPPING[f"celestial_notes_moon_{i}"] = f"thaumcraft:item/celestial/moon{i}"
for i in range(1, 5):
    REMAPPING[f"celestial_notes_stars_{i}"] = f"thaumcraft:item/celestial/stars{i}"

# B. genuinely missing textures -> generate a placeholder (distinct hue each)
PLACEHOLDERS = {
    "thaumometer": (196, 148, 58),
    "research_notes": (110, 86, 140),
    "complete_notes": (150, 112, 74),
    "primal_charm": (72, 150, 96),
}


def make_placeholder(path: Path, rgb) -> None:
    from PIL import Image

    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    r, g, b = rgb
    # outer dark border
    for x in range(16):
        for y in range(16):
            if x in (0, 15) or y in (0, 15):
                img.putpixel((x, y), (r // 3, g // 3, b // 3, 255))
    # inner fill
    for x in range(1, 15):
        for y in range(1, 15):
            img.putpixel((x, y), (r, g, b, 255))
    # a simple 4x4 diamond highlight in the centre
    cx = cy = 7.5
    for x in range(1, 15):
        for y in range(1, 15):
            if abs(x - cx) + abs(y - cy) <= 3:
                img.putpixel((x, y), (min(255, r + 70), min(255, g + 70), min(255, b + 70), 255))
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def main() -> int:
    assets = Path(sys.argv[1]).resolve()
    ns = assets.name
    models_dir = assets / "models" / "item"
    tex_root = assets / "textures"

    def tex_exists(ref: str) -> bool:
        # ref like thaumcraft:item/foo -> assets/thaumcraft/textures/foo.png
        p = ref.split(":", 1)[1] if ":" in ref else ref
        return (tex_root / f"{p}.png").exists()

    fixed = []
    for item, ref in REMAPPING.items():
        mp = models_dir / f"{item}.json"
        if not mp.exists():
            continue
        m = json.loads(mp.read_text())
        old = m.get("textures", {}).get("layer0")
        if tex_exists(ref):
            m.setdefault("textures", {})["layer0"] = ref
            mp.write_text(json.dumps(m, indent=2) + "\n")
            fixed.append(f"{item}: {old} -> {ref}")
        else:
            print(f"  WARN repoint target missing: {item} -> {ref}")

    for item, rgb in PLACEHOLDERS.items():
        ref = f"{ns}:item/{item}"
        if not tex_exists(ref):
            p = tex_root / "item" / f"{item}.png"
            make_placeholder(p, rgb)
            fixed.append(f"{item}: generated placeholder texture")

    print(f"fixed {len(fixed)} references:")
    for f in fixed:
        print(f"  {f}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
