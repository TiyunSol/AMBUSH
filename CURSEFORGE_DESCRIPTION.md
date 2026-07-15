# Ambush

Ambush is a data-driven NeoForge encounter framework for Minecraft 1.21.1. It creates hostile events for individual players without global selectors, chunk generation, KubeJS, or hard-coded pack scripts.

Pack authors define encounters in datapack JSON files. Encounters can spawn ordinary mobs, passengers, equipment, effects, sounds, delayed waves, projectile rain, conditional reinforcements, and fully assembled Sable aircraft.

## Features

- Per-player ownership, targeting, cooldowns, and cleanup.
- Compact and expanded JSON definition formats.
- Interval, portal, active-block, and kill-count triggers.
- Height, biome, dimension, time, weather, ocean, and line-of-sight conditions.
- Land, air, and water entity placement.
- Recursive passengers, equipment, potion effects, sounds, and persistent mobs.
- Sable aircraft assembled through Aeronautics Discovery's supported command pipeline.
- Player-facing, cardinal, fleet, surrounding, and tangent/orbit spawn orientations.
- Seated pillager crews with moving-sublevel-aware crossbow aiming.
- Balloon fill, engine burn time, Y-dependent throttle, container loot, and fixed child cargo.
- Restart-safe cooldowns, delayed modern actions, aircraft lifetimes, and damage-threshold cleanup.
- Data-defined cleanup effects and complete parent/child/split-fragment removal.
- Optional Create Big Cannons shell rain with timed fuzes and safe targeting rings.

## Bundled encounters

The release includes four aircraft schematics and balloon encounters for single ships and three-ship formations. Balloon encounters have low automatic chances. All non-balloon examples and the exhaustive master reference have automatic chance zero, but operators can invoke them manually.

The master example documents every supported action and major alternative. It is a stress test intended for disposable test worlds.

## Data packs and commands

Definitions are loaded from:

```text
data/<namespace>/ambushes/<id>.json
```

Structure templates are loaded from:

```text
data/<namespace>/structure/<path>.nbt
```

Permission-level-2 commands:

```text
/ambush list
/ambush validate
/ambush state
/ambush <id>
/ambush <id> <player>
```

Definitions supplied by a world datapack can be reloaded with `/reload`; rebuilding Ambush is not required.

## Core requirements

- Minecraft 1.21.1
- NeoForge 21.1 or newer

## Optional integrations

- Create 6.0.10
- Create Aeronautics/Simulated 1.3.0
- Sable 2.0.3
- Create Aeronautics Discovery 1.4.4
- Create Big Cannons 5.11.7
- Ritchie's Projectile Library 2.1.2, required transitively by CBC

Generic entity, passenger, equipment, effect, sound, and vanilla rain definitions work without any of these optional mods. Sable aircraft actions fail closed unless the compatible Sable/Discovery stack is loaded. CBC shell actions skip when CBC is absent. Datapacks should reference only content available in their installed mod set.

When using physics aircraft, install the selected integration stack on the server and connecting clients. Back up worlds before testing aircraft or large encounter definitions.

## Compatibility and limits

Ambush does not provide automatic orbital steering; orbit modes set only the aircraft's initial tangent. Sable and Aeronautics physics determine its later path.

The runtime does not generate chunks. Aircraft placement requires already-loaded clear space, and assembly may safely skip when no valid location exists.

Legacy static structure/platform actions have no Sable lifecycle and should not be used for production aircraft. Use `sable_structure` or `sable_formation`.

This is an independent Create Aeronautics compatibility mod and is not affiliated with or endorsed by the Create, Sable, Aeronautics, or Aeronautics Discovery teams.
