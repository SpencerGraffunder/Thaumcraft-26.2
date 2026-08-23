#!/usr/bin/env python3
"""Migrate 26.2 Item method signature changes across the source tree.

Handles:
  1. appendHoverText(ItemStack, @Nullable Level, List<Component>, TooltipFlag)
       -> appendHoverText(ItemStack, Item.TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)
     `tooltip.add(x)` -> `builder.accept(x)`
  2. inventoryTick(ItemStack, Level, Entity, int, boolean)
       -> inventoryTick(ItemStack, ServerLevel, Entity, @Nullable EquipmentSlot)
     super.inventoryTick(...) call remapped; `isSelected || slotId == 0` -> MAINHAND/OFFHAND check
"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src" / "main" / "java" / "thaumcraft"


def ensure_import(text: str, imp: str) -> str:
    if f"import {imp};" in text:
        return text
    lines = text.split("\n")
    last_import = -1
    for i, ln in enumerate(lines):
        if ln.startswith("import "):
            last_import = i
    if last_import >= 0:
        lines.insert(last_import + 1, f"import {imp};")
    else:
        lines.insert(0, f"import {imp};")
    return "\n".join(lines)


def fix_append_hover_text(text: str) -> str:
    old_sig = re.compile(
        r"public void appendHoverText\s*\(\s*ItemStack\s+stack\s*,\s*@Nullable\s+Level\s+level\s*,\s*List<Component>\s+tooltip\s*,\s*TooltipFlag\s+flag\s*\)"
    )
    new_sig = ("public void appendHoverText(ItemStack stack, Item.TooltipContext context, "
               "TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag)")
    if old_sig.search(text):
        text = old_sig.sub(new_sig, text)
        text = re.sub(r"\btooltip\.add\(", "builder.accept(", text)
        text = ensure_import(text, "java.util.function.Consumer")
    return text


def fix_inventory_tick(text: str) -> str:
    old_sig = re.compile(
        r"public void inventoryTick\s*\(\s*ItemStack\s+(\w+)\s*,\s*Level\s+(\w+)\s*,\s*Entity\s+(\w+)\s*,\s*int\s+(\w+)\s*,\s*boolean\s+(\w+)\s*\)"
    )
    def sig_repl(m):
        stack, level, entity, slot, sel = m.groups()
        return f"public void inventoryTick({stack} stack, ServerLevel {level}, Entity {entity}, EquipmentSlot slot)"
    text2 = old_sig.sub(sig_repl, text)
    if text2 != text:
        text = text2
        text = re.sub(r"super\.inventoryTick\(\s*(\w+)\s*,\s*(\w+)\s*,\s*(\w+)\s*,\s*\w+\s*,\s*\w+\s*\)",
                      r"super.inventoryTick(\1, \2, \3, slot)", text)
        text = re.sub(r"\bisSelected\s*\|\|\s*slotId\s*==\s*0\b",
                      "slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND", text)
        text = ensure_import(text, "net.minecraft.world.entity.EquipmentSlot")
        text = ensure_import(text, "net.minecraft.server.level.ServerLevel")
    return text


def main():
    changed = []
    for f in sorted(ROOT.rglob("*.java")):
        text = f.read_text(encoding="utf-8")
        orig = text
        text = fix_append_hover_text(text)
        text = fix_inventory_tick(text)
        if text != orig:
            f.write_text(text, encoding="utf-8")
            changed.append(str(f.relative_to(ROOT)))
    print(f"Changed {len(changed)} files:")
    for c in changed:
        print("  ", c)


if __name__ == "__main__":
    main()
