#!/usr/bin/env python3
"""Generates every texture the mod ships, so no binary art needs to live in Git.

The output is plain RGBA PNG written by hand (zlib + struct), which keeps the repository free of
image tooling dependencies. Re-run after changing a colour scheme::

    python3 tools/generate_textures.py

Textures produced:
  * entity/void_titan.png and entity/void_titan_rage.png  - boss UV sheets
  * item/void_titan_spawn_egg.png                          - spawn egg icon
  * gui/ability/*.png                                      - ability icons
  * animeki_logo.png                                       - mod logo
"""
from __future__ import annotations

import os
import struct
import zlib

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "src", "main", "resources")


def write_png(path: str, width: int, height: int, pixels: list[list[tuple[int, int, int, int]]]) -> None:
    raw = bytearray()
    for row in pixels:
        raw.append(0)  # filter: none
        for r, g, b, a in row:
            raw += bytes((r, g, b, a))

    def chunk(tag: bytes, data: bytes) -> bytes:
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    payload = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header)
               + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(payload)


def blank(width: int, height: int, colour=(0, 0, 0, 0)) -> list[list[tuple[int, int, int, int]]]:
    return [[colour for _ in range(width)] for _ in range(height)]


def fill(pixels, x0, y0, w, h, colour) -> None:
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if 0 <= y < len(pixels) and 0 <= x < len(pixels[0]):
                pixels[y][x] = colour


def shade(colour, factor: float):
    r, g, b, a = colour
    return (min(255, int(r * factor)), min(255, int(g * factor)), min(255, int(b * factor)), a)


def noise_fill(pixels, x0, y0, w, h, colour, seed=1, amount=0.12) -> None:
    state = seed
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            state = (state * 1103515245 + 12345) & 0x7FFFFFFF
            delta = 1.0 + (((state >> 16) % 100) / 100.0 - 0.5) * amount
            pixels[y][x] = shade(colour, delta)


def titan_sheet(rage: bool) -> list[list[tuple[int, int, int, int]]]:
    """64x64 humanoid UV sheet matching VoidTitanModel's box layout."""
    pixels = blank(64, 64, (0, 0, 0, 0))
    armour = (26, 14, 46, 255) if not rage else (52, 10, 18, 255)
    trim = (108, 60, 208, 255) if not rage else (255, 92, 92, 255)
    glow = (190, 140, 255, 255) if not rage else (255, 170, 120, 255)

    # Head (10x10) + crown spike area.
    noise_fill(pixels, 0, 0, 10, 10, armour, seed=3)
    fill(pixels, 2, 3, 3, 2, glow)
    fill(pixels, 6, 3, 3, 2, glow)
    fill(pixels, 0, 0, 10, 1, trim)
    # Hat/helmet overlay (10x10) - broken up so it reads as plating.
    noise_fill(pixels, 32, 0, 10, 10, shade(armour, 1.25), seed=7)
    for i in range(0, 10, 3):
        fill(pixels, 32, i, 10, 1, trim)

    # Body (14x14) with a shoulder yoke drawn in the extra 16x4 strip below it.
    noise_fill(pixels, 16, 16, 14, 14, armour, seed=11)
    fill(pixels, 16, 16, 14, 2, trim)
    fill(pixels, 21, 20, 4, 6, glow)
    for y in range(24, 30, 2):
        fill(pixels, 16, y, 14, 1, shade(trim, 0.8))
    noise_fill(pixels, 16, 30, 16, 4, shade(armour, 1.2), seed=13)

    # Arms (5x16 each) at 40,16 mirrored.
    noise_fill(pixels, 40, 16, 5, 16, shade(armour, 0.9), seed=17)
    fill(pixels, 40, 16, 5, 2, trim)
    fill(pixels, 41, 26, 3, 3, glow)

    # Legs (6x18 each) at 0,16 mirrored.
    noise_fill(pixels, 0, 16, 6, 18, shade(armour, 0.85), seed=19)
    fill(pixels, 0, 16, 6, 2, trim)
    fill(pixels, 0, 32, 6, 2, shade(trim, 0.7))
    return pixels


def spawn_egg() -> list[list[tuple[int, int, int, int]]]:
    pixels = blank(16, 16, (0, 0, 0, 0))
    base = (20, 10, 40, 255)
    spot = (154, 107, 255, 255)
    for y in range(3, 15):
        spread = 2 if y < 6 else 4
        for x in range(8 - spread, 8 + spread):
            pixels[y][x] = base
    # specks
    for (x, y) in ((6, 6), (9, 7), (7, 10), (10, 11), (8, 13), (5, 9), (11, 8)):
        pixels[y][x] = spot
    for x in range(5, 11):
        pixels[4][x] = spot
    return pixels


