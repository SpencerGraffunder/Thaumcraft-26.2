#!/usr/bin/env python3
"""Dump error contexts for given symbol substrings to a file."""
import re, sys

log = "/tmp/tc_compile.log"
out = open("/tmp/ctx_out.txt", "w")
lines = open(log, encoding="utf-8", errors="replace").read().splitlines()

targets = sys.argv[1:] or ["variable random", "variable super", "method getTag()",
                           "method hasTag()", "method location()", "class MultiBufferSource",
                           "variable Builder"]

# Build list of (idx, file, line, msg) for error lines
errs = []
for i, ln in enumerate(lines):
    m = re.match(r"^(.*\.java):(\d+): error: (.*)$", ln)
    if m:
        errs.append((i, m.group(1), int(m.group(2)), m.group(3)))

for t in targets:
    out.write(f"\n########## {t} ##########\n")
    count = 0
    for (i, f, ln, msg) in errs:
        # find the symbol detail line within next 4 lines
        block = "\n".join(lines[i:i+5])
        if t in block:
            out.write(f"\n{f.split('src/main/java/')[-1]}:{ln}\n")
            out.write(block + "\n")
            count += 1
            if count >= 4:
                break
    out.write(f"--- ({count} shown) ---\n")

out.close()
print("done")
