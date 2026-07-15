# Ambush

Ambush is a server-side NeoForge 1.21.1 mod for configurable, per-player hostile encounters. Encounter definitions are data-driven JSON files loaded from every server datapack under:

```text
data/<namespace>/ambushes/<id>.json
```

The mod is independent of MineColonies, raids, Steam Age, TACZ, and unrelated gameplay mods. This build requires Create Aeronautics: Discovery 1.4.4 and Sable 2.0.3 because Sable structure actions use Discovery's supported assembly and lifecycle pipeline.

## Installation

Install `ambush-0.1.0.jar` on the server. It is intended to be distributed with a normal CurseForge modpack. Definitions may be bundled inside the mod or supplied by a separate datapack. Put a datapack in a world’s `datapacks` directory, run `/reload`, and the definitions become available without rebuilding the mod.

The mod does not generate chunks, scan every entity, edit saves, alter other mods’ files, or depend on KubeJS.

## Commands

Commands require permission level 2.

```text
/ambush list
/ambush validate
/ambush state
/ambush <id>
/ambush <id> <player>
```

Examples:

```text
/ambush example_zombie_pair
/ambush ambush:example_cannon_balloon Mateo
/ambush ambush:example_cannon_balloon_fleet Mateo
```

Leaving out the player targets the command executor. A command still obeys the definition’s normal conditions. For example, a night-only encounter will not run during the day when manually invoked. `/ambush validate` reports the number of definitions accepted by the reload listener. `/ambush state` reports loaded definitions, loaded owned entities, and tracked cooldown entries.

## Definition format

The compact format is supported for simple pack authoring:

```json
{
  "trigger": "interval",
  "interval": 1200,
  "cooldown": 12000,
  "chance": 5,
  "min_y": 50,
  "max_y": 179,
  "min_time": 13000,
  "max_time": 23000,
  "biomes": ["minecraft:plains"],
  "dimensions": ["minecraft:overworld"],
  "active_blocks": ["minecraft:beacon"],
  "radius": 40,
  "attempts": 32,
  "spawns": [],
  "effects": ["minecraft:darkness:2:0"],
  "sounds": ["minecraft:entity.ghast.warn"]
}
```

The expanded format is better for reusable datapacks and is also accepted:

```json
{
  "schema_version": 1,
  "enabled": true,
  "trigger": {
    "type": "interval",
    "check_every_ticks": 1200,
    "cooldown_ticks": 24000,
    "chance": {"base": 0.05}
  },
  "conditions": {
    "height": {"min": 50, "max": 179},
    "time": {"min": 13000, "max": 23000},
    "biomes": ["minecraft:plains"],
    "dimensions": ["minecraft:overworld"],
    "active_blocks": ["minecraft:beacon"]
  },
  "wave": {
    "radius": 40,
    "maximum_attempts_per_member": 32,
    "groups": []
  }
}
```

### Timing and probability

- `interval`: how often the definition is checked, in game ticks. `20` is approximately one second.
- `cooldown`: cooldown after a successful encounter, in seconds in compact format.
- `check_every_ticks`: expanded-format check interval.
- `cooldown_ticks`: expanded-format cooldown.
- `chance`: compact percentage from `0` to `100`.
- Expanded `chance.base`: probability from `0.0` to `1.0`.
- A definition only consumes a cooldown after at least one entity was successfully spawned.

Examples:

```json
{"trigger":"interval","interval":1200,"cooldown":36000,"chance":1,"spawns":[{"entity":"minecraft:zombie","count":2}]}
```

```json
{"trigger":{"type":"interval","check_every_ticks":6000,"cooldown_ticks":72000,"chance":{"base":0.02}},"wave":{"groups":[{"entity":"minecraft:warden","count":1}]}}
```

### Location conditions

- `min_y` / `max_y`: player Y range.
- `min_time` / `max_time`: Minecraft day-time ticks, from `0` through `24000`.
- `biomes`: exact biome IDs. Multiple entries are alternatives.
- `dimensions`: exact dimension IDs.
- `active_blocks`: block IDs checked around the player for `block_active` encounters.
- `radius`: maximum horizontal distance from the player.
- `attempts`: maximum candidate attempts. This is a safety limit, not a guarantee that every member spawns.

Examples:

```json
{"trigger":"interval","min_y":180,"max_y":320,"min_time":13000,"max_time":23000,"biomes":["minecraft:stony_peaks","minecraft:jagged_peaks"],"spawns":[{"entity":"minecraft:phantom","count":4}]}
```