def ability_icon(kind: str) -> list[list[tuple[int, int, int, int]]]:
    pixels = blank(16, 16, (0, 0, 0, 0))
    border = (16, 16, 22, 230)
    fill(pixels, 0, 0, 16, 16, border)
    fill(pixels, 1, 1, 14, 14, (28, 30, 40, 230))
    if kind == "ring":
        for y in range(2, 14):
            for x in range(2, 14):
                d = ((x - 8) ** 2 + (y - 8) ** 2) ** 0.5
                if 3.4 < d < 5.4:
                    pixels[y][x] = (120, 220, 255, 255)
        fill(pixels, 7, 3, 2, 10, (200, 240, 255, 255))
    elif kind == "burst":
        for i in range(16):
            pixels[7][i] = (255, 220, 130, 255)
            pixels[8][i] = (255, 220, 130, 255)
            pixels[i][7] = (255, 240, 180, 255)
            pixels[i][8] = (255, 240, 180, 255)
        fill(pixels, 6, 6, 4, 4, (255, 255, 255, 255))
    elif kind == "sphere":
        for y in range(2, 14):
            for x in range(2, 14):
                d = ((x - 8) ** 2 + (y - 8) ** 2) ** 0.5
                if d < 4.0:
                    pixels[y][x] = (255, 250, 210, 255) if d < 2.0 else (255, 205, 90, 255)
    elif kind == "arrow":
        for i in range(12):
            pixels[12 - i][4 + i // 2] = (150, 235, 255, 255)
            pixels[12 - i][11 - i // 2] = (150, 235, 255, 255)
        fill(pixels, 3, 11, 10, 2, (200, 245, 255, 255))
    elif kind == "fade":
        for y in range(3, 13):
            for x in range(3, 13):
                pixels[y][x] = (200, 190, 255, 120)
        fill(pixels, 4, 4, 8, 8, (60, 40, 90, 180))
    elif kind == "down":
        for i in range(9):
            fill(pixels, 5 + i // 3, 3 + i, 6 - 2 * (i // 3), 1, (255, 190, 120, 255))
        fill(pixels, 6, 12, 4, 2, (255, 230, 190, 255))
    elif kind == "up":
        for i in range(9):
            fill(pixels, 4 + i // 4, 12 - i, 8 - 2 * (i // 4), 1, (255, 120, 120, 255))
    elif kind == "star":
        fill(pixels, 7, 2, 2, 12, (255, 255, 255, 255))
        fill(pixels, 2, 7, 12, 2, (255, 255, 255, 255))
        fill(pixels, 4, 4, 8, 8, (255, 240, 200, 200))
    else:
        fill(pixels, 4, 4, 8, 8, (255, 255, 255, 255))
    return pixels


def logo() -> list[list[tuple[int, int, int, int]]]:
    size = 128
    pixels = blank(size, size, (10, 8, 20, 255))
    for y in range(size):
        for x in range(size):
            dx = x - size / 2
            dy = y - size / 2
            d = (dx * dx + dy * dy) ** 0.5
            if d < 34:
                factor = 1.0 - d / 34
                pixels[y][x] = (int(200 + 55 * factor), int(140 + 90 * factor), 255, 255)
            elif d < 46:
                pixels[y][x] = (60, 30, 110, 255)
            if 48 < d < 52 and (x + y) % 7 < 3:
                pixels[y][x] = (255, 220, 120, 255)
    return pixels


def main() -> None:
    write_png(os.path.join(ROOT, "assets/animeki/textures/entity/void_titan.png"), 64, 64, titan_sheet(False))
    write_png(os.path.join(ROOT, "assets/animeki/textures/entity/void_titan_rage.png"), 64, 64, titan_sheet(True))
    write_png(os.path.join(ROOT, "assets/animeki/textures/item/void_titan_spawn_egg.png"), 16, 16, spawn_egg())
    icons = {
        "ki_blast": "sphere", "charged_ki_blast": "burst", "energy_beam": "beam",
        "power_clash": "ring", "dash_strike": "arrow", "vanish": "fade",
        "ground_slam": "down", "meteor_strike": "up", "ultimate_sphere": "sphere",
    }
    for name, kind in icons.items():
        write_png(os.path.join(ROOT, "assets/animeki/textures/gui/ability", name + ".png"), 16, 16,
                  ability_icon(kind))
    write_png(os.path.join(ROOT, "animeki_logo.png"), 128, 128, logo())
    print("textures generated")


if __name__ == "__main__":
    main()
