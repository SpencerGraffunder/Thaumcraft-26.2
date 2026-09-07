#!/usr/bin/env python3
"""macOS GUI input driver via CoreGraphics (ctypes, no pyobjc needed).

Usage:
  mouse.py click X Y            # left click at DISPLAY coords (Retina-safe)
  mouse.py move X Y
  mouse.py drag X1 Y1 X2 Y2     # press-move-release
  mouse.py type TEXT            # unicode text (works for any characters)
  mouse.py key NAME             # Enter|Tab|Escape|Space|Backspace|Return
  mouse.py dclick X Y           # double click

Display coords = the 1512x942 logical space you see in screenshots'
display mapping; the script multiplies by (physical/logical) itself.
"""
import ctypes
import sys
import time

cg = ctypes.CDLL("/System/Library/Frameworks/CoreGraphics.framework/CoreGraphics")

kCGHIDEventTap = 0
CGEventMouseMoved = 5
CGEventLeftMouseDown = 1
CGEventLeftMouseUp = 2
CGEventKeyDown = 10
CGEventKeyUp = 11
CGMouseButtonLeft = 0

class CGPoint(ctypes.Structure):
    _fields_ = [("x", ctypes.c_double), ("y", ctypes.c_double)]

cg.CGEventCreateMouseEvent.argtypes = [ctypes.c_void_p, ctypes.c_uint32, CGPoint, ctypes.c_uint32]
cg.CGEventCreateMouseEvent.restype = ctypes.c_void_p
cg.CGEventCreateKeyboardEvent.argtypes = [ctypes.c_void_p, ctypes.c_uint16, ctypes.c_bool]
cg.CGEventCreateKeyboardEvent.restype = ctypes.c_void_p
cg.CGEventPost.argtypes = [ctypes.c_uint32, ctypes.c_void_p]
cg.CGEventSetType.argtypes = [ctypes.c_void_p, ctypes.c_uint32]
cg.CGEventKeyboardSetUnicodeString.argtypes = [ctypes.c_void_p, ctypes.c_uint16, ctypes.c_char_p]
cg.CGEventSetIntegerValueField.argtypes = [ctypes.c_void_p, ctypes.c_int64, ctypes.c_int64]
cg.CFRelease.argtypes = [ctypes.c_void_p]
# kCGEventKeyboardEventKeycode = 1, kCGKeyboardEventAutorepeat = 9
KEYCODE_FIELD = 1

KEYS = {
    "enter": 36, "return": 36, "tab": 48, "escape": 53, "esc": 53,
    "space": 49, "backspace": 51, "delete": 51,
    "w": 13, "a": 0, "s": 1, "d": 2, "e": 14, "t": 17, "r": 15, "f": 3,
    "1": 18, "2": 19, "3": 20, "4": 21, "5": 22,
    "up": 126, "down": 125, "left": 123, "right": 124,
}

def hold_key(name, ms):
    code = KEYS[name.lower()]
    down = cg.CGEventCreateKeyboardEvent(None, code, True)
    post(down)
    time.sleep(ms / 1000.0)
    up = cg.CGEventCreateKeyboardEvent(None, code, True)
    cg.CGEventSetType(up, CGEventKeyUp)
    post(up)


def rclick(x, y):
    post(mouse_event(CGEventMouseMoved, x, y))
    time.sleep(0.02)
    post(mouse_event(3, x, y, 1))
    post(mouse_event(4, x, y, 1))

def pt(x, y):
    return CGPoint(float(x), float(y))
def mouse_event(etype, x, y, button=CGMouseButtonLeft):
    e = cg.CGEventCreateMouseEvent(None, etype, pt(x, y), button)
    return e

def post(e):
    if e:
        cg.CGEventPost(kCGHIDEventTap, e)

def click(x, y):
    post(mouse_event(CGEventMouseMoved, x, y))
    time.sleep(0.02)
    post(mouse_event(CGEventLeftMouseDown, x, y))
    time.sleep(0.03)
    post(mouse_event(CGEventLeftMouseUp, x, y))

def dclick(x, y):
    click(x, y)
    time.sleep(0.06)
    click(x, y)

def move(x, y):
    post(mouse_event(CGEventMouseMoved, x, y))

def drag(x1, y1, x2, y2, steps=24):
    post(mouse_event(CGEventMouseMoved, x1, y1))
    post(mouse_event(CGEventLeftMouseDown, x1, y1))
    for i in range(1, steps + 1):
        x = x1 + (x2 - x1) * i / steps
        y = y1 + (y2 - y1) * i / steps
        post(mouse_event(CGEventMouseMoved, x, y))
        time.sleep(0.008)
    post(mouse_event(CGEventLeftMouseUp, x2, y2))

def type_text(text):
    buf = text.encode("utf-8")
    for ch in text:
        b = ch.encode("utf-8")
        down = cg.CGEventCreateKeyboardEvent(None, 0, True)
        cg.CGEventKeyboardSetUnicodeString(down, len(b), b)
        post(down)
        up = cg.CGEventCreateKeyboardEvent(None, 0, True)
        cg.CGEventSetType(up, CGEventKeyUp)
        cg.CGEventKeyboardSetUnicodeString(up, len(b), b)
        post(up)
        time.sleep(0.01)

def key(name):
    code = KEYS[name.lower()]
    down = cg.CGEventCreateKeyboardEvent(None, code, True)
    post(down)
    up = cg.CGEventCreateKeyboardEvent(None, code, True)
    cg.CGEventSetType(up, CGEventKeyUp)
    post(up)

def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(2)
    cmd = sys.argv[1]
    args = [a for a in sys.argv[2:]]
    if cmd == "click":
        click(float(args[0]), float(args[1]))
    elif cmd == "dclick":
        dclick(float(args[0]), float(args[1]))
    elif cmd == "move":
        move(float(args[0]), float(args[1]))
    elif cmd == "drag":
        drag(float(args[0]), float(args[1]), float(args[2]), float(args[3]))
    elif cmd == "type":
        type_text(args[0])
    elif cmd == "key":
        key(args[0])
    elif cmd == "hold":
        hold_key(args[0], float(args[1]))
    elif cmd == "rclick":
        rclick(float(args[0]), float(args[1]))
    else:
        print(__doc__)
        sys.exit(2)

if __name__ == "__main__":
    main()
