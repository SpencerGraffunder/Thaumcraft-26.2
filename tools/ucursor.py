#!/usr/bin/env python3
"""Absolute cursor via a transient uinput device (bypasses the broken G305 path).

Usage (root):
  ucursor.py probe
  ucursor.py click X Y
  ucursor.py move X Y
  ucursor.py hold-ms N click X Y     # keep device alive longer for slow compositors
"""
import sys, time, os

SCREEN_W, SCREEN_H = 2560, 1440

def make_device():
    from evdev import UInput, ecodes as e
    ui = UInput({
        e.EV_KEY: [e.BTN_LEFT, e.BTN_RIGHT],
        e.EV_ABS: {
            e.ABS_X: (0, SCREEN_W - 1, 0, 0, 0),
            e.ABS_Y: (0, SCREEN_H - 1, 0, 0, 0),
        },
    }, name="evdev-cursor")
    return ui

def main():
    if len(sys.argv) < 2:
        print(__doc__); return
    cmd = sys.argv[1]
    args = sys.argv[2:]
    hold = 0.8
    if cmd == "move":
        x, y = int(args[0]), int(args[1])
        ui = make_device()
        time.sleep(hold)
        ui.write(3, 0, x)   # ABS_X
        ui.write(3, 1, y)   # ABS_Y
        ui.syn()
        time.sleep(hold)
        ui.close()
    elif cmd == "click":
        x, y = int(args[0]), int(args[1])
        ui = make_device()
        time.sleep(hold)
        ui.write(3, 0, x)
        ui.write(3, 1, y)
        ui.syn()
        time.sleep(0.25)
        ui.write(1, 0x110, 1)
        ui.syn()
        time.sleep(0.08)
        ui.write(1, 0x110, 0)
        ui.syn()
        time.sleep(hold)
        ui.close()
    elif cmd == "probe":
        ui = make_device()
        print("device created:", ui.fd)
        time.sleep(1.5)
        ui.close()
        print("done")

if __name__ == "__main__":
    main()
