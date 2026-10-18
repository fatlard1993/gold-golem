#!/usr/bin/env python3
"""
The gold golem's body and skin, its spawn egg and its mod menu icon: one description of the golem,
written out as the model Pandorical builds and the sheet it is painted on, so the two can never
disagree.

The golem is a list of parts and the cubes on them, in the game's model units (sixteen to a block,
y down, the feet at 24), written where each cube sits on the standing golem rather than relative to
its part, so they read as the golem they draw. The script works out each cube's place on the sheet,
writes the model JSON with those places in it, and then paints every face of every cube by asking
what that spot on the golem is: the brow, a band on the arm, the bell on its head. Moving a cube
moves its paint with it.

The parts are the iron golem's by name, because the iron golem's model class is what animates them:
it finds head, both arms and both legs by name, turns the head where the golem looks, and swings
the arms and legs about their pivots as it walks and strikes. The shape is this golem's own,
between the iron golem and the copper golem. It is drawn at the iron golem's full height and shrunk
by the golem's scale attribute, so what a player sees and what they can hit are the same size.

Two things are kept off the sheet on purpose. The iron golem's renderer lays its crack textures over
whatever model it draws, at the iron golem's places on the sheet; every cube here is placed where
those textures are empty, or the cracks would land as stray marks on the wrong parts. And the
flower an iron golem holds out is never drawn, because a gold golem never offers one.

Usage: python3 generate_textures.py
"""

import io
import json
import re
import zipfile
from dataclasses import dataclass, field
from pathlib import Path

from PIL import Image

HERE = Path(__file__).parent
MOD_ID = "gold-golem-justfatlard"
ASSETS = HERE / "src/main/resources/assets" / MOD_ID
SKIN = ASSETS / "textures/entity/gold_golem.png"
MODEL = ASSETS / "pandorical/entity_models/gold_golem.json"
EGG = ASSETS / "textures/item/gold_golem_spawn_egg.png"
ICON = ASSETS / "icon.png"


def minecraft_jar():
    """The cached jar for the version this mod targets, which is where the crack textures, the egg's
    outline and the icon's gold block come from."""
    version = None
    for line in (HERE / "gradle.properties").read_text().splitlines():
        key, sep, value = line.partition("=")
        if sep and key.strip() == "minecraft_version":
            version = value.strip()
    jar = Path.home() / f".gradle/caches/fabric-loom/{version}/minecraft-merged.jar"
    if not jar.exists():
        raise SystemExit(f"no cached Minecraft jar at {jar}: build the mod once first")
    return jar


def vanilla(path):
    with zipfile.ZipFile(minecraft_jar()) as jar:
        image = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/" + path))).convert("RGBA")
        image.load()
        return image


SHEET = (128, 128)


@dataclass
class Cube:
    role: str
    origin: tuple   # where its least corner is on the standing golem
    size: tuple
    # The cube on the other side this one copies: same place on the sheet, drawn mirrored.
    twin: "Cube | None" = None
    uv: tuple = None

    @property
    def lo(self):
        return self.origin

    @property
    def hi(self):
        return tuple(o + s for o, s in zip(self.origin, self.size))


@dataclass
class Part:
    name: str
    pivot: tuple
    cubes: list = field(default_factory=list)


def mirrored(cube: Cube) -> Cube:
    """The same cube on the golem's other side, sharing its paint."""
    (x, y, z), (w, h, d) = cube.origin, cube.size
    return Cube(cube.role, (-x - w, y, z), cube.size, twin=cube)


def pair(name, pivot, cubes):
    """Right and left from the right-hand one: the left mirrors it. Right is -x, as the game has it."""
    return [Part("right_" + name, pivot, cubes),
            Part("left_" + name, (-pivot[0], pivot[1], pivot[2]), [mirrored(c) for c in cubes])]


# --- The golem -------------------------------------------------------------------------------
#
# Between the two golems the game already has. From the iron golem: the nose, the heavy brow over
# deep-set red eyes, arms that hang past the hips, and a body broad enough to look like it means it.
# From the copper golem: a head wider than it is tall, set forward, a body no taller than the legs
# are long, and something of its block on top - the copper golem wears a lightning rod; this one
# wears a bell, because a note block over gold plays the bell. It stands about two thirds of an iron
# golem's height once its scale attribute is applied.

