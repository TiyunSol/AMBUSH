# AMBUSH Datapack Guide

Ambush is a server-authoritative NeoForge 1.21.1 mod for data-driven hostile encounters. Definitions are loaded from every active server datapack at:

```text
data/<namespace>/ambushes/<id>.json
```

Definitions are validated during reload. Invalid trigger names, malformed values, unsupported action modes, unsafe limits, and missing required resources are rejected with a useful server-log error rather than being silently ignored.

Create, Create Aeronautics/Simulated, Sable, and Create Big Cannons are optional integrations. Generic entity, sound, effect, and vanilla projectile encounters work without them. Sable actions fail closed when their required runtime is unavailable.

This document is the complete, current authoring reference for compact and expanded definitions, ordinary encounters, Sable vessels and formations, redstone hardware, containers, and Create Big Cannons reloads. Start with a small entity encounter, then use the vessel sections only after the basic definition validates and runs correctly.

## Installation and reloads

Install Ambush on the server and connecting clients when using its client-synchronised features such as fog. External datapack JSON changes require `/reload`; adding, replacing, or changing bundled AMBUSH resources requires a full game or server restart.

Use `/reload`, then `/ambush admin check` after every external datapack reload. Test a named definition by running `/ambush <namespace:id>` directly.

---

## 1. Datapack location

Place each encounter definition here:

```text
<server-world>/datapacks/<your-pack>/data/<namespace>/ambushes/<id>.json
```

Example layout:

```text
my-ambushes/
├── pack.mcmeta
└── data/
    └── myambushes/
        └── ambushes/
            └── midnight_horde.json
```

`pack.mcmeta` for Minecraft 1.21.1:

```json
{
  "pack": {
    "pack_format": 48,
    "description": "My AMBUSH encounters"
  }
}
```

The example above creates the encounter ID:

```text
myambushes:midnight_horde
```

### Make encounters available in every new world

Do not put a pack in an individual world's `datapacks` folder if it must be
available automatically in every new world. Bundle the definitions with
AMBUSH instead.

For a bundled AMBUSH definition, add the JSON file to the mod's resources:

```text
src/main/resources/data/<namespace>/ambushes/<id>.json
```

For example:

```text
src/main/resources/data/myambushes/ambushes/midnight_horde.json
```

Build and install the updated AMBUSH jar, then fully restart the game or
server. Minecraft then loads the bundled definition whenever AMBUSH loads,
including in newly created worlds. No per-world copy is needed.

Bundled definitions still use normal datapack rules. One definition can work in
every dimension unless its `dimensions` field restricts it. Structure templates
used by a bundled definition belong beside it in the same resources tree:

```text
src/main/resources/data/<namespace>/structure/<path>.nbt
```

For content that must stay outside the AMBUSH jar but still load automatically,
use a server-wide data-pack loader already installed in the pack. That loader,
not AMBUSH, determines its configuration path and reload behavior. AMBUSH will
load the definitions once the server has loaded that data pack.

AMBUSH validates definitions during datapack reload. Invalid triggers, malformed values, unsupported action modes, unsafe limits, and missing required resources are reported in the server log rather than being silently accepted.

---

## 2. Your first encounter

Create `data/myambushes/ambushes/midnight_horde.json`:

```json
{
  "trigger": "interval",
  "interval": 1200,
  "cooldown": 600,
  "chance": 5,
  "min_time": 13000,
  "max_time": 23000,
  "dimensions": ["minecraft:overworld"],
  "radius": 34,
  "attempts": 32,
  "spawns": [
    {
      "entity": "minecraft:zombie",
      "count": 9,
      "persistent": true
    },
    {
      "entity": "minecraft:husk",
      "count": 3,
      "persistent": true
    }
  ],
  "sounds": ["minecraft:entity.zombie.ambient"]
}
```

Then run:

```mcfunction
/reload
/ambush admin check
/ambush myambushes:midnight_horde
```

Running a definition by name never rolls its `chance`, and it clears that definition's cooldown group. It does still require every condition to be satisfied: the trigger requirement, time range, Y range, biome, and dimension. If the run fails, the command prints the reason.

To test outside those conditions, use always mode. `/ambush always` toggles always mode for the player who runs it; while it is ON, `/ambush <namespace:id>` skips the condition check entirely, so a definition can be started at the wrong time of day, in the wrong biome, or away from its required blocks. Run `/ambush always` a second time to turn it off before testing natural behavior.

---

## 3. Compact-format fields

| Field | Meaning |
|---|---|
| `trigger` | Encounter trigger. Use `interval` for ordinary periodic encounters. |
| `interval` | How often AMBUSH checks this definition, in ticks. `20` ticks is about one second. |
| `cooldown` | Delay after a successful encounter, in seconds. |
| `cooldown_group` | Shared cooldown key. Definitions in the same group share one cooldown. Defaults to `default`. |
| `chance` | Chance from `0` to `100` each time the definition is checked. |
| `min_time` / `max_time` | Minecraft daytime range from `0` to `24000`. |
| `dimensions` | Allowed dimension IDs. |
| `biomes` | Allowed biome IDs. Multiple values are alternatives. |
| `min_y` / `max_y` | Allowed Y-level range. |
| `radius` | Maximum horizontal placement distance. |
| `attempts` | Maximum placement attempts. This is a safety limit, not a guarantee. |
| `spawns` | List of entity groups to create. |
| `effects` | Effects applied after at least one entity successfully spawns. |
| `sounds` | Sound event IDs played after at least one entity successfully spawns. |
| `active_blocks` | Block IDs checked by the `block_active` trigger. |
| `weight` | Relative selection weight when several definitions are eligible at once. |
| `allow_peaceful` | Set to `true` to let the definition spawn on Peaceful difficulty. |

A cooldown is consumed only if the encounter successfully creates at least one entity.

---

## 4. Trigger types

### Interval

`interval` is the normal trigger. It checks the definition on the configured schedule.

