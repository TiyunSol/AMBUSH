# Ambush

(TEMPORARY) - I am creating a modpack that will be an example for this mod. If you have ambush ideas, schematics, or suggestions please join the discord! [https://discord.gg/BHEQthrSSD](https://discord.gg/BHEQthrSSD)

Ambush is a server-authoritative NeoForge 1.21.1 mod for configurable, per-player hostile encounters. Install it on the server and connecting clients because the full feature set includes client fog/audio payloads. Encounter definitions are data-driven JSON files loaded from every server datapack under:

Definitions enter the runtime through a Mojang `Codec` boundary and recursive action validation. Unknown trigger/action modes and invalid bounded fields are rejected during datapack reload instead of being silently ignored.

```text
data/<namespace>/ambushes/<id>.json
```

Advanced definitions use the object-form `trigger`, and easy definitions must
explicitly set `"format": "easy"`. Removed aliases and action names are
rejected rather than silently translated, so a datapack written for 1.0.x may
need updating before it will reload.

Create, Create Aeronautics/Simulated, Sable, and Create Big Cannons are optional integrations. Generic entity, sound, effect, and vanilla rain datapacks work without them. Sable actions fail closed unless the required Sable runtime is available. Ambush contains its own guarded Sable assembly adapter and does not require an external assembly helper mod.

## Installation

Install `ambush-1.1.4.jar` on the server and connecting clients. It is intended to be distributed with a normal CurseForge modpack. Definitions may be bundled inside the mod or supplied by a separate datapack. Put a datapack in a world’s `datapacks` directory, run `/reload`, and the definitions become available without rebuilding the mod.

The mod does not edit other mods’ files or require a scripting platform. Ordinary encounters remain loaded-chunk-only; Sable ship assembly synchronously loads only the bounded destination and internal plot chunks required for that ship.

## Making your own encounters

**Start here: [DATAPACK_GUIDE.md](DATAPACK_GUIDE.md).** It walks through the
datapack layout, a complete working encounter, presets, mobs, unlocks, the
commands, and troubleshooting. No Java, no rebuilding — write a JSON file, run
`/reload`, and it is live.

You can also copy the `ambush_easy_template` folder out of the jar and edit
`data/my_pack/ambushes/my_first_ambush.json` as a starting point.

For the full encounter format, advanced features, examples, and troubleshooting,
see [DATAPACK_GUIDE.md](DATAPACK_GUIDE.md).

## Commands

Help and listing are available to players. Starting or clearing encounters and
the admin tools require permission level 2.

```text
/ambush
/ambush list
/ambush <id>
/ambush <id> <player>
/ambush player <player>
/ambush player <player> <id>
/ambush always
/ambush clear
/ambush enable [pack]
/ambush disable [pack]
/ambush admin check
/ambush admin check <id>
/ambush admin debug
/ambush admin weights
/ambush admin unlocks
/ambush admin unlockall
/ambush admin inspect
/ambush admin spawning
/ambush admin spawning status
/ambush admin spawning <id> [enable|disable]
```

`/ambush always` toggles persistent always mode for the executing player.
While on, `/ambush <id>` skips the entire eligibility check: trigger
requirement, time, height, biome, dimension, portal proximity, and required
nearby blocks. `always` has no encounter argument: toggle it on, then run
`/ambush <id>`. Run `/ambush always` again to turn it off. Running `/ambush`
with no argument reports the current state.

Running a definition by name never rolls its chance and always clears its
cooldown group, so always mode is only needed to bypass conditions.
`/ambush <id> <player>` targets another player and ignores conditions
regardless of always mode.

`/ambush admin check` reports loaded, dependency-hidden, and rejected
definitions. Adding an ID runs the detailed read-only preflight. `/ambush
clear` cancels persisted actions and removes active AMBUSH-owned mobs, ships,
boss bars, tracked audio, and fog while preserving chance and cooldown history.

`/ambush admin inspect` reports the nearest active AMBUSH vessel’s template,
local origin, configured controls, detected hardware categories, block IDs, and
schematic-local positions. It is intended for creating or troubleshooting
data-driven ship, boat, and car definitions.

`/ambush admin spawning` toggles natural spawning for the whole server; while
it is off nothing spawns on its own but named encounters still run.
`/ambush admin spawning status` reports that toggle plus any individually
disabled definitions, and `/ambush admin spawning <id>` reads or changes one
definition. `/ambush enable [pack]` and `/ambush disable [pack]` work on a
whole datapack namespace; a blank argument targets `ambush`, the bundled set.

`/ambush admin weights` reports each definition's effective weight, base
weight, current chance, and cooldown group. `/ambush admin unlocks` reports
unlock progress, and `/ambush admin unlockall` unlocks everything for the
executing player.

## Vehicles, boats, and potato cannons

Ambush supports data-driven ground vehicles through `sable_car`. Cars use a
surface-placement adapter and explicit `car_controls` positions for steering,
clutch, reverse, and throttle hardware.

Car controllers support:

- `broadside`: circles at the configured distance while keeping a selected side
  aimed toward its target, with automatic chase outside `target_range`.
- `chase`: direct pursuit using `chase_controller`.
- Optional stationary-target braking through
  `broadside_brake_min_range`, `broadside_brake_range`, and
  `target_still_speed`.

`sable_boat` places a vessel on eligible open water. Ambush resolves the
exposed water surface at each candidate anchor and counts consecutive water
blocks downward; `minimum_water_depth` (default `2`, range 1–256) rejects
shallow water and `water_spawn_height` (default `2`, range 1–16) sets the
height above that surface. After assembly Ambush never changes a boat's
position or velocity — Sable/Aeronautics handle all motion and buoyancy, and
no waterline lock is applied. Boats use the normal crew, cannon, redstone,
event, steering, propulsion, cleanup, and container systems, but cannot use
`altitude_controller` or `envelope_fill`.

A `sable_formation` member may explicitly be a `sable_structure`,
`sable_boat`, or `sable_car`, so a single fleet can mix airships with boats or
cars. Each member needs its own valid placement data and a unique
`structure_key`.

Redstone-driven machines can use `power_positions` alongside visible button
`positions`. For sequenced activations, entries are paired by index, allowing
each button press to power its matching receiver even when moving-assembly
redstone wiring is not directly adjacent.

The bundled Pillager Potato Car demonstrates broadside and forward mounted
potato cannons, staggered side firing, hopper-loaded ammunition, and
data-driven ground-vehicle controls. The bundled boat encounters demonstrate
tiny broadside, forward autocannon, and fishing-boat configurations.

## Runtime safety and transactional fleets

`ambush-common.toml` contains server-authoritative limits for simultaneous ships, entities, projectiles, scheduled actions, chunk-loading distance, and nested fleet depth. Oversized definitions are rejected during reload with the JSON path and exceeded limit. Heavy Sable work is limited to one assembly, finalization, or cleanup transition per dimension tick.

Sable formations are transactional. Ambush assembles one member, waits for a valid Sable physics body, spawns and verifies its living crew and requested hardware state, then queues the next member. A failed member is safely removed and all unstarted members and source-dependent scheduled actions are cancelled. Previously verified ships remain unless `rollback_on_failure: true` is explicitly configured. Actions that must fail the encounter should set `"required": true`; rejection then returns the exact action ID and reason instead of reporting success with zero accepted actions.

Use `hardware_requirements` on a Sable structure or formation member to require minimum counts after assembly: `minimum_seats`, `minimum_carpets`, `minimum_levers`, `minimum_analog_controls`, `minimum_engines_or_propellers`, `minimum_balloon_components`, and `minimum_steering_controls`. Requested fill, analog, propulsion, seating, crew, and steering state is read back after application. Failed verification stops dependent actions.

Bundled encounters use production IDs. Toggle `/ambush always` on, then test
one with `/ambush <id>`.

While debug mode is enabled, lifecycle work reports `scheduled`, `blocked by crew gate`, `waiting for source structure`, `executed`, or `failed` with a reason. Crew reports include living and configured counts. Analog/throttle verification reports the component, local position, requested value, and readable post-write value. Envelope verification reports each member's balloon count, capacity, requested fill, and observed fill. Sable does not expose a stable pre-assembly craft mass-versus-lift margin API, so preflight identifies that limitation and runtime diagnostics verify the gas state that Sable actually accepted. These messages remain suppressed during normal gameplay.

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
- `cooldown_group`: shared cooldown key; definitions in the same group share one cooldown. Defaults to `default`.
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

`structure` rolls only inside a matching loaded structure piece; see "Structure triggers and chance buildup" below.

`kill` runs after the target kills a qualifying entity. `kill_count` sets how many kills are required, from `1` to `10000`, and defaults to `1`. `kill_entity` restricts it to one entity ID; omit it to count any kill.

```json
{"trigger":"kill","kill_entity":"minecraft:pillager","kill_count":12,"cooldown":1800,"chance":100,"spawns":[{"entity":"minecraft:vindicator","count":3}]}
```

`vanilla_raid_wave` is driven by raid events rather than the periodic check, so it does not run from a schedule.

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
- `target`: `nearby_players` (default), `owner`, or `none`.
- `nearby_player_range`: target radius around the encounter owner; default `64`.
- `aggro_through_walls`: permits retargeting even when terrain blocks visibility.
- `effects`: effects applied directly to the spawned mob or passenger.
- `placement`: reserved for future placement adapters; unknown placement values safely use the normal bounded placement path.

Fixed count:

```json
{"entity":"minecraft:zombie","count":2,"tags":["front_line"],"persistent":true}
```

Random count:

```json
{"entity":"minecraft:pillager","count":{"min":6,"max":12},"tags":["hostile_group"]}
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
    {"entity":"minecraft:pillager","count":1,"persistent":true,"tags":["airborne_attacker"]}
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
  "minecraft:entity.pillager.celebrate"
]
```

Effects and sounds run only after at least one member spawns.

Sounds may also be scheduled as restart-safe encounter actions. `after_ticks` may be omitted for an immediate sound, while `at` accepts `player` or `structure`:

```json
{"type":"sound","sound":"minecraft:entity.pillager.celebrate","after_ticks":240,"at":"player","volume":1.5,"pitch":1.0}
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
  {"type":"cbc_shell_rain","entity":"createbigcannons:medium_shell","count":4,"height":30,"spread":24}
]
```

If an optional mod or entity ID is absent, that action skips the missing entity rather than crashing the server. CBC shell rain reports a zero-shell result as a failed scheduled action instead of silently succeeding; use `required: true` for an encounter that must not continue without its artillery.

Potion-cloud rain:

```json
"actions":[{"type":"potion_rain","effect":"minecraft:poison","count":8,"height":14,"spread":20}]
```

Structure placement uses the vanilla structure-template command and can reference structures registered by datapacks or mods:

```json
"actions":[{"type":"structure","template":"ambush:cannonballoon"}]
```

### Non-Sable micro structures

`micro_structure` places a small data-defined block composition on a safe, already-loaded surface without Sable or a structure-template file. Every block supplies a namespaced `block`, an `offset: [x,y,z]`, and optionally `properties` plus a `replace` rule (`air`, `replaceable`, or `surface`). Actions are limited to 128 unique offsets within 16 blocks of the anchor and 64 bounded placement attempts. Block entities, protected blocks, unloaded chunks, and unsuitable terrain are rejected.

```json
"actions":[{
  "type":"micro_structure",
  "min_radius":24,
  "radius":48,
  "avoid_line_of_sight":true,
  "lifetime_seconds":300,
  "blocks":[
    {"block":"minecraft:sculk","offset":[0,0,0],"replace":"surface"},
    {"block":"minecraft:sculk_sensor","offset":[0,1,0],"replace":"air"}
  ],
  "entities":[{
    "entity":"minecraft:warden",
    "count":1,
    "offsets":[[0,1,0]],
    "target":"owner"
  }]
}]
```

`entities` use normal mob fields and spawn at offsets relative to the same anchor. Original block states are persisted and restored after `lifetime_seconds` (or the lower-level `lifetime_ticks`) or when `/ambush clear` is used. If both time fields are present, `lifetime_ticks` takes precedence. Cleanup restores at most 128 blocks per tick, never force-loads a chunk, and leaves a position alone if a player changed the placed block. At most 64 micro structures are tracked globally and 16 per owner. The bundled `ambush:surface_sculk_breach` demonstrates a hidden sculk patch, sensors, a summoning shrieker, a catalyst, and two surface wardens that restores after five minutes.

### Sable structure actions

Sable structures are ordinary structure-template NBT files under:

```text
data/<namespace>/structure/<path>.nbt
```

They are assembled asynchronously through Ambush's guarded Sable adapter. `template` is any namespaced structure ID, so datapacks may add new aircraft without rebuilding Ambush. Static blocks and retained entities such as Simulated honey glue must remain in the NBT; do not save an already-assembled Create contraption or nested Sable sublevel.

To bound fleet spawn cost, Ambush prepares at most one previously unloaded destination chunk per server turn. As soon as the complete footprint is loaded, it places the template and invokes Simulated assembly in the same turn. The resulting Sable sublevel immediately receives an AMBUSH force-load ticket. After a short settlement window, loot, hoppers, cannons, crew, propulsion, and envelope fill are applied one bounded stage at a time through the assembled plot accessor. The ship remains at its real world pose throughout this sequence.

```json
"actions":[{
  "type":"sable_structure",
  "template":"my_pack:airship/hostile_balloon",
  "assembly_origin":[4,2,6],
  "placement":"air",
  "spawn_distance":64,
  "offset_y":12,
  "schematic_front":"north",
  "facing":"player",
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
      "friendly_fire":false,
      "tags":["hostile_crew"],
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
- `assembly_origin`: required placed-template-local `[x,y,z]` coordinate of a solid block connected to the intended craft. Coordinates are measured in the raw saved NBT before rotation; AMBUSH applies `schematic_rotation` to that coordinate. AMBUSH never scans the volume to guess this block.
- `offset_x`, `offset_y`, `offset_z`: world anchor relative to the targeted player's block position.
- `spawn_distance`: optional radial horizontal distance from the player. When present, Ambush chooses an angle around the player; `spawn_angle_degrees` can fix that angle. `offset_y` still controls height.
- `spawn_bearing_degrees`: formation-member bearing relative to the player facing. It makes a fleet's position reproducible while keeping the saved structure's facing data-driven.
- `placement`: use `air` to require a clear template-sized air volume. Ambush loads the bounded footprint before the one-pass placement and searches upward from the configured offset.
- `air_search_attempts` and `air_step`: bounded upward search controls for `placement: "air"`; defaults are `8` attempts and `4` blocks.
- `yaw_degrees`: rotation applied after assembly.
- `schematic_front`: direction the saved template considers its front; defaults to `north`.
- `schematic_rotation`: an additional data-defined rotation: `none`, `clockwise_90`, `clockwise_180`, or `counterclockwise_90`. Sable actions also accept finite `schematic_rotation_degrees` for arbitrary yaw. Static `structure` actions use the four vanilla template rotations.
- `facing` or `direction`: desired `north`, `east`, `south`, `west`, or `player`. `face_player: true` is an equivalent convenience option. Explicit direction fields supersede `yaw_degrees`.
- `ship_stage_delay_ticks`: delay between template preparation
  and each loot/hopper/cannon/crew/propulsion/envelope step; default `5`,
  valid range `1`–`100`.
- `ship_settle_delay_ticks`: delay after Sable assembly before hardware
  scanning begins; default `10`, valid range `1`–`200`.
- `lifetime_ticks`: cleanup delay after successful assembly. The default is `6000` ticks (5 minutes). Use JSON `null`, `"none"`, or `"permanent"` for no automatic cleanup. The deadline and pending cleanup survive world/server restarts.
- `destroyed_cleanup_percent`: optional percentage of the initially occupied parent-and-child Sable blocks that may be destroyed before the remaining structure is cleaned up. Values above `100` clamp to `100`; omit it, use `0`, `null`, `"none"`, or `"disabled"` to disable damage cleanup. The occupied-position baseline and cleanup state survive restarts, and added blocks cannot offset destroyed original blocks.
- Cleanup removes Ambush-created fixed constraints first, unregisters the Aeronautics flyover, releases command force-load tickets, and directly removes the complete Sable family child-first. Initial child sublevels, later split fragments, and the parent are all included; nearby-player fragment-promotion rules do not preserve Ambush debris after threshold cleanup.
- `despawn_effect`: optional data-driven effect played immediately before timed or damage-threshold cleanup. `"explosion"` or an object with `type: "explosion"` is supported. `power` defaults to `3.0`; `fire` and `block_damage` default to `false`. Omit it or use `"none"` for silent cleanup.
- `attach_child_sublevels`: when `true`, Ambush creates fixed Sable constraints between the parent and persistent child bodies, such as converted vanilla barrels. Constraint anchors are persisted and rebuilt after server or world restarts. Leave this false for intentionally detachable physics cargo.
- `envelope_fill`: optional initial Aeronautics balloon fill fraction from `0.0` to `1.0`. It is applied once after the balloon graph initializes; burners and leaks control the gas normally afterward. It is not an altitude controller — use `altitude_controller` for ongoing lift control.
- `cannonballoon_flight_profile`: defaults to `true` for `template: "ambush:cannonballoon"` and may be set to `false` to opt out. The profile starts at fill `1.0`, rejects underground targets, and maps the player Y level to Simulated analog throttle: `<=80` is `3`; `>=81` is `4`.
- `engine_burn_ticks` or `engine_burn_seconds`: initial remaining burn time applied to every Simulated portable engine in the assembled parent. `3000` ticks is 2.5 minutes.
- `engine_superheated`: optional boolean. When `true`, Ambush invokes Simulated's portable-engine superheated state in addition to the configured burn time. It is ignored safely when Simulated/Aeronautics is unavailable.
- `throttle_signal`: fixed Simulated throttle-lever output from `1` to `15`.
- For a five-minute, full-power ship, use `"engine_burn_seconds":300` and `"throttle_signal":15`. These are ordinary datapack fields, not hard-coded fleet behavior.
- `throttle_signal_by_y`: ordered Y rules captured when the ambush starts. Each rule accepts `min_y`, `max_y`, and a clamped `signal` from `1` to `15`; the first matching rule wins.
- `container_loot`: one rule or an array of rules applied to unopened structure-NBT containers after assembly. Each rule requires `loot_table`, may filter `blocks` or exact `positions`, and may specify `seed`, `replace_existing`, or `lootr`. When the active Sable stack converts containers into persistent child sublevels, Ambush follows the parent/child association and applies loot in those child plots.
- `entities`: entities created after successful assembly. An entry may use `entity`, `nbt.id`, or both. When both are present, `entity` wins.
- `nbt`: either a JSON object or an SNBT string. Position and Ambush ownership are applied after NBT loading.
- `local: [x,y,z]` or `local_x/local_y/local_z`: placement relative to the template's minimum corner. Missing X/Z coordinates default to the template center.
- `seat`: when `true`, the entity is associated with the completed Sable sublevel and mounted in the nearest unoccupied Create seat after Sable transfers it into the sublevel plot.
- `target_range`: owning-player range, in blocks, at which a normal spawned Ambush mob becomes hostile. It also sets that mob's follow-range attribute.
- `aggro_range`: optional `4`–`512` block activation radius. `deaggro_range` is the larger distance at which an existing target is dropped; mobs remain passive until a valid player re-enters `aggro_range`.
- `nearby_player_range`: players within this distance of the originally ambushed player become valid targets. It defaults to `32`. Normal mobs require line of sight unless `aggro_through_walls` is true. Seated Sable crew use live world-space distance and keep their normal equipment and attack goals.
- Ambush never replaces a seated mob's held weapon or fabricates arrows for it. Weapon-specific behavior still comes from that entity and weapon's normal goals; Ambush supplies owner targeting and transformed aiming.
- `friendly_fire`: defaults to `false`. Set it to `true` only when Ambush-owned mobs should be able to damage other Ambush mobs with the same owner.
- `extra_health`: adds `0`–`2048` maximum health and heals the newly created mob to that new maximum.
- `extra_damage`: adds `0`–`1024` damage to attacks made by that mob, including its owned projectiles. It does not modify unrelated entities.
- `boss_bar`: `true` for defaults, `false` to disable, or an object with `name`, `color`, `overlay`, `audience` (`owner`, `nearby`, or `all`), and `range`. Mob boss bars follow the mob's actual health and are removed on death or cleanup.
- `reinforcements`: up to 32 persistent lifecycle events for that mob. Each event has a unique `id`, a `trigger`, optional `repeat_ticks`, and up to 32 `actions`. Use `{"type":"health_percent","at_or_below_percent":50}` or `{"type":"time_alive","after_ticks":2400}`. Trigger state, accepted-action cursor, and spawn time are stored on the mob, so reloads and server restarts neither lose a budget-deferred action nor repeat an acknowledged one-shot event.
- `target`: `nearby_players` by default, `owner`, or `none`.

Mob reinforcement actions can call another named encounter with `{"type":"ambush","ambush":"your_pack:reinforcements"}`, create a private `inline_ambush`, queue a compatible `sable_structure`/`sable_formation`, spawn `conditional_spawn` or entity waves, and launch directional or ordinary arrow, potion, entity, or CBC shell rains. Normal dependency hiding, generation-depth limits, active-ship limits, placement budgets, and per-tick action caps still apply.

Sable-only definitions may use an empty `spawns` array. Queuing the structure then counts as the successful ambush action; a dummy mob is not required.

### Testing new datapack definitions

Put or edit definitions under the active world's `datapacks` folder, or in a loaded server resource/data pack, using `data/<namespace>/ambushes/<id>.json`. Run the vanilla `/reload` command. Ambush rebuilds its definition registry during that reload and logs compatible, dependency-hidden, and rejected counts. Then use `/ambush list`, `/ambush admin check`, or tab completion to confirm the new ID before testing it. Toggle `/ambush always` on, then run `/ambush <namespace>:<id>`.

Changing JSON data only requires `/reload`; changing the Ambush jar, Java code, or bundled resources still requires a game/server restart.

Bundled Ambush encounters are embedded under `data/ambush/ambushes/` with production IDs. Ordinary encounters may trigger naturally; unlockable encounters use progress and activation instead of chance. To force-test a compatible ID, enable `/ambush always` and then run `/ambush <id>`.

### Conditional and delayed actions

Actions may include `conditions` with `time: "day"|"night"`, `weather: "clear"|"rain"|"stormy"`, `over_ocean`, `min_y`, `max_y`, or `dimensions`. `conditional_spawn` accepts ordinary spawn entries plus `after_ticks`, `min_radius`, `radius`, and `attempts`. Set `direction: "front"` and `arc_degrees` to constrain candidates to the player's forward view cone. Spawn entries accept `equipment: {"mainhand":"minecraft:crossbow","offhand":"..."}` and `crossbow_range` for a higher-priority vanilla crossbow goal at the requested firing distance. Passengers, water/air placement, line-of-sight avoidance, persistence, effects, equipment, owner targeting, and `friendly_fire` use the same fields as normal spawn groups.

`directional_cbc_shell_rain` supports `block`, `item`, `fuze`, `fuze_ticks`, `velocity`, `source_height`, `source_forward_offset`, `spread`, `target_spread`, `target_safe_radius`, `target_height_offset`, and either `count`/`after_ticks` or a `bursts` array. Set `ballistic: true` with a data-driven `gravity` (CBC HE shells use `0.05`) to compensate the firing vector for gravity; use `target_spread: 0` for an exact player target. `start_distance` delays each rain until its source is horizontally within that many blocks of the owner; a named Sable `source_structure` is resolved from the active ship every poll, so a moving ship can enter range and begin firing. A named source waits using `source_poll_ticks` (default `20`) and fails after the persisted action's bounded `max_attempts`. Set `source_fallback: "owner"` to deliberately use the owner's current position when that source cannot resolve; the default is `source_fallback: "wait"`. `start_distance_poll_ticks` controls the range recheck interval (default `20`). With `source_structures`, `source_delay_ticks` adds a per-source stagger in listed order: `0`, one delay, two delays, and so on. For example, `source_delay_ticks:20` makes the first listed ship fire now, the next ship one second later, and the third two seconds later. `source_forward_offset` moves the launch point from the structure toward the player so projectiles can clear the ship. `spread` changes launch positions; `target_spread` changes the aim radius. `target_safe_radius` excludes the center of that aim radius; it is not a distance condition that prevents firing nearby. When the encounter also queues a Sable structure, its exact named member UUID and resolved spawn anchor are persisted with each wave. Delayed sounds, conditional spawns, and directional rains are SavedData-backed and survive world/server restarts.

### Sable formations

`sable_formation` applies one shared Sable definition to every object in `members`. Each member may override any top-level formation field, including `type`, `template`, `structure_key`, `spawn_distance`, `spawn_bearing_degrees`, `offset_y`, entities, lifetime, and engine settings. This permits heterogeneous fleets: for example, one `ambush:cannonballoon`, one `ambush:airballoonfloat`, and one `ambush:barrelballoon` in the same encounter, or a mix of airships, boats, and cars. Fields omitted by a member inherit from the parent action. Give each member a unique `structure_key` when later actions need to target it, and do not give the formation parent its own `structure_key` or inherited members can resolve to the same assembly slot. Bearings are relative to the player's facing direction at encounter start: `0` is ahead, `90` is one side, and `-90` is the other. Use `facing: "player"` to rotate every north-authored ship toward the player.

Active ships use UUID-staggered maintenance rather than running every subsystem or every fleet member on the same tick. `maintenance_interval_ticks` is data-driven from `5` through `200` and defaults to `10`; lower values react faster but cost more CPU. Static setup stops after success, hardware is cached, child discovery runs once per second, cannon-shot routing uses a sublevel index, and general Ambush mob targeting shares one owner/entity index instead of rescanning every loaded entity for each feature.

Use `facing: "orbit_clockwise"` or `facing: "orbit_counterclockwise"` to rotate a north-authored ship tangent to its spawn radius around the player. This sets the initial propulsion direction only; Sable/Aeronautics physics still determine the actual path and do not provide automatic centripetal steering.

Named origins let later actions select one or many ships. Use `source_structure: "fleet_lead"` for one ship or `source_structures: ["fleet_lead","fleet_left","fleet_right"]` to duplicate every configured burst from all listed ships. Each scheduled burst persists its own resolved origin, so formation shell waves remain correct after a restart.

## Surround and stealth encounters

Spawn groups are sampled independently around the owner, so a group such as:

```json
{"entity":"minecraft:pillager","count":{"min":12,"max":18},"avoid_line_of_sight":true,"tags":["surrounding_attacker"]}
```

creates a distributed surround rather than placing the entire group at one coordinate. Line-of-sight rejection supports enemies that initially spawn hidden and then walk toward the player. Advanced delayed-alert and structure-approach phases remain an extension point; the current runtime provides the safe hidden spawn and owner targeting foundation.

## Complete showcase definition

The example below shows how several of Ambush's spawn, targeting, effect, and condition systems can be combined in one encounter.

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
    {"entity":"minecraft:phantom","count":{"min":3,"max":6},"avoid_line_of_sight":true,"persistent":true,"tags":["mountain_ambush"],"passengers":[{"entity":"minecraft:pillager","count":1,"persistent":true,"tags":["mountain_attacker"]}]},
    {"entity":"minecraft:evoker","count":1,"avoid_line_of_sight":true,"persistent":true,"tags":["mountain_ambush"]}
  ],
  "effects":["minecraft:darkness:2:0","minecraft:glowing:5:0"],
  "sounds":["minecraft:entity.ghast.warn","minecraft:entity.pillager.celebrate"]
}
```

## Bundled content

The mod embeds environmental, structure, weather, portal, activity-earned, and optional Sable encounters. Activity encounters can react to structure-specific kills, mined blocks and mining height, altitude crossings, timed travel, dimension transitions, biome residence, and boat residence. The bundled airship, boat, and car encounters — such as `ambush:ship_pillager_airship`, `ambush:boat_pillager_forward_autocannon`, and `ambush:pillager_potato_car` — double as working examples of every vessel action type.

The definitions use the same datapack format available to pack creators and are not hard-coded special cases. A pack creator can delete, override, disable, or replace them with a higher-priority datapack.

## Reloading and troubleshooting

1. Place JSON files under `data/<namespace>/ambushes/`.
2. Run `/reload`.
3. Run `/ambush list`.
4. Test with `/ambush <id>` or `/ambush <id> <player>`.
5. Check the server log for NeoForge reload or JSON errors.

Use namespaced IDs when a datapack has more than one source:

```text
/ambush always
/ambush ambush:overworld_portal_piglin_incursion
```

If a definition runs by name but never occurs on its own, check that natural spawning is enabled with `/ambush admin spawning status`, that its namespace has not been disabled with `/ambush disable`, and its weight and unlock state with `/ambush admin weights` and `/ambush admin unlocks`.

Use a small chance, a long cooldown, and a bounded attempt count for large multiplayer encounters. Avoid definitions that spawn hundreds of mobs, use very large radii, or create many overlapping periodic checks.

## Design limits

Ambush creates bounded encounters and targets their owner. Broader quest, claim, world-generation, and general-purpose behavior systems require their own dedicated integrations.

Living Sable encounters remain loaded through the server's final world save. Ambush does not remove their Sable force-load tickets from `ServerStoppingEvent`, because unloading active physics ships between the pause save and final save can stall shutdown. Tickets are still removed by normal encounter cleanup, `/ambush clear`, and direct Sable-family destruction.

## Directional entity and projectile waves

`directional_entity_wave` schedules timed waves from one direction or named Sable structures. Every wave accepts `after_ticks` and either one definition directly or an `entries` array. Action fields are inherited by waves and entries; children override the parent. Entry kinds are `entity`, `arrow`, and `potion`.

```json
{
  "type": "directional_entity_wave",
  "origin_offset_x": 40,
  "origin_offset_z": -50,
  "source_height": 24,
  "velocity": 1.6,
  "spread": 8,
  "target_spread": 4,
  "waves": [
    {"after_ticks": 0, "entries": [
      {"kind": "arrow", "arrow": "minecraft:spectral_arrow", "count": 8},
      {"kind": "entity", "entity": "minecraft:small_fireball", "count": 2}
    ]},
    {"after_ticks": 40, "entries": [
      {"kind": "potion", "item": "minecraft:splash_potion", "potion": "minecraft:strong_poison", "count": 3}
    ]}
  ]
}
```

Use `source_structure` or `source_structures` and `source_delay_ticks` as with directional shell rain. `start_distance` delays execution until the moving source is close enough. `spread` controls launch-point spread, `target_spread` controls the aim radius around the player, and `target_height_offset` adjusts aim height.

Arrow actions are item-driven. Set `arrow` to any registered `ArrowItem`, including vanilla arrows, spectral arrows, tipped arrows, and custom modded arrow items. For a tipped arrow, add a registered `potion`. This field works in `directional_arrow_rain`, `directional_entity_wave`, and ordinary `arrow_rain`. An unavailable optional-mod item produces no projectile and a server-log result; it does not create a dependency.

Potions use `kind: "potion"`, optional `item: "minecraft:splash_potion"` or `minecraft:lingering_potion`, and a registered `potion`. `directional_potion_rain` may also be used directly.

## Structure triggers and chance buildup

Trigger type `structure` rolls only while the player's loaded position is inside a matching structure piece. It does not locate structures, force chunks, or generate terrain. `conditions.structures` accepts IDs and tags such as `#minecraft:village`. Define reusable local lists in `conditions.structure_groups`, then select one or more with `structure_group` or `use_structure_groups`. Direct selectors and selected groups are ORed. A definition with no resolvable selector never matches.

```json
{
  "trigger": {
    "type": "structure",
    "check_every_ticks": 1200,
    "chance": {
      "mode": "build_up",
      "base": 0.02,
      "increase_on_failure": 0.015,
      "max": 0.20,
      "reset_on_success": true
    }
  },
  "conditions": {
    "structure_groups": {
      "all_villages": ["#minecraft:village"],
      "ruins": ["minecraft:trial_chambers", "minecraft:ancient_city"]
    },
    "use_structure_groups": ["all_villages"],
    "structures": ["minecraft:stronghold"]
  }
}
```

`check_every_ticks` controls the proc period (`1200` is one minute). Advanced definitions require an object-form `trigger`; chance values are probabilities from `0.0` to `1.0`. Top-level `trigger`, `interval`, `cooldown`, and `chance` fields are rejected. `mode: "flat"` always rolls `base`. `mode: "build_up"` adds `increase_on_failure` after each eligible failed roll, capped at `max`. Failure state is per player and ambush, persists in world SavedData, and resets after success when `reset_on_success` is true. Eligibility, proc interval, and cooldown are checked before a failure can increase chance. If multiple successful candidates share a cooldown group, `weight` selects one proportionally; it defaults to `100`, and `0` disables only automatic selection. Unlock definitions may add `weight_scaling` with `linear` or `multiplier` mode, `start_at`, `per_unit`, `minimum`, and `maximum`; `/ambush admin weights` reports the effective and base weight when they differ.

Unlock progress events are `kill`, `kill_at_structure`, `mine_block`, `y_level_crossing`, `travel_distance`, `portal_use`, `biome_time`, and `boat_time`. Deaths and mining are event-driven; movement, biome, crossing, and boat state are sampled once per second. Time-window progress, decimal travel, activation, and completion persist in world data. `/ambush admin unlocks` reports each unlockable definition's tracked event and progress; `/ambush admin unlockall` unlocks them for testing. See [DATAPACK_GUIDE.md](DATAPACK_GUIDE.md) for the full datapack reference.

`type: "raid"` provides vanilla-style persistent waves with one shared boss bar, a horn before every wave, named wave groups, living-member clear checks, victory presentation, restart recovery, and bounded cleanup. Set `count_toward_wave: false` on a spawn entry for passive mounts or scenery entities that must not hold the wave open. Group `on_spawn_actions` can launch directional arrow or entity rains from every successful group spawn position. The bundled `ambush:rare_thunder_bone_tempest_raid` demonstrates hidden land-only archers, non-counting skeleton-horse mounts, lightning sword troops, longer later-wave directional indicators, and a final champion.

## Sable redstone activations

Every `sable_structure` or member of a `sable_formation` may contain `redstone_activations`. Each entry activates once, is tracked in persistent Sable encounter state, and begins counting time when that individual sublevel passes transactional operational verification. Range checks use the sublevel's current projected world-space center, so moving ships activate correctly rather than using their original spawn anchor.

```json
"redstone_activations": [
  {
    "component": "analog_lever",
    "blocks": ["simulated:throttle_lever"],
    "signal": 5,
    "range": 90,
    "horizontal_only": true
  },
  {
    "component": "lever",
    "block": "minecraft:lever",
    "state": "on",
    "after_seconds": 10
  },
  {
    "component": "button",
    "blocks": ["#minecraft:buttons"],
    "button_ticks": 30,
    "after_ticks": 240
  }
]
```

Trigger fields:

- `range` or `distance`: player distance from the moving sublevel center.
- `horizontal_only`: ignore vertical distance when true.
- `after_ticks` or `after_seconds`: delay after that sublevel becomes operationally verified. The timer is persisted and does not begin until the physics body, requested seats, living operational crew, and requested hardware have passed transactional verification.
- `min_player_y` and `max_player_y`: require the owner to be inside this Y band. Y is an additional condition, so it can be combined with range/time. Define separate activation entries with separate `positions` to operate individual components at different player heights.
- `minimum_height_above_player` and `maximum_height_above_player`: require the moving ship center to be within a player-relative vertical band. Combine the minimum with `horizontal_only: true`, a short range, and `require: "all"` for an overhead payload release.
- `player_y_bands`: preferred exact-one selector. Each non-overlapping band may override `positions`, `block`/`blocks`, `signal`, `state`, and other activation fields. Exactly the first validated matching band is merged into the parent activation. `on_no_match` may be `wait` (default) or `complete`.
- `require: "any"`: default; range or time may activate it.
- `require: "all"`: when both are provided, wait until both are true.
- With no range or delay, the component activates immediately after assembly.
- `require_living_crew`: defaults to `true`. At least one living entity created by that specific Sable action must remain alive. Set it to `false` for uncrewed automation.
- Entity entries may set `fill_all_seats: true` with `seat: true` to create one mob for every available Create seat. `spawn_on_blocks` accepts block IDs or tags such as `#minecraft:planks` and places the requested extra mobs on randomly selected safe matching deck blocks.
- `player_direction`: continuously tests the owner's live direction in the moving sublevel's local frame. Values are `front`, `front_right`, `right`, `back_right`, `behind`, `back_left`, `left`, `front_left`, `above`, and `below`. `direction_tolerance_degrees` controls each sector's half-width. Combine this with `range`, `require: "all"`, `positions`, and the default crew gate to activate side-specific buttons, levers, or analog components only while living crew remains. Add `repeat_ticks` to re-evaluate and pulse a matching activation at a bounded interval; omit it for the ordinary one-shot behavior.
- `sequence_interval_ticks`: staggers a multi-button activation in array order. Four broadside buttons with `button_ticks: 20` and `sequence_interval_ticks: 30` press one for 20 ticks, wait at least 10, then press the next. The sequence resets after its final control, so a new cycle cannot overlap an unfinished one.
- `power_positions`: paired by index with `positions`, for controls whose mounted device must receive redstone power in addition to the visible button changing state.
- `cannon_alignment`: a per-cannon arc gate containing `mount_local`, `aim_local`, and `arc_degrees`. It compares the live target against a full 3-D vector in template-local coordinates starting at that cannon's mount, so each weapon fires only inside its own arc. Use one activation per weapon when arcs differ.
- Initial `throttle_signal`/`throttle_signal_by_y` application is crew-gated too. Set `throttle_requires_living_crew: false` only for intentionally autonomous ships.

Component fields:

- `component: "analog_lever"`: sets `signal` from `0` through `15`. Ambush supports Simulated throttle levers reflectively and other analog block entities exposing a conventional signal/power/level setter. It also supports integer block-state properties named `power`, `signal`, `level`, or `strength`.

### Live steering-wheel controls

Sable actions may include `steering_controls`. Each control continuously calculates the owner's position in the moving ship's local frame and applies a target angle to matching steering-wheel block entities. This is live tracking: as the ship turns or the owner moves from one side to another, the selected angle changes.

```json
"steering_controls": [{
  "block": "simulated:steering_wheel",
  "range": 256,
  "horizontal_only": true,
  "require_living_crew": true,
  "update_ticks": 10,
  "mode": "continuous",
  "max_angle": 45,
  "behind_direction": "last"
}]
```

- `mode: "continuous"` converts the exact live horizontal bearing into the steering wheel's mechanical −45° through +45° range. Right is negative (`−45` through `0`) and left is positive (`0` through `45`) by default. A player slightly to one side produces a small proportional turn; a player at the side or behind produces a full 45° turn. A directly-behind target uses the previous turn side by default, making the ship continue circling instead of oscillating.
- `behind_direction` accepts `last` (default), `left`, or `right`. `behind_lateral_deadzone` controls how close to directly behind the target must be before that rule applies.
- `mode: "sectors"` selects `front`, `front_right`, `right`, `back_right`, `behind`, `back_left`, `left`, or `front_left`. Optional `above` and `below` mappings can be supplied; when a custom mapping omits them, the wheel retains its previous angle while the owner is vertically outside the horizontal sectors.
- `direction_angles` is data-driven and may map each sector to any angle from −45° through +45°. `max_angle` may reduce but cannot exceed the wheel's 45° mechanical limit.
- `invert`, `angle_scale`, and `angle_offset` transform the selected angle before clamping. `invert_with_propulsion_direction` defaults to the action-level `steering_follows_propulsion_direction`, which defaults to `true`.
- `range`, `horizontal_only`, `require_living_crew`, `update_ticks`, `block`/`blocks`, and optional local `positions` control when and which wheels update. Crew gating defaults on.
- Controls operate independently for every member of a formation, so each ship steers using the owner's live position relative to that individual ship.
- If a craft turns consistently the wrong way, verify `schematic_front` first, then use `invert`.

### Propulsion direction

Set propulsion direction independently on a Sable structure or formation:

```json
"engine_direction": "reverse",
"propeller_direction": "forward",
"steering_follows_propulsion_direction": true
```

Both direction fields accept `forward` and `reverse`. `engine_direction` changes each portable engine's real rotation-direction behavior. `propeller_direction` changes each Aeronautics propeller bearing's thrust handedness; small or smart propellers driven by a portable engine should use `engine_direction` instead.

Direction may also track the player's live position relative to each moving ship:

```json
"engine_direction": "forward",
"engine_direction_by_player_direction": {
  "default": "forward",
  "back_right": "reverse",
  "behind": "reverse",
  "back_left": "reverse"
}
```

`engine_direction_by_player_direction` and `propeller_direction_by_player_direction` accept `default`, `front`, `front_right`, `right`, `back_right`, `behind`, `back_left`, `left`, `front_left`, `above`, and `below`. Every value is `forward` or `reverse`. An omitted sector uses `default`, then the static direction field. Direction is evaluated in the individual ship's current local frame and propulsion components are updated only when the resolved direction changes.

When propulsion-aware steering is enabled, the wheel angle reverses if exactly one of the engine or propeller directions is reversed. It does not reverse when both or neither are reversed. This exclusive-or rule prevents double reversal. Set action-level `steering_follows_propulsion_direction: false`, or control-level `invert_with_propulsion_direction: false`, to disable automatic compensation. The bundled boss ships run forward while the owner is ahead or beside them, switch to reverse in rear sectors, and compensate their steering automatically.

- `component: "lever"`: sets a vanilla lever. `state` may be `on`, `off`, or `toggle`.
- `component: "button"`: presses a vanilla button and schedules its release. `button_ticks` controls the pressed duration.
- `component: "any"`: handles vanilla levers/buttons and compatible analog components.
- `block` accepts one exact block ID. `blocks` accepts IDs and block tags.
- Without `positions`, every matching component in the sublevel plot is activated. `positions` limits activation to schematic-local `[x,y,z]` coordinates or `{x,y,z}` objects measured from the plot minimum. Set `absolute_positions: true` only when intentionally using Sable plot coordinates.

Neighbor updates are emitted inside the sublevel after activation. Missing optional analog-lever mods do not prevent Ambush from loading; unmatched entries produce a bounded server warning and are marked complete rather than making another attempt forever. The two bundled boss examples use the real throttle lever in their Sable ships; imported schematics can use the same vanilla lever/button syntax for controls that contain those blocks.

## Sable block health and boss bars

A Sable structure can expose its blocks as a data-driven boss health bar. Ambush captures the occupied-block baseline once after assembly. Health is `original baseline positions not yet observed as destroyed / original occupied positions`. Once an original position is observed as air it is permanently damaged for that encounter, so repairing or replacing it cannot heal the bar. A block replaced before a health scan observes the empty state still counts as intact. The scan is shared by boss bars, percentage events, and `destroyed_cleanup_percent`, and retains the 262,144-position safety cap.

Ongoing health checks use a persisted round-robin cursor. `health_scan_budget` requests `1` through `1024` block checks for that ship on its staggered maintenance turn and defaults to `256`. Large ships and fleets therefore update progressively instead of repeatedly scanning every ship in full.

Health covers the owned parent and split children correlated through Ambush's Sable split listener. Child lineage and cleanup deadlines are persisted, so a restart does not discard scheduled fragment cleanup. Inline reinforcement ships remain separate encounter actions unless they share a fleet-health ID.

Split fragments have an explicit data-driven cleanup policy:

```json
"split_fragment_cleanup": {
  "mode": "timed",
  "despawn_after_ticks": 200
}
```

- `mode: "timed"` removes each newly split Sable child after `despawn_after_ticks` (minimum 20 ticks). The deadline persists across restarts.
- `mode: "normal"` or `"disabled"` leaves the fragment to Sable's normal lifecycle.
- The older `split_off_despawn` and `split_off_despawn_ticks` fields remain accepted for existing datapacks.
- The policy is inherited by formation members unless that member overrides it.

```json
"boss_bar": {
  "name": "Hostile Cannon Balloon",
  "color": "red",
  "overlay": "notched_10",
  "visibility": "owner",
  "range": 128,
  "darken_screen": false,
  "play_music": false,
  "create_world_fog": false
}
```

- `boss_bar: true` enables defaults. Visibility defaults to `nearby`, so every player in the configured range sees the same live bar; use `visibility: "owner"` only for an owner-only bar.
- `name` is literal display text.
- `color`: `pink`, `blue`, `red`, `green`, `yellow`, `purple`, or `white`.
- `overlay`: `progress`, `notched_6`, `notched_10`, `notched_12`, or `notched_20`.
- `visibility: "owner"` shows it only to the encounter owner.
- `visibility: "nearby"` shows it to players within `range` of the moving structure.
- `visibility: "all"` shows it to every player in that dimension.
- `darken_screen`, `play_music`, and `create_world_fog` expose the corresponding vanilla boss-bar flags.

Boss-bar viewers and progress are synchronized while the sublevel is active. The bar is removed when the structure is cleaned up, disappears, or the server stops. Every formation member may override `boss_bar`, including its name and visibility, so a fleet may have one bar per ship or bars only on selected members.

## Sable lifecycle and block-percentage events

`sable_events` is the general one-shot/repeating event system for assembled sublevels. Each entry needs a stable unique `id`, a `trigger`, and an `actions` array. Fired one-shot IDs and repeating timestamps persist in world SavedData. Event actions are inserted into the persisted Ambush action queue with deterministic IDs and execute on the following server tick, outside Sable's lifecycle iteration. The queue drains at most 64 due actions per tick.

```json
"sable_events": [
  {
    "id": "spawn_horn",
    "trigger": {"type": "spawn"},
    "actions": [
      {"type": "sound", "sound": "minecraft:entity.pillager.celebrate", "volume": 1.5, "pitch": 1.0}
    ]
  },
  {
    "id": "health_50_reinforcements",
    "trigger": {"type": "block_percent", "at_or_below_percent": 50},
    "actions": [
      {"type": "sound", "sound": "minecraft:entity.evoker.prepare_summon"},
      {"type": "ambush", "ambush": "my_pack:reinforcement_wave", "force": true}
    ]
  },
  {
    "id": "health_25_support_ship",
    "trigger": {"type": "block_percent", "at_or_below_percent": 25},
    "actions": [
      {
        "type": "sable_structure",
        "template": "my_pack:support_ship",
        "assembly_origin": [4, 1, 5],
        "structure_key": "support_ship",
        "placement": "air",
        "spawn_distance": 100,
        "offset_y": 30
      }
    ]
  },
  {
    "id": "destroyed_sound",
    "trigger": {"type": "death"},
    "require_living_crew": false,
    "actions": [
      {"type": "sound", "sound": "minecraft:entity.generic.explode", "volume": 2.0}
    ]
  }
]
```

Trigger types:

- `spawn`: fires after the sublevel and configured entities have completed initial setup.
- `range`: fires when the owner reaches `range`/`distance`; supports `horizontal_only`.
- `time`: fires after `after_ticks` or `after_seconds` from completed assembly.
- `player_y`: fires while the owner is between `min_y` and `max_y`.
- `block_percent`, `health_percent`, or `percent`: fires when block health reaches or falls below `at_or_below_percent`/`percent`.
- `death` or `destroyed`: fires when block health reaches zero or when `destroyed_cleanup_percent` declares the structure destroyed.
- `repeat_ticks`: when positive, permits spawn/range/time/player-Y events to fire repeatedly at the specified interval while their trigger remains true. Percentage and death events are always one-shot; datapack reload rejects `repeat_ticks` on them.

Use death-triggered events for reward or final cleanup behavior rather than a low block-percent threshold. When one damage scan crosses several percentage thresholds, Ambush evaluates every newly eligible threshold in descending percentage order and queues each one exactly once.

Event actions:

- `sound`: plays a registered sound at the moving sublevel center. Supports `volume`, `pitch`, and `audible_distance`. When `audible_distance` is present, Ambush projects the playback point onto the live ship-to-player line at that distance from the player. The sound therefore remains nearby and audible while its stereo direction still points toward the actual ship. Use `at: "player"` only when direction is not wanted.
- `particle`: emits a registered vanilla or modded particle at the moving structure center, including from block/health-percentage events. `particle` accepts normal particle command syntax; `count` is 1-4096, `spread_x`, `spread_y`, `spread_z` control the volume, and `speed` controls particle motion. Use `at: "player"` for the target position or `audience: "owner"` to send only to the originally ambushed player.
- `redstone`: accepts the same component, block/tag, signal, state, button duration, and schematic-local position fields as `redstone_activations`. An optional nested `activation` object is also accepted.
- `ambush`: triggers another loaded definition using `ambush` or `id`. `force` defaults to `true`; set it false to honor the referenced ambush's conditions. Persisted generation depth prevents delayed recursion beyond eight generations.
- `sable_structure`, `sable_boat`, `sable_car`, or `sable_formation`: queues an inline additional vessel or fleet using the normal Sable schema. Child lineage and generation depth persist across restarts. `max_generation_depth` remains datapack-controlled; AMBUSH applies a shared limit of 32 active ships per owner.
- `fog`: applies the ordinary per-player fog action.

Events default to `require_living_crew: true`: at least one living entity spawned by that exact Sable action must remain alive. Configured living entities default to `crew: true`; set `crew: false` on cargo, prisoners, decorative mobs, or other living passengers that should not keep a ship operational. Seats never count as crew. `seat: true` riders are tracked separately as requested seats; unseated deck crew remain operational crew and do not need to be passengers. Debug output reports `seated=X/Y` separately from `livingCrew=A/B`. This prevents abandoned ships from firing weapons, calling reinforcements, or activating redstone. Death/destroyed events default to `false` so their final sounds and effects can still run. Override the field on an event when needed; individual nested actions can additionally set `require_living_crew: true`.

Sounds at spawn, range, percentage, and death are all ordinary `sound` actions under the corresponding trigger, rather than separate hard-coded sound fields. This allows multiple sounds, chained actions, and per-event crew gating.

## Per-player fog actions

The `fog` action changes vanilla fog only for the targeted player. New fog replaces that player's previous Ambush fog. It is cleared on expiration, login/logout, respawn, or dimension change. By default it does not replace water, lava, or powder-snow fog.

```json
{
  "type": "fog",
  "near_distance": 2,
  "far_distance": 30,
  "color": "#58636f",
  "duration_seconds": 20,
  "fade_in_ticks": 40,
  "fade_out_ticks": 60,
  "shape": "sphere",
  "override_fluid_fog": false,
  "after_ticks": 0
}
```

- `near_distance` or `fog_start`: distance where fog begins.
- `far_distance` or `render_distance`: distance where fog becomes opaque and world rendering is hidden.
- `color`: `#RRGGBB` or an array such as `[88,99,111]` or `[0.35,0.39,0.44]`. Separate `red`, `green`, and `blue` fields are also accepted.
- `duration_ticks` or `duration_seconds`: duration, capped at 1,728,000 ticks (24 real-time hours at 20 TPS).
- `fade_in_ticks` and `fade_out_ticks`: smooth transition durations.
- `shape`: `sphere` or `cylinder`.
- `override_fluid_fog`: also replace water/lava/powder-snow fog when true.
- `clear: true`: immediately removes active Ambush fog for that player.

Fog actions support ordinary action `conditions`, `after_ticks`, and persisted scheduling. Fog-only ambushes are valid: accepted actions now count as encounter success for cooldown, chance reset, and command confirmation. The server and client must both run this Ambush build because fog uses an Ambush network payload. Shader mods may reinterpret vanilla fog, so test the target shader stack separately. The bundled fog examples exercise timed fog independently of optional boss structures.

## Bundled boss example

The bundled boss encounter applies fog before assembly so the reveal is not delayed by Sable assembly. Its horn uses `audible_distance: 8`, placing the sound eight blocks from the player along the live direction toward the ship. `fireworks` is a lifecycle or ordinary action with `count`, `height`, `spread`, `flight` (1-3), `shape`, `colors`, and `fade_colors`; the boss emits twelve large, twinkling purple-and-gold rockets when destroyed.

Use it together with the other bundled definitions the mod loads from `data/ambush/ambushes/`. Copy a definition into a pack-owned namespace before changing it.

## Schema-hardening fields

The following fields remove the need for helper definitions and manual coordinate calculations.

### Absolute placement and distant assembly

- `absolute_y`: places a `structure`, `sable_structure`, or formation member at an exact world Y. It overrides `offset_y` only for the Y coordinate; horizontal distance, bearing, and X/Z offsets still apply.
- `load_chunks_for_placement` has been removed and is rejected during reload because bounded ship footprint and Sable plot chunks are now always loaded by the one-pass assembler.
- `air_search_direction`: `up`, `down`, or `both` (default). `air_search_attempts`, `air_step`, and `air_search_rings` bound the obstruction search. The search never leaves build height and keeps the template-volume safety cap.

### Formation inheritance and fleet health

Formation members are isolated by default. They inherit spatial, lifetime, cleanup, facing, steering, and fleet fields, but do not silently inherit `entities`, `sable_events`, or `container_loot`.

`ship_ai.mode: "orbit"` uses the broadside distance controller continuously so a powered, steerable ship circles the player with its selected side facing inward. Configure `distance`, `distance_tolerance`, `correction_range`, and `side`; fleets should use separate distances when members need concentric paths. A balloon without horizontal propulsion can hover above a target but cannot orbit.

- `members_inherit_parent: true`: restores complete parent inheritance for all members.
- Per member, `inherit_parent` overrides the formation default.
- Per member, `inherit_entities`, `inherit_events`, and `inherit_loot` independently control the three high-risk payload fields. A member's explicitly supplied payload is always retained.
- `fleet_health: true` creates one shared block-health group and makes the first member its declared event/boss-bar leader. The runtime honors `shared_boss_leader` instead of choosing a leader by random action UUID. The boss bar is keyed to the owner and shared group, so losing one member cannot leave an action-keyed orphan bar or rename the group accidentally.
- `fleet_health` may instead be an object containing `boss_bar`, `events`, and `cleanup_at_percent`. The generated group is private to that particular fleet spawn. An explicit `shared_boss_id` remains supported when several separately declared actions must share health. Formation-member lifecycle fields and `fleet_health.events` receive the same recursive reload validation as top-level Sable actions.
- `cleanup_at_health_percent` cleans up when remaining block health reaches that percentage. Prefer a final `{"type":"cleanup"}` lifecycle action when cleanup must happen after other actions at the same threshold.
- `boss_health_mode` selects `blocks` (default), `entities`, or `combined`. Entity health uses the configured crew's captured maximum health and current living health; combined health adds block and crew pools. Percentage events and fleet aggregation use the selected pool.

### Seat and block predicates

`seat_predicate`, `seat_predicates`, and object entries in `spawn_on_blocks` use the same matcher:

```json
{
  "block": "create:white_seat",
  "tag": "namespace:allowed_seats",
  "properties": {"facing": "north"},
  "block_entity_nbt": "{Color:0b}"
}
```

All supplied fields must match. `block` can also be a `#namespace:tag` string. `block_entity_nbt` is a partial recursive NBT match, so a predicate can select color or another stable block-entity field without requiring an exact copy of unrelated NBT. `seat_predicates` is an OR list. Ambush records each selected seat position on its rider and preserves that assignment through Sable's delayed seating pass. Crew placement is entirely datapack-owned: there are no mob-type seat defaults, and with `seat: true` and no selector a mob uses every detected seat. Always inspect the actual block state and block-entity NBT produced by the installed Create/Sable versions before writing a predicate.

### Persistent lifecycle actions and inline reinforcements

Every Sable lifecycle event accepts the ordinary rain and wave actions: directional CBC shells, directional arrows, potions, entities, vertical rains, `entity_wave`, `conditional_spawn`, sounds, fog, fireworks, effects, Sable structures, and formations. Nested `after_ticks` or `delay_ticks` is stored in world saved data, so restarting the server does not discard the delay.

- `inline_ambush` contains an `actions` array or a `definition` object and executes it without registering another top-level command ID. Use it for private reinforcement waves or fleets.
- `sable_structure` and `sable_formation` inside an event are also private inline actions and do not require helper encounter files.
- `cleanup` requests owner-correct cleanup of the source ship or its shared-health fleet. Put cleanup after projectiles, rewards, sounds, or reinforcements in the event action array.
- Generation depth and active-ship caps still apply to recursive reinforcement content.
- Persisted actions use bounded attempts with exponential delays. Their attempt
  count, source ship, and maximum attempts survive restart. Set `max_attempts`
  on a nested action from `1` through `200` (default `20`). Automatic
  health cleanup waits for source-linked lifecycle actions to finish.
- Top-level entity waves and CBC bursts use the same persisted scheduler; they are no longer memory-only.

### Combined container loot

Each `container_loot` rule may use `loot_tables` plus `combine: true`. Ambush rolls every listed table and inserts the combined generated stacks into empty slots of each matching unopened container. `replace_existing: true` clears the inventory first. Rules remain ordered; use one combined rule when several tables must contribute to the same container rather than relying on several rules that overwrite one another.

```json
{
  "blocks": ["minecraft:barrel"],
  "loot_tables": ["minecraft:chests/simple_dungeon", "minecraft:chests/pillager_outpost"],
  "combine": true,
  "replace_existing": true,
  "seed": 42
}
```

A rule may also target exact schematic-local `positions`, which is the reliable form when several containers need different tables. Loot tables live at `data/<namespace>/loot_table/<path>.json` and must ship in the same active pack as the definition.

Hoppers are ammunition and automation inventories. Fill them with `initial_hopper_contents` (or `hopper_contents`) for a fixed stack list, or with a `container_loot` rule targeting their positions.

### Optional Lootr containers

When Lootr is installed, vessel loot containers are converted to per-player containers so every player who boards gets their own roll of the table instead of the first one aboard taking everything. Ambush has no compile-time dependency on Lootr: when Lootr is absent the same authored table is applied through the ordinary path and nothing about the definition changes.

Conversion is **on by default** for vessel containers. `lootr_compatibility` on a `sable_structure`, `sable_boat`, `sable_car`, or `sable_formation` action sets the default for every `container_loot` rule on that action; `lootr` on one rule overrides it.

```json
"lootr_compatibility": true,
"container_loot": [
  {"positions": [[6, 3, 4]], "loot_table": "my_pack:captains_chest"},
  {"positions": [[4, 2, 1]], "loot_table": "my_pack:ammunition", "lootr": false, "replace_existing": true}
]
```

Hoppers are never converted under any setting, because replacing one would break the machine it feeds. A rule with more than one loot table is never converted either, since Lootr holds a single table per container; use one `loot_table` for any container that should be per-player.

Conversion happens inside the assembled plot rather than at the template's dimension coordinates, because the container is on a Sable sub-level while the vessel is moving. If Lootr is absent, reports itself not ready, declines the block, or the conversion fails at any step, Ambush restores the original block and applies the identical authored table through the ordinary path. The physical chest is never deleted and the loot is never lost. `replace_existing` applies only on that ordinary path — a converted container takes its whole contents from the table. Each container is filled once and recorded in persistent state, so a restart does not re-roll a chest a player already found. Unsuccessful conversions are logged at DEBUG level.

### Steering, crew aiming, and safe throttle caps

Seated Ambush mobs retain their normal weapon and vanilla goals. Ambush transforms the live player direction into Sable plot space before updating their target and look direction, allowing crossbow or compatible modded-weapon behavior to aim from a moving sublevel without replacing the held item. `aggro_range` remains data-driven, and owner-tag friendly-fire cancellation remains enabled when `friendly_fire` is false.

`engine_stress_management` provides a deterministic throttle safety cap:

```json
"engine_stress_management": {"mode":"capped", "maximum_signal":4}
```

The only supported mode is `capped`. The resolved Y-level throttle is never allowed above `maximum_signal`. This is intentionally deterministic: Ambush does not guess an engine's safe capacity from undocumented internals. Datapack authors must select a cap proven safe for the schematic. Steering still requires compatible, correctly oriented hardware in the schematic; data can control or validate existing hardware but cannot manufacture a missing propulsion system.

`chase_controller` adds live owner pursuit without replacing Sable physics:

```json
"chase_controller": {
  "target_range": 320,
  "stop_distance": 28,
  "resume_distance": 36,
  "throttle_signal": 4,
  "idle_signal": 0,
  "maximum_signal": 4,
  "update_ticks": 10,
  "max_angle": 45
}
```

It supplies continuous steering toward the owner with stop/resume hysteresis.
A meaningful gap between the stop and resume distances prevents repeated
direction changes. The stress-management cap limits every analog signal.

`altitude_controller` determines the analog signal from the ship's live world
height instead of selecting a fixed band from the player's height:

```json
"altitude_controller": {
  "mode": "player_offset",
  "offset": 12,
  "tolerance": 4,
  "gain": 0.16,
  "minimum_signal": 0,
  "hover_signal": 3,
  "maximum_signal": 4,
  "max_signal_step": 1,
  "failsafe_signal": 3
}
```

`player_offset` targets `player Y + offset`. `absolute` uses `target_y`.
Within `tolerance`, the controller uses `hover_signal`; outside it, the signal
changes proportionally by `gain` and is clamped. Existing
`throttle_signal_by_y` remains the fallback when no altitude controller is
declared. `max_signal_step` limits each update to prevent analog oscillation.
If the owner, pose, or living crew is unavailable, steering returns to zero
and the analog controls move toward `failsafe_signal`. Additional explicit
safety fields include `ground_clearance_enabled`,
`minimum_ground_clearance`, `descent_arrest_enabled`,
`descent_arrest_margin`, `velocity_lookahead_ticks`, `integral_gain`, and
`integral_limit`.

`ship_ai` changes horizontal steering behavior:

```json
"ship_ai": {
  "mode": "broadside",
  "distance": 52,
  "distance_tolerance": 8,
  "correction_range": 32,
  "height_offset": 12,
  "side": "alternate",
  "target_range": 384
}
```

- `chase` points the bow toward the player.
- `tnt_drop` uses direct approach steering, holds at the altitude-controller offset, and stops steering once it reaches the configured `stop_distance`; pair it with an overhead redstone activation to release a falling payload.
- `broadside` keeps the selected left or right side toward the player. When
  outside the distance band it biases the bow inward or outward, producing a
  bounded orbit instead of simply stopping.
- `boarding` uses the same side-on controller with a shorter preferred
  distance, allowing the ship to approach alongside.
- `overhead` and `flyover` are `sable_structure`-only and require an
  `altitude_controller`. `overhead` steers to the player's X/Z position and
  holds there; `flyover` steers through it without the overhead stop. Author
  altitude with `match_player_y: false` and an `altitude_controller.offset`.
- `side` accepts `left`, `right`, `nearest`, or stable per-ship `alternate`.
- `range_holding_thrust_reversal: true` enables range-holding reverse thrust; supply distinct `reverse_at_distance` and `resume_forward_at_distance` for hysteresis. `recovery_turn_enabled: true` enables the full-lock recovery turn.

`height_offset` is also the default player-relative altitude offset when an
altitude controller omits `offset`. These controls command existing Simulated
analog and steering hardware; they do not teleport the ship or bypass Sable
physics. `hardware_requirements` can fail closed and clean up an incompatible
schematic after assembly. To fire mounted cannons downward or diagonally,
physically aim the schematic's mount that way and gate it with a per-cannon
`cannon_alignment` vector; Ambush does not rotate cannon hardware.

### Placement and optional-runtime safety

Every Sable footprint chunk is checked, not only the corners. `absolute_y` locks vertical search while still allowing bounded horizontal obstruction search. Non-air placement defaults to `require_clear_volume: true`. Failed staging cleanup removes only Ambush-tagged entities and positions recorded as changed in that placement receipt; it never clears the complete bounding box.

Sable assembly is capability-gated on both Sable and Simulated. Seat requests additionally require Create. Missing optional integrations fail the action instead of creating a permanently throwing lifecycle entry. Generic Ambush encounters remain usable without any of these mods.

## Cowardice and encounter audio

A boss or main Sable action may define `cowardice`. When that structure reaches its configured cleanup threshold, Ambush marks the owner's surviving Ambush mobs as cowards, clears their targets, makes their navigation flee the nearest player, and shortens their despawn deadline. Surviving Sable actions receive the same shortened lifetime and may steer away from the player.

```json
"cowardice": {
  "enabled": true,
  "despawn_after_ticks": 600,
  "speed": 1.25,
  "ships_run_away": true,
  "ship_throttle_signal": 2,
  "include_shared_boss_members": false
}
```

`encounter_audio` repeats a registered sound event while its lifecycle anchor remains alive. On a Sable action it runs until that ship/fleet leader is cleaned up. At definition root it tracks a living owner-tagged entity carrying `main_entity_tag` (default `ambush_main_entity`); add that tag to the intended main spawn. `repeat_ticks` should match the resource-pack sound's length. Custom music is supplied by a normal resource pack (`sounds.json` plus `.ogg`) and referenced by its registered sound ID; datapacks cannot reference arbitrary computer files.

```json
"encounter_audio": {
  "sound": "yourpack:music.airship_battle",
  "main_entity_tag": "ambush_main_entity",
  "repeat_ticks": 2400,
  "audience": "nearby",
  "volume": 1.0,
  "pitch": 1.0
}
```

## Optional CBC cannon assembly and first load

On CBC 5.11.7, Ambush can power a ship's cannon-assembly controls and load its
mounted big cannons through CBC's public mounted-contraption, hand-loading, and
sync APIs. The optional integration is inert when CBC is absent and fails
closed on an unverified CBC version.

Finalization is a staged pipeline: one hardware scan feeds requirements, loot,
hoppers, cannon assembly, per-mount cannon loading, crew, and propulsion.
Every stage and every container, hopper, crew spawn, and cannon mount has a
persistent checkpoint. A later attempt resumes the unfinished unit without duplicating
loot, mobs, or ammunition.

Use `cannon_assembly` to power native controls before loading:

```json
"cannon_assembly": {
  "component": "levers",
  "signal": 15,
  "required": true
}
```

`power_mounts_directly: true` sends an explicit compatibility pulse for a
schematic whose CBC mount needs it instead of its authored redstone wiring; it
defaults to `false`. `cannon_assembly.force_mount_assembly: true` is an
additional explicit recovery option for a mount saved powered but without a
live contraption.

Use `initial_cannon_loads` for the first shot. `mount_local` is measured from
the Sable plot minimum. Set `apply_to_all: true` to load every discovered fixed
cannon mount with one rule. `initial_cannon_load_after_ticks` preloads
hand-loaded guns once the vessel is operational, independently of range and
aim, so the guns can stand by while the vessel approaches.

```json
"initial_cannon_loads": [{
  "id": "first_shot",
  "apply_to_all": true,
  "required": true,
  "shell_stack_snbt": "{id:\"createbigcannons:solid_shot\",count:1}",
  "propellant_stack_snbt": "{id:\"createbigcannons:big_cartridge\",count:1,components:{\"createbigcannons:power\":1}}"
}]
```

Use only `propellant_stack_snbt` for cannon propellant; powder charges are
intentionally unsupported. Occupied, damaged, unsupported, or unassembled
cannons are never overwritten. Normal native redstone controls fire the
completed first load. Post-shot automatic reload uses `cannon_reloads` and
`cannon_reload_delay_ticks`; it is a separate compatibility path and is not
required by the bundled cannon barge.

For independently aimed mounts, define one `redstone_activations` entry per
cannon, pair its real button with `cannon_alignment`, and add
`fire_mount_local` when moving-hull wiring does not reliably deliver a CBC fire
edge. Ambush then pulses both the visible button and that mount's native fire
input. Never use one broad activation as a substitute for several differently
aimed cannons. CBC-related diagnostics are INFO-level and report assembly,
preload status, range/alignment skips, button pulses, direct mount activation,
successful shots, and reloads.

## Optional FTB Quests hooks and contract items

FTB Quests is optional. Ambush loads without it, and these fields and tags are
ignored when it is not installed.

Tag an FTB quest with `ambush:trigger:<namespace:id>` to start that definition
when the quest starts. By default, online team members are sources and targets.
Optional target tags are `ambush:target:self`, `ambush:target:player:<name>`,
`ambush:target:nearest_other`, `ambush:target:random_online`, and
`ambush:target:all_other`.

An encounter can complete tagged tasks at start, success, or failure:

```json
"ftb_quests": {
  "on_start_task_tag": "ambush:pirates_started",
  "on_success_task_tag": "ambush:pirates_defeated",
  "on_failure_task_tag": "ambush:pirates_failed"
}
```

`failure_events` tracks the ordinary mobs and Sable vessels created by an
encounter. It succeeds when `minimum_kill_percent` is met before
`timeout_ticks`; otherwise its actions run once. Supported failure actions are
server commands and chained Ambush definitions. The command runs as the server
at permission level 2, `{player}` is replaced with the target's name, and
chained ambushes still use the normal chain-safety checks.

```json
"failure_events": {
  "timeout_ticks": 12000,
  "minimum_kill_percent": 75,
  "actions": [
    {"type": "command", "command": "say {player} failed the convoy"},
    {"type": "ambush", "ambush": "my_pack:reinforcements"}
  ]
}
```

`quest_contract` is an action-level object that lets a player start an
encounter by using an item and choose its target by naming the item. It works
without FTB Quests; any means of giving the item works.

```json
"quest_contract": {
  "item": "minecraft:paper",
  "attempt_interval_ticks": 600
}
```

`item` is the required item registry ID. `attempt_interval_ticks` is the retry
interval while the encounter cannot yet spawn; it defaults to `600` and must be
between `20` and `72000`. The item must carry `minecraft:custom_data` with an
`ambush_contract` string equal to the declaring definition's own ID. If two
definitions claim the same item with different IDs, the contract is ignored and
a warning is logged.

The player renames the item in an anvil to an online player's exact name, then
right-clicks it. An unrenamed contract or an offline target prints a message
and consumes nothing. On a successful claim the item is consumed and Ambush
retries the encounter against that player every `attempt_interval_ticks` until
it can spawn safely. Retries use the definition's normal conditions, so a
contract for a night-time encounter waits for night, and a pending contract
survives the target logging out.

```text
give {p} minecraft:paper[minecraft:custom_data={ambush_contract:"my_pack:cannon_fleet"},minecraft:custom_name='{"text":"Cannon Fleet Contract — Rename to Target","italic":false}'] 1
```

### External boundaries

Ambush can safely validate IDs, placement, loaded chunks, and its own payloads, but it cannot force a shader to honor vanilla fog or infer undocumented schematic semantics. Fog therefore still requires the same Ambush network protocol on client and server, and shader behavior must be tested with the target graphics stack. Schematic resources are immutable datapack resources: replacing a source schematic outside the pack does not replace the copied `data/<namespace>/structure/<name>.nbt` file.
