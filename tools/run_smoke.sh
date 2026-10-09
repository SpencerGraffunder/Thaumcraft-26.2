#!/bin/bash
# ============================================================================
# In-game smoke run (Layer 3 of the audit pipeline).
#
# Boots the real dedicated dev server (CI=true ./gradlew runServer) with
# TC_SMOKE=1 so ThaumcraftSmoke runs its assertion battery at server start,
# then stops the server and greps the verdict:
#   SMOKE: <n> checks passed                -> exit 0
#   SMOKE: <n> passed, <m> FAILED: ...      -> exit 1
#
# Log: /tmp/tc_smoke_server.log
# ============================================================================
set -u
cd "$(dirname "$0")/.."

export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
LOG=/tmp/tc_smoke_server.log
: > "$LOG"

# Stop any leftover dev server first
lsof -ti tcp:25565 -sTCP:LISTEN 2>/dev/null | xargs -r kill -9 2>/dev/null

CI=true TC_SMOKE=1 ./gradlew runServer --console=plain > "$LOG" 2>&1 &
GRADLE_PID=$!

# Wait for the server to finish starting (up to 4 minutes)
booted=0
for i in $(seq 1 240); do
  if grep -q "Done (" "$LOG" 2>/dev/null; then
    booted=1
    break
  fi
  if ! kill -0 "$GRADLE_PID" 2>/dev/null; then
    break
  fi
  sleep 1
done
if [ "$booted" -ne 1 ]; then
  echo "SMOKE-RUNNER: server did not reach 'Done' (see $LOG)" >&2
  kill -9 "$GRADLE_PID" 2>/dev/null
  exit 1
fi

# Let the smoke battery (ServerStartedEvent) finish logging
sleep 6

# Stop the server: SIGINT the java process bound to the server port
SRV_PID=$(lsof -ti tcp:25565 -sTCP:LISTEN 2>/dev/null | head -1)
if [ -n "${SRV_PID:-}" ]; then
  kill -INT "$SRV_PID" 2>/dev/null
  for i in $(seq 1 30); do
    kill -0 "$SRV_PID" 2>/dev/null || break
    sleep 1
  done
  kill -0 "$SRV_PID" 2>/dev/null && kill -9 "$SRV_PID" 2>/dev/null
fi
kill "$GRADLE_PID" 2>/dev/null
wait "$GRADLE_PID" 2>/dev/null

echo "--- SMOKE verdict ---"
grep -E "\[SMOKE\]|SMOKE: [0-9]+" "$LOG" | sed 's/^/  /'
if grep -qE "SMOKE: [0-9]+ checks passed" "$LOG"; then
  exit 0
fi
exit 1
