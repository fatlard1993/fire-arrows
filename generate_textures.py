#!/usr/bin/env python3
"""
Draw the arrows, out of the game's own arrow.

Each is vanilla's arrow with its head changed and nothing else: the shaft, the fletching and the
angle are the arrow a player already knows, so the three read as arrows first and as what they
carry second. The head is the grey pixels at the top right of the arrow; only those are touched.

  torch_arrow      the head is a torch's flame
  fire_arrow       the head is a fire charge's embers, glowing at the tip
  explosive_arrow  a block of TNT, five pixels square, where the head was

The icon is the explosive arrow, eight times over.

Usage: python3 generate_textures.py [path/to/minecraft-merged-deobf-<version>.jar]
"""
import glob
import io
import os
import pathlib
import sys
import zipfile

from PIL import Image

HERE = pathlib.Path(__file__).parent
ASSETS = HERE / "src/main/resources/assets/fire-arrows-justfatlard"
OUT = ASSETS / "textures/item"


def find_jar(argv):
    if len(argv) > 1:
        return argv[1]
    version = None
    for line in (HERE / "gradle.properties").read_text().splitlines():
        key, _, value = line.partition("=")
        if key.strip() == "minecraft_version":
            version = value.strip()
    pattern = os.path.expanduser(
        "~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/"
        f"{version}/minecraft-merged-deobf-{version}.jar")
    found = glob.glob(pattern)
    if not found:
        sys.exit(f"no Minecraft {version} jar under the Loom cache; build once, or pass its path")
    return found[0]


def texture(jar, path):
    with zipfile.ZipFile(jar) as z:
        return Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def head_pixels(arrow):
    """The arrowhead: grey pixels in the top-right corner, where the tip points."""
    out = []
    for y in range(8):
        for x in range(8, 16):
            r, g, b, a = arrow.getpixel((x, y))
            if a and max(r, g, b) - min(r, g, b) < 24:
                out.append((x, y, (r + g + b) / 3))
    return out


def recolour_head(arrow, ramp):
    """Paint the head through a dark-to-light ramp, by how light each grey pixel was."""
    out = arrow.copy()
    pixels = head_pixels(arrow)
    lo = min(v for _, _, v in pixels)
    hi = max(v for _, _, v in pixels)
    for x, y, v in pixels:
        t = 0 if hi == lo else (v - lo) / (hi - lo)
        out.putpixel((x, y), ramp[min(len(ramp) - 1, int(t * len(ramp)))] + (255,))
    return out


def with_tnt(arrow, tnt):
    """The head cleared, and a five-pixel TNT block in its place, in TNT's own colours."""
    out = arrow.copy()
    for x, y, _ in head_pixels(arrow):
        out.putpixel((x, y), (0, 0, 0, 0))
    red, dark_red = tnt.getpixel((1, 1)), tnt.getpixel((2, 1))
    white, black = tnt.getpixel((1, 8)), tnt.getpixel((3, 7))
    block = [
        [dark_red, red, dark_red, red, dark_red],
        [red, dark_red, red, dark_red, red],
        [white, black, white, black, white],
        [red, dark_red, red, dark_red, red],
        [dark_red, red, dark_red, red, dark_red],
    ]
    for row, colours in enumerate(block):
        for col, colour in enumerate(colours):
            out.putpixel((10 + col, 1 + row), colour[:3] + (255,))
    return out


def main():
    jar = find_jar(sys.argv)
    arrow = texture(jar, "item/arrow")
    tnt = texture(jar, "block/tnt_side")

    # A torch's flame: bright and yellow all through, where a fire charge's embers are dark.
    flame = [(0xC8, 0x6E, 0x00), (0xFF, 0xA8, 0x00), (0xFF, 0xD8, 0x00), (0xFF, 0xF6, 0x9E)]
    embers = [(0x3A, 0x16, 0x08), (0x8A, 0x2A, 0x06), (0xD8, 0x5A, 0x0E), (0xF8, 0x9E, 0x1E),
              (0xFF, 0xD8, 0x5A)]

    OUT.mkdir(parents=True, exist_ok=True)
    recolour_head(arrow, flame).save(OUT / "torch_arrow.png")
    recolour_head(arrow, embers).save(OUT / "fire_arrow.png")
    explosive = with_tnt(arrow, tnt)
    explosive.save(OUT / "explosive_arrow.png")
    explosive.resize((128, 128), Image.NEAREST).save(ASSETS / "icon.png")
    print(f"  3 arrows -> {OUT.relative_to(HERE)}, icon -> {(ASSETS / 'icon.png').relative_to(HERE)}")


if __name__ == "__main__":
    main()
