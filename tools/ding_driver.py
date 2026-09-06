#!/usr/bin/env python3
"""Persistent GNOME remote-desktop input driver.

Verified protocol (mutter src/backends/meta-remote-desktop-session.c):
  1. RemoteDesktop.CreateSession()            -> rpath  (our conn becomes peer)
  2. Get(rpath, SessionId)                    -> uuid
  3. ScreenCast.CreateSession({remote-desktop-session-id: uuid}) -> spath
  4. ScreenCast.Session.RecordVirtual({})     -> stream path
  5. ScreenCast.Session.Start()               (starts streams -> "active")
  6. RemoteDesktop.Session.Start()
  7. Notify*(rpath, ...)                      (sender == peer_name -> allowed)

Commands on stdin (one per line):
  move X Y | click X Y | key NAME | type TEXT | scroll N
  status | quit
  NAME: Enter Escape Tab Space Backspace Up Down Left Right F1..F12 a-z 0-9
"""
import sys, time
import gi
gi.require_version('Gio', '2.0')
gi.require_version('GLib', '2.0')
from gi.repository import Gio, GLib

SC = "org.gnome.Mutter.ScreenCast"
SIF = SC + ".Session"
RD = "org.gnome.Mutter.RemoteDesktop"
RIF = RD + ".Session"

KEYSYMS = {
    "Enter": 0xFF0D, "Escape": 0xFF1B, "Tab": 0xFF09, "Space": 0x20,
    "Backspace": 0xFF08, "Up": 0xFF52, "Down": 0xFF54,
    "Left": 0xFF51, "Right": 0xFF53,
    "F1": 0xFFBE, "F2": 0xFFBF, "F3": 0xFFC0, "F4": 0xFFC1,
    "F5": 0xFFC2, "F6": 0xFFC3, "F7": 0xFFC4, "F8": 0xFFC5,
    "F9": 0xFFC6, "F10": 0xFFC7, "F11": 0xFFC8, "F12": 0xFFC9,
    "Shift": 0xFFE1, "Ctrl": 0xFFE3,
}
for ch in "abcdefghijklmnopqrstuvwxyz":
    KEYSYMS[ch] = ord(ch.upper())
for ch in "0123456789":
    KEYSYMS[ch] = ord(ch)

BTN_LEFT = 272

def main():
    conn = Gio.bus_get_sync(Gio.BusType.SESSION, None)

    def call(dest, path, iface, method, var=None):
        res = conn.call_sync(dest, path, iface, method, var, None,
                             Gio.DBusCallFlags.NONE, -1, None)
        return res.unpack()

    def prop(dest, path, iface, name):
        v = call(dest, path, "org.freedesktop.DBus.Properties", "Get",
                 GLib.Variant("(ss)", (iface, name)))
        return v[0]

    # 1-2. RemoteDesktop session (our connection becomes the peer)
    (rpath,) = call(RD, "/org/gnome/Mutter/RemoteDesktop", RD, "CreateSession")
    rdid = prop(RD, rpath, RIF, "SessionId")
    # 3. ScreenCast session linked to it
    (spath,) = call(SC, "/org/gnome/Mutter/ScreenCast", SC, "CreateSession",
                    GLib.Variant("(a{sv})", ({"remote-desktop-session-id": GLib.Variant("s", rdid)},)))
    print(f"sc session: {spath}", flush=True)

    # 4. stream (must exist before Start)
    (stream,) = call(SC, spath, SIF, "RecordVirtual",
                     GLib.Variant("(a{sv})", ({},)))
    print(f"stream: {stream}", flush=True)

    # 5. RD Start (internally starts the linked screencast session + EIS)
    call(RD, rpath, RIF, "Start")
    print("remotedesktop started (screencast active)", flush=True)

    def move(x, y):
        call(RD, rpath, RIF, "NotifyPointerMotionAbsolute",
             GLib.Variant("(sdd)", (stream, float(x), float(y))))

    def click(x, y):
        move(x, y)
        time.sleep(0.25)
        call(RD, rpath, RIF, "NotifyPointerButton",
             GLib.Variant("(ib)", (BTN_LEFT, True)))
        time.sleep(0.08)
        call(RD, rpath, RIF, "NotifyPointerButton",
             GLib.Variant("(ib)", (BTN_LEFT, False)))

    def key(name):
        ks = KEYSYMS.get(name)
        if ks is None:
            print(f"error: unknown key {name}", flush=True)
            return
        call(RD, rpath, RIF, "NotifyKeyboardKeysym",
             GLib.Variant("(ub)", (ks, True)))
        time.sleep(0.06)
        call(RD, rpath, RIF, "NotifyKeyboardKeysym",
             GLib.Variant("(ub)", (ks, False)))
        time.sleep(0.1)

    def scroll(steps):
        call(RD, rpath, RIF, "NotifyPointerAxisDiscrete",
             GLib.Variant("(ui)", (0, steps)))

    def type_text(text):
        for ch in text:
            if ch == " ":
                key("Space")
            elif ch in KEYSYMS:
                key(ch)
            else:
                print(f"error: cannot type {ch!r}", flush=True)

    print("ready", flush=True)
    for line in sys.stdin:
        parts = line.split()
        if not parts:
            continue
        cmd = parts[0]
        try:
            if cmd == "move":
                move(int(parts[1]), int(parts[2]))
            elif cmd == "click":
                click(int(parts[1]), int(parts[2]))
            elif cmd == "key":
                key(parts[1])
            elif cmd == "type":
                type_text(parts[1])
            elif cmd == "scroll":
                scroll(int(parts[1]))
            elif cmd == "status":
                print("alive", flush=True)
            elif cmd == "quit":
                break
            else:
                print(f"error: unknown cmd {cmd}", flush=True)
        except Exception as e:
            print(f"error: {e}", flush=True)
    print("exiting", flush=True)

if __name__ == "__main__":
    main()
