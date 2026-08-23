#!/usr/bin/env python3
"""
Fix remaining spawnAtLocation(ItemStack[, y]) calls (the ones whose args contain
parentheses that the pass-4 regex could not match). Error-guided via javac errors.

Rewrites each flagged line's spawnAtLocation(...) call with a paren-balancing
parser and prepends the ServerLevel argument.
"""
import pathlib
import re
import sys

SRC = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java"
ERRFILE = sys.argv[1] if len(sys.argv) > 1 else ""

flagged = {}
for line in open(ERRFILE, encoding="utf-8", errors="replace"):
    m = re.match(r"^\s*(.+?):(\d+): error: (.*)$", line)
    if not m:
        continue
    f, ln, msg = m.group(1), int(m.group(2)), m.group(3)
    if "spawnAtLocation" in msg and "no suitable method" in msg:
        flagged.setdefault(f, set()).add(ln)


def parse_call(text: str, idx: int):
    """text[idx:] starts at 'spawnAtLocation('. Returns (close_paren_idx, args)."""
    i = idx + len("spawnAtLocation(")
    depth = 1
    args = []
    cur = []
    while i < len(text):
        c = text[i]
        if c == "(":
            depth += 1
            cur.append(c)
        elif c == ")":
            depth -= 1
            if depth == 0:
                break
            cur.append(c)
        elif c == "," and depth == 1:
            args.append("".join(cur).strip())
            cur = []
        else:
            cur.append(c)
        i += 1
    args.append("".join(cur).strip())
    return i, args


def rewrite_line(line: str) -> str:
    out = []
    i = 0
    while True:
        idx = line.find("spawnAtLocation(", i)
        if idx == -1:
            out.append(line[i:])
            break
        out.append(line[i:idx])
        end, args = parse_call(line, idx)
        pre = line[:idx]
        m = re.search(r"([A-Za-z_][\w]*)\s*\.\s*$", pre)
        recv = m.group(1) if m else "this"
        new_call = "spawnAtLocation((ServerLevel) " + recv + ".level()"
        for a in args:
            new_call += ", " + a
        new_call += ")"
        out.append(new_call)
        i = end + 1
    return "".join(out)


changed = 0
for f, lines in sorted(flagged.items()):
    p = pathlib.Path(f)
    if not p.exists():
        print("(missing)", f)
        continue
    text = p.read_text(encoding="utf-8")
    src_lines = text.splitlines(keepends=True)
    dirty = False
    for ln in sorted(lines):
        if ln <= len(src_lines):
            nl = rewrite_line(src_lines[ln - 1])
            if nl != src_lines[ln - 1]:
                src_lines[ln - 1] = nl
                dirty = True
    if dirty:
        new_text = "".join(src_lines)
        if "import net.minecraft.server.level.ServerLevel;" not in new_text:
            lines2 = new_text.splitlines(keepends=True)
            for i, l in enumerate(lines2):
                if l.startswith("package "):
                    lines2.insert(i + 1, "import net.minecraft.server.level.ServerLevel;\n")
                    break
            new_text = "".join(lines2)
        p.write_text(new_text, encoding="utf-8")
        changed += 1
        print("  ", p)

print(f"files changed: {changed}")