```json
{
  "trigger": "interval",
  "interval": 1200,
  "chance": 5,
  "spawns": [
    { "entity": "minecraft:zombie", "count": 2 }
  ]
}
```

### Portal

`portal` requires a Nether portal block nearby.

```json
{
  "trigger": "portal",
  "interval": 20,
  "cooldown": 600,
  "chance": 100,
  "spawns": [
    { "entity": "minecraft:piglin", "count": 4 }
  ]
}
```

### Block active

`block_active` checks nearby blocks listed in `active_blocks`. It can also recognize compatible block entities that expose positive progress.

```json
{
  "trigger": "block_active",
  "interval": 100,
  "cooldown": 900,
  "chance": 8,
  "active_blocks": ["createoreexcavation:extractor"],
  "spawns": [
    { "entity": "minecraft:zombie", "count": 5 }
  ]
}
```

### Structure

`structure` requires the target to be standing on a piece of a listed structure. It matches only already-loaded structure pieces: it never performs a locate and never generates chunks.

```json
{
  "trigger": "structure",
  "interval": 200,
  "cooldown": 1200,
  "chance": 20,
  "structures": [
    "minecraft:pillager_outpost",
    "#minecraft:village"
  ],
  "spawns": [
    { "entity": "minecraft:pillager", "count": 4 }
  ]
}
```

Entries are structure IDs, or structure tags when prefixed with `#`. A definition with no resolvable selector never matches. For a reusable set of selectors, declare `structure_groups` as a named object and select from it with `structure_group` or `use_structure_groups`.

### Kill

`kill` runs after the target kills a qualifying entity. `kill_count` sets how many kills are required, from `1` to `10000`, and defaults to `1`. `kill_entity` restricts it to one entity ID; omit it to count any kill.

```json
{
  "trigger": "kill",
  "kill_entity": "minecraft:pillager",
  "kill_count": 12,
  "cooldown": 1800,
  "chance": 100,
  "spawns": [
    { "entity": "minecraft:vindicator", "count": 3 }
  ]
}
```

`vanilla_raid_wave` is also accepted. It is driven by raid events rather than by the periodic check, so it does not run from a schedule and is not selected by `/ambush player`.

Unknown trigger names fail validation.

---

## 5. Spawn groups

Each entry in `spawns` is a separate entity group.

```json
{
  "entity": "minecraft:pillager",
  "count": 6,
  "persistent": true,
  "tags": ["raider_patrol"]
}
```

| Field | Meaning |
|---|---|
| `entity` | Required entity registry ID. |
| `count` | A fixed amount or a range such as `{ "min": 2, "max": 6 }`. |
| `persistent` | Prevents normal despawning. |
| `tags` | Custom entity tags. |
| `target` | `owner` or `none`. `owner` is the default. |
| `aggro_through_walls` | Keeps retargeting through terrain. |
| `effects` | Effects applied to the spawned entity. |
| `passenger` | One passenger for each spawned entity. |
| `passengers` | A nested list of passengers. |

Random count:

```json
{
  "entity": "minecraft:pillager",
  "count": { "min": 4, "max": 8 },
  "persistent": true
}
```

Mounted or nested entities:

```json
{
  "entity": "minecraft:phantom",
  "count": 2,
  "passengers": [
    {
      "entity": "minecraft:pillager",
      "count": 1,
      "persistent": true
    }
  ]
}
```

---

## 6. Conditions

Use conditions to limit when an encounter can run.

```json
{
  "trigger": "interval",
  "interval": 1200,
  "chance": 5,
  "min_y": 50,
  "max_y": 179,
  "min_time": 13000,
  "max_time": 23000,
  "biomes": [
    "minecraft:plains",
    "minecraft:sunflower_plains"
  ],
  "dimensions": [
    "minecraft:overworld"
  ],
  "spawns": [
    { "entity": "minecraft:zombie", "count": 4 }
  ]
}
```

Use exact registry IDs. Multiple biome or dimension entries are alternatives.

---

## 7. Effects and sounds

Effects use `effect_id:duration_seconds:amplifier`.

```json
{
  "trigger": "interval",
  "interval": 1200,
  "chance": 2,
  "effects": [
    "minecraft:darkness:6:0",
    "minecraft:slowness:4:0"
  ],
  "sounds": [
    "minecraft:entity.ghast.warn"
  ],
  "spawns": [
    { "entity": "minecraft:witch", "count": 1 }
  ]
}
```

---

## 8. Commands

Most commands require permission level 2. `/ambush` and `/ambush list` are available to any player.

| Command | Purpose |
|---|---|
| `/ambush` | Prints the command summary and the current always-mode state. |
| `/ambush list` | Lists loaded definitions. |
| `/ambush <namespace:id>` | Runs one named encounter against the player who ran it. |
| `/ambush <namespace:id> <player>` | Runs one named encounter against a named player, ignoring that encounter's conditions. |
| `/ambush player <player>` | Runs a randomly selected eligible encounter against a named player. |
| `/ambush player <player> <namespace:id>` | Runs one named encounter against a named player, using that player's always-mode setting. |
| `/ambush always` | Toggles always mode for the player who runs it. Takes no encounter ID. |
| `/ambush clear` | Cancels active AMBUSH work and removes AMBUSH-owned encounter content. |
| `/ambush enable [pack]` | Enables natural spawning for a datapack namespace. Blank defaults to `ambush`. |
| `/ambush disable [pack]` | Disables natural spawning for a datapack namespace. Blank defaults to `ambush`. |
| `/ambush admin check` | Reports how many definitions loaded, plus any hidden for missing mods and any rejected. |
| `/ambush admin check <namespace:id>` | Read-only preflight for one definition and its requirements. |
| `/ambush admin inspect` | Inspects the nearest active AMBUSH vessel and reports its local hardware coordinates, block IDs, controls, loot rules, and controller configuration. |
| `/ambush admin debug` | Toggles server diagnostics. |
| `/ambush admin weights` | Reports each definition's effective weight, base weight, current chance, and cooldown group. |
| `/ambush admin unlocks` | Reports unlock progress for definitions that declare one. |
| `/ambush admin unlockall` | Unlocks every unlockable definition for the player who runs it. |
| `/ambush admin spawning` | Toggles server-wide natural spawning. |
| `/ambush admin spawning status` | Reports the server-wide toggle and any individually disabled definitions. |
| `/ambush admin spawning <namespace:id> [enable\|disable]` | Reads or changes one definition's natural-spawn toggle. |