```json
{"trigger":"interval","dimensions":["minecraft:the_nether"],"spawns":[{"entity":"minecraft:piglin","count":6}]}
```

### Trigger types

`interval` is the normal periodic trigger. It evaluates the location and condition fields.

```json
{"trigger":"interval","interval":1200,"chance":5,"spawns":[{"entity":"minecraft:zombie","count":2}]}
```

`portal` requires a Nether portal block within roughly three blocks of the player.

```json
{"trigger":"portal","interval":20,"cooldown":36000,"chance":100,"spawns":[{"entity":"minecraft:piglin","count":4}]}
```

`block_active` checks nearby blocks listed by `active_blocks`. It also recognizes block entities exposing a positive `progress` value, which allows compatibility with many processing machines without a hard dependency on their mod.

```json
{"trigger":"block_active","interval":100,"cooldown":18000,"chance":8,"active_blocks":["createoreexcavation:extractor"],"spawns":[{"entity":"minecraft:zombie","count":5}]}
```

The trigger field is deliberately extensible. Unknown trigger names fail closed instead of silently creating an uncontrolled encounter.

## Spawn groups

Every entry in `spawns` or `wave.groups` is independent. Supported fields are:

- `entity`: registry entity ID; required.
- `count`: fixed integer or `{ "min": 2, "max": 6 }`.
- `passenger`: shorthand for one passenger.
- `passengers`: recursive passenger array.
- `avoid_line_of_sight`: default `true`; candidates visible to the player are rejected.
- `persistent`: keeps the mob from naturally despawning.
- `tags`: arbitrary entity tags.
- `target`: `owner` (default) or `none`.
- `aggro_through_walls`: keeps retargeting the owner even when terrain blocks visibility.
- `effects`: effects applied directly to the spawned mob or passenger.
- `placement`: reserved for future placement adapters; unknown placement values safely use the normal bounded placement path.

Fixed count:

```json
{"entity":"minecraft:zombie","count":2,"tags":["front_line"],"persistent":true}
```

Random count:

```json
{"entity":"minecraft:pillager","count":{"min":6,"max":12},"tags":["raider"]}
```

Multiple groups:

```json
"spawns":[
  {"entity":"minecraft:pillager","count":12},
  {"entity":"minecraft:vindicator","count":4},
  {"entity":"minecraft:witch","count":2}
]
```

Phantom riders:

```json
{
  "entity":"minecraft:phantom",
  "count":8,
  "avoid_line_of_sight":true,
  "passengers":[
    {"entity":"minecraft:pillager","count":1,"persistent":true,"tags":["airborne_raider"]}
  ]
}
```

Nested passengers:

```json
{
  "entity":"minecraft:ghast",
  "count":2,
  "passengers":[
    {"entity":"minecraft:pillager","passengers":[{"entity":"minecraft:parrot"}]}
  ]
}
```

Non-hostile or neutral entities can be spawned by setting `target` to `none`:

```json
{"entity":"minecraft:iron_golem","count":1,"target":"none","persistent":true}
```

Mob effects and wall aggro:

```json
{
  "entity":"minecraft:ravager",
  "count":2,
  "aggro_through_walls":true,
  "effects":["minecraft:resistance:30:1","minecraft:speed:30:1"],
  "tags":["wall_hunter"]
}
```

Wall-aggro mobs are periodically retargeted to their owner while loaded. This is intentionally opt-in because it can make enclosed encounters much more dangerous.

## Visibility, safety, and targeting

The runtime samples bounded positions around the owner, rejects occupied positions, rejects positions visible through a direct raycast when line-of-sight avoidance is enabled, and assigns hostile mobs directly to the owner rather than using global entity selectors.

All spawned entities receive `ambush_owned` and an owner UUID tag. This makes ownership distinguishable from ordinary mobs and prevents the mod from claiming unrelated entities. The runtime does not generate chunks or perform a global mob scan.

Owned entities are periodically lifecycle-checked. Loaded owned entities whose owner is no longer present and which exceed the built-in lifetime guard are discarded, preventing abandoned encounters from accumulating indefinitely while leaving ordinary mobs untouched.

If there is no safe candidate, that member is skipped and the encounter may fail without consuming its cooldown. This is intentional: unsafe or visible fallback spawning is worse than a missed encounter.

## Effects and sounds

Effects use the compact string form `effect_id:seconds:amplifier`:

```json
"effects":[
  "minecraft:darkness:2:0",
  "minecraft:glowing:5:0"
]
```

