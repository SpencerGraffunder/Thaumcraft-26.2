---
name: minecraft-gui
description: Launch, verify, screenshot, and drive the Thaumcraft 26.2 Minecraft client/server on the GNOME Wayland box or the macOS box (Modrinth App + ./gradlew). Use when asked to test the mod in-game, check item/block rendering, read load-time errors, or connect the client to the dev server.
---

# Driving the Thaumcraft 26.2 Minecraft GUI

Covers launching the client/server, verifying changes, taking screenshots, reading
load errors, and injecting GUI input — on **this** box: GNOME **Wayland**, Modrinth App
profile, and the `./gradlew` dev environment. Repo: `/home/graffunder/Documents/Thaumcraft-26.2`.

## TL;DR — the thing that saves you time

**Most verification needs NO GUI input at all.** Run `CI=true ./gradlew runClient`, wait
for the main menu, and **read the log**. Model baking, missing-texture/model warnings, and
load errors all land in the log — not the GUI. Only reach for GUI input when the check
genuinely requires it (open JEI to render items, click through menus, join a server).

## Environment facts (verified 2026-09-05)

| What | Value |
|------|-------|
| Repo | `/home/graffunder/Documents/Thaumcraft-26.2` |
| Display | **GNOME Wayland**, screen **2560x1440**, `DISPLAY=:0` (XWayland) |
| Build/install | `CI=true ./gradlew build` → `build/libs/thaumcraft-*.jar` |
| Modrinth profile | `~/.local/share/ModrinthApp/profiles/NeoForge 26.2/` |
| — mods dir | `…/profiles/NeoForge 26.2/mods/` (drop the built jar here) |
| — profile logs / crashes | `…/profiles/NeoForge 26.2/logs/`, `…/crash-reports/` |
| — profile world | `…/profiles/NeoForge 26.2/saves/<world>/` |
| Bundled JRE | `~/.local/share/ModrinthApp/meta/java_versions/zulu25.36.205-ca-jre25.0.4.1-linux_x64/bin/java` |
| Game version | `26.2` = build **26.2.0.75** (client & server must match) |
| Dev env workdir | `run/` (gradle `workingDirectory project.file('run')`) |
| — dev world | `run/world/` |
| — dev logs / crashes | `run/logs/latest.log`, `run/crash-reports/` |
| — dev mods | `run/mods/` (auto-seeded from source by NeoGradle) |
| Dev server port | **25565**, `online-mode=false` |
| Launcher log | `~/.local/share/ModrinthApp/launcher_logs/session_*.log` |
| `sudo` | **passwordless** → the root input tools below run directly |
| Input tools | `tools/evdev_input.py`, `tools/ucursor.py`, `tools/ding_driver.py`, `tools/screenshot.py` |

## The Wayland constraint (read this first)

GNOME Wayland is **focus-only** for input:

- The `computer` tool's `click` / `press` / `type` only reach the **currently focused**
  window. You **cannot** programmatically focus or raise a window. If the target window
  isn't focused, input silently lands nowhere (or throws `BackgroundUnavailable`).