Use `/ambush admin check <namespace:id>` before testing a complicated definition. It performs a read-only preflight and reports definition, template, placement, formation, hardware, lifecycle, and loot-rule issues without spawning encounter content. Use `/ambush admin inspect` after spawning a vessel to verify its schematic-local hardware and controls.

### Always mode

Always mode is a per-player toggle, not an argument. `/ambush always` turns it on, and a second `/ambush always` turns it off. The command reports the new state, and `/ambush` on its own reports the current state.

While always mode is ON, `/ambush <namespace:id>` skips the entire eligibility check for that player: trigger requirement, Y range, time range, biome, dimension, portal proximity, and required nearby blocks. This is the correct way to test a definition out of context. Turn it off again before judging whether the definition behaves correctly on its own.

Always mode affects only manual runs. It does not change natural spawning.

### Natural spawning

`/ambush admin spawning` toggles natural spawning for the whole server. While it is disabled nothing spawns on its own, but running a definition by name still works. `/ambush admin spawning status` reports that toggle together with any definitions disabled individually, and `/ambush admin spawning <namespace:id>` reads or changes one definition's own toggle.

`/ambush enable [pack]` and `/ambush disable [pack]` work on a whole datapack namespace at once. The argument is a namespace, such as `myambushes`; leaving it blank targets `ambush`, the bundled set.

### Weights and unlocks

When several definitions are eligible at the same moment, one is selected using each definition's `weight`. `/ambush admin weights` prints that weight, the base value it came from, the definition's current chance, and its `cooldown_group`, so competing definitions can be compared directly.

A definition may also declare an `unlock` block with a `progress` object, which gates it behind tracked player progress. `/ambush admin unlocks` lists every definition that declares one, along with the event it tracks and the player's current progress toward it. `/ambush admin unlockall` marks them all unlocked for testing.

---

## 9. Troubleshooting

**The definition is missing from `/ambush list`.**  
Run `/reload`, then read the server log. The validation error identifies the rejected definition and reason.

**The definition loads but does not run naturally.**  
Run `/ambush <namespace:id>` first; if it fails, the command reports why. Then turn on `/ambush always` and run it again. If it works only with always mode ON, the definition itself is valid and one of its conditions is not being met — check the trigger, time range, dimension, biome, and Y range. Turn always mode back off with a second `/ambush always` before testing natural behavior again.

**The definition runs by name but never occurs on its own.**  
Check that natural spawning is enabled with `/ambush admin spawning status`, and that its namespace has not been disabled with `/ambush disable`. Then check its `chance` and `weight` with `/ambush admin weights`, and its unlock state with `/ambush admin unlocks`.

**Entities do not appear.**  
Increase `attempts` carefully and use a larger `radius`. Placement is bounded and may fail when no valid location exists.

**A large or complex definition behaves unexpectedly.**  
Run `/ambush admin check <namespace:id>`. It reports validation, required templates, placement settings, formation members, hardware requirements, lifecycle schedules, named wave sources, fill declarations, and loot rules without creating encounter content.

---

## 10. Advanced definitions

The expanded format is intended for reusable datapacks and advanced behavior.

```json
{
  "schema_version": 1,
  "enabled": true,
  "trigger": {
    "type": "interval",
    "check_every_ticks": 1200,
    "cooldown_ticks": 24000,
    "chance": {
      "base": 0.05
    }
  },
  "conditions": {
    "dimensions": ["minecraft:overworld"]
  },
  "wave": {
    "radius": 40,
    "maximum_attempts_per_member": 32,
    "groups": [
      {
        "entity": "minecraft:pillager",
        "count": 5,
        "persistent": true
      }
    ]
  }
}
```

The `chance` object takes `base` as a fraction from `0` to `1`. Its `mode` is `flat` by default; `build_up` raises the chance by `increase_on_failure` after each failed roll, up to `max`, and resets on success unless `reset_on_success` is `false`.

Use the expanded format when the compact format cannot express what you need. The following sections and the current authoring additions below cover the supported vessel, hardware, and formation behavior.

---

## 11. Airships and Sable structures

Airships use the `sable_structure` action. They require a compatible Sable,
Simulated, and Create installation. The template is a normal structure NBT file:

```text
data/<namespace>/structure/<path>.nbt
```

For this action:

```json
"template": "myambushes:airship/hostile_balloon"
```

the template file must be:

```text
data/myambushes/structure/airship/hostile_balloon.nbt
```

Save the normal, unassembled structure template. Do not save an already
assembled Create contraption or a nested Sable sublevel. Required static blocks
and retained entities, including Simulated honey glue when used by the craft,
must remain in the template.

### First airship

This is a complete advanced definition that assembles one airship. Keep
`chance` at `0` while testing, turn on always mode with `/ambush always`, and
start it with `/ambush myambushes:<id>`.

```json
{
  "schema_version": 1,
  "enabled": true,
  "trigger": {
    "type": "interval",
    "check_every_ticks": 1200,
    "cooldown_ticks": 12000,
    "chance": { "base": 0.0 }
  },
  "conditions": {
    "dimensions": ["minecraft:overworld"]
  },
  "spawns": [],
  "actions": [
    {
      "type": "sable_structure",
      "template": "myambushes:airship/hostile_balloon",
      "placement": "air",
      "spawn_distance": 64,
      "offset_y": 12,
      "schematic_front": "north",
      "facing": "player",
      "max_retries": 5,
      "lifetime_ticks": 6000,
      "attach_child_sublevels": true,
      "envelope_fill": 0.5,
      "engine_burn_ticks": 3000,
      "entities": [
        {
          "entity": "minecraft:pillager",
          "count": 2,
          "local": [4, 2, 6],
          "seat": true,
          "persistent": true,
          "target": "owner",
          "friendly_fire": false,
          "tags": ["hostile_crew"]
        }
      ]
    }
  ]
}
```