PARTS = [
    Part("head", (0, -3, -1), [
        Cube("head", (-6, -12, -7), (12, 9, 10)),
        Cube("nose", (-1, -7, -9), (2, 4, 2)),
        # The game's own bell, smaller: a body six across over a rim two wider.
        Cube("bell_lip", (-4, -14, -6), (8, 2, 8)),
        Cube("bell", (-3, -19, -5), (6, 5, 6)),
    ]),
    Part("body", (0, -3, 0), [
        Cube("torso", (-8, -3, -5), (16, 12, 10)),
        Cube("waist", (-6, 9, -4), (12, 2, 8)),
    ]),
    *pair("arm", (-9.5, -2, 0), [Cube("arm", (-13, -3, -3), (5, 18, 6))]),
    *pair("leg", (-4.5, 11, 0), [Cube("leg", (-8, 11, -3.5), (7, 13, 7))]),
]


# --- The paint -------------------------------------------------------------------------------
#
# Polished, the way gold is: no grain, each plate in a few broad flat bands, lit at the top and
# darkening toward its lower edge, with a short diagonal glint across the upper corner of the
# larger faces, the way the gold ingot catches the light. Seams and bands in a darker tone where the
# copper golem has its rivets and plate lines, and the game's face shading does the rest. The gold
# is kept a little muted, nearer an ingot in shadow than the gold block's glare, because the game's
# golems are all low tones beside the blocks they are named for. Every texel is asked for by where
# its middle is on the standing golem.