- **Screenshots work regardless of focus**: `computer` → `window.screenshot()`, or
  `scrot`, or in-game **F2** (saves to the profile's `screenshots/`).
- The **Modrinth App is a Tauri webview** — its internals are **not** exposed over AT-SPI,
  so `computer`'s `.ax()` / `.find()` see only the top-level window node. You cannot
  address its buttons by role/title; you'd need pixel coordinates + a real input device.
- **The working input path is synthetic *hardware* input** — Wayland can't tell it apart
  from a real mouse/keyboard, so it is accepted regardless of which window is focused:
  - `sudo python3 tools/evdev_input.py move|click|key|type` — one-shot, real evdev devices
    (mouse `event5` Logitech G305 *relative*, keyboard `event11`).
  - `sudo python3 tools/ucursor.py click X Y` / `move X Y` — **absolute** uinput cursor
    (more reliable than the relative G305 path for point-and-click).
  - `python3 tools/ding_driver.py` — persistent GNOME remote-desktop (D-Bus) session;
    reads `move X Y | click X Y | key NAME | type TEXT | scroll N | status | quit` on stdin.

**Coordinate math for pixel clicks:** input tools take **absolute screen** coords (0..2560,
0..1440). To click a button you see in a window screenshot:
`abs_x = window.x + btn_x_in_screenshot`, `abs_y = window.y + btn_y_in_screenshot`.
Get `window.x/y` from `computer` → `desktop.windows()`.
## macOS (Spencers-MacBook-Air — verified 2026-09-06)

Different box, different failure modes. The repo here lives on an **SMB share**
(`//graffunder@192.168.4.200/ai-workspace`), which changes the build flow.

| What | Value |
|------|-------|
| Repo | `/Users/spencer/ai-workspace/Thaumcraft-26.2` (**SMB mount** — Gradle cannot hash files here) |
| Local build dir | `~/mc-build/Thaumcraft-26.2` (repo copy on local disk) |
| JDK 25 | `brew install openjdk@25` → `JAVA_HOME=/opt/homebrew/opt/openjdk@25` (keg-only) |
| Modrinth App | `/Applications/Modrinth App.app`; data at `~/Library/Application Support/ModrinthApp/` |
| Profile | `…/ModrinthApp/profiles/NeoForge 26.2/` (mods/, logs/, saves/) |
| Bundled JRE | `…/ModrinthApp/meta/java_versions/zulu25…-macosx_aarch64/Contents/Home` (JRE only — no javac) |
| Display | **1680x1050 logical**, Retina 2x → screenshots are **3360x2100** |
| Input | `tools/mouse_mac.py` (CoreGraphics: click/drag/hold/type/key — preferred); `cliclick` for one-shot clicks only (no drag support) |
| Screenshots | `screencapture -x /tmp/shot.png` |

### Build (SMB workaround)

Gradle's `FileHasher` fails with `Operation not supported` on `smbfs`. Build from a local copy:

```bash
rm -rf ~/mc-build/Thaumcraft-26.2
cp -a /Users/spencer/ai-workspace/Thaumcraft-26.2 ~/mc-build/   # slow over SMB — don't let it time out
cd ~/mc-build/Thaumcraft-26.2
CI=true JAVA_HOME=/opt/homebrew/opt/openjdk@25 ./gradlew build
# jar: build/libs/thaumcraft-6.2.0+26.2.jar (~11MB with resources)
cp build/libs/thaumcraft-*.jar "$HOME/Library/Application Support/ModrinthApp/profiles/NeoForge 26.2/mods/"
```

If the local tree is missing files relative to git (partial copy), `git checkout -- .`
restores them from HEAD before rebuilding. A 3.7MB jar = no resources; FML dies with
`NullPointerException ... "version" is null` during discovery (missing
`META-INF/neoforge.mods.toml`) until rebuilt complete.

### Launch — use the Modrinth App, not a manual java command

```bash
open -a "Modrinth App"
# screenshot, then click the green Play button (top-right of the instance card)
LOG="$HOME/Library/Application Support/ModrinthApp/profiles/NeoForge 26.2/logs/latest.log"
until grep -q "Setting user" "$LOG"; do sleep 3; done
```

- Game window title: `Minecraft NeoForge* 26.2` (854x508); main menu in ~40s.
- Manual launch (reconstructed from `meta/versions/26.2-26.2.0.76/26.2-26.2.0.76.json`)
  gets past FML discovery but **dies in FML early display**: `Failed to find a primary
  monitor / glfwGetPrimaryMonitor failed` — even though a standalone GLFW test in the
  same session finds the monitor fine, and the app-launched client works. Use the app.
  (Production launches need `--fml.mcVersion`, `--fml.neoForgeVersion`,
  `--fml.neoFormVersion` — missing them is the discovery-stage NPE.)

### Coordinates (pixel clicking)

- cliclick space = logical **1680x1050**.
- Screenshots are **3360x2100** (2x) → `cliclick_x = screenshot_px / 2`.
- Agent image previews (~1568x980) are *not* cliclick space: scale by 1680/1568 = 1.0714.
- Window geometry: `osascript -e 'tell application "System Events" to tell process "java" to get {position, size} of windows'`.

### Permissions & display sleep (the two things that bite)

- **TCC**: Terminal needs **Screen Recording** + **Accessibility**. macOS 26 also pops a
  *"bypass the system private window picker"* prompt on first `screencapture` — **the
  user must click Allow**; synthetic clicks do not reach TCC dialogs.
- **Display sleep** (5 min): screenshots are solid black when the display is asleep.
  `caffeinate -u -t 2` wakes it just before `screencapture`;
  `caffeinate -d -t 600 &` keeps it awake while driving the game.
- `screencapture` fails with "could not create image from display" until the TCC grant.

### Dedicated server + multiplayer (verified 2026-09-06)

Production-style server straight from the Modrinth profile (bundled Zulu JRE +
patched server jar), no Gradle involved. Script: `tools/launch-mc-server-macos.sh`
(also `~/mc-build/launch-mc-server.sh`):

- Server dir `~/mc-build/server` (world/mods/logs). **`cd` into it before exec** —
  the vanilla 26.2 server main uses CWD as gameDir and *rejects* `--gameDir`,
  `--assetsDir`, `--versionType`, `--version` ("not a recognized option"). Accepted
  args: `--nogui --port 25565` + `--fml.mcVersion/--fml.neoForgeVersion/--fml.neoFormVersion`.
- Boots to `Done (` in ~8s; Thaumcraft fully initializes (148 research entries, 13
  seals, golem parts, crystal clusters in worldgen).
- Client join: Modrinth App **Play** → `Multiplayer` → `Direct Connection` → type
  `localhost:25565` → `Join Server`. Handshake clean; in-world; mobs spawn/kill
  (cave spider, creeper); `/give`, `/gamemode`, Thaumium Block placement, Pech Wand
  right-click all work; **zero exceptions in server+client logs**.
- Player ops from console: `hub`/pty `op Specnor` etc. Player needs op for `/give`/
  `/gamemode`.

### macOS input & window quirks (verified 2026-09-06)

- **`cliclick` cannot drag** (no mouse-down/move/up actions). Use
  `tools/mouse_mac.py` (CoreGraphics via ctypes, zero deps) for everything:
  `click|dclick|move|drag|rclick|type TEXT|key NAME|hold KEY MS`.
- **Background-window clicks are swallowed** — macOS spends the first click
  activating the window. Click the same point **twice**, or raise the app first.
  The Terminal app re-grabs frontmost whenever the agent shell writes to it, so do
  raise+click inside one shell command.
- `set frontmost` via System Events and window-geometry queries are unreliable/empty
  for the Modrinth app and the game — don't depend on them; double-click works.
- **Display sleep** blackens `screencapture` (and `screencapture -R` fails outright
  with "could not create image from rect"). During long GUI sessions hold
  `caffeinate -dimsu -t 2700 &` to keep the display awake.
- **Crosshair zoom without PIL:** `sips -c 500 700 shot.png --out zoom.png` crops
  **centred**, and the in-game crosshair sits at image centre — instant zoom of
  exactly where you're aiming.
- **Aiming the camera:** synthetic LMB-drag (mouse-look) did not rotate the view on
  this box. Use `/tp @s ~ ~ ~ <yaw> <pitch>` (pitch 90 = straight down) — reliable.
  The new 26.2 `/rotate <target> rotation|facing …` command wants an unknown number
  of doubles and failed in every form tried; don't bother.
- In-game right-click acts at the **crosshair** (screen centre), not the mouse
  cursor — the click position only matters for hitting the window.

## Workflows

### 1. Build + install into the Modrinth profile
```bash
cd /home/graffunder/Documents/Thaumcraft-26.2
CI=true ./gradlew build
cp build/libs/thaumcraft-*.jar ~/.local/share/ModrinthApp/profiles/"NeoForge 26.2"/mods/
```

### 2. Launch the dev client (preferred for verification)
Loads the mod from **source** (no jar needed), JEI included.
```bash
cd /home/graffunder/Documents/Thaumcraft-26.2
CI=true ./gradlew runClient > /tmp/runclient.log 2>&1 &
# reach main menu in ~60-90s:
until grep -q "Setting user: Dev" /tmp/runclient.log 2>/dev/null; do sleep 2; done
grep "Mods loaded" /tmp/runclient.log
```
- Window title: `Minecraft NeoForge 26.2` (default size 854x517, not focused by default).
- Check the log for warnings (next section) **instead of** trying to read the screen.

### 3. Launch the dedicated server (for multiplayer testing)
```bash
cd /home/graffunder/Documents/Thaumcraft-26.2
CI=true ./gradlew runServer > /tmp/runserver.log 2>&1 &
until grep -qE "Done \(" /tmp/runserver.log 2>/dev/null; do sleep 2; done
```
Listens on **25565**; world at `run/world`. To stop: SIGINT/Ctrl+C (typing `stop` into the
console is **not** forwarded to the process).

### 4. Verify rendering / load — log-based, no GUI needed
```bash
LOG=run/logs/latest.log        # or /tmp/runclient.log for the dev client
grep -c "Missing item model for" $LOG            # purple/black items — want 0
grep "Missing texture references in model thaumcraft" $LOG   # missing sub-textures (e.g. particle)
grep -iE "WARN|ERROR" $LOG | grep -i thaumcraft  # mod-specific problems
grep "Setting user: Dev" $LOG && grep "Mods loaded" $LOG     # reached main menu?
```
"Missing item model for: N" (N > 0) = those items render **purple/black**.
"Missing texture references in model …: particle" = a 3D element model lacks a `particle`
texture — **cosmetic** (block-break particles only); the item itself renders fine.

### 5. Take a screenshot
```bash
# full screen -> ASCII (works for a text-only agent; no focus needed)
python3 tools/screenshot.py --full
# a specific X window id
python3 tools/screenshot.py <window-id>
# save a PNG too
python3 tools/screenshot.py --save /tmp/shot.png --full
# read one pixel (RGB) to confirm a button colour / hitbox
python3 tools/screenshot.py --pixel X Y --full
```
Or via the `computer` tool (returns a viewable image + a saved full-res path):
```js
const wins = await desktop.windows();
const mc = wins.find(w => /Minecraft/i.test((w.app||"") + (w.title||"")));
await mc.screenshot();
```

### 6. Inject GUI input (only when the check requires it)
```bash
# move the real pointer to an absolute screen position
sudo python3 tools/evdev_input.py move 1280 720
# click at absolute coords (evdev relative path)
sudo python3 tools/evdev_input.py click 1280 720
# absolute uinput click (preferred for precise point-and-click)
sudo python3 tools/ucursor.py click 1280 720
# keys (a-z, 0-9, Enter, Escape, Tab, Space, Backspace, arrows, F1-F12, Shift, Ctrl)
sudo python3 tools/evdev_input.py key F2            # in-game screenshot
sudo python3 tools/evdev_input.py key Enter
sudo python3 tools/evdev_input.py type "localhost:25565"
```
Typical menu flow (title screen → join): click `Multiplayer` → click the seeded
`Local Dev` server row (or `Direct Connect`) → type `localhost:25565` → `Enter`.
Screenshot after each step to confirm state before the next click.

### 7. Connect client → server
- The profile already has a **seeded `servers.dat`** (entry `Local Dev` → `localhost:25565`),
  so the server appears in the Multiplayer list — one click to join.
- Dev client (`runClient`) uses `run/`; seed `run/servers.dat` the same way if absent.
- Both sides must be the **same** game build (26.2.0.75). Mismatch → handshake reject.

### 8. Read load-time / pre-main-menu errors
Some failures show in the **GUI before the main menu** (crash screen, mod-load failure).
Capture them without fighting input:
```bash
ls -t run/crash-reports/*.txt 2>/dev/null | head -1   # dev env
ls -t ~/.local/share/ModrinthApp/profiles/"NeoForge 26.2"/crash-reports/*.txt | head -1
tail -50 run/logs/latest.log
python3 tools/screenshot.py --full   # grab the on-screen crash text as ASCII
```

### 9. Close / kill
```bash
pkill -f "fml.startup.Client"   # dev client
pkill -f "fml.startup.Server"   # dev server
```

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Items render **purple/black** | missing `models/item/<id>.json` ClientItem, or model→texture unresolvable | `grep "Missing item model for" log`; add the ClientItem + a real texture ref |
| `Missing texture references in model …: particle` | 3D element model (casters/grapple) lacks `particle` | add `"particle": "thaumcraft:..."` to the 3D model; cosmetic only |
| Client can't join dev server | version mismatch, or missing S2C channels | both on 26.2.0.75; see TODO.md P0 multiplayer fix (PacketClientWiring) |
| `computer` input does nothing | target window not focused (Wayland) | use `tools/ucursor.py` / `evdev_input.py` (synthetic hardware) instead |
| `computer` `.ax()` finds no buttons | Modrinth App is a webview (no AT-SPI tree) | pixel-click via uinput, or drive the game (not the launcher) |
| First-launch "Enter World Name"/intro | fresh profile | this profile is already past it (`options.txt` present) |
| `neoFormPatch` fails | running without `CI` | prefix every gradle run with `CI=true` |

## GUI layout notes (for pixel clicking)

- Title screen buttons are centred, ~200px wide x 20px tall, ~4px vertical gaps, top-to-bottom:
  `Singleplayer`, `Multiplayer`, `Realms`… (below the centre logo).
- Multiplayer screen: `Direct Connect` bottom-left, `Add Server` bottom-right; the seeded
  `Local Dev` row is in the server list (centre).
- In-game: `E` = inventory, `R` = JEI recipe book (renders **all** item icons — the surest way
  to force-render every item and surface any still-broken model), `F2` = screenshot.
- Always screenshot to confirm state before and after each input action.