Sounds are registry sound IDs played to the target player:

```json
"sounds":[
  "minecraft:entity.ghast.warn",
  "minecraft:event.raid.horn"
]
```

Effects and sounds run only after at least one member spawns.

Sounds may also be scheduled as restart-safe encounter actions. `after_ticks` may be omitted for an immediate sound, while `at` accepts `player` or `structure`:

```json
{"type":"sound","sound":"minecraft:event.raid.horn","after_ticks":240,"at":"player","volume":1.5,"pitch":1.0}
```

## Encounter actions

An encounter may contain an `actions` array. Actions run after the first successful mob spawn and are bounded to a maximum of 128 instances per action.

Arrow rain:

```json
"actions":[{"type":"arrow_rain","count":32,"height":18,"spread":24}]
```

Generic entity rain can target vanilla or modded entity IDs. This is the compatibility path for projectile entities such as Create Big Cannons shells; the exact entity ID must exist in the loaded registry:

```json
"actions":[
  {"type":"entity_rain","entity":"minecraft:fireball","count":6,"height":24,"spread":18},
  {"type":"shell_rain","entity":"createbigcannons:medium_shell","count":4,"height":30,"spread":24}
]
```

If an optional mod or entity ID is absent, that action skips the missing entity rather than crashing the server.

Potion-cloud rain:

```json
"actions":[{"type":"potion_rain","effect":"minecraft:poison","count":8,"height":14,"spread":20}]
```

Structure placement uses the vanilla structure-template command and can reference structures registered by datapacks or mods:

```json
"actions":[{"type":"structure","template":"ambush:cannonballoon"}]
```

### Sable structure actions

Sable structures are ordinary structure-template NBT files under:

```text
data/<namespace>/structure/<path>.nbt
```

They are assembled asynchronously through Aeronautics Discovery. `template` is any namespaced structure ID, so datapacks may add new aircraft without rebuilding Ambush. Static blocks and retained entities such as Simulated honey glue must remain in the NBT; do not save an already-assembled Create contraption or nested Sable sublevel.

```json
"actions":[{
  "type":"sable_structure",
  "template":"my_pack:airship/raider_balloon",
  "placement":"air",
  "spawn_distance":64,
  "offset_y":12,
  "base_facing":"north",
  "facing":"player",
  "max_retries":5,
  "lifetime_ticks":6000,
  "despawn_effect":{"type":"explosion","power":3.0,"fire":false,"block_damage":false},
  "attach_child_sublevels":true,
  "envelope_fill":0.5,
  "engine_burn_ticks":3000,
  "throttle_signal_by_y":[{"max_y":80,"signal":3},{"min_y":81,"signal":4}],
  "container_loot":[{"blocks":["minecraft:barrel"],"loot_table":"minecraft:chests/simple_dungeon"}],
  "entities":[
    {
      "entity":"minecraft:pillager",
      "count":2,
      "local":[4,2,6],
      "seat":true,
      "persistent":true,
      "target":"owner",
      "tags":["raider_crew"],
      "nbt":{"Health":30.0,"CanPickUpLoot":false}
    },
    {
      "nbt":"{id:\"minecraft:vindicator\",Health:40.0f,HandItems:[{id:\"minecraft:iron_axe\",count:1},{}]}",
      "local_x":7,
      "local_y":2,
      "local_z":6
    }
  ]
}]
```