Sable-only definitions may use an empty `spawns` array. Queueing the Sable
structure is then treated as the successful encounter action; a dummy mob is
not required.

### Core airship fields

| Field | Meaning |
|---|---|
| `template` | Required namespaced structure-template ID. |
| `placement` | Use `air` to require a clear, already-loaded, template-sized air volume. |
| `spawn_distance` | Horizontal distance from the encounter target. |
| `offset_y` | Vertical offset. |
| `spawn_angle_degrees` | Optional fixed angle around the encounter target. |
| `schematic_front` | Direction the saved template considers its front. Defaults to `north`. `base_facing` is the legacy alias. |
| `facing` | `north`, `east`, `south`, `west`, or `player`. |
| `max_retries` | Bounded assembly attempts. Default `5`; maximum `20`. |
| `lifetime_ticks` | Automatic cleanup delay. Default `6000` ticks. Use `null`, `"none"`, or `"permanent"` to disable automatic cleanup. |
| `envelope_fill` | Requested balloon fill after assembly. |
| `engine_burn_ticks` | Requested portable-engine burn duration. |
| `entities` | Crew or other entities created after successful assembly. |

`placement: "air"` searches upward from the configured offset without
generating chunks. `air_search_attempts` and `air_step` control that bounded
search; their defaults are `8` attempts and `4` blocks.

Use `/ambush admin check <namespace:id>` before testing an airship. It checks the
definition, template resources, placement settings, crew declarations, named
sources, fill declarations, and hardware requirements without spawning the
craft.

---

## 12. Airship crew, controls, and cannon buttons

Crew is declared in the vessel action's `entities` list. `local` coordinates
are measured from the template's minimum corner. Crew placement is entirely
datapack-owned: use `seat_predicate` / `seat_predicates` to select exact block
IDs (including any Create seat color), or use `spawn_on_blocks` for a carpet or
other deck block. There are no mob-type seat defaults. With `seat: true` and no
selector, the mob uses every detected seat; use a selector whenever different
mobs need different positions.

`redstone_activations` operates controls after the airship has assembled. Each
activation is tracked in the persistent airship encounter state.

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

| Field | Meaning |
|---|---|
| `component` | `analog_lever`, `lever`, or `button`. |
| `blocks` / `block` | Matching block IDs or tags in the assembled airship. |
| `signal` | Analog-lever signal from `1` to `15`. |
| `state` | Lever state: `on` or `off`. |
| `button_ticks` | How long a button remains pressed. |
| `range` / `distance` | Distance condition measured from the moving airship center. |
| `after_ticks` / `after_seconds` | Delay after that airship finishes assembly. |
| `require_living_crew` | Requires at least one living entity created by that airship. Defaults to `true`. |

With no range or delay, an activation runs immediately after assembly. Set
`require_living_crew: false` only for intentionally uncrewed automation.

---

## 13. Create Big Cannons reloads

`cannon_reloads` is an optional integration for assembled Sable ships. It is
supported for Create Big Cannons 5.11.7 and is disabled with one warning on
unverified CBC versions. It does nothing when Create, CBC, Sable, or Simulated
is unavailable.

The reload action runs after a successful shot. It requires at least one living
configured crew member and only works with an automatically loadable
quick-firing breech.

```json
"cannon_reloads": [
  {
    "id": "main_cannon",
    "mount_local": [3, 2, 3],
    "reload_after_ticks": 100,
    "shell_stack_snbt": "{id:\"createbigcannons:solid_shot\",count:1}",
    "propellant_stack_snbt": "{id:\"createbigcannons:big_cartridge\",count:1,components:{\"createbigcannons:power\":1}}"
  }
]
```

| Field | Meaning |
|---|---|
| `id` | Stable name for this reload rule. |
| `mount_local` | Cannon-mount block-entity coordinates, measured from the Sable plot minimum. |
| `reload_after_ticks` | Delay after a successful shot before the reload attempt. |
| `shell_stack_snbt` | Shell item stack in SNBT form. |
| `propellant_stack_snbt` | Filled Big Cartridge stack in SNBT form. |

AMBUSH uses CBC's public cannon-loading API and does not write cannon NBT.
Powder charges are intentionally unsupported. A missing, occupied, damaged,
unsupported, or crewless cannon fails closed and is never overwritten.

Use a button `redstone_activation` only when the imported schematic already
contains a button connected to the cannon controls. AMBUSH can operate and
validate existing hardware; it cannot create missing cannon, engine, steering,
or propulsion hardware.

---

## 14. Current vessel, hardware, and formation authoring

This section documents the current vessel features. It supersedes older examples that omit `sable_car`, `power_positions`, or `/ambush admin inspect`. All local coordinates use `[x, y, z]` from the saved schematic's minimum corner. Set `assembly_origin` to the schematic-local block that must become the assembled vessel origin.

### Vessel action types

| Type | Required placement | Required controls | Intended use |
|---|---|---|---|
| `sable_structure` | Usually `air`; other supported placement modes are allowed when suitable. | Depends on the schematic and configured controller. | Airships and general assembled structures. |
| `sable_boat` | `water` | `ship_ai` | Boats placed on eligible water with no AMBUSH physics intervention. `altitude_controller` and `envelope_fill` are not valid. |
| `sable_car` | `surface` | `ship_ai` and `car_controls` | Ground vehicles. `altitude_controller` and `envelope_fill` are not valid. |
| `sable_formation` | Inherited or supplied per member. | Per-member. | A coordinated set of vessel members. |

Every vessel action needs a namespaced `template`. Vessel templates are stored at `data/<namespace>/structure/<path>.nbt`; a template ID such as `myambushes:vehicles/scout` resolves to `data/myambushes/structure/vehicles/scout.nbt`.