def rgb(hex_):
    return tuple(int(hex_[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


GOLD = {
    "shine": rgb("#f7e07c"),
    "light": rgb("#eccb55"),
    "base": rgb("#dcb342"),
    "mid": rgb("#c99b35"),
    "shade": rgb("#a8782a"),
    "dark": rgb("#7d5520"),
    "deep": rgb("#4f3414"),
    # The iron golem's eyes, so the two read as kin.
    "eye": (141, 0, 42, 255),
    "eye_dark": (30, 0, 14, 255),
}

def near(value, edge, within=1):
    return abs(value - edge) < within


def band(y, at):
    return near(y, at, 0.6)


def plate(cube, face, x, y, z):
    """Plain polished gold: bands down the face from lit to shaded, and a glint on a big face."""
    p = GOLD
    (x0, y0, z0), (x1, y1, z1) = cube.lo, cube.hi
    if face == "down":
        return p["shade"]
    if face == "up":
        return p["light"]
    if near(y, y1):
        return p["dark"]
    across, lo, hi = (x, x0, x1) if face in ("north", "south") else (z, z0, z1)
    down = y - y0
    # Two texels down from the top, three along, falling left to right: only on faces big enough
    # to hold it, and only the faces turned toward the light.
    if face in ("north", "west") and hi - lo >= 5 and y1 - y0 >= 6:
        if 1 <= down < 4 and near(across - lo - down, 1, 0.6):
            return p["shine"]
    share = down / (y1 - y0)
    if share < 0.15:
        return p["light"]
    if share < 0.6:
        return p["base"]
    return p["mid"]


def paint_golem(cube, face, x, y, z):
    p = GOLD
    role = cube.role
    sides = face in ("north", "south", "east", "west")
    if role == "head" and face == "north":
        # The iron golem's brow and eyes: a heavy dark brow straight across, two deep sockets under
        # it with the red eye toward the nose.
        if band(y, -9.5) and abs(x) < 5:
            return p["dark"]
        if band(y, -8.5) and 1.5 < abs(x) < 4:
            return p["eye"] if abs(x) < 3 else p["eye_dark"]
    if role == "head" and sides and band(y, -4.5):
        return p["shade"]
    if role == "nose":
        if face == "north":
            return p["dark"] if near(y, cube.hi[1]) else p["light"]
        return p["shade"]
    if role.startswith("bell"):
        if face == "down":
            return p["deep"]
        if role == "bell_lip":
            return p["shade"] if sides and near(y, cube.hi[1]) else p["mid"]
        if face == "up" or near(y, cube.lo[1]):
            return p["shine"]
        if band(y, cube.hi[1] - 1.5):
            return p["shade"]
        return p["light"] if face in ("north", "west") else p["base"]
    if role == "torso" and sides:
        # A band across the chest where the copper golem has its plate line, a rivet at each end
        # of it, and a rivet over each of those on the front.
        front = face in ("north", "south")
        across = x if front else z
        lo, hi = (cube.lo[0], cube.hi[0]) if front else (cube.lo[2], cube.hi[2])
        rivet = near(across, lo + 1.5, 0.6) or near(across, hi - 1.5, 0.6)
        if band(y, 2.5):
            return p["deep"] if rivet else p["shade"]
        if face == "north" and band(y, -1.5) and rivet:
            return p["deep"]
    if role == "waist" and sides:
        return p["deep"] if int(x + z + 20) % 3 == 0 else p["dark"]
    if role == "arm" and sides:
        if band(y, 5.5) or band(y, 11.5):
            return p["shade"]
        if y > 12:
            # The hand, a shade darker, with the gaps between the fingers on the front.
            if near(y, cube.hi[1]):
                return p["dark"]
            if face == "north" and y > 13 and int(x + 20) % 2 == 0:
                return p["dark"]
            return p["mid"]
    if role == "leg" and sides:
        if band(y, 17.5):
            return p["shade"]
        if y > 21:
            return p["dark"] if near(y, cube.hi[1]) else p["mid"]
    return plate(cube, face, x, y, z)


# --- The sheet -------------------------------------------------------------------------------

def crack_mask():
    """Where on its sheet the iron golem's crack textures have anything to draw: the places a cube
    here must not use, since the cracks are laid over this model at the same places."""
    cracks = [vanilla(f"entity/iron_golem/iron_golem_crackiness_{level}.png") for level in ("low", "medium", "high")]
    width, height = SHEET
    return [[any(c.getpixel((u, v))[3] for c in cracks) for u in range(width)] for v in range(height)]


def footprint(cube):
    """A cube's rectangle on the sheet, in the game's box layout: two rows, the top and bottom
    above the four sides."""
    w, h, d = cube.size
    return int(2 * (d + w)), int(d + h)


def place(parts):
    """Each cube that has paint of its own, given a free spot on the sheet clear of the cracks,
    biggest first."""
    width, height = SHEET
    taken = crack_mask()
    own = [c for part in parts for c in part.cubes if c.twin is None]
    for cube in sorted(own, key=lambda c: (-footprint(c)[1], -footprint(c)[0])):
        fw, fh = footprint(cube)
        spot = None
        for v in range(height - fh + 1):
            for u in range(width - fw + 1):
                if all(not taken[v + j][u + i] for j in range(fh) for i in range(fw)):
                    spot = (u, v)
                    break
            if spot:
                break
        if spot is None:
            raise SystemExit(f"no room clear of the cracks for {cube.role} {cube.size}")
        for j in range(fh):
            for i in range(fw):
                taken[spot[1] + j][spot[0] + i] = True
        cube.uv = spot
    for part in parts:
        for c in part.cubes:
            if c.twin is not None:
                c.uv = c.twin.uv


def faces(cube):
    """Every texel of a cube's faces: where on the sheet, which face, and where its middle is on the
    standing golem. The layout is the game's own: in the top row the top then the bottom, in the row
    under it the golem's right side, its front, its left side and its back."""
    u, v = cube.uv
    w, h, d = (int(s) for s in cube.size)
    (x0, y0, z0), (x1, y1, z1) = cube.lo, cube.hi
    for i in range(w):
        for j in range(d):
            yield (u + d + i, v + j), "up", (x0 + i + .5, y0, z1 - j - .5)
            yield (u + d + w + i, v + j), "down", (x0 + i + .5, y1, z1 - j - .5)
    for k in range(h):
        y = y0 + k + .5
        for i in range(d):
            yield (u + i, v + d + k), "west", (x0, y, z1 - i - .5)
            yield (u + d + w + i, v + d + k), "east", (x1, y, z0 + i + .5)
        for i in range(w):
            yield (u + d + i, v + d + k), "north", (x0 + i + .5, y, z0)
            yield (u + 2 * d + w + i, v + d + k), "south", (x1 - i - .5, y, z1)


def paint(parts, painter):
    sheet = Image.new("RGBA", SHEET, (0, 0, 0, 0))
    pixels = sheet.load()
    for part in parts:
        for cube in part.cubes:
            if cube.twin is not None:
                continue
            for (px, py), face, (x, y, z) in faces(cube):
                pixels[px, py] = painter(cube, face, x, y, z)
    return sheet


def model(parts):
    """The model as Pandorical reads it: each cube relative to its part's pivot."""
    out = {}
    for part in parts:
        px, py, pz = part.pivot
        cubes = []
        for c in part.cubes:
            x, y, z = c.origin
            cube = {"origin": [x - px, y - py, z - pz], "size": list(c.size), "uv": list(c.uv)}
            if c.twin is not None:
                cube["mirror"] = True
            cubes.append(cube)
        out[part.name] = {"pivot": list(part.pivot), "cubes": cubes}
    return {
        "texture": f"{MOD_ID}:textures/entity/gold_golem.png",
        "texture_size": list(SHEET),
        "parts": out,
    }


def tidy(value):
    if isinstance(value, dict):
        return {k: tidy(v) for k, v in value.items()}
    if isinstance(value, list):
        return [tidy(v) for v in value]
    if isinstance(value, float) and value.is_integer():
        return int(value)
    return value


def compact(data):
    """JSON a person can read: one line per cube."""
    text = json.dumps(tidy(data), indent="\t")
    text = re.sub(r"\[\s+([-\d.,\s]+?)\s+\]", lambda m: "[" + ", ".join(m.group(1).split()).replace(",,", ",") + "]", text)
    text = re.sub(r"\{\s+(\"origin\"[^{}]*?)\s+\}", lambda m: "{" + " ".join(m.group(1).split()) + "}", text)
    return text + "\n"


# --- The egg and the icon --------------------------------------------------------------------

def egg():
    """The spawn egg's outline, from the iron golem's egg, filled with gold lit from the top left,
    and this golem's face on it: the bell on top, the brow, the red eyes and the nose."""
    shape = vanilla("item/iron_golem_spawn_egg.png")
    filled = {(x, y) for y in range(shape.height) for x in range(shape.width) if shape.getpixel((x, y))[3]}
    out = Image.new("RGBA", shape.size, (0, 0, 0, 0))
    p = GOLD
    for x, y in filled:
        outline = any((x + dx, y + dy) not in filled for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        lit = x + y
        if outline:
            colour = p["dark"] if lit > 13 else p["shade"]
        elif lit < 9:
            colour = p["shine"] if lit < 7 else p["light"]
        elif lit > 19:
            colour = p["mid"]
        else:
            colour = p["base"]
        out.putpixel((x, y), colour)
    marks = {
        (7, 1): "shade", (8, 1): "shade",
        (6, 2): "shade", (7, 2): "light", (8, 2): "mid", (9, 2): "shade",
        (5, 3): "dark", (6, 3): "dark", (7, 3): "dark", (8, 3): "dark", (9, 3): "dark", (10, 3): "dark",
        (4, 6): "dark", (5, 6): "dark", (6, 6): "dark", (7, 6): "dark", (8, 6): "dark", (9, 6): "dark", (10, 6): "dark", (11, 6): "dark",
        (5, 7): "eye_dark", (6, 7): "eye", (9, 7): "eye", (10, 7): "eye_dark",
        (7, 7): "light", (8, 7): "light", (7, 8): "light", (8, 8): "light", (7, 9): "dark", (8, 9): "dark",
    }
    for (x, y), tone in marks.items():
        out.putpixel((x, y), p[tone])
    return out


def front_view(parts, painter):
    """The golem seen square from the front, standing still: every cube's front face, the
    furthest drawn first. One pixel to a texel."""
    cubes = [c for part in parts for c in part.cubes]
    left = min(c.lo[0] for c in cubes)
    top = min(c.lo[1] for c in cubes)
    width = int(max(c.hi[0] for c in cubes) - left)
    height = int(max(c.hi[1] for c in cubes) - top)
    view = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for cube in sorted(cubes, key=lambda c: -c.lo[2]):
        source = cube.twin or cube
        w, h = int(cube.size[0]), int(cube.size[1])
        for i in range(w):
            for k in range(h):
                # A mirrored cube shows its twin's paint flipped, as the game draws it.
                si = w - 1 - i if cube.twin else i
                x = source.lo[0] + si + .5
                y = cube.lo[1] + k + .5
                colour = painter(source, "north", x, y, source.lo[2])
                view.putpixel((int(cube.lo[0] - left + i), int(cube.lo[1] - top + k)), colour)
    return view


def icon(parts, painter):
    """The golem from the front on the gold block, darkened, as a 128 square."""
    figure = front_view(parts, painter)
    scale = max(1, min(120 // figure.width, 120 // figure.height))
    figure = figure.resize((figure.width * scale, figure.height * scale), Image.NEAREST)
    block = vanilla("block/gold_block.png").point(lambda c: c * 3 // 5).resize((128, 128), Image.NEAREST)
    block.putalpha(255)
    block.alpha_composite(figure, ((128 - figure.width) // 2, (128 - figure.height) // 2))
    return block


def main() -> None:
    place(PARTS)
    for path in (SKIN, MODEL, EGG):
        path.parent.mkdir(parents=True, exist_ok=True)
    paint(PARTS, paint_golem).save(SKIN)
    MODEL.write_text(compact(model(PARTS)))
    egg().save(EGG)
    picture = icon(PARTS, paint_golem)
    picture.save(ICON)
    picture.save(HERE / "icon.png")
    print(f"gold_golem: {sum(len(p.cubes) for p in PARTS)} cubes; skin, model, egg and icon written")


if __name__ == "__main__":
    main()
