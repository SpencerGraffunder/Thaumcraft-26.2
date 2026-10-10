#!/bin/bash
# ============================================================================
# Audit gate — the machine-enforced "is the port done?" signal.
#
# Runs every parity oracle (tools/audit_*.py, tools/recipe_parity.py) and
# exits 0 only when ALL oracles pass. Each oracle is a bounded 1.12-vs-port
# diff with an objective green state (0 findings). See docs/AUDIT-LEDGER.md.
#
# Usage:
#   tools/run_all_audits.sh            # run all oracles, print table
#   tools/run_all_audits.sh <oracle>   # run a single oracle (e.g. audit_models)
#
# Bootstrap: the oracles read two caches that are volatile in /tmp on macOS,
# so they live in .reference/ (gitignored). If .reference/vdata is missing
# this script regenerates it from the gradle + Modrinth caches.
# ============================================================================
set -u
cd "$(dirname "$0")/.."

# ---------------------------------------------------------------- bootstrap
if [ ! -d .reference/vdata/vanilla/data ]; then
  echo "[bootstrap] .reference/vdata missing — regenerating from caches..."
  NEOFORGE_UNIVERSAL=$(find "$HOME/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge" \
      -name "neoforge-*-universal.jar" 2>/dev/null | head -1)
  VANILLA_JAR=$(ls "$HOME/Library/Application Support/ModrinthApp/meta/versions/"26.3*/*.jar 2>/dev/null | head -1)
  if [ -z "${NEOFORGE_UNIVERSAL:-}" ] || [ -z "${VANILLA_JAR:-}" ]; then
    echo "[bootstrap] ERROR: cannot find neoforge universal jar or vanilla 26.3 jar." >&2
    echo "[bootstrap]        set .reference/vdata up manually (see docs/AUDIT-LEDGER.md)." >&2
    exit 2
  fi
  mkdir -p .reference/vdata
  unzip -q -o "$NEOFORGE_UNIVERSAL" 'data/*' -d .reference/vdata/nf
  unzip -q -o "$VANILLA_JAR" 'data/*' 'assets/minecraft/lang/*' -d .reference/vdata/vanilla
  echo "[bootstrap] done."
fi

ORACLES=(
  audit_aspects        # 1.12 ConfigAspects vs port (aspect values)
  recipe_parity        # 1.12 recipe pairs vs port (ingredients/gates/aspects)
  audit_k_research     # research JSON internal refs (functional vs display)
  audit_k2_resources   # research<->recipe cross-refs (gates + page recipe ids)
  audit_client_items   # client item definitions exist + resolve
  audit_models         # model/texture/blockstate wiring
  audit_resources      # model->texture + Java->resource file refs
  audit_inv_loops      # inventory read-while-write loops
  audit_lang           # every registry id has a modern-convention lang key
)

if [ "$#" -ge 1 ]; then
  ORACLES=("$@")
fi

fail=0
echo "=== AUDIT GATE ($(date +%H:%M:%S)) ==="
printf "%-20s %-6s %s\n" "ORACLE" "STATE" "DETAIL"
for n in "${ORACLES[@]}"; do
  log="/tmp/audit_gate_$n.log"
  if python3 "tools/$n.py" > "$log" 2>&1; then
    # pull the headline number from the log when the script prints one
    detail=$(grep -oE "(242 pairs|matched pairs: [0-9]+|TOTAL missing file references: [0-9]+|total read-while-write loops: [0-9]+|all registry ids have lang keys)" "$log" | tail -1)
    printf "%-20s %-6s %s\n" "$n" "PASS" "${detail:-}"
  else
    detail=$(grep -cE "MISSING|unresolved|divergent|FAIL" "$log" 2>/dev/null)
    printf "%-20s %-6s %s (log: %s)\n" "$n" "FAIL" "$detail finding-line(s)" "$log"
    fail=1
  fi
done

# --- runtime smoke (Layer 3) ------------------------------------------------
# Boots a real dedicated server in a fresh world with TC_SMOKE=1; the mod runs
# its in-game assertions and logs SMOKE-PASS / SMOKE-FAIL lines.
if [ "${1:-}" = "all" ] || [ "$#" -eq 0 ]; then
  if [ -f src/main/java/thaumcraft/common/lib/smoke/ThaumcraftSmoke.java ]; then
    if tools/run_smoke.sh > /tmp/audit_gate_smoke.log 2>&1; then
      printf "%-20s %-6s %s\n" "smoke (in-game)" "PASS" "$(grep -oE 'SMOKE: [0-9]+ checks passed' /tmp/audit_gate_smoke.log | tail -1)"
    else
      printf "%-20s %-6s %s (log: %s)\n" "smoke (in-game)" "FAIL" "$(grep -cE '\[SMOKE\] FAIL' /tmp/audit_gate_smoke.log) failing check(s)" /tmp/audit_gate_smoke.log
      fail=1
    fi
  else
    printf "%-20s %-6s %s\n" "smoke (in-game)" "SKIP" "ThaumcraftSmoke.java not present yet"
  fi
fi

echo "=================================="
if [ "$fail" -eq 0 ]; then
  echo "GATE: GREEN — all oracles pass"
else
  echo "GATE: RED — fix the FAIL rows above (see docs/AUDIT-LEDGER.md)"
fi
exit $fail