`sable_formation` has a `members` array. Members inherit compatible parent fields and override only what differs. A member may explicitly be `sable_structure`, `sable_boat`, or `sable_car`, so one fleet can mix airships with boats or cars. Keep every member's placement, template, and hardware independently valid.

### Ocean-boat placement

`sable_boat` resolves the exposed water surface at each candidate anchor and
counts consecutive water blocks downward. `minimum_water_depth` rejects shallow
water. After assembly AMBUSH never changes the boat's position or velocity;
Sable/Aeronautics alone handle all motion and buoyancy.

```json
{
  "type": "sable_boat",
  "placement": "water",
  "minimum_water_depth": 8,
  "ship_ai": {"mode": "chase", "distance": 16, "correction_range": 16, "target_range": 128}
}
```

Both values are optional: the defaults are `minimum_water_depth: 2` and
`water_spawn_height: 2`. Depth must be 1–256 blocks and spawn height 1–16.

### Ground-car definition

Cars are surface-anchored. Use `offset_y` when the schematic needs to spawn above the resolved ground surface. Their `ship_ai` supports broadside or chase selection; `car_controls` maps that intent to the car's actual clutch, reversing, throttle, and steering hardware.

```json
{
  "type": "sable_car",
  "template": "myambushes:vehicles/pillager_car",
  "assembly_origin": [6, 1, 2],
  "placement": "surface",
  "offset_y": 2,
  "spawn_distance": 72,
  "schematic_front": "east",
  "facing": "player",
  "ship_ai": {
    "mode": "broadside",
    "distance": 20,
    "distance_tolerance": 3,
    "correction_range": 12,
    "side": "alternate",
    "target_range": 32
  },
  "chase_controller": {
    "stop_distance": 9,
    "resume_distance": 14,
    "target_range": 96
  },
  "car_controls": {
    "left_turn_lever": [2, 3, 3],
    "right_turn_lever": [2, 3, 1],
    "clutch_lever": [7, 2, 2],
    "reverse_lever": [6, 2, 2],
    "drive_signal": 15,
    "turn_inner_signal": 1,
    "idle_signal": 0,
    "broadside_brake_min_range": 17,
    "broadside_brake_range": 32,
    "target_still_speed": 0.01,
    "turn_deadband_degrees": 6,
    "update_ticks": 10
  }
}
```

With `ship_ai.mode: "broadside"`, a car keeps the chosen side toward its target and circles within `target_range`; it uses the chase controller when the target is farther away. It brakes only when the target is sufficiently still and inside the configured broadside brake band. `ship_ai.mode: "chase"` is the separate, data-driven direct-approach mode.

For the steering layout above, the local lever named `right_turn_lever` turns the car left and `left_turn_lever` turns it right. Use `/ambush admin inspect` to verify the actual coordinates and block IDs after assembly.

### Redstone activations and sequenced cannon fire

`redstone_activations` can target `positions` explicitly. This is the most reliable form for a schematic with multiple controls. For buttons, use `state: "button"`; `button_ticks` is the press duration. `repeat_ticks` requests a later cycle while its target conditions still hold.

`sequence_interval_ticks` staggers a multi-button activation in array order. For example, four broadside buttons with `button_ticks: 20` and `sequence_interval_ticks: 30` press one button for 20 ticks, wait at least 10 ticks, then press the next. The sequence resets after its final control, so a new cycle cannot overlap an unfinished sequence.

`power_positions` is optional but important for controls whose mounted device must receive redstone power in addition to the visible button changing state. When both arrays are present, entry *n* in `positions` is paired with entry *n* in `power_positions`.

Cannon behavior is datapack-defined. Use `cannon_assembly` to select the
assembly controls, `initial_cannon_loads` / `cannon_reloads` for CBC loading,
and `redstone_activations` for firing controls. AMBUSH does not infer a cannon
battery from a mob or seat choice. `power_mounts_directly` defaults to `false`;
set it to `true` only for a schematic whose CBC mount needs the explicit
compatibility pulse instead of its authored redstone wiring.

```json
{
  "component": "button",
  "positions": [[4, 0, 0], [5, 0, 0], [6, 0, 0], [7, 0, 0]],
  "power_positions": [[4, 1, 1], [5, 1, 1], [6, 1, 1], [7, 1, 1]],
  "state": "button",
  "button_ticks": 20,
  "sequence_interval_ticks": 30,
  "repeat_ticks": 20,
  "range": 32,
  "horizontal_only": true,
  "vertical_tolerance": 2,
  "player_direction": "left",
  "direction_tolerance_degrees": 35,
  "require_living_crew": true
}
```

`player_direction` may limit a battery to `left`, `right`, or `front`, as measured from the vessel. Use `horizontal_only` with `vertical_tolerance` for short-range hardware that should engage targets near its firing height. Match every listed control and receiver to real blocks in the schematic; AMBUSH does not add missing redstone or cannon hardware.

For a per-cannon arc gate, add `cannon_alignment` to that cannon's own firing activation. It compares the current target against a full 3-D vector in template-local coordinates, starting at that cannon's `mount_local`; no hull-wide broadside approximation is used. The activation fires only while the target lies inside `arc_degrees` of `aim_local`.

```json
{
  "component": "buttons",
  "positions": [[4, 2, 10]],
  "power_positions": [[5, 2, 10]],
  "state": "button",
  "button_ticks": 8,
  "range": 96,
  "repeat_ticks": 30,
  "cannon_alignment": {
    "mount_local": [6, 2, 10],
    "aim_local": [0, -1, -1],
    "arc_degrees": 12
  }
}
```

`mount_local` is the real cannon/muzzle location in the template and `aim_local` is its real barrel direction (it need not be unit length). Use one activation per weapon when their arcs differ. The same feature applies to straight-firing autocannons: use their barrel origin and a narrow vector, for example `"aim_local": [0, 0, -1]` with `"arc_degrees": 3`. This gate is optional and has no effect on existing activations.

### Airship movement, crew, formations, and lifecycle

