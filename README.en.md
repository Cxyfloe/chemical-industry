# Chemical Industry

> [English](README.en.md) | [中文](README.md)

A chemical engineering addon for [Create](https://github.com/Creators-of-Create/Create), for Minecraft 1.21.1 / NeoForge.

Bring the modern chemical industry into Minecraft: from your first chunk of sulfur ore to a full chemical plant — roasting ore, making acids, electrolysis, refining metals, separating air.

## What's in it

**A complete chemical progression.** Ores are not just for smelting into ingots: sulfur ore becomes sulfuric acid, rock salt electrolyzes into chlorine and caustic soda, bauxite turns into aluminum through acid and base treatment, pyrite gives you iron oxide in a single roast... every material has its place, and the whole chain connects as you play.

**Four main machines:**

- **Fluidized Bed Furnace** — the big boiler: roasting ore, making sulfuric acid, and steelmaking
- **Electrolyzer** — a 3×N multiblock cell that splits metals and gases with power
- **Air Compressor** — squeezes air into compressed air
- **Condenser Pipe** — feed it compressed air and a fan, and it will actually freeze water into ice

**20+ fluids.** Sulfuric acid, hydrochloric acid, nitric acid, salt solutions, liquid mercury — plus gases like chlorine, hydrogen and oxygen. Gases need a canister; watch out for leaking pipes.

**Air separation.** Compressed air goes into a distillation tower (from *Create: Diesel Generators*) and comes out as oxygen, nitrogen and rare gases — the latter being important late-game material.

**A little safety education.** Blocked pipes explode, acid mixed with water explodes, electrolyzing hydrofluoric acid sends you straight back to spawn. It's the chemical industry — there's a price.

**Thermometer.** Glass filled with mercury makes a thermometer whose needle follows the environment: red near lava, blue in the snow.

**Ponder tutorials.** Every machine has a built-in animated tutorial — press W while holding the block.

## Getting started

1. Mine sulfur ore and roast it in the Fluidized Bed to make sulfuric acid — the starting point of everything
2. Use acid and salt to make hydrochloric and nitric acid
3. Build an electrolyzer and extract metals and gases
4. Accumulate metals and upgrade to alloy tools and armor
5. Build an air compressor + distillation tower and separate air
6. Then it's up to you: aluminum, fluorine, precious metals...

## Dependencies

| Mod | Version | Purpose |
|-----|---------|---------|
| Create | 6.0.9+ | Core dependency (kinetics, machinery) |
| Create: Crafts & Additions | 1.5.10 | Power for the electrolyzer |
| Create: Diesel Generators | 1.3.8 | Distillation tower (air separation) |

## Installation

1. Install NeoForge 21.1.80 (for MC 1.21.1)
2. Install the three dependencies above
3. Drop the jar into `.minecraft/mods/`

## About development

This mod was developed via **vibe coding**: I design the gameplay, work out the progression, and test in-game; the AI writes the code, calls APIs, and creates Ponder tutorials. The code may not be textbook, but every mechanic has been verified in actual gameplay.

## Credits

- The [Create](https://github.com/Creators-of-Create/Create) team — this mod builds entirely on their work
- The [Ponder](https://github.com/Creators-of-Create/Ponder) library — the tutorial system
- Everyone willing to give it a try

## License

MIT (code) / CC BY 4.0 (documentation)
