#!/usr/bin/env python3
"""Screenshot an X11 window or the full screen and render it as ASCII.

Usage:
  screenshot.py <window-id>            # capture window (scrot -w)
  screenshot.py --full                 # capture full screen
  screenshot.py --save out.png <wid>   # also save PNG
  screenshot.py --cols 120 <wid>       # ASCII width (default 100)
  screenshot.py --pixel X Y <wid>      # print RGB at window-relative pixel

The ASCII render lets a text-only agent verify on-screen layout (button
positions, dialogs, crash screens) without image input support.
Luminance is mapped to Unicode shade blocks, dark -> light:
  ' .:-=+*#%@'
"""
import subprocess
import sys
import tempfile
import os

CHARS = " .:-=+*#%@#"


def capture(window_id: str | None, save_to: str | None) -> str:
    fd, path = tempfile.mkstemp(suffix=".png")
    os.close(fd)
    os.unlink(path)  # scrot refuses to overwrite; give it a fresh name
    cmd = ["scrot"]
    if window_id:
        cmd += ["-w", window_id]
    cmd.append(path)
    subprocess.run(cmd, check=True)
    return path


def render(path: str, cols: int = 100) -> str:
    from PIL import Image

    img = Image.open(path).convert("L")
    w, h = img.size
    rows = max(1, int(cols * h / w / 2))  # chars are ~2:1 tall
    small = img.resize((cols, rows))
    px = small.load()
    out = []
    for y in range(rows):
        line = ""
        for x in range(cols):
            v = px[x, y]
            line += CHARS[min(len(CHARS) - 1, v * len(CHARS) // 256)]
        out.append(line)
    return f"[{w}x{h} -> {cols}x{rows}]\n" + "\n".join(out)


def pixel(path: str, x: int, y: int) -> None:
    from PIL import Image

    img = Image.open(path).convert("RGB")
    print(f"pixel({x},{y}) = {img.getpixel((x, y))}")


def main() -> None:
    args = sys.argv[1:]
    save_to = None
    cols = 100
    pixel_at = None
    if "--save" in args:
        i = args.index("--save")
        save_to = args[i + 1]
        del args[i : i + 2]
    if "--cols" in args:
        i = args.index("--cols")
        cols = int(args[i + 1])
        del args[i : i + 2]
    if "--pixel" in args:
        i = args.index("--pixel")
        pixel_at = (int(args[i + 1]), int(args[i + 2]))
        del args[i : i + 3]
    window_id = None
    if args and args[0] != "--full":
        window_id = args[0]
    path = capture(window_id, save_to)
    if pixel_at:
        pixel(path, *pixel_at)
    else:
        print(render(path, cols))
    os.unlink(path)


if __name__ == "__main__":
    main()