For an airship, `envelope_fill` is a one-time initial balloon fill; it is not an altitude controller. Use `altitude_controller` for ongoing lift control, and keep its lever changes gradual enough for the craft's buoyancy to respond. Useful fields include `mode` (`player_offset` or `absolute`), `offset`, `tolerance`, `hover_signal`, `minimum_signal`, `maximum_signal`, `velocity_lookahead_ticks`, and terrain-clearance limits.

`ship_ai` supports `chase`, `broadside`, `orbit`, `boarding`, `tnt_drop`, `overhead`, `flyover`, and `disabled`. `overhead` and `flyover` are airship-only (`sable_structure`) modes and require an `altitude_controller`: `overhead` steers to the player’s X/Z position and holds there, while `flyover` steers through that position without the overhead stop. Author altitude with `ship_ai.match_player_y: false` and an `altitude_controller.offset` (or a player-Y band) to keep the hull above the target. To fire real mounted cannons down or diagonally down, physically aim the schematic’s mount that way and use a per-cannon `redstone_activations.cannon_alignment` vector with suitable range and real `positions`/`power_positions`; AMBUSH does not rotate cannon hardware. Chase uses a `chase_controller`; a meaningful gap between its reverse and resume distances prevents repeated direction changes. `steering_controls` describes the steering hardware. If a craft turns consistently the wrong way, verify `schematic_front` first, then use the steering control's `invert` setting when appropriate.

Vessel `entities` are crew declarations. Use `local` or `local_x`, `local_y`, and `local_z` to place them relative to the template; use `seat: true` only with suitable seats in the schematic. `seat_predicates`, `equipment`, targeting fields, persistence, and friendly-fire settings allow the crew to match the vessel's intended role. Controls that require living crew stop operating once that configured crew is gone.

For formations, give each member a unique `structure_key`. Do not give the formation parent its own `structure_key`, or inherited members can resolve to the same assembly slot. Member bearings are relative to the initial target-facing direction. `fleet_health` may be used where a shared fleet-health presentation is wanted.

`sable_events` attach restart-safe lifecycle behavior to a vessel. Supported triggers include spawn, range, time, target height, block-percent, crew-state, death, and destroyed state. Use death-triggered events for reward or final cleanup behavior rather than a low block-percent threshold. Timed cleanup uses `lifetime_ticks`; `destroyed_cleanup_percent`, `despawn_effect`, `completion_actions`, and child-cleanup settings provide additional controlled lifecycle behavior.

### Ordinary actions and advanced encounter behavior

The `actions` array supports the following current action families:

| Family | Action types |
|---|---|
| Direct encounter control | `raid`, `conditional_spawn`, `inline_ambush`, `sound`, `fog`, `fireworks` |
| Entity and projectile waves | `entity_wave`, `directional_entity_wave`, `arrow_rain`, `directional_arrow_rain`, `entity_rain`, `directional_entity_rain`, `potion_rain`, `directional_potion_rain`, `cbc_shell_rain`, `directional_cbc_shell_rain` |
| Static placement | `structure`, `micro_structure`, `block_platform` |
| Assembled vessels | `sable_structure`, `sable_boat`, `sable_car`, `sable_formation` |

Use `conditional_spawn` for delayed, bounded spawning with an action-level `conditions` object. Directional wave actions support a direction and arc so they can be constrained to a desired approach. The rain actions use count, height, spread, timing, and target controls; directional shell rain can additionally use a named vessel source, source delay, velocity, fuze, gravity, and safe-target radius. CBC shell rain now reports a zero-shell result as a failed scheduled action instead of silently succeeding; use `required: true` for an encounter that must not continue without its artillery.

`micro_structure` is the data-defined alternative to an NBT template for small, temporary surface compositions. It uses bounded offsets, safe replacement rules, optional contained entities, and a lifetime that restores its original blocks. `structure` uses a normal registered structure template; `block_platform` is for bounded platform-style placement. Each should be tested separately before combining it with delayed waves or vessel actions.

### Hopper and container loot

Use `container_loot` to link embedded containers to a loot table after the vessel assembles. Explicit `positions` are preferred whenever several hoppers need different ammunition. `replace_existing: true` ensures schematic contents are replaced by the linked table.

```json
"container_loot": [
  {
    "positions": [[4, 2, 1], [5, 2, 1]],
    "loot_table": "myambushes:side_ammunition_hopper",
    "replace_existing": true
  },
  {
    "positions": [[10, 2, 2]],
    "loot_table": "myambushes:front_ammunition_hopper",
    "replace_existing": true
  }
]
```

Loot tables live at `data/<namespace>/loot_table/<path>.json`. A five-slot hopper table should generate five entries or rolls when all five slots must be filled. For a mixed ammunition hopper, give each desired item a weighted entry and set the generated item count to the requested stack size. Validate item IDs and test the assembled container, not only the JSON reload.
## Lootr containers

Lootr is an optional integration. When Lootr is installed, a vessel's loot
containers can be converted to per-player containers, so every player who
boards gets their own roll of the table instead of the first one aboard taking
everything. AMBUSH has no compile-time dependency on Lootr: when Lootr is
absent, the same authored table is applied through the ordinary vanilla path
and nothing about the definition changes.

This applies to the `container_loot` rules on `sable_structure`, `sable_boat`,
`sable_car`, and `sable_formation` actions.

### Defaults

Lootr conversion is **on by default** for vessel containers. A definition that
already uses `container_loot` needs no changes to benefit from it.

Two fields control it:

| Field | Level | Meaning |
|---|---|---|
| `lootr_compatibility` | Vessel action | Default for every `container_loot` rule on that action. Defaults to `true`. |
| `lootr` | One `container_loot` rule | Overrides the action default for that rule only. |

```json
{
  "type": "sable_structure",
  "template": "myambushes:airship/hostile_balloon",
  "lootr_compatibility": true,
  "container_loot": [
    {
      "positions": [[6, 3, 4]],
      "loot_table": "myambushes:captains_chest"
    },
    {
      "positions": [[4, 2, 1], [5, 2, 1]],
      "loot_table": "myambushes:side_ammunition_hopper",
      "lootr": false,
      "replace_existing": true
    }
  ]
}
```

