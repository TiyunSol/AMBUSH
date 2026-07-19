# Ambush — Data-Driven Player Encounters

Ambush is a configurable encounter framework for Minecraft 1.21.1 on NeoForge. It gives modpack and datapack creators a validated way to build hostile events around individual players without writing Java or maintaining server scripts.

An encounter can be two zombies approaching from outside the player's view, a delayed underground assault, an ocean attack, a directional arrow volley, or a moving multi-ship boss fleet with shared block health, crew-gated weapons, fog, sounds, reinforcements, steering, rewards, and complete cleanup.

Definitions are ordinary datapack JSON:

```text
data/<namespace>/ambushes/<id>.json
```

Imported structure templates use:

```text
data/<namespace>/structure/<path>.nbt
```

## Built for modpack authors

Ambush evaluates and owns encounters per player. Cooldowns, chance buildup, counters, scheduled actions, targets, structures, and cleanup are not accidentally shared across the whole server. Named cooldown groups let cave, surface, air, ocean, and structure encounters operate independently or share a deliberate lockout.

Authors control the evaluation interval, flat or weighted chance, chance gained after failed rolls, maximum buildup, and reset behavior. `/ambush weights` explains active rarity values during development. Every bundled example has natural chance zero, so installing the mod does not enable test encounters in normal play.

## Triggers and conditions

Definitions can react to:

- Periodic interval checks.
- Time of day and weather.
- Dimensions, biome IDs, and biome tags.
- Surface, cave, ocean-surface, underwater, and air environments.
- Height ranges and remaining above or below a height for a duration.
- Entering loaded structures selected by ID, structure tag, or custom structure group.
- Visiting multiple structure types.
- Portal creation or nearby portal state.
- Nearby active blocks and compatible block entities.
- Killing configured entities or numbers of entities.
- Player-kill counters.
- Carrying configured items or item counts.
- Trading with villagers.

Structure checks use loaded structure data. Ambush does not locate distant structures, generate terrain, or synchronously force new chunks merely to roll an encounter.

## Placement that feels intentional

Groups can spawn on land, in water, underwater, or in open air. Definitions can request individuals, formations, rings, clustered rings, surrounding groups, directional cones, and staged waves.

Placement is bounded and loaded-chunk safe. Ordinary ambushers avoid direct player line of sight unless a definition opts out. Authors can control minimum and maximum distance, hidden placement, water permission, air spawning, cluster size, direction, approach behavior, and whether mobs attack immediately.

Entities can receive equipment, effects, attributes, passengers, tags, persistence, owner targeting, aggression ranges, through-wall aggression, flying follow behavior, and spawn or despawn effects. Waves may trigger after a timer, after the previous wave dies, or through both requirements together.

## Directional waves and projectile rain

Ambush supports vanilla arrows, spectral arrows, tipped arrows, registry-defined modded arrows, potions, generic entities, and validated optional projectiles.

Directional actions can originate from a world direction, a resolved encounter origin, or a named moving structure. Authors can configure:

- Initial delays and repeating burst schedules.
- Different entries in each wave.
- Launch height and forward offset.
- Source direction and source spread.
- Exact player targeting or target-area spread.
- Safe radii around the target.
- Velocity, inaccuracy, and gravity compensation.
- Activation only when the source enters range.
- Per-source staggering across a fleet.

This can produce an arrow volley from an approaching formation, vertically falling shells whose origin matches an attacking ship, or a potion barrage that begins only when the source reaches firing range.

## Optional moving structures and fleets

With a compatible Sable and Aeronautics stack, datapacks can import normal structure-template NBT files as moving sublevel encounters. Ambush includes its own guarded assembly adapter and does not require a separate assembly-helper mod.

A fleet may use a different schematic for every member. Each ship can override its template, location, facing, crew, events, lifetime, engines, throttle, steering, loot, and structure key. This supports mixed formations rather than repeating one identical ship.

Optional structure features include:

- Player-facing, cardinal, surrounding, and tangent placement.
- Per-seat crew filling and additional safe deck spawning.
- Loot tables for structure containers.
- Balloon fill, portable-engine burn time, superheated state, and throttle signals.
- Player-height-based redstone and throttle rules.
- Live directional buttons, levers, and analog components.
- Proportional steering based on the player's live position in each ship's local frame.
- Engine or propeller reversal selected by player-relative direction.
- Crew-gated actions so abandoned ships stop operating.
- Persistent child structures and bounded split-fragment cleanup.

Propulsion-aware steering compensates when exactly one engine or propeller direction is reversed. Direction maps can make a ship move forward while the player is ahead and reverse only while the player is behind it.

## Structure health and boss events

A supported moving structure can expose its original blocks as health. Ambush records occupied blocks and displays the remaining percentage through an optional boss bar. Damage that was already observed cannot be erased by replacing a block.

Multiple ships can share one health pool and boss bar. Events may run when health crosses a percentage, when the structure survives for a duration, when the owner enters range, or when the structure is defeated. Events can launch projectile waves, summon ground or air reinforcements, add ships, chain another encounter, activate redstone, play directional sounds, apply fog, create fireworks, or spawn a reward structure.

Health work uses global and per-structure budgets, allowing large fleets to update progressively instead of scanning every block every tick.

## Reliability and diagnostics

Definitions are validated during datapack reload. Unknown triggers, malformed fields, invalid chance units, missing registry entries, unsafe counts, invalid structure selectors, and unsupported action values report actionable errors instead of being silently accepted.

Scheduled actions and encounter state survive restarts. Owned entities and structures are indexed by owner for correct targeting and cleanup. Optional integrations are detected at runtime and are not hard dependencies; unavailable optional actions fail closed while generic encounters continue working.

Ambush also includes bounded retries, loaded-chunk placement, generation-depth limits, active-ship limits, health and action budgets, owner-correct cleanup, and shutdown loading-ticket release.

## Operator commands

Commands require permission level 2:

```text
/ambush <type> [player]
/ambush always <type> [player]
/ambush list
/ambush validate
/ambush weights
/ambush state
/ambush debug
```

`/ambush always` bypasses chance, cooldown, and normal trigger restrictions for testing. `/reload` applies external datapack changes without rebuilding the mod. Debug mode writes detailed results to the server console instead of spamming players.

## Included examples

Five command-only examples are embedded:

```text
ambush:example_boss
ambush:example_boss_fleet
ambush:example_cannon_balloon_fleet
ambush:example_directional_arrow_rain
ambush:example_master
```

The boss examples demonstrate structure health, shared fleet health, lifecycle events, fog, directional sound, mixed projectile waves, reinforcements, redstone, rewards, crew, loot, steering, and live propulsion. The master example is an exhaustive schema reference intended for disposable test worlds.

## Requirements and compatibility

- Minecraft 1.21.1
- NeoForge 21.1.228 or newer
- Java 21

Create, Create Aeronautics/Simulated, Sable, and Create Big Cannons are optional. Generic mobs, vanilla projectiles, sounds, effects, triggers, cooldowns, and commands work without them.

Ambush is released under the MIT License. The repository includes complete source, Gradle wrapper, full schema documentation, examples, and build instructions.
