# Ambush 0.1.2

## Framework

- Added validated datapack definitions, per-player ownership, targeting, persistent cooldowns, cooldown groups, chance buildup, counters, and cleanup.
- Added flat, weighted, and failure-buildup chance models with configurable evaluation intervals.
- Added admin-only commands, tab completion, validation, weight inspection, state reporting, debug logging, and forced testing.

## Triggers and placement

- Added interval, portal, active-block, environmental-duration, kill, inventory, trade, weather, altitude, biome, ocean, underwater, surface, and structure triggers.
- Added structure IDs, structure tags, reusable structure groups, and loaded-structure approach tracking.
- Added ground, water, underwater, air, ring, cluster, surround, formation, and out-of-sight placement.

## Waves and projectiles

- Added delayed, interval, death-gated, and chained entity waves with equipment, effects, passengers, ownership, aggression, and flying follow behavior.
- Added vanilla, spectral, tipped, and registry-defined modded arrows.
- Added directional arrows, potions, entities, validated projectiles, and CBC shell rains.
- Added burst schedules, moving sources, activation ranges, launch height, source staggering, spread, exact targeting, safe radii, and ballistic compensation.

## Structures and bosses

- Added guarded Sable assembly without a separate assembly-helper dependency.
- Added mixed-template fleets, persistent state, seated crews, deck spawning, container loot, child structures, and bounded cleanup.
- Added structure-health boss bars, shared fleet health, percentage events, survival events, rewards, redstone, directional detection, steering, and propulsion controls.
- Added proportional steering and player-relative engine/propeller direction maps. Boss ships run forward while the owner is ahead or beside them and reverse only in rear sectors.
- Loading tickets are released during shutdown saving and restored after loading.

## Bundled examples and fixes

- Reduced bundled content to five command-only examples with natural chance zero.
- Added boss, shared-health boss fleet, cannon-balloon fleet, directional arrow rain, and exhaustive master examples.
- Fixed invisible or partially loaded ships, incorrect facing, steering signs, front-facing reverse propulsion, command cooldown interference, missing suggestions, percentage events, fog cleanup, directional sounds, projectile targeting, seated entities, ranged friendly fire, and incomplete-action chaining.
- Bounded health scans, scheduled actions, retries, cleanup, and optional-integration failures to reduce stalls and error loops.
