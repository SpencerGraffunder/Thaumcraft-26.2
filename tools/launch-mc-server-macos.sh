#!/bin/bash
# Launches a dedicated NeoForge server for the "NeoForge 26.2" Modrinth profile,
# using the same 26.2.0.76 client/server jar the Modrinth App uses (version-matched client).
# Usage: launch-mc-server.sh
set -euo pipefail

MA="$HOME/Library/Application Support/ModrinthApp"
VER="$MA/meta/versions/26.2-26.2.0.76"
SERVER_DIR="${SERVER_DIR:-$HOME/mc-build/server}"
JRE="$MA/meta/java_versions/zulu25.36.205-ca-jre25.0.4.1-macosx_aarch64/Contents/Home/bin/java"
ASSETS="$MA/meta/assets"
NATIVES="$MA/meta/natives/26.2-26.2.0.76"

CP=$(python3 - "$VER/26.2-26.2.0.76.json" "$MA/meta/libraries" <<'PY'
import json, sys, os
manifest, libroot = sys.argv[1], sys.argv[2]
d = json.load(open(manifest))

def rule_matches(rule):
    if 'os' in rule:
        o = rule['os']
        if 'name' in o:
            names = {'osx': True, 'osx-arm64': True}
            if not names.get(o['name'], False):
                return False
        if 'arch' in o and o['arch'] != 'aarch64':
            return False
    if 'features' in rule:
        for k, v in rule['features'].items():
            if v is not None and not v:
                return False
    return True

def library_included(lib):
    rules = lib.get('rules')
    if not rules:
        return True
    included = False
    for r in rules:
        if rule_matches(r):
            included = (r.get('action') == 'allow')
    return included

cp = []
seen = set()
for lib in d['libraries']:
    # Production layout: only include_in_classpath=true libs go on the classpath.
    # The neoforge universal jar (include_in_classpath=false) is added by FML
    # itself from the libraryDirectory, together with the patched game jar.
    if lib.get('include_in_classpath', True) is False:
        continue
    if not library_included(lib):
        continue
    parts = lib['name'].split(':')
    g, a, v = parts[0], parts[1], parts[2]
    fname = f"{a}-{v}.jar" if len(parts) == 3 else f"{a}-{v}-{parts[3]}.jar"
    path = os.path.join(libroot, g.replace('.', '/'), a, v, fname)
    if not os.path.exists(path):
        print(f"WARNING: missing library {lib['name']} -> {path}", file=sys.stderr)
        continue
    if path in seen:
        continue
    seen.add(path)
    cp.append(path)
print(":".join(cp))
PY
)

mkdir -p "$SERVER_DIR/mods"
# seed required files once
[ -f "$SERVER_DIR/eula.txt" ] || echo "eula=true" > "$SERVER_DIR/eula.txt"
if [ ! -f "$SERVER_DIR/server.properties" ]; then
cat > "$SERVER_DIR/server.properties" <<'EOF'
motd=Thaumcraft 26.2 Dev
port=25565
online-mode=false
allow-list=false
allow-cheats=true
max-tick-time=300000
enable-command-block=true
spawn-protection=0
view-distance=6
simulation-distance=4
level-seed=
level-type=minecraft:normal
generate-structures=true
EOF
fi
# make sure the mod is in the server mods dir
cp -f "$MA/profiles/NeoForge 26.2/mods/"thaumcraft-*.jar "$SERVER_DIR/mods/" 2>/dev/null || \
  cp -f "$HOME/mc-build/Thaumcraft-26.2/build/libs/thaumcraft-6.2.0+26.2.jar" "$SERVER_DIR/mods/"

cd "$SERVER_DIR"
exec "$JRE" \
  -Xmx4G \
  --sun-misc-unsafe-memory-access=allow \
  --enable-native-access=ALL-UNNAMED \
  -Djava.library.path="$NATIVES/java" \
  -Djna.tmpdir="$NATIVES/jna" \
  -Dorg.lwjgl.system.SharedLibraryExtractPath="$NATIVES/lwjgl" \
  -Dio.netty.native.workdir="$NATIVES/netty" \
  -Dminecraft.launcher.brand=ModrinthApp \
  -Dminecraft.launcher.version=1.0 \
  -Djava.net.preferIPv6Addresses=system \
  -DlibraryDirectory="$MA/meta/libraries" \
  --add-opens java.base/java.lang.invoke=ALL-UNNAMED \
  --add-exports jdk.naming.dns/com.sun.jndi.dns=java.naming \
  -cp "$VER/26.2-26.2.0.76.jar:$CP" \
  net.neoforged.fml.startup.Server \
  --nogui \
  --port 25565 \
  --fml.mcVersion "26.2" \
  --fml.neoForgeVersion "26.2.0.76" \
  --fml.neoFormVersion "26.2.0.76"
