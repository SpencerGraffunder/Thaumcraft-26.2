#!/usr/bin/env python3
"""Bucket javac errors from a compile log.

Usage: python3 tools/bucket_errors.py /tmp/tc_compile.log
Prints: total errors, unique (file,line,msg), top messages, and for
'cannot find symbol' the top missing symbols with locations.
"""
import re
import sys
from collections import Counter

log = sys.argv[1] if len(sys.argv) > 1 else "/tmp/tc_compile.log"
text = open(log, encoding="utf-8", errors="replace").read()

# Split into error blocks: a line with 'error:' followed by detail lines
lines = text.splitlines()
errors = []  # (file, line_no, message)
i = 0
while i < len(lines):
    m = re.match(r"^(.*\.java):(\d+): error: (.*)$", lines[i])
    if m:
        f, ln, msg = m.group(1), int(m.group(2)), m.group(3)
        # collect detail lines (symbol/location/required/found) until next error or blank-ish
        details = []
        j = i + 1
        while j < len(lines) and not re.match(r"^.*\.java:\d+: error:", lines[j]):
            s = lines[j].strip()
            if not s or s.startswith(">") or s.startswith("Note:"):
                break
            details.append(s)
            j += 1
        errors.append((f, ln, msg, details))
        i = j
    else:
        i += 1

print(f"TOTAL ERRORS: {len(errors)}")
uniq = {(f, l, m) for f, l, m, _ in errors}
print(f"UNIQUE (file,line,msg): {len(uniq)}")

msgs = Counter(m for _, _, m, _ in errors)
print("\n--- TOP MESSAGES ---")
for msg, c in msgs.most_common(30):
    print(f"{c:6d}  {msg[:120]}")

# cannot find symbol breakdown
sym = Counter()
symloc = {}
for f, l, m, d in errors:
    if "cannot find symbol" in m:
        for dl in d:
            if dl.startswith("symbol:"):
                s = dl.split(":", 1)[1].strip()
                sym[s] += 1
                symloc.setdefault(s, Counter())[f.split("src/main/java/")[-1]] += 1
print("\n--- TOP MISSING SYMBOLS ---")
for s, c in sym.most_common(50):
    top_files = ", ".join(f"{k}({v})" for k, v in symloc[s].most_common(3))
    print(f"{c:6d}  {s[:80]}   e.g. {top_files[:120]}")

# files with most errors
files = Counter(f.split("src/main/java/")[-1] for f, _, _, _ in errors)
print("\n--- TOP FILES BY ERROR COUNT ---")
for f, c in files.most_common(25):
    print(f"{c:6d}  {f}")
