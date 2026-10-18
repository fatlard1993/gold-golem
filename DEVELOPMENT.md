# Development

## Building

```
./gradlew build
```

Compiles against Pandorical's working tree as a subproject.

## Installing

Server-side. Drop the jar in the server's `mods/` alongside Pandorical 15.21 or later, which is the version that lets a mod add an entity on a vanilla one's base. Players need Pandorical client-side to see the golem in gold; without it they are shown an iron golem.

## How it fits together

- `GoldGolem` extends `IronGolem` and adds no synced data, which is what lets a Pandorical client run the iron golem's class in its place and a vanilla client be sent a plain iron golem. Gilt lives on the server only, is saved as `Gilt`, and comes back one every `GILT_REGEN_TICKS`.
- `TendWoundedGoal` walks it to the nearest hurt ally out of reach of its pulse; the pulse itself is in `aiStep`, so it mends whoever is near whether or not it walked to them.
- `CarvedPumpkinBlockMixin` checks the gold T before the game checks its own golems, and tells dispensers a gold T is worth a pumpkin.
- `PiglinAiMixin` widens the one check piglins make before targeting a player, `isWearingSafeArmor`, to include standing near a gold golem.

## The art

```
python3 generate_textures.py
```

Writes the golem's body (`pandorical/entity_models/gold_golem.json`) and paints its skin (`textures/entity/gold_golem.png`) from one table of boxes, so a box cannot move on the sheet without its paint moving with it; then paints the spawn egg and the icon (both copies) from the same paint. Needs Pillow, and the cached Minecraft jar from one build.

The parts carry the iron golem's names - `head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg` - because `IronGolemModel` finds them by name and swings them; a body missing one is refused and the golem is drawn as a plain iron golem. The pivots are this body's own: the arms swing from the shoulder, not from the middle of the chest as the iron golem's do. The gold is painted smooth, the way polished gold is: broad bands on each plate from lit to shaded and a short glint on the big faces, with no grain. The body is drawn at the iron golem's height and shrunk by `GoldGolem.SCALE`, so the hitbox and the drawing agree.

Every box is placed where the iron golem's crack textures are empty: `IronGolemRenderer` lays them over whatever model it draws, at the iron golem's places on the sheet. That is why the golem never cracks, and why a box that will not fit says "no room clear of the cracks" rather than overlapping one.

`GoldGolemPictured` photographs a gold golem beside an iron one into `build/run/clientGameTest/screenshots/` (and `screenshots/` with `-Ppictures`): the way to see a change to the body.

## Tests

```
./gradlew runClientGameTest -Ptest.level=full
```

Runs the smoke test, `GoldGolemPictured`, which also checks the client draws the golem from its own body rather than the iron golem's, and `GoldGolemBehaviour`, which lays a floor in the sky and holds the golem to the readme: a gold T builds one and an iron T still builds an iron golem; it mends a hurt villager for one gilt, and mends nobody with none; nuggets and carrots feed it, an ingot repairs it, and its gilt comes back by itself; a player beside one is safe from piglins and one far away is not; and it is drawn by the iron golem's renderer, and sent to a vanilla client as an iron golem.

On a machine with no display, `xvfb-run -a -s "-screen 0 1920x1080x24"` in front of it works: the game falls back from OpenGL to Vulkan on llvmpipe.

## What to check by hand

- **It looks right walking and swinging**, not only standing: `/summon gold-golem-justfatlard:gold_golem` and hit it.
- **Piglins in a bastion** leave you alone with a golem walked in on a lead, and turn on you when you open a chest.