Set `lootr_compatibility: false` on the action to opt a whole vessel out, or
`"lootr": false` on a single rule to opt out one set of containers.

### What is never converted

- **Hoppers are never converted**, under any setting. They are ammunition and
  automation inventories, and replacing one would break the machine it feeds.
  A broad block selector or a rule that opts in explicitly still cannot convert
  a hopper.
- **A rule with more than one loot table is never converted.** Lootr holds one
  table per container, so a rule using `loot_tables` with several entries falls
  back to the vanilla path and fills the container by generating from each table
  in turn. Use a single `loot_table` for any container that should be per-player.

### Failure is safe

Conversion happens inside the assembled plot, not at the template's coordinates
in the dimension, because the container is on a Sable sub-level while the vessel
is moving.

If Lootr is not installed, reports itself not ready, declines the block, or the
conversion fails at any step, AMBUSH restores the original block and applies the
exact same authored table through the ordinary vanilla path. The physical chest
is never deleted from the vessel and the loot is never lost. Unsuccessful
conversions are logged at DEBUG level; a failed rollback is logged as a warning.

`replace_existing` applies only on the vanilla path. A converted Lootr container
takes its entire contents from the table, so schematic contents in it are
irrelevant.

Each container is filled once and recorded in the vessel's persistent state, so
a server restart does not re-roll a chest a player has already found.

### Choosing targets

A `container_loot` rule selects its containers the same way with or without
Lootr:

- `positions` (or `position`) names exact schematic-local coordinates. This is
  the reliable form when different containers need different tables.
- `blocks` matches every container of the listed block IDs.
- With neither, the rule applies to every container detected on the vessel.

Loot tables live at `data/<namespace>/loot_table/<path>.json` and must ship in
the same active pack as the definition.

### Filling hoppers instead

Because hoppers cannot be Lootr containers, fill them one of two ways:

- `initial_hopper_contents` (or `hopper_contents`) on the vessel action, which
  fills every detected hopper with a fixed list of stacks.
- A `container_loot` rule targeting the hopper positions, which fills them from
  a loot table through the vanilla path. Use `replace_existing: true` when the
  schematic's own contents must be discarded first.

Both are per-player-agnostic: every player sees the same hopper contents, which
is what ammunition feeds need.

### Testing

1. Confirm the loot tables are present in the same active pack, then run
   `/reload` and `/ambush admin check <namespace:id>`.
2. Spawn the vessel and run `/ambush admin inspect` to confirm the container
   coordinates and block IDs match the rule.
3. Open the container in-game. A converted Lootr container shows Lootr's own
   per-player behavior; an unconverted one is an ordinary chest with generated
   contents.
4. With Lootr installed, check with a second player that each gets an
   independent inventory.
5. If a container did not convert, enable `/ambush admin debug` and check the
   server log for the reason at DEBUG level. The most common causes are a rule
### Hardware requirements and inspection

`hardware_requirements` makes a vessel fail closed when a necessary category is missing. Supported checks include minimum seats, levers, buttons, analog controls, and engines or propellers. Keep these requirements aligned with the actual schematic so a template change cannot silently create an unusable vessel.

After spawning an AMBUSH vessel, run:

```mcfunction
/ambush admin inspect
```

The command selects the nearest active AMBUSH vessel and prints its action and template, schematic-local origin, configured controller and car controls, required hardware, local positions for detected hardware categories, and actual block IDs. Copy those local positions into `positions`, `power_positions`, `car_controls`, `container_loot`, or cannon-related rules. The full inspection report is also written to the server log.

### Final vessel test sequence

1. Confirm the template and all referenced loot tables are present under the same active datapack namespace.
2. Run `/reload`, then `/ambush admin check` and `/ambush admin check <namespace:id>`.
3. Turn on always mode with `/ambush always`, then start the definition with `/ambush <namespace:id>`.
4. Run `/ambush admin inspect` and correct any local-coordinate or hardware mismatch before tuning controller timings.
5. Test container contents and every redstone control, including a full sequenced battery cycle.
6. Run `/ambush clear` once to remove the complete AMBUSH-owned batch.
7. Turn always mode back off with a second `/ambush always`, then confirm the definition still runs under its own conditions.

The examples in this guide are the supported starting point. Validate every definition against the installed AMBUSH build before distributing it.

---


### External-pack delivery

External datapacks are the default deliverable. A complete pack contains
`pack.mcmeta` plus every generated definition, structure template, and custom
loot table beneath `data/<namespace>/`. Do not assume an encounter can refer to
a custom template or loot table that is not included in its active pack.

Bundling resources inside the AMBUSH jar remains supported when explicitly
requested. External resource edits use `/reload`; changed bundled resources
require a full game or server restart.

### Boats, cars, and mixed formations

- `sable_boat` requires `placement: "water"` and `ship_ai`. It may use the
  normal crew, cannon, redstone, event, steering, propulsion, cleanup, and
  container systems, but it cannot use `altitude_controller` or
  `envelope_fill`.
- `minimum_water_depth` is the number of continuous water blocks required below
  the exposed water surface. Its default is `2`.
- `water_spawn_height` controls the height above that exposed surface. Its
  default is `2` blocks.
- AMBUSH finds an eligible water surface and then leaves boat buoyancy and
  vertical movement to Sable/Aeronautics. It does not impose a waterline lock.
- `sable_car` requires `placement: "surface"`, `ship_ai`, and `car_controls`.
  Cars cannot use balloon or altitude-controller fields.
- A `sable_formation` member may explicitly be a `sable_structure`,
  `sable_boat`, or `sable_car`; mixed fleets are valid. Each member needs its
  own valid placement data and unique `structure_key`.

### Explicit vessel controls

Schematic state is preserved unless the action explicitly opts in to an AMBUSH
control. This prevents runtime code from silently replacing values saved in a
template.

