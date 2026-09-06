#!/usr/bin/env python3
"""Synthetic input via real evdev devices (works on GNOME Wayland).

Mouse (event5, Logitech G305) is RELATIVE — move() computes deltas from the
current X-reported pointer position. Keyboard (event11) takes Linux keycodes.

Usage (must run as root — devices are root:input):
  evdev_input.py probe
  evdev_input.py move X Y
  evdev_input.py click [X Y]
  evdev_input.py key KEY [KEY ...]     # F2, Enter, Down, Tab, Escape, a-z, 0-9
  evdev_input.py type TEXT
"""
import sys, time, os, struct, subprocess

MOUSE = "/dev/input/event5"     # Logitech G305 (relative)
KEYBOARD = "/dev/input/event11"  # CM Storm TKL

KEYS = {
    "a": 30, "b": 48, "c": 46, "d": 32, "e": 18, "f": 33, "g": 34, "h": 35,
    "i": 23, "j": 36, "k": 37, "l": 38, "m": 50, "n": 49, "o": 24, "p": 25,
    "q": 16, "r": 19, "s": 31, "t": 20, "u": 22, "v": 47, "w": 17, "x": 45,
    "y": 21, "z": 46,
    "0": 11, "1": 2, "2": 3, "3": 4, "4": 5, "5": 6, "6": 7, "7": 8, "8": 9,
    "9": 10,
    "Enter": 28, "Escape": 1, "Tab": 15, "Space": 57, "Backspace": 14,
    "Up": 103, "Down": 108, "Left": 105, "Right": 106,
    "F1": 59, "F2": 60, "F3": 61, "F4": 62, "F5": 63, "F6": 64,
    "F7": 65, "F8": 66, "F9": 67, "F10": 68, "F11": 87, "F12": 88,
    "Shift": 42, "Ctrl": 29,
}

def _write(fd, etype, code, value, delay=0.002):
    os.write(fd, struct.pack("llhhI", 0, 0, etype, code, value & 0xFFFFFFFF))
    time.sleep(delay)

def _sync(fd):
    _write(fd, 0, 0, 0, 0.004)

def _pos():
    """Current pointer position via xdotool (X mirrors the Wayland pointer)."""
    out = subprocess.run(
        ["xdotool", "getmouselocation"],
        env={**os.environ, "DISPLAY": ":0"},
        capture_output=True, text=True).stdout
    x = int(out.split("x:")[1].split()[0])
    y = int(out.split("y:")[1].split()[0])
    return x, y

def move(x, y):
    cx, cy = _pos()
    dx, dy = x - cx, y - cy
    if dx == 0 and dy == 0:
        return
    # send in a few steps so libinput doesn't drop it
    steps = 3
    fd = os.open(MOUSE, os.O_WRONLY | os.O_NONBLOCK)
    for i in range(1, steps + 1):
        _write(fd, 2, 0, dx * i // steps - (dx * (i - 1) // steps))  # REL_X
        _write(fd, 2, 1, dy * i // steps - (dy * (i - 1) // steps))  # REL_Y
        _sync(fd)
        time.sleep(0.03)
    os.close(fd)

def click(x=None, y=None):
    if x is not None:
        move(x, y)
        time.sleep(0.2)
    fd = os.open(MOUSE, os.O_WRONLY | os.O_NONBLOCK)
    _write(fd, 1, 0x110, 1, 0.01)
    _sync(fd)
    time.sleep(0.06)
    _write(fd, 1, 0x110, 0, 0.01)
    _sync(fd)
    os.close(fd)

def key(*names):
    fd = os.open(KEYBOARD, os.O_WRONLY | os.O_NONBLOCK)
    for n in names:
        code = KEYS.get(n)
        if code is None:
            os.close(fd)
            raise SystemExit(f"unknown key {n}")
        _write(fd, 1, code, 1, 0.02)
        _sync(fd)
        time.sleep(0.05)
        _write(fd, 1, code, 0, 0.02)
        _sync(fd)
        time.sleep(0.08)
    os.close(fd)

def type_text(text):
    for ch in text:
        if ch == " ":
            key("Space")
        elif ch in KEYS:
            key(ch)
        else:
            raise SystemExit(f"cannot type {ch!r}")

def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return
    cmd, args = sys.argv[1], sys.argv[2:]
    if cmd == "probe":
        print("pointer at", _pos())
    elif cmd == "move":
        move(int(args[0]), int(args[1]))
        print("now at", _pos())
    elif cmd == "click":
        if len(args) >= 2:
            click(int(args[0]), int(args[1]))
        else:
            click()
    elif cmd == "key":
        key(*args)
    elif cmd == "type":
        type_text(args[0])
    else:
        print(__doc__)

if __name__ == "__main__":
    main()
