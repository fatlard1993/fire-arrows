# Fire Arrows - Development Guide

For what the mod is and how it plays, see [README.md](README.md).

## Installation

Install server-side alongside its declared dependencies (see `fabric.mod.json`); connecting clients need only Pandorical. Version targets live in `gradle.properties` (Minecraft, loader, Fabric API) and `fabric.mod.json` (Java).

## Building

`./gradlew build` builds the jar into `build/libs`. Pandorical is compiled as a sibling project (`../pandorical`, see `settings.gradle`).

## How it works

- Each arrow is a `CarryingArrowItem`, an `ArrowItem` in `#minecraft:arrows`, so bows, crossbows and dispensers take it as they take any arrow. What it fires is the game's own `Arrow`, tagged with its `Charge`.
- `AbstractArrowHitMixin` hands each hit to `ArrowHits` before the game handles it. A fire arrow is fired burning and needs nothing more: the game's own handling of a burning arrow lights what it hits.
- A spent torch or fire arrow has its pickup swapped for a plain arrow, through `AbstractArrowAccessor`.

## Art

`python3 generate_textures.py` draws the three item textures and the icon from the game's own arrow and TNT textures, read out of the Minecraft jar in the Loom cache. It needs Pillow.

## Tests

`xvfb-run -a ./gradlew runClientGameTest` starts a client, joins a world and fires each arrow at what it is for: a torch arrow into a wall, a fire arrow into TNT and into a cow, an explosive arrow into a wall. It checks what is left, and that every arrow is ammunition and has its recipe.
