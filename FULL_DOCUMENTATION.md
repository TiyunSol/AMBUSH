# Ambush

Ambush is a server-side NeoForge 1.21.1 mod for configurable, per-player hostile encounters. Encounter definitions are data-driven JSON files loaded from every server datapack under:

```text
data/<namespace>/ambushes/<id>.json
```

Create, Create Aeronautics/Simulated, Sable, and Create Big Cannons are optional integrations. Generic entity, sound, effect, and vanilla rain datapacks work without them. Sable actions fail closed unless the required Sable runtime is available. Ambush contains its own guarded Sable assembly adapter and does not require an external assembly helper mod.

## Installation

Install `ambush-0.1.2.jar` on the server and connecting clients. It is intended to be distributed with a normal CurseForge modpack. Definitions may be bundled inside the mod or supplied by a separate datapack. Put a datapack in a world’s `datapacks` directory, run `/reload`, and the definitions become available without rebuilding the mod.

The mod does not generate chunks, edit other mods’ files, or require a scripting platform.

## Commands

Commands require permission level 2.

```text
/ambush list
/ambush validate
/ambush state
/ambush <id>
/ambush <id> (player)
```

Examples:

```text
/ambush always ambush:example_boss
/ambush always ambush:example_boss_fleet (player)
/ambush always ambush:example_cannon_balloon_fleet (player)
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

They are assembled asynchronously through Ambush's guarded Sable adapter. `template` is any namespaced structure ID, so datapacks may add new aircraft without rebuilding Ambush. Static blocks and retained entities such as Simulated honey glue must remain in the NBT; do not save an already-assembled Create contraption or nested Sable sublevel.

```json
"actions":[{
  "type":"sable_structure",
  "template":"my_pack:airship/hostile_balloon",
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
- `offset_x`, `offset_y`, `offset_z`: world anchor relative to the targeted player's block position.
- `spawn_distance`: optional radial horizontal distance from the player. When present, Ambush chooses an angle around the player; `spawn_angle_degrees` can fix that angle. `offset_y` still controls height.
- `spawn_bearing_degrees`: formation-member bearing relative to the player facing. It makes a fleet's position reproducible while keeping the saved structure's facing data-driven.
- `placement`: use `air` to require a clear, already-loaded template-sized air volume. Ambush searches upward from the configured offset without generating chunks.
- `air_search_attempts` and `air_step`: bounded upward search controls for `placement: "air"`; defaults are `8` attempts and `4` blocks.
- `yaw_degrees`: rotation applied after assembly.
- `base_facing`: direction the saved template points before rotation; defaults to `north`.
- `facing` or `direction`: desired `north`, `east`, `south`, `west`, or `player`. `face_player: true` is an equivalent convenience option. Explicit direction fields supersede `yaw_degrees`.
- `max_retries`: bounded assembly attempts; default `5`, maximum `20`.
- `lifetime_ticks`: cleanup delay after successful assembly. The default is `6000` ticks (5 minutes). Use JSON `null`, `"none"`, or `"permanent"` for no automatic cleanup. The deadline and pending cleanup survive world/server restarts.
- `destroyed_cleanup_percent`: optional percentage of the initially occupied parent-and-child Sable blocks that may be destroyed before the remaining structure is cleaned up. Values above `100` clamp to `100`; omit it, use `0`, `null`, `"none"`, or `"disabled"` to disable damage cleanup. The occupied-position baseline and cleanup state survive restarts, and added blocks cannot offset destroyed original blocks.
- Cleanup removes Ambush-created fixed constraints first, unregisters the Aeronautics flyover, releases command force-load tickets, and directly removes the complete Sable family child-first. Initial child sublevels, later split fragments, and the parent are all included; nearby-player fragment-promotion rules do not preserve Ambush debris after threshold cleanup.
- `despawn_effect`: optional data-driven effect played immediately before timed or damage-threshold cleanup. `"explosion"` or an object with `type: "explosion"` is supported. `power` defaults to `3.0`; `fire` and `block_damage` default to `false`. Omit it or use `"none"` for silent cleanup.
- `attach_child_sublevels`: when `true`, Ambush creates fixed Sable constraints between the parent and persistent child bodies, such as converted vanilla barrels. Constraint anchors are persisted and rebuilt after server or world restarts. Leave this false for intentionally detachable physics cargo.
- `envelope_fill`: optional initial Aeronautics balloon fill fraction from `0.0` to `1.0`. It is applied once after the balloon graph initializes; burners and leaks control the gas normally afterward.
- `cannonballoon_flight_profile`: defaults to `true` for `template: "ambush:cannonballoon"` and may be set to `false` to opt out. The profile starts at fill `1.0`, rejects underground targets, and maps the player Y level to Simulated analog throttle: `<=80` is `3`; `>=81` is `4`.
- `engine_burn_ticks` or `engine_burn_seconds`: initial remaining burn time applied to every Simulated portable engine in the assembled parent. `3000` ticks is 2.5 minutes.
- `engine_superheated`: optional boolean. When `true`, Ambush invokes Simulated's portable-engine superheated state in addition to the configured burn time. It is ignored safely when Simulated/Aeronautics is unavailable.
- `throttle_signal`: fixed Simulated throttle-lever output from `1` to `15`.
- For a five-minute, full-power ship, use `"engine_burn_seconds":300` and `"throttle_signal":15`. These are ordinary datapack fields, not hard-coded fleet behavior.
- `throttle_signal_by_y`: ordered Y rules captured when the ambush starts. Each rule accepts `min_y`, `max_y`, and a clamped `signal` from `1` to `15`; the first matching rule wins.
- `container_loot`: one rule or an array of rules applied to unopened structure-NBT containers after assembly. Each rule requires `loot_table`, may filter `blocks`, and may specify `seed` or `replace_existing`. When the active Sable stack converts containers into persistent child sublevels, Ambush follows the parent/child association and applies loot in those child plots.
- `entities`: entities created after successful assembly. An entry may use `entity`, `nbt.id`, or both. When both are present, `entity` wins.
- `nbt`: either a JSON object or an SNBT string. Position and Ambush ownership are applied after NBT loading.
- `local: [x,y,z]` or `local_x/local_y/local_z`: placement relative to the template's minimum corner. Missing X/Z coordinates default to the template center.
- `seat`: when `true`, the entity is associated with the completed Sable sublevel and mounted in the nearest unoccupied Create seat after Sable transfers it into the sublevel plot.
- `target_range`: owning-player range, in blocks, at which a normal spawned Ambush mob becomes hostile. It also sets that mob's follow-range attribute. It does not override the vanilla AI of seated Sable entities.
- `aggro_range`: optional `4`–`512` block activation radius for a normal Ambush mob. Ambush checks it once per second, only for the encounter owner, and sets the vanilla mob target after that player enters the range. Line of sight is required unless `aggro_through_walls` is also `true`. This never disables or replaces vanilla AI. On seated Sable entities it raises vanilla follow range, but cannot make a vanilla target goal see a player in another Sable sublevel.
- Seated Sable entities use their normal vanilla AI and equipment. Ambush only transfers and seats them; it does not inject targets, aim, or project arrows.
- `friendly_fire`: defaults to `true` for backward compatibility. Set it to `false` to prevent Ambush-owned mobs from damaging other Ambush mobs with the same owner. `allow_friendly_fire` is accepted as an alias.
- `target`: `owner` by default or `none`.

Sable-only definitions may use an empty `spawns` array. Queuing the structure then counts as the successful ambush action; a dummy mob is not required.

### Testing new datapack definitions

Put or edit definitions under the active world's `datapacks` folder, or in a loaded server resource/data pack, using `data/<namespace>/ambushes/<id>.json`. Run the vanilla `/reload` command. Ambush rebuilds its definition registry during that reload and logs the number loaded and rejected. Then use `/ambush validate`, `/ambush list`, or tab completion to confirm the new ID before testing it with `/ambush always <namespace>:<id>`.

Changing JSON data only requires `/reload`; changing the Ambush jar, Java code, or bundled resources still requires a game/server restart.

All bundled Ambush demonstrations are embedded in the mod under `data/ambush/ambushes/`, use IDs beginning with `ambush:example_`, and are command-only. The loader forces every `ambush:example_*` definition to chance `0`, so it cannot trigger naturally even if its example JSON contains an interval or a nonzero demonstration chance. Test one with `/ambush always <id>`.


### Conditional and delayed actions

Actions may include `conditions` with `time: "day"|"night"`, `weather: "clear"|"rain"|"stormy"`, `over_ocean`, `min_y`, `max_y`, or `dimensions`. `conditional_spawn` accepts ordinary spawn entries plus `after_ticks`, `min_radius`, `radius`, and `attempts`. Set `direction: "front"` and `arc_degrees` to constrain candidates to the player's forward view cone. Spawn entries accept `equipment: {"mainhand":"minecraft:crossbow","offhand":"..."}` and `crossbow_range` for a higher-priority vanilla crossbow goal at the requested firing distance. Passengers, water/air placement, line-of-sight avoidance, persistence, effects, equipment, owner targeting, and `friendly_fire` use the same fields as normal spawn groups.

`directional_cbc_shell_rain` supports `block`, `item`, `fuze`, `fuze_ticks`, `velocity`, `source_height`, `source_forward_offset`, `spread`, `target_spread`, `target_safe_radius`, `target_height_offset`, and either `count`/`after_ticks` or a `bursts` array. Set `ballistic: true` with a data-driven `gravity` (CBC HE shells use `0.05`) to compensate the firing vector for gravity; use `target_spread: 0` for an exact player target. `start_distance` delays each rain until its source is horizontally within that many blocks of the owner; a named Sable `source_structure` is resolved from the active ship every poll, so a moving ship can enter range and begin firing. `start_distance_poll_ticks` controls the recheck interval (default `20`). With `source_structures`, `source_delay_ticks` adds a per-source stagger in listed order: `0`, one delay, two delays, and so on. For example, `source_delay_ticks:20` makes the first listed ship fire now, the next ship one second later, and the third two seconds later. `source_forward_offset` moves the launch point from the structure toward the player so projectiles can clear the ship. `target_safe_radius` prevents direct targeting inside a configurable ring around the player. When the encounter also queues a Sable structure, its resolved spawn anchor is persisted with each shell wave. Delayed sounds, conditional spawns, and directional rains are SavedData-backed and survive world/server restarts.

### Sable formations

`sable_formation` applies one shared Sable definition to every object in `members`. Each member may override any top-level formation field, including `template`, `structure_key`, `spawn_distance`, `spawn_bearing_degrees`, `offset_y`, entities, lifetime, and engine settings. This permits heterogeneous fleets: for example, one `ambush:cannonballoon`, one `ambush:airballoonfloat`, and one `ambush:barrelballoon` in the same encounter. Fields omitted by a member inherit from the parent action. Give each member a unique `structure_key` when later actions need to target it. Bearings are relative to the player's facing direction at encounter start: `0` is ahead, `90` is one side, and `-90` is the other. Use `facing: "player"` to rotate every north-authored ship toward the player.

Use `facing: "orbit_clockwise"` or `facing: "orbit_counterclockwise"` to rotate a north-authored ship tangent to its spawn radius around the player. This sets the initial propulsion direction only; Sable/Aeronautics physics still determine the actual path and do not provide automatic centripetal steering.

Named origins let later actions select one or many ships. Use `source_structure: "fleet_lead"` for one ship or `source_structures: ["fleet_lead","fleet_left","fleet_right"]` to duplicate every configured burst from all listed ships. Each scheduled burst persists its own resolved origin, so formation shell waves remain correct after a restart.

## Surround and stealth encounters

Spawn groups are sampled independently around the owner, so a group such as:

```json
{"entity":"minecraft:pillager","count":{"min":12,"max":18},"avoid_line_of_sight":true,"tags":["surrounding_attacker"]}
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
    {"entity":"minecraft:phantom","count":{"min":3,"max":6},"avoid_line_of_sight":true,"persistent":true,"tags":["mountain_ambush"],"passengers":[{"entity":"minecraft:pillager","count":1,"persistent":true,"tags":["mountain_attacker"]}]},
    {"entity":"minecraft:evoker","count":1,"avoid_line_of_sight":true,"persistent":true,"tags":["mountain_ambush"]}
  ],
  "effects":["minecraft:darkness:2:0","minecraft:glowing:5:0"],
  "sounds":["minecraft:entity.ghast.warn","minecraft:entity.pillager.celebrate"]
}
```

## Bundled content

The mod embeds exactly five command-only definitions: `ambush:example_boss`, `ambush:example_boss_fleet`, `ambush:example_master`, `ambush:example_directional_arrow_rain`, and `ambush:example_cannon_balloon_fleet`. The two boss definitions contain their own health-triggered waves, swarms, reinforcements, rewards, and lifecycle behavior; they do not depend on hidden helper encounters. Every embedded example has natural chance zero.

The definitions are examples of the format, not hard-coded special cases. A pack creator can delete, override, disable, or replace them with a higher-priority datapack.

## Reloading and troubleshooting

1. Place JSON files under `data/<namespace>/ambushes/`.
2. Run `/reload`.
3. Run `/ambush list`.
4. Test with `/ambush <id>` or `/ambush <id> (player)`.
5. Check the server log for NeoForge reload or JSON errors.

Use namespaced IDs when a datapack has more than one source:

```text
/ambush always ambush:example_cannon_balloon_fleet (player)
```

Use a small chance, a long cooldown, and a bounded attempt count for large multiplayer encounters. Avoid definitions that spawn hundreds of mobs, use very large radii, or create many overlapping periodic checks.

## Design limits

Ambush creates bounded encounters and targets their owner. Broader quest, claim, world-generation, and general-purpose AI systems require their own dedicated integrations.

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

Arrow actions are item-driven. Set `arrow` (aliases: `arrow_item` or `item`) to any registered `ArrowItem`, including vanilla arrows, spectral arrows, tipped arrows, and custom modded arrow items. For a tipped arrow, add a registered `potion`. These fields work in `directional_arrow_rain`, `directional_entity_wave`, and ordinary `arrow_rain`. An unavailable optional-mod item produces no projectile and a server-log result; it does not create a dependency.

Potions use `kind: "potion"`, optional `item: "minecraft:splash_potion"` or `minecraft:lingering_potion`, and a registered `potion`. `directional_potion_rain` may also be used directly.

## Structure triggers and chance buildup

Trigger type `structure` rolls only while the player's loaded position is inside a matching structure piece. It does not locate structures, force chunks, or generate terrain. `conditions.structures` accepts IDs and tags such as `#minecraft:village`. Define reusable local lists in `conditions.structure_groups`, then select one or more with `structure_group` or `use_structure_groups`. Direct selectors and selected groups are ORed.

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

`check_every_ticks` controls the proc period (`1200` is one minute). Expanded chance values are probabilities from `0.0` to `1.0`; compact top-level `chance` remains a percentage from `0` to `100` for compatibility. `mode: "flat"` always rolls `base`. `mode: "build_up"` adds `increase_on_failure` after each eligible failed roll, capped at `max`. Failure state is per player and ambush, persists in world SavedData, and resets after success when `reset_on_success` is true. Eligibility, proc interval, and cooldown are checked before a failure can increase chance. `/ambush weights` reports current effective chance, base, mode, failures, cap, proc interval, and cooldown.

The bundled projectile test is `ambush:example_directional_arrow_rain`. The boss and boss-fleet examples additionally demonstrate embedded directional arrow and potion waves. Like all embedded examples, they are forced to natural chance zero; use `/ambush always <id>` to test them, then copy and rename one into your own namespace to test natural triggering.

## Sable redstone activations

Every `sable_structure` or member of a `sable_formation` may contain `redstone_activations`. Each entry activates once, is tracked in persistent Sable encounter state, and begins counting time when that individual sublevel finishes assembly. Range checks use the sublevel's current projected world-space center, so moving ships activate correctly rather than using their original spawn anchor.

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
- `after_ticks` or `after_seconds`: delay after that sublevel completes.
- `min_player_y` and `max_player_y`: require the owner to be inside this Y band. Y is an additional condition, so it can be combined with range/time. Define separate activation entries with separate `positions` to operate individual components at different player heights.
- `player_y_bands`: preferred exact-one selector. Each non-overlapping band may override `positions`, `block`/`blocks`, `signal`, `state`, and other activation fields. Exactly the first validated matching band is merged into the parent activation. `on_no_match` may be `wait` (default) or `complete`.
- `require: "any"`: default; range or time may activate it.
- `require: "all"`: when both are provided, wait until both are true.
- With no range or delay, the component activates immediately after assembly.
- `require_living_crew`: defaults to `true`. At least one living entity created by that specific Sable action must remain alive. Set it to `false` for uncrewed automation.
- Entity entries may set `fill_all_seats: true` with `seat: true` to create one mob for every available Create seat. `spawn_on_blocks` accepts block IDs or tags such as `#minecraft:planks` and places the requested extra mobs on randomly selected safe matching deck blocks.
- `player_direction`: continuously tests the owner's live direction in the moving sublevel's local frame. Values are `front`, `front_right`, `right`, `back_right`, `behind`, `back_left`, `left`, `front_left`, `above`, and `below`. `direction_tolerance_degrees` controls each sector's half-width. Combine this with `range`, `require: "all"`, `positions`, and the default crew gate to activate side-specific buttons, levers, or analog components only while living crew remains. Add `repeat_ticks` to re-evaluate and pulse a matching activation at a bounded interval; omit it for the ordinary one-shot behavior.
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

### Propulsion direction

Set propulsion direction independently on a Sable structure or formation:

```json
"engine_direction": "reverse",
"propeller_direction": "forward",
"steering_follows_propulsion_direction": true
```

Both direction fields accept `forward` and `reverse`. `reverse_engines` and `reverse_propellers` are boolean aliases. `engine_direction` changes each portable engine's real rotation-direction behavior. `propeller_direction` changes each Aeronautics propeller bearing's thrust handedness; small or smart propellers driven by a portable engine should use `engine_direction` instead.

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

Neighbor updates are emitted inside the sublevel after activation. Missing optional analog-lever mods do not prevent Ambush from loading; unmatched entries produce a bounded server warning and are marked complete rather than retrying forever. The two bundled boss examples use the real throttle lever in their Sable ships; the master example documents vanilla lever/button syntax for imported schematics that contain those blocks.

## Sable block health and boss bars

A Sable structure can expose its blocks as a data-driven boss health bar. Ambush captures the occupied-block baseline once after assembly. Health is `original baseline positions not yet observed as destroyed / original occupied positions`. Once an original position is observed as air it is permanently damaged for that encounter, so repairing or replacing it cannot heal the bar. A block replaced before a health scan observes the empty state still counts as intact. The scan is shared by boss bars, percentage events, and `destroyed_cleanup_percent`, and retains the 262,144-position safety cap.

Ongoing health checks use a persisted round-robin cursor and a global budget of 8,192 block checks per server tick across all Ambush ships. `health_scan_budget` on a Sable action requests up to that many checks for that ship per turn and defaults to `4096`; the global limit always wins. Very large fleets therefore update progressively instead of scanning every ship in full on one tick.

Health currently covers the owned parent Sable sublevel. Unrelated or externally split child sublevels are not silently included because Sable does not expose a reliable ownership lineage for arbitrary splits. Inline child ships created by lifecycle events receive their own health state and optional boss bar.

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

- `boss_bar: true` enables defaults.
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

When one damage scan crosses several percentage thresholds, Ambush evaluates every newly eligible threshold in descending percentage order and queues each one exactly once.

Event actions:

- `sound`: plays a registered sound at the moving sublevel center. Supports `volume`, `pitch`, and `audible_distance`. When `audible_distance` is present, Ambush projects the playback point onto the live ship-to-player line at that distance from the player. The sound therefore remains nearby and audible while its stereo direction still points toward the actual ship. Use `at: "player"` only when direction is not wanted.
- `redstone` or `redstone_activation`: accepts the same component, block/tag, signal, state, button duration, and schematic-local position fields as `redstone_activations`. An optional nested `activation` object is also accepted.
- `ambush`: triggers another loaded definition using `ambush` or `id`. `force` defaults to `true`; set it false to honor the referenced ambush's conditions. Persisted generation depth prevents delayed recursion beyond eight generations.
- `sable_structure` or `sable_formation`: queues an inline additional ship or fleet using the normal Sable schema. Child lineage and generation depth persist across restarts. `max_generation_depth` defaults to `8`, and `max_active_ships` defaults to `16` per owner; either may be lowered on the nested action.
- `fog`: applies the ordinary per-player fog action.

Events default to `require_living_crew: true`: at least one living entity spawned by that exact Sable action must remain alive. Configured living entities default to `crew: true`; set `crew: false` on cargo, prisoners, decorative mobs, or other living passengers that should not keep a ship operational. Seats never count as crew. This prevents abandoned ships from firing weapons, calling reinforcements, or activating redstone. Death/destroyed events default to `false` so their final sounds and effects can still run. Override the field on an event when needed; individual nested actions can additionally set `require_living_crew: true`.

Sounds at spawn, range, percentage, and death are all ordinary `sound` actions under the corresponding trigger, rather than separate hard-coded sound fields. This allows multiple sounds, chained actions, and per-event crew gating. The complete command-only boss demonstrations are `ambush:example_boss` and `ambush:example_boss_fleet`.

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

Fog actions support ordinary action `conditions`, `after_ticks`, and persisted scheduling. Fog-only ambushes are valid: accepted actions now count as encounter success for cooldown, chance reset, and command confirmation. The server and client must both run this Ambush build because fog uses an Ambush network payload. Shader mods may reinterpret vanilla fog, so test the target shader stack separately. Both bundled boss examples exercise timed fog.

## Sol III boss example

`ambush:example_boss` is a larger command-only boss test built from the bundled `sol_s_e_3.nbt` schematic. It spawns the Superheated Sol III 140 blocks away and begins approaching with a five-second, 128-block fog reveal. It demonstrates air placement and player-facing orientation, one pillager on every available seat, six additional safe deck guards, boss bar, crew-gated player-Y throttle bands, a nearby directional horn, fog, threshold redstone, three kinds of arrows at 75% health, potion volleys at 50%, two additional Sable airships, and a 72-104-block out-of-sight ground swarm at 25%. All boss-ship barrels receive Pillager Outpost loot; the defeated-boss reward ship's barrels and chests receive End City treasure. Test it with `/ambush always ambush:example_boss` in a world that has Sable, Simulated, Create, and the schematic's required blocks.

The boss applies fog before assembly so the reveal is not delayed by Sable assembly. Its horn uses `audible_distance: 8`, placing the sound eight blocks from the player along the live direction toward the ship. `fireworks` is a lifecycle or ordinary action with `count`, `height`, `spread`, `flight` (1-3), `shape`, `colors`, and `fade_colors`; the boss emits twelve large, twinkling purple-and-gold rockets when destroyed.

Use it together with the examples under `src/main/resources/data/ambush/ambushes/`. Copy an example into a pack-owned namespace before changing it; bundled `ambush:example_*` definitions are deliberately command-only (`chance: 0`) and must remain non-natural test content.