- `template`: required structure resource ID.
- `offset_x`, `offset_y`, `offset_z`: world anchor relative to the targeted player's block position.
- `spawn_distance`: optional radial horizontal distance from the player. When present, Ambush chooses an angle around the player; `spawn_angle_degrees` can fix that angle. `offset_y` still controls height.
- `placement`: use `air` to require a clear, already-loaded template-sized air volume. Ambush searches upward from the configured offset without generating chunks.
- `air_search_attempts` and `air_step`: bounded upward search controls for `placement: "air"`; defaults are `8` attempts and `4` blocks.
- `yaw_degrees`: rotation applied by Discovery after assembly.
- `base_facing`: direction the saved template points before rotation; defaults to `north`.
- `facing` or `direction`: desired `north`, `east`, `south`, `west`, or `player`. `face_player: true` is an equivalent convenience option. Explicit direction fields supersede `yaw_degrees`.
- `max_retries`: bounded assembly attempts; default `5`, maximum `20`.
- `lifetime_ticks`: cleanup delay after successful assembly. The default is `6000` ticks (5 minutes). Use JSON `null`, `"none"`, or `"permanent"` for no automatic cleanup. The deadline and pending cleanup survive world/server restarts.
- `destroyed_cleanup_percent`: optional percentage of the initially occupied parent-and-child Sable blocks that may be destroyed before the remaining structure is cleaned up. Values above `100` clamp to `100`; omit it, use `0`, `null`, `"none"`, or `"disabled"` to disable damage cleanup. The occupied-position baseline and cleanup state survive restarts, and added blocks cannot offset destroyed original blocks.
- Cleanup removes Ambush-created fixed constraints first, unregisters the Aeronautics flyover, releases command force-load tickets, and directly removes the complete Sable family child-first. Initial child sublevels, later split fragments, and the parent are all included; nearby-player fragment-promotion rules do not preserve Ambush debris after threshold cleanup.
- `despawn_effect`: optional data-driven effect played immediately before timed or damage-threshold cleanup. `"explosion"` or an object with `type: "explosion"` is supported. `power` defaults to `3.0`; `fire` and `block_damage` default to `false`. Omit it or use `"none"` for silent cleanup.
- `attach_child_sublevels`: when `true`, Ambush creates fixed Sable constraints between the parent and Discovery-created persistent child bodies, such as converted vanilla barrels. Constraint anchors are persisted and rebuilt after server or world restarts. Leave this false for intentionally detachable physics cargo.
- `envelope_fill`: optional initial Aeronautics balloon fill fraction from `0.0` to `1.0`. It is applied once after the balloon graph initializes; burners and leaks control the gas normally afterward.
- `engine_burn_ticks` or `engine_burn_seconds`: initial remaining burn time applied to every Simulated portable engine in the assembled parent. `3000` ticks is 2.5 minutes.
- `throttle_signal`: fixed Simulated throttle-lever output from `1` to `15`.
- `throttle_signal_by_y`: ordered Y rules captured when the ambush starts. Each rule accepts `min_y`, `max_y`, and a clamped `signal` from `1` to `15`; the first matching rule wins.
- `container_loot`: one rule or an array of rules applied to unopened structure-NBT containers after assembly. Each rule requires `loot_table`, may filter `blocks`, and may specify `seed` or `replace_existing`. Aeronautics Discovery deliberately converts each vanilla barrel into a persistent child Sable sublevel; Ambush follows that parent/child association and applies loot in those child plots.
- `entities`: entities created after successful assembly. An entry may use `entity`, `nbt.id`, or both. When both are present, `entity` wins.
- `nbt`: either a JSON object or an SNBT string. Position and Ambush ownership are applied after NBT loading.
- `local: [x,y,z]` or `local_x/local_y/local_z`: placement relative to the template's minimum corner. Missing X/Z coordinates default to the template center.
- `seat`: when `true`, the entity is associated with the completed Sable sublevel and mounted in the nearest unoccupied Create seat after Sable transfers it into the sublevel plot.
- Seated crossbow mobs use plot-aware aiming because vanilla AI cannot target an overworld entity from Sable plot-space. `seat_crossbow_range` controls their maximum world-space firing range, `seat_crossbow_interval_ticks` controls time between shots, and `seat_crossbow_aim_height_offset` compensates for projectile drop. They face the owner, visibly reload, verify an unobstructed world-space shot, and fire from the moving structure without using cross-level vanilla targeting.
- `target`: `owner` by default or `none`.

Sable-only definitions may use an empty `spawns` array. Queuing the structure then counts as the successful ambush action; a dummy mob is not required.

### Conditional and delayed actions

Actions may include `conditions` with `time: "day"|"night"`, `weather: "clear"|"rain"|"stormy"`, `over_ocean`, `min_y`, `max_y`, or `dimensions`. `conditional_spawn` accepts ordinary spawn entries plus `after_ticks`, `min_radius`, `radius`, and `attempts`. Set `direction: "front"` and `arc_degrees` to constrain candidates to the player's forward view cone. Spawn entries accept `equipment: {"mainhand":"minecraft:crossbow","offhand":"..."}` and `crossbow_range` for a higher-priority vanilla crossbow goal at the requested firing distance. Passengers, water/air placement, line-of-sight avoidance, persistence, effects, equipment, and owner targeting use the same fields as normal spawn groups.

`directional_cbc_shell_rain` supports `block`, `item`, `fuze`, `fuze_ticks`, `velocity`, `source_height`, `source_forward_offset`, `spread`, `target_spread`, `target_safe_radius`, `target_height_offset`, and either `count`/`after_ticks` or a `bursts` array. `source_forward_offset` moves the launch point from the structure toward the player so projectiles can clear the ship. `target_safe_radius` prevents direct targeting inside a configurable ring around the player. When the encounter also queues a Sable structure, its resolved spawn anchor is persisted with each shell wave. Delayed sounds, conditional spawns, and directional rains are SavedData-backed and survive world/server restarts.

