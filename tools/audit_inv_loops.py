#!/usr/bin/env python3
"""Audit inventory loops that read from a live container while mutating it.

Pattern we are looking for: a loop whose body both calls a read
(getStackInSlot/insertItemSlot) and a mutation (setStackInSlot/extractItem/insertItem)
on the SAME receiver variable. In 26.3 a Container#setItem fires setChanged(), which can
replace the backing NonNullList; a WorldlyContainerWrapper/VanillaContainerWrapper built on
top of it then reads past the shrunken list and AbstractContainerMenu#getItem throws
"out of bounds slot index N".
"""
import re
import sys
from pathlib import Path

READS = ("getStackInSlot", "insertItemSlot")
WRITES = ("setStackInSlot", "extractItem", "insertItem")
ROOT = Path("src/main/java")


def find_loops(src: str):
    """Yield (start_line, header, body) for each for/while loop, outermost first."""
    for m in re.finditer(r"\b(for|while)\s*\(", src):
        i = m.end() - 1  # at '('
        depth = 0
        while i < len(src):
            c = src[i]
            if c == "(":
                depth += 1
            elif c == ")":
                depth -= 1
                if depth == 0:
                    break
            i += 1
        header = src[m.start():i + 1]
        # find body braces
        j = src.find("{", i)
        if j == -1 or j > i + 200:
            continue
        depth = 0
        k = j
        while k < len(src):
            c = src[k]
            if c == "{":
                depth += 1
            elif c == "}":
                depth -= 1
                if depth == 0:
                    break
            k += 1
        yield m.start(), header, src[j:k + 1]


def receivers(body: str, names):
    out = set()
    for n in names:
        for m in re.finditer(r"\b(" + n + r")\s*\(\s*([A-Za-z_][A-Za-z0-9_]*)", body):
            out.add((n, m.group(2)))
    return out


def main():
    findings = 0
    for path in sorted(ROOT.rglob("*.java")):
        src = path.read_text(encoding="utf-8", errors="replace")
        lines = src.splitlines()
        seen_spans = []
        for off, header, body in find_loops(src):
            reads = receivers(body, READS)
            writes = receivers(body, WRITES)
            recv_read = {r for _, r in reads}
            recv_write = {r for _, r in writes}
            shared = recv_read & recv_write
            if not shared:
                continue
            line = src[:off].count("\n") + 1
            # skip nested duplicates of the same receiver/line region
            if any(abs(line - l) < 3 for l, _ in seen_spans):
                continue
            seen_spans.append((line, header[:60]))
            findings += 1
            print(f"{path}:{line}: {header[:90].strip()}")
            print(f"    receivers: {', '.join(sorted(shared))}")
            # show the mutating lines
            for pat in WRITES + READS:
                for m in re.finditer(r"\b" + pat + r"\s*\(", body):
                    bl = src[:off + body.index(m.group(0), 0) if False else off].count("\n")
            body_off = src.find(body, off)
            for ln, text in enumerate(body.splitlines()):
                if any(re.search(r"\b" + w + r"\s*\(", text) for w in WRITES + READS):
                    print(f"      L{line + ln + 1}: {text.strip()[:110]}")
            print()
    print(f"total read-while-write loops: {findings}")
    return 1 if findings else 0


if __name__ == "__main__":
    sys.exit(main())
