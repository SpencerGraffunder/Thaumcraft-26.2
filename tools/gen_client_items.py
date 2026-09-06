#!/usr/bin/env python3
"""Generate 26.2 ClientItem files (assets/thaumcraft/items/<id>.json) for every
registered Thaumcraft item.

MC 26.2's model system only bakes an item's model when a ClientItem definition
exists at assets/<ns>/items/<id>.json. Vanilla ships one per item (1537 of them);
the 1.20.1->26.2 port of Thaumcraft shipped none, so every item rendered as the
purple/black missing-model texture.

For each registered item id this script writes items/<id>.json pointing at the
best available model:
  1. models/item/<id>.json  -> "thaumcraft:item/<id>"   (preferred)
  2. models/block/<id>.json -> "thaumcraft:block/<id>"  (block items w/o item model)
  3. textures/item/<id>.png -> create a generated item model, then reference it
  4. neither                -> reported, skipped (content gap)

It also reports models that reference textures which are missing on disk, so
those can be fixed separately.

Usage:
  python3 tools/gen_client_items.py <assets_dir> <registered_ids_file>
    assets_dir           e.g. src/main/resources/assets/thaumcraft
    registered_ids_file  one bare item id per line (no namespace)
"""
import json
import sys
from pathlib import Path


def model_texture_refs(model: dict, root: Path, ns: str) -> list[str]:
    """Collect texture references from a model dict (handles parent + textures)."""
    refs = []
    for tex in model.get("textures", {}).values():
        if isinstance(tex, str):
            refs.append(tex)
    return refs


def resolve_ref(ns: str, item: str, assets: Path) -> tuple[str | None, str | None]:
    """Return (model_ref, note) for an item, creating a model if needed."""
    item_model = assets / "models" / "item" / f"{item}.json"
    block_model = assets / "models" / "block" / f"{item}.json"
    texture = assets / "textures" / "item" / f"{item}.png"

    if item_model.exists():
        return f"{ns}:item/{item}", None
    if block_model.exists():
        return f"{ns}:block/{item}", None
    if texture.exists():
        # create a flat generated item model
        item_model.parent.mkdir(parents=True, exist_ok=True)
        model = {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"{ns}:item/{item}"},
        }
        item_model.write_text(json.dumps(model, indent=2) + "\n")
        return f"{ns}:item/{item}", "created-model"
    return None, "no-model-no-texture"


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__)
        return 2
    assets = Path(sys.argv[1]).resolve()
    ns = assets.name  # "thaumcraft"
    ids = [ln.strip() for ln in Path(sys.argv[2]).read_text().splitlines() if ln.strip()]

    items_dir = assets / "items"
    items_dir.mkdir(exist_ok=True)

    written = 0
    created_models = 0
    skipped = []
    missing_tex = []

    for item in sorted(ids):
        ref, note = resolve_ref(ns, item, assets)
        if ref is None:
            skipped.append((item, note))
            continue
        if note == "created-model":
            created_models += 1
        client_item = {"model": {"type": "minecraft:model", "model": ref}}
        (items_dir / f"{item}.json").write_text(
            json.dumps(client_item, indent=2) + "\n"
        )
        written += 1

        # report models that reference textures missing on disk
        if ref.startswith(f"{ns}:item/"):
            mp = assets / "models" / "item" / f"{item}.json"
            try:
                m = json.loads(mp.read_text())
            except Exception:
                continue
            for tex in m.get("textures", {}).values():
                if not isinstance(tex, str):
                    continue
                # resolve ns:path -> assets/<ns>/textures/<path>.png
                if ":" in tex:
                    tns, tpath = tex.split(":", 1)
                else:
                    tns, tpath = ns, tex
                if not (assets.parent / tns / "textures" / f"{tpath}.png").exists():
                    missing_tex.append((item, tex))

    print(f"registered items : {len(ids)}")
    print(f"items/ written   : {written}")
    print(f"models created   : {created_models}")
    print(f"skipped          : {len(skipped)}")
    for item, note in skipped:
        print(f"  - {item} ({note})")
    print(f"missing textures : {len(missing_tex)}")
    for item, tex in missing_tex[:40]:
        print(f"  - {item} -> {tex}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