### Sable formations

`sable_formation` applies one shared Sable definition to every object in `members`. Each member overrides fields such as `structure_key`, `spawn_distance`, `spawn_bearing_degrees`, and `offset_y`. Bearings are relative to the player's facing direction at encounter start: `0` is ahead, `90` is one side, and `-90` is the other. Use `facing: "player"` to rotate every north-authored ship toward the player.

Use `facing: "orbit_clockwise"` or `facing: "orbit_counterclockwise"` to rotate a north-authored ship tangent to its spawn radius around the player. This sets the initial propulsion direction only; Sable/Aeronautics physics still determine the actual path and do not provide automatic centripetal steering.

Named origins let later actions select one or many ships. Use `source_structure: "fleet_lead"` for one ship or `source_structures: ["fleet_lead","fleet_left","fleet_right"]` to duplicate every configured burst from all listed ships. Each scheduled burst persists its own resolved origin, so formation shell waves remain correct after a restart.

## Surround and stealth encounters

Spawn groups are sampled independently around the owner, so a group such as:

```json
{"entity":"minecraft:pillager","count":{"min":12,"max":18},"avoid_line_of_sight":true,"tags":["surrounding_raider"]}
```

creates a distributed surround rather than placing the entire group at one coordinate. Line-of-sight rejection supports enemies that initially spawn hidden and then walk toward the player. Advanced delayed-alert and structure-approach phases remain an extension point; the current runtime provides the safe hidden spawn and owner targeting foundation.

## Complete showcase definition

The exhaustive, disabled-by-default reference is documented in `MASTER_EXAMPLE.md` and bundled as `ambush:example_master`. It includes all current action syntax, notes mutually exclusive alternatives, and marks unsafe legacy static actions so they do not run during a normal master test.

```json
{
  "trigger":"interval",
  "interval":1200,
  "cooldown":24000,
  "chance":4,
  "min_y":120,
  "max_y":320,
  "min_time":13000,
  "max_time":23000,
  "biomes":["minecraft:stony_peaks","minecraft:jagged_peaks"],
  "dimensions":["minecraft:overworld"],
  "radius":48,
  "attempts":64,
  "spawns":[
    {"entity":"minecraft:phantom","count":{"min":3,"max":6},"avoid_line_of_sight":true,"persistent":true,"tags":["mountain_ambush"],"passengers":[{"entity":"minecraft:pillager","count":1,"persistent":true,"tags":["mountain_raider"]}]},
    {"entity":"minecraft:evoker","count":1,"avoid_line_of_sight":true,"persistent":true,"tags":["mountain_ambush"]}
  ],
  "effects":["minecraft:darkness:2:0","minecraft:glowing:5:0"],
  "sounds":["minecraft:entity.ghast.warn","minecraft:event.raid.horn"]
}
```

## Bundled content

The mod includes generic entity/rain examples, four Sable balloon templates, single-balloon encounters, three cannon-balloon formation encounters, and the exhaustive master reference. All non-balloon examples and the master reference have automatic chance zero. Balloon encounters retain low automatic chances and may also be invoked manually.

The definitions are examples of the format, not hard-coded special cases. A pack creator can delete, override, disable, or replace them with a higher-priority datapack.

## Reloading and troubleshooting

1. Place JSON files under `data/<namespace>/ambushes/`.
2. Run `/reload`.
3. Run `/ambush list`.
4. Test with `/ambush <id>` or `/ambush <id> <player>`.
5. Check the server log for NeoForge reload or JSON errors.

Use namespaced IDs when a datapack has more than one source:

```text
/ambush ambush:example_cannon_balloon_surround
```

Use a small chance, a long cooldown, and a bounded attempt count for large multiplayer encounters. Avoid definitions that spawn hundreds of mobs, use very large radii, or create many overlapping periodic checks.

## Design limits

Ambush is intentionally not a raid replacement, quest system, structure generator, claim system, or general-purpose mob AI framework. It creates bounded encounters and targets their owner. Complex custom equipment, loot tables, delayed multi-wave followups, structure predicates, claims protection, and mod-specific machine APIs should be implemented by future compatibility adapters or ordinary datapack systems rather than hidden assumptions in the core runtime.