- `propeller_direction` changes propulsion direction only when present.
- `bearing_never_place: true` changes mechanical-bearing placement mode only
  when explicitly set.
- `steering_controls` changes steering-wheel limits only when the array is
  present. Use `max_angle` for the maximum requested turn angle and `invert`
  when the real schematic orientation requires it.
- `ship_ai.range_holding_thrust_reversal: true` enables range-holding reverse
  thrust. Supply distinct `reverse_at_distance` and
  `resume_forward_at_distance` values to provide hysteresis.
- `ship_ai.recovery_turn_enabled: true` enables the full-lock recovery turn.
- `clutch_when_aligned: true` or `clutch_when_above: true` enables AMBUSH
  clutch control. Omit both to leave the schematic clutch untouched.
- `engine_burn_ticks` or `engine_burn_seconds` sets portable-engine burn time;
  `engine_superheated: true` sets its superheated state.

Airship safety and altitude behavior should also be authored explicitly. Useful
fields include `ground_clearance_enabled`, `minimum_ground_clearance`,
`descent_arrest_enabled`, `descent_arrest_margin`, `velocity_lookahead_ticks`,
`integral_gain`, `integral_limit`, and `max_signal_step`.

### CBC assembly, loading, and firing

`cannon_assembly` prepares the real assembly controls. Use
`power_mounts_directly: true` only where the schematic needs a direct
compatibility pulse. `cannon_assembly.force_mount_assembly: true` is an
additional explicit recovery option for a mount saved powered but without a
live contraption.

Use `initial_cannon_load_after_ticks` to preload hand-loaded CBC guns after
the vessel becomes operational. This happens independently of range and aim,
so the guns can stand by while the vessel approaches. Use
`cannon_reload_delay_ticks` for post-shot reload timing and
`cannon_reloads` when the reload rules differ from the initial load.

For independently aimed mounts, define one `redstone_activations` entry per
cannon. Pair its real button with `cannon_alignment`, and add
`fire_mount_local` when moving-hull wiring does not reliably deliver a CBC fire
edge. AMBUSH then pulses both the visible button and the named CBC mount's
native fire input at normal redstone strength. Never use one broad activation
as a substitute for several differently aimed cannons.

```json
{
  "positions": [[4, 2, 8]],
  "state": "button",
  "signal": 15,
  "button_ticks": 10,
  "repeat_ticks": 240,
  "range": 96,
  "require": "all",
  "require_living_crew": true,
  "fire_mount_local": [4, 2, 9],
  "cannon_alignment": {
    "mount_local": [4, 2, 9],
    "aim_local": [-1, 0, 0],
    "arc_degrees": 30
  }
}
```

For cannon diagnosis, inspect the server log. CBC-related AMBUSH diagnostics
are INFO-level and report assembly, preload status, range/alignment skips,
button pulses, direct mount activation, successful shots, and reloads.

### Optional FTB Quests hooks

FTB Quests is optional. AMBUSH loads without it; these fields and tags are
ignored when FTB Quests is not installed.

Tag an FTB quest with `ambush:trigger:<namespace:id>` to start that definition
when the quest starts. By default, online team members are sources and targets.
Optional target tags are `ambush:target:self`,
`ambush:target:player:<name>`, `ambush:target:nearest_other`,
`ambush:target:random_online`, and `ambush:target:all_other`.

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
`timeout_ticks`; otherwise its actions run once. Supported failure actions
include server commands and chained AMBUSH definitions.

```json
"failure_events": {
  "timeout_ticks": 12000,
  "minimum_kill_percent": 75,
  "actions": [
    { "type": "command", "command": "say {player} failed the convoy" },
    { "type": "ambush", "ambush": "myambushes:reinforcements" }
  ]
}
```

The command runs as the server at permission level 2. `{player}` is replaced
with the target player's name; chained ambushes still use AMBUSH's normal
chain-safety checks.

### Contract items

`quest_contract` is an action-level object that lets a player start an
encounter by using an item, and choose its target by naming the item. It works
without FTB Quests; any means of giving the item works.

```json
"quest_contract": {
  "item": "minecraft:paper",
  "attempt_interval_ticks": 600
}
```

| Field | Meaning |
|---|---|
| `item` | Required item registry ID the contract is carried on. |
| `attempt_interval_ticks` | Retry interval while the encounter cannot yet spawn. Default `600`; must be between `20` and `72000`. |

The item itself must carry `minecraft:custom_data` with an `ambush_contract`
string equal to the ID of the definition that declares the contract. Both must
match: the held item's registry ID against `item`, and the `ambush_contract`
value against the definition's own ID. If two definitions claim the same item
with different IDs, the contract is ignored and a warning is written to the
server log.

The player renames the item in an anvil to an online player's exact name, then
right-clicks it. Using an unrenamed contract prints a reminder and consumes
nothing. Naming a player who is not online prints a message and consumes
nothing. On a successful claim the item is consumed, and AMBUSH retries the
encounter against that player every `attempt_interval_ticks` until it can spawn
safely, reporting completion to the player who used the contract. Retries use
the definition's normal conditions, so a contract for a night-time encounter
waits for night. A pending contract survives the target logging out and resumes
when they return.

An FTB Quests reward can hand out a contract with an ordinary `give` command:

```text
give {p} minecraft:paper[minecraft:custom_data={ambush_contract:"myambushes:cannon_fleet"},minecraft:custom_name='{"text":"Cannon Fleet Contract — Rename to Target","italic":false}'] 1
```

`{p}` is the FTB Quests player placeholder. The item ID, the `ambush_contract`
value, and the corresponding `quest_contract` declaration must all agree.

---

## Compatibility

Create, Create Aeronautics/Simulated, Sable, FTB Quests, Lootr and Create Big Cannons are optional integrations.

Ordinary entity, sound, effect, and vanilla encounter definitions work without them. Sable actions fail closed when their required runtime is unavailable. A generic action that references a missing optional entity is skipped safely and reported in the server log rather than crashing the server. Use `/ambush admin check <namespace:id>` to identify missing requirements before testing.
