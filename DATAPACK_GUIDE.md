# AMBUSH Datapack Guide

(DEV NOTE: AMBUSH will be recieving even more content, and in the next update past 1.1.6, some of these things may be different, they will still work but you will be less limited).
Ambush is a server-authoritative NeoForge 1.21.1 mod for data-driven hostile encounters. Definitions are loaded from every active server datapack at:

```text
data/<namespace>/ambushes/<id>.json
```

Definitions are validated during reload. Invalid trigger names, malformed values, unsupported action modes, unsafe limits, and missing required resources are rejected with a useful server-log error rather than being silently ignored.

Create, Create Aeronautics/Simulated, Sable, and Create Big Cannons are optional integrations. Generic entity, sound, effect, and vanilla projectile encounters work without them. Sable actions fail closed when their required runtime is unavailable.

This document is a practical AMBUSH 1.1.6 authoring guide for easy and expanded definitions, ordinary encounters, natural vessels, factions, Sable vessels and formations, presentation, progression, hardware, containers, and optional integrations. AMBUSH has a large data surface, so the installed JAR remains the final authority for validation. Start with a small entity encounter, then add advanced systems only after the basic definition validates and runs correctly.

The encounter examples below use the names, IDs, and JSON bundled in
AMBUSH 1.1.6. Complete definitions can be copied as shown; labelled excerpts
show only the named part of a definition and belong with the rest of that
encounter. Run a bundled encounter manually with a leading underscore, such as
`/ambush _normal_night_zombie_horde`. For a namespaced external ID use
`/ambush _<namespace:id>`. The underscore is required only by the manual spawn
argument; administrative lookups keep the ordinary resource ID, for example
`/ambush admin check ambush:normal_night_zombie_horde`.

## Installation and reloads

Install Ambush on the server and connecting clients when using its client-synchronised features such as fog. Install your encounters as an external datapack in the world's `datapacks` folder. After changing the datapack, run `/reload`.

Use `/reload`, then `/ambush admin check` after every external datapack reload. Test a named definition with the manual spawn form `/ambush _<namespace:id>`.

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
    └── ambush/
        └── ambushes/
            └── normal_night_zombie_horde.json
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

The layout above uses the bundled encounter ID:

```text
ambush:normal_night_zombie_horde
```

### Using the examples in your datapack

The encounters shown in this guide are already included with AMBUSH. You can
run them by their listed IDs without creating a datapack.

To change one of them, copy its complete definition into your external pack
under the same namespace and filename. For Midnight Horde, use:

```text
<server-world>/datapacks/<your-pack>/data/ambush/ambushes/normal_night_zombie_horde.json
```

An enabled datapack with higher priority overrides the definition with the
same ID. Leave the installed mod untouched. The datapack resource stack performs the
override. After editing the external file, run `/reload`, then:

```mcfunction
/ambush admin check ambush:normal_night_zombie_horde
```

To create a separate encounter from a complete example, put it under your
own namespace and filename. Its command ID comes from that location.
Labelled excerpts are parts of existing definitions, not complete encounter
files.

Structure templates and custom loot tables for your own content belong in
your external pack:

```text
data/<namespace>/structure/<path>.nbt
data/<namespace>/loot_table/<path>.json
```

Keep references to AMBUSH's existing templates, ship profiles, and loot tables
when using them; those resources are supplied by the installed mod. Include
every custom resource your pack adds.

### Make encounters available in other worlds

For modpacks or encounters you want available in every world, we recommend
an optional global datapack loader such as
[Moonlight Lib](https://github.com/MehVahdJukaar/Moonlight/blob/1.21/README.md)
or [Fragmentum](https://obscurialithium.github.io/docs/fragmentum-layer/).
Use a version compatible with Minecraft 1.21.1 and NeoForge, and follow the
chosen mod's instructions for its global datapack location.

These mods are optional. AMBUSH encounters also work through ordinary external
datapacks: place your pack in each world's `datapacks` folder, enable it, then
run `/reload` and `/ambush admin check`.

AMBUSH validates definitions during datapack reload. Invalid triggers,
malformed values, unsupported action modes, unsafe limits, and missing required
resources are reported in the server log rather than being silently accepted.

---

## 2. Your first encounter: Midnight Horde

Midnight Horde is already bundled in AMBUSH. Its definition is at
`data/ambush/ambushes/normal_night_zombie_horde.json`:

Complete bundled definition: **Midnight Horde** (`ambush:normal_night_zombie_horde`).

```json
{
  "format": "easy",
  "name": "Midnight Horde",
  "description": "A broad zombie horde surrounds travelers after dark.",
  "preset": "night",
  "chance": 0.9,
  "cooldown_seconds": 600,
  "cooldown_group": "normal_night",
  "distance": 34,
  "conditions": {
    "dimensions": [
      "minecraft:overworld"
    ]
  },
  "mobs": [
    {
      "mob": "minecraft:zombie",
      "amount": 9,
      "avoid_line_of_sight": true
    },
    {
      "mob": "minecraft:husk",
      "amount": 3,
      "avoid_line_of_sight": true
    }
  ],
  "sound": "minecraft:entity.zombie.ambient",
  "weight": 25
}
```

Then run:

```mcfunction
/reload
/ambush admin check
/ambush _normal_night_zombie_horde
```

Running a definition by name never rolls its `chance`, and it clears that definition's cooldown group. It does still require every condition to be satisfied: the trigger requirement, time range, Y range, biome, and dimension. If the run fails, the command prints the reason.

To test outside those conditions, use always mode. `/ambush always` toggles always mode for the player who runs it; while it is ON, `/ambush _<namespace:id>` skips the condition check entirely, so a definition can be started at the wrong time of day, in the wrong biome, or away from its required blocks. Run `/ambush always` a second time to turn it off before testing natural behavior.

---

## 3. Easy-format definitions

AMBUSH 1.1.6 bundles **187** encounter definitions, and **83** of them use
`"format": "easy"`. Easy format is not a separate runtime system. During
reload, AMBUSH expands it into the same trigger, wave, condition, action, and
unlock model used by expanded definitions.

Midnight Horde from the previous section is a typical easy definition:

```json
{
  "format": "easy",
  "name": "Midnight Horde",
  "preset": "night",
  "chance": 0.9,
  "cooldown_seconds": 600,
  "cooldown_group": "normal_night",
  "distance": 34,
  "mobs": [
    {"mob": "minecraft:zombie", "amount": 9},
    {"mob": "minecraft:husk", "amount": 3}
  ],
  "sound": "minecraft:entity.zombie.ambient",
  "weight": 25
}
```

### Easy root fields

| Field | Meaning |
|---|---|
| `format` | Set to `"easy"` to use the easy compiler. |
| `preset` | Convenience preset. Defaults to `ground`. It can choose the trigger, add a simple condition, and choose the default mob placement. |
| `mobs` | Easy-format spawn groups. Each entry becomes an ordinary expanded spawn group. |
| `mobs[].mob` | Entity registry ID. It is expanded to `entity`. |
| `mobs[].amount` | Fixed count or the same count value accepted by an expanded spawn group. It is expanded to `count`. |
| `distance` | Outer horizontal spawn radius in blocks. If omitted, the easy compiler defaults to `56`, or `16` when `spawn_near_player` is true. |
| `minimum_distance` | Inner horizontal spawn radius in blocks. Defaults to `40`, or `4` when `spawn_near_player` is true. |
| `spawn_near_player` | Changes the easy distance defaults to the nearer `4..16` block band. It does not bypass normal placement safety. |
| `attempts` | Maximum placement attempts per spawned member. Defaults to `48`. |
| `check_every_seconds` | Interval-trigger check period in **seconds**. Defaults to `60`; the compiler multiplies it by 20 to create `check_every_ticks`. |
| `cooldown_seconds` | Successful-encounter cooldown in **seconds**. Defaults to `300`; the compiler multiplies it by 20 to create `cooldown_ticks`. |
| `chance` | Easy-format chance in **percentage points**. `5` means 5%; `0.5` means 0.5%. The compiler divides this number by 100 for the expanded chance policy. Defaults to `5`. |
| `cooldown_group` | Shared cooldown key. If omitted, AMBUSH derives one from the encounter name. |
| `weight` | Ordinary encounter-selection weight. Defaults to `100`. This is not the same as `natural_airship.encounter_weight`. |
| `conditions` | Normal conditions object. Easy format does not prevent use of the expanded condition system. |
| `sound` | One sound ID. It becomes a one-entry `sounds` array unless `sounds` is already supplied. |
| `sounds` | Expanded sound list; passed through unchanged. |
| `actions`, `wave`, `unlock`, `weight_scaling`, `first_unlock_weight` | Advanced fields that the easy compiler passes through, so an easy definition can still use advanced behavior. |

Easy-format timing is intentionally human-readable. For example, Frozen Stray
Patrol uses `check_every_seconds: 60`, `cooldown_seconds: 780`, and
`chance: 0.42`. Those become a 1,200-tick check period, a 15,600-tick cooldown,
and an expanded flat chance of `0.0042`, which is **0.42%** per check.

### Presets

The preset supplies defaults; explicit fields and a normal `conditions` object
can still add further restrictions.

| Preset | Default trigger / condition | Default mob placement |
|---|---|---|
| `ground` | `interval`; no extra preset condition | `land` |
| `night` | `interval`; adds `time: "night"` | `land` |
| `portal` | `portal` | `land` |
| `village` | `structure`; adds `#minecraft:village` | `land` |
| `outpost` | `structure`; adds `minecraft:pillager_outpost` | `land` |
| `water` | `interval` | `water` |
| `sky` | `interval`; adds a height band using `min_y` / `max_y` | `air` |
| `thunder`, `storm`, `stormy` | `interval`; adds thunder weather | `land` |
| `rain`, `rainy` | `interval`; adds rain weather | `land` |

For `sky`, the easy defaults are `min_y: 128` and `max_y: 512` when those
fields are not supplied.

A root `trigger` can override the preset's trigger. This is why bundled easy
encounters can use `trigger: "kill"`, `trigger: "structure"`, or other
supported triggers while still using easy mobs and timing fields.

### Easy mob entries

An easy mob entry starts with `mob` and `amount`, but it can use the ordinary
spawn-group presentation and behavior fields too. Unless overridden, the easy
compiler supplies `persistent: true`, `avoid_line_of_sight: true`,
`target: "nearby_players"`, and `friendly_fire: false`. Its default placement
comes from the preset as shown above.

For example, the bundled Spider Jockey Hunt uses `mob` / `amount` for the
spider while retaining the expanded `passengers` array. Other bundled easy
encounters use `equipment`, `effects`, `spawn_animation`, `reinforcements`,
`boss_bar`, `banner`, and `particles` inside easy mob entries.

### Easy chance versus expanded chance

Do not mix the two units:

- Easy root `chance` is a percentage: `4` = 4%, `0.5` = 0.5%.
- Expanded `trigger.chance.base` and `max` are fractions from `0` to `1`:
  `0.04` = 4%, `0.005` = 0.5%.

The expanded chance object may use `mode: "flat"` or `mode: "build_up"`.
`build_up` adds `increase_on_failure` after failed rolls up to `max`, and resets
on success unless `reset_on_success` is false.

### Core expanded fields

When writing an expanded definition, or reading what easy format compiles into,
these are the corresponding core fields:

| Field | Meaning |
|---|---|
| `trigger.type` | Encounter trigger, such as `interval`, `portal`, `structure`, or `kill`. |
| `trigger.check_every_ticks` | Check period in **ticks**. 20 ticks is about one second. |
| `trigger.cooldown_ticks` | Successful-encounter cooldown in **ticks**. |
| `trigger.chance` | Expanded chance policy; `base` / `max` use 0..1 fractions. |
| `cooldown_group` | Shared cooldown key. |
| `conditions` | Dimension, biome, height, time, structure, reputation, and other eligibility gates. |
| `wave` | Expanded ordinary entity wave. |
| `actions` | Expanded action list. |
| `weight` | Relative ordinary selection weight. |
| `allow_peaceful` | Allows the definition to create its supported content on Peaceful when true. |

A cooldown is consumed only after the encounter succeeds rather than merely
being considered for selection.

---

## 4. Trigger types

### Interval

`interval` is the normal trigger. It checks the definition on the configured schedule. Invasion Raiders has zero chance and weight because it is bundled invasion content; run its ID manually to inspect this wave.

Complete bundled definition: **Invasion Raiders** (`ambush:invasion_raiders`).

```json
{
  "name": "Invasion Raiders",
  "description": "Four crossbow Pillagers form a ranged raiding wave for AMBUSH invasions.",
  "trigger": {
    "type": "interval",
    "chance": {
      "mode": "flat",
      "base": 0,
      "max": 0
    }
  },
  "weight": 0,
  "wave": {
    "radius": 48,
    "groups": [
      {
        "entity": "minecraft:pillager",
        "count": 4,
        "persistent": true,
        "avoid_line_of_sight": false,
        "equipment": {
          "mainhand": "minecraft:crossbow"
        },
        "aggro_range": 128,
        "deaggro_range": 160,
        "nearby_player_range": 192,
        "tags": [
          "ambush_invasion_content"
        ]
      }
    ]
  }
}
```

### Portal

`portal` requires a Nether portal block nearby.

Complete bundled definition: **Nether Portal Warband** (`ambush:normal_nether_portal_warband`).

```json
{
  "format": "easy",
  "name": "Nether Portal Warband",
  "description": "A mixed Nether warband emerges near an active portal.",
  "preset": "portal",
  "chance": 4,
  "cooldown_seconds": 1200,
  "cooldown_group": "normal_portal",
  "distance": 34,
  "conditions": {
    "dimensions": [
      "minecraft:overworld",
      "minecraft:the_nether"
    ]
  },
  "mobs": [
    {
      "mob": "minecraft:zombified_piglin",
      "amount": 6,
      "avoid_line_of_sight": true,
      "particles": "minecraft:portal"
    },
    {
      "mob": "minecraft:piglin",
      "amount": 4,
      "avoid_line_of_sight": true,
      "equipment": {
        "mainhand": "minecraft:crossbow"
      }
    }
  ],
  "sound": "minecraft:block.nether_portal.ambient",
  "weight": 25
}
```

### Block active

`block_active` checks nearby blocks listed in `active_blocks`. It can also recognize compatible block entities that expose positive progress.

Complete bundled definition: **End Portal Breach** (`ambush:normal_end_portal_breach`).

```json
{
  "format": "easy",
  "name": "End Portal Breach",
  "description": "Endermen and endermites spill from an active End portal.",
  "trigger": "block_active",
  "chance": 5,
  "cooldown_seconds": 1200,
  "cooldown_group": "normal_portal",
  "distance": 30,
  "conditions": {
    "active_blocks": [
      "minecraft:end_portal"
    ],
    "dimensions": [
      "minecraft:overworld"
    ]
  },
  "mobs": [
    {
      "mob": "minecraft:endermite",
      "amount": 8,
      "avoid_line_of_sight": true,
      "particles": "minecraft:portal"
    },
    {
      "mob": "minecraft:enderman",
      "amount": 3,
      "avoid_line_of_sight": true,
      "particles": "minecraft:reverse_portal"
    }
  ],
  "sound": "minecraft:block.end_portal.spawn",
  "weight": 60
}
```

### Structure

`structure` requires the target to be standing on a piece of a listed structure. It matches only already-loaded structure pieces: it never performs a locate and never generates chunks.

Complete bundled definition: **Outpost Reserves** (`ambush:normal_structure_outpost_reserves`).

```json
{
  "format": "easy",
  "name": "Outpost Reserves",
  "description": "An outpost captain calls hidden reserves and an arrow volley.",
  "trigger": "structure",
  "preset": "ground",
  "chance": 4,
  "weight": 60,
  "cooldown_seconds": 1200,
  "cooldown_group": "normal_structure",
  "distance": 42,
  "conditions": {
    "structures": [
      "minecraft:pillager_outpost"
    ],
    "dimensions": [
      "minecraft:overworld"
    ]
  },
  "mobs": [
    {
      "mob": "minecraft:pillager",
      "amount": 7,
      "avoid_line_of_sight": true,
      "equipment": {
        "mainhand": "minecraft:crossbow"
      }
    },
    {
      "mob": "minecraft:vindicator",
      "amount": 1,
      "avoid_line_of_sight": true,
      "extra_health": 35,
      "extra_damage": 3,
      "banner": "black",
      "reinforcements": [
        {
          "id": "outpost_second_line",
          "trigger": {
            "type": "health_percent",
            "at_or_below_percent": 60
          },
          "actions": [
            {
              "type": "conditional_spawn",
              "radius": 34,
              "min_radius": 14,
              "spawns": [
                {
                  "entity": "minecraft:pillager",
                  "count": 5,
                  "avoid_line_of_sight": true,
                  "persistent": true,
                  "target": "owner",
                  "friendly_fire": false
                },
                {
                  "entity": "minecraft:vindicator",
                  "count": 2,
                  "avoid_line_of_sight": true,
                  "persistent": true,
                  "target": "owner",
                  "friendly_fire": false
                }
              ]
            },
            {
              "type": "directional_arrow_rain",
              "arrow": "minecraft:arrow",
              "pickup": "disallowed",
              "count": 18,
              "source_height": 26,
              "spread": 16,
              "target_spread": 6,
              "velocity": 1.55
            }
          ]
        }
      ]
    }
  ],
  "sound": "minecraft:entity.pillager.celebrate"
}
```

Entries are structure IDs, or structure tags when prefixed with `#`. A definition with no resolvable selector never matches. For a reusable set of selectors, declare `structure_groups` as a named object and select from it with `structure_group` or `use_structure_groups`.

### Kill

`kill` runs after the target kills a qualifying entity. The Bone Tempest excerpt below shows its real trigger and conditions; its five raid waves remain in the full bundled definition. `kill_count` sets how many kills are required, from `1` to `10000`, and defaults to `1`. `kill_entity` restricts it to one entity ID; omit it to count any kill.

Excerpt from the bundled definition: **The Bone Tempest** (`ambush:rare_thunder_bone_tempest_raid`).

```json
{
  "format": "easy",
  "name": "The Bone Tempest",
  "trigger": "kill",
  "kill_entity": "minecraft:skeleton",
  "kill_count": 1,
  "chance": 0.5,
  "weight": 20,
  "cooldown_seconds": 7200,
  "cooldown_group": "rare_kill_raid",
  "conditions": {
    "time": "night",
    "weather": "thunder",
    "dimensions": [
      "minecraft:overworld"
    ]
  }
}
```

`vanilla_raid_wave` is also accepted. It is driven by raid events rather than by the periodic check, so it does not run from a schedule and is not selected by `/ambush player`.

Unknown trigger names fail validation.

---

## 5. Spawn groups

Each entry in `spawns` is a separate entity group.

Excerpt from the bundled definition: **Invasion Raiders** (`ambush:invasion_raiders`).

```json
{
  "entity": "minecraft:pillager",
  "count": 4,
  "persistent": true,
  "avoid_line_of_sight": false,
  "equipment": {
    "mainhand": "minecraft:crossbow"
  },
  "aggro_range": 128,
  "deaggro_range": 160,
  "nearby_player_range": 192,
  "tags": [
    "ambush_invasion_content"
  ]
}
```

| Field | Meaning |
|---|---|
| `entity` | Required entity registry ID. |
| `count` | A fixed amount or a range with `min` and `max`. |
| `persistent` | Prevents normal despawning. |
| `tags` | Custom entity tags. |
| `target` | `owner` or `none`. `owner` is the default. |
| `aggro_through_walls` | Keeps retargeting through terrain. |
| `effects` | Effects applied to the spawned entity. |
| `passenger` | One passenger for each spawned entity. |
| `passengers` | A nested list of passengers. |


Spider Jockey Hunt uses the easy format for its spider group and the expanded passenger fields for each skeleton:

Excerpt from the bundled definition: **Spider Jockey Hunt** (`ambush:normal_night_spider_jockeys`).

```json
{
  "mob": "minecraft:spider",
  "amount": 5,
  "avoid_line_of_sight": true,
  "passengers": [
    {
      "entity": "minecraft:skeleton",
      "count": 1,
      "avoid_line_of_sight": true,
      "persistent": true,
      "target": "owner",
      "friendly_fire": false,
      "equipment": {
        "mainhand": "minecraft:bow"
      }
    }
  ]
}
```

---

## 6. Conditions

Use conditions to limit when an encounter can run.

Complete bundled definition: **Frozen Stray Patrol** (`ambush:normal_frozen_stray_patrol`).

```json
{
  "format": "easy",
  "name": "Frozen Stray Patrol",
  "description": "A light stray patrol stalks exposed players through snowy terrain.",
  "preset": "ground",
  "chance": 0.42,
  "weight": 55,
  "check_every_seconds": 60,
  "cooldown_seconds": 780,
  "cooldown_group": "normal_frozen_strays",
  "distance": 46,
  "minimum_distance": 32,
  "conditions": {
    "dimensions": [
      "minecraft:overworld"
    ],
    "biomes": [
      "minecraft:snowy_plains",
      "minecraft:ice_spikes",
      "minecraft:snowy_taiga",
      "minecraft:grove",
      "minecraft:snowy_slopes"
    ]
  },
  "mobs": [
    {
      "mob": "minecraft:stray",
      "amount": 4,
      "avoid_line_of_sight": true,
      "persistent": true,
      "equipment": {
        "mainhand": "minecraft:bow"
      }
    }
  ],
  "sound": "minecraft:entity.stray.ambient"
}
```

Use exact registry IDs. Multiple biome or dimension entries are alternatives.

---

## 7. Effects and sounds

Effects use `effect_id:duration_seconds:amplifier`.

Complete bundled definition: **Overworld Portal Piglin Incursion** (`ambush:overworld_portal_piglin_incursion`).

```json
{
  "format": "easy",
  "name": "Overworld Portal Piglin Incursion",
  "cooldown_group": "overworld_portal_piglin_incursion_spawns_portal_",
  "description": "Piglins rush players near an Overworld portal.",
  "preset": "portal",
  "chance": 0.05,
  "cooldown_seconds": 1800,
  "distance": 32,
  "conditions": {
    "dimensions": [
      "minecraft:overworld"
    ]
  },
  "mobs": [
    {
      "mob": "minecraft:piglin",
      "amount": 4,
      "aggro_through_walls": true,
      "effects": [
        "minecraft:speed:120:1"
      ],
      "banner": "red",
      "particles": "minecraft:portal",
      "tags": [
        "portal_incursion_piglin"
      ]
    },
    {
      "mob": "minecraft:piglin_brute",
      "amount": 2,
      "aggro_through_walls": true,
      "effects": [
        "minecraft:speed:120:1"
      ],
      "banner": "black",
      "particles": "minecraft:portal",
      "tags": [
        "portal_incursion_brute"
      ]
    }
  ],
  "sound": "minecraft:entity.piglin.angry",
  "weight": 60
}
```

---

## 8. Commands

Most administrative commands require permission level 2. `/ambush` and
`/ambush list` are available to ordinary players. Command permissions may also
be affected by the server's normal permission setup.

### Manual spawn IDs use `_`

AMBUSH 1.1.6 deliberately distinguishes a root manual-spawn argument from its
other subcommands. A manual encounter ID **must begin with `_`**:

```mcfunction
/ambush _normal_night_zombie_horde
```

For an explicitly namespaced ID, put the underscore before the resource ID:

```text
/ambush _<namespace:id>
```

The underscore is command syntax; it is **not** part of the datapack resource
ID. The file remains `data/<namespace>/ambushes/<id>.json`, and admin commands
still refer to `<namespace:id>` without `_`.

| Command | Purpose |
|---|---|
| `/ambush` | Prints the command summary and current always-mode state. |
| `/ambush list` | Lists loaded encounter definitions. |
| `/ambush _<namespace:id>` | Manually runs one named encounter against the command player. |
| `/ambush _<namespace:id> <player>` | Manually runs one named encounter against a named player. |
| `/ambush player <player>` | Runs a randomly selected eligible ordinary encounter against a named player. |
| `/ambush player <player> <namespace:id>` | Runs one selected definition through the player-command path. |
| `/ambush always` | Toggles per-player always mode for manual testing. |
| `/ambush clear` | Cancels active AMBUSH work and removes AMBUSH-owned encounter content. |
| `/ambush enable [pack]` | Enables natural spawning for a datapack namespace. Blank defaults to `ambush`. |
| `/ambush disable [pack]` | Disables natural spawning for a datapack namespace. Blank defaults to `ambush`. |
| `/ambush rate` | Shows the current natural-spawn rate multiplier. |
| `/ambush rate reset` | Restores the natural-spawn rate multiplier to `1.0`. |
| `/ambush rate up` | Doubles the current natural-spawn rate multiplier. |
| `/ambush rate down` | Halves the current natural-spawn rate multiplier. |
| `/ambush rate <multiplier>` | Sets the natural-spawn rate multiplier; the 1.1.6 command accepts `0.1` through `20.0`. |
| `/ambush admin check` | Reports loaded definitions and any hidden or rejected definitions. |
| `/ambush admin check <namespace:id>` | Read-only preflight for one definition and its requirements. |
| `/ambush admin inspect` | Inspects the nearest active AMBUSH vessel and reports local hardware/controller data. |
| `/ambush admin debug` | Toggles server diagnostics. |
| `/ambush admin weights` | Reports effective/base ordinary weights, chance, and cooldown groups. |
| `/ambush admin reputation` | Shows the command player's faction reputation standings. |
| `/ambush admin reputation set <target> <faction> <value>` | Sets a player's faction reputation for testing. The command parser accepts `-100000..100000`; the faction definition can impose its own standing range. |
| `/ambush admin unlocks` | Reports progress for definitions that declare an unlock. |
| `/ambush admin unlockall` | Unlocks every unlockable definition for the command player. |
| `/ambush admin spawning` | Toggles server-wide natural spawning. |
| `/ambush admin spawning status` | Reports the server-wide toggle and individually disabled definitions. |
| `/ambush admin spawning <namespace:id> [enable\|disable]` | Reads or changes one definition's natural-spawn toggle. |
| `/ambush invasion` | Opens the invasion command tree / reports invasion command usage. |
| `/ambush invasion start [profile] [waves]` | Starts an invasion; the bundled default profile is `ambush:standard` when no profile is supplied. |
| `/ambush invasion join` / `leave` / `ready` / `status` | Player participation and invasion status controls. |
| `/ambush invasion stop` | Stops the active invasion. |
| `/ambush invasion classify` | Runs the invasion classification command. |

In **this 1.1.6 JAR**, invasion commands are a root subtree under
`/ambush invasion` rather than part of the `admin` subtree.

Use `/ambush admin check <namespace:id>` before testing a complicated
definition. It performs a read-only preflight and reports definition, template,
placement, formation, hardware, lifecycle, and loot-rule issues without
spawning encounter content. Use `/ambush admin inspect` after spawning a vessel
to verify its schematic-local hardware and controls.

### Always mode

Always mode is a per-player toggle, not an argument. `/ambush always` turns it
on, and a second `/ambush always` turns it off. The command reports the new
state, and `/ambush` on its own reports the current state.

While always mode is ON, `/ambush _<namespace:id>` skips the normal eligibility
check for that player's manual run: trigger requirement, Y range, time range,
biome, dimension, portal proximity, and required nearby blocks. It is the
correct way to test a definition out of context. Turn always mode off before
judging natural behavior.

Always mode affects manual testing. It does not convert an encounter into a
natural spawn and does not replace the natural-airship scheduler.

### Natural-spawn controls and rate

`/ambush admin spawning` controls the server-wide natural-spawning switch.
`/ambush admin spawning status` reports that switch together with per-definition
disables. `/ambush enable [pack]` and `/ambush disable [pack]` operate on a
whole datapack namespace.

`/ambush rate` is a separate multiplier for natural-spawn pacing. `1.0` is the
normal rate. Use the explicit numeric form when tuning; `up`, `down`, and
`reset` are convenient test shortcuts. It does not rewrite a datapack's
`chance`, `weight`, or `natural_airship.encounter_weight` fields.

### Weights, reputation, and unlocks

`/ambush admin weights` is for the ordinary encounter-selection layer. Natural
vessels additionally use their natural-airship curve, pool, tier, reputation,
and `encounter_weight`, documented below.

`/ambush admin reputation` exposes the reputation state used by faction
conditions and natural-airship reputation bands. The `set` form is useful for
testing hostile, neutral, and allied content without grinding the standing in
a test world.

`/ambush admin unlocks` lists definitions with unlock progress and the player's
current state. `/ambush admin unlockall` is a test shortcut; authored unlock
behavior is controlled by the definition's `unlock` block.

---

## 9. Troubleshooting

**The definition is missing from `/ambush list`.**  
Run `/reload`, then read the server log. The validation error identifies the rejected definition and reason.

**The definition loads but does not run naturally.**  
Run `/ambush _<namespace:id>` first; if it fails, the command reports why. Then turn on `/ambush always` and run it again. If it works only with always mode ON, the definition itself is valid and one of its conditions is not being met — check the trigger, time range, dimension, biome, and Y range. Turn always mode back off with a second `/ambush always` before testing natural behavior again.

**The definition runs by name but never occurs on its own.**  
Check that natural spawning is enabled with `/ambush admin spawning status`, and that its namespace has not been disabled with `/ambush disable`. Then check its `chance` and `weight` with `/ambush admin weights`, and its unlock state with `/ambush admin unlocks`.

**Entities do not appear.**  
Increase `attempts` carefully and use a larger `radius`. Placement is bounded and may fail when no valid location exists.

**A large or complex definition behaves unexpectedly.**  
Run `/ambush admin check <namespace:id>`. It reports validation, required templates, placement settings, formation members, hardware requirements, lifecycle schedules, named wave sources, fill declarations, and loot rules without creating encounter content.

---

## 10. Advanced definitions

The expanded format is intended for reusable datapacks and advanced behavior.

Complete bundled definition: **Invasion Vanguard** (`ambush:invasion_vanguard`).

```json
{
  "name": "Invasion Vanguard",
  "description": "Three axe-bearing Vindicators form the opening vanguard of an AMBUSH invasion.",
  "trigger": {
    "type": "interval",
    "chance": {
      "mode": "flat",
      "base": 0,
      "max": 0
    }
  },
  "weight": 0,
  "wave": {
    "radius": 48,
    "groups": [
      {
        "entity": "minecraft:vindicator",
        "count": 3,
        "persistent": true,
        "avoid_line_of_sight": false,
        "equipment": {
          "mainhand": "minecraft:iron_axe"
        },
        "aggro_range": 128,
        "deaggro_range": 160,
        "nearby_player_range": 192,
        "tags": [
          "ambush_invasion_content"
        ]
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

Excerpt from the bundled definition: **Tiny Skeleton Balloon** (`ambush:balloon_skelly_tiny_10`).

```json
{
  "template": "ambush:skellyballoontiny"
}
```

the template file must be:

```text
data/ambush/structure/skellyballoontiny.nbt
```

Save the normal, unassembled structure template. Do not save an already
assembled Create contraption or a nested Sable sublevel. Required static blocks
and retained entities, including Simulated honey glue when used by the craft,
must remain in the template.

### First airship

Tiny Bogged Balloon is a complete bundled definition that uses the bundled
`ambush:bogged_balloon_tiny` ship profile. It requires Create, Sable, Simulated,
and Aeronautics. Turn on always mode with `/ambush always`, then start it with
`/ambush _bogged_balloon_tiny_early`.

Complete bundled definition: **Tiny Bogged Balloon** (`ambush:bogged_balloon_tiny_early`).

```json
{
  "name": "Tiny Bogged Balloon",
  "description": "A tiny bogged balloon drifts in with a poisoned archer aboard.",
  "natural_airship": {
    "curve": "ambush:default",
    "pool": "undead",
    "threat_tier": "tiny",
    "progression_stage": 0,
    "encounter_weight": 1.1,
    "budget": "normal",
    "attitudes": [
      "hostile"
    ]
  },
  "requires_mods": [
    "create",
    "sable",
    "simulated",
    "aeronautics"
  ],
  "trigger": {
    "type": "interval",
    "check_every_ticks": 1200,
    "cooldown_ticks": 36000,
    "chance": {
      "mode": "flat",
      "base": 0.012,
      "max": 0.012,
      "reset_on_success": true
    }
  },
  "cooldown_group": "bogged_balloon_tiny",
  "weight": 8.0,
  "conditions": {
    "dimensions": [
      "minecraft:overworld"
    ],
    "biomes": [
      "#minecraft:is_swamp"
    ],
    "height": {
      "min": 40,
      "max": 252
    }
  },
  "actions": [
    {
      "type": "profile",
      "profile": "ambush:bogged_balloon_tiny",
      "overrides": {
        "id": "bogged_balloon_tiny_early",
        "structure_key": "bogged_balloon_tiny_early",
        "spawn_distance": 26,
        "minimum_spawn_distance": 20,
        "maximum_clear_player_distance": 30,
        "avoid_line_of_sight": true
      }
    }
  ]
}
```

### Reusable ship profiles

AMBUSH 1.1.6 ships **19** reusable vessel profiles under:

```text
data/<namespace>/ambush_ship_profiles/<id>.json
```

A profile document wraps one concrete vessel action. The bundled
`ambush:bogged_balloon_tiny` profile begins like this:

```json
{
  "schema_version": 1,
  "action": {
    "faction": "undead",
    "type": "sable_structure",
    "template": "ambush:bogged_balloon_tiny",
    "placement": "air",
    "schematic_front": "north",
    "facing": "player",
    "spawn_distance": 26,
    "minimum_spawn_distance": 20,
    "maximum_clear_player_distance": 30
  }
}
```

An encounter reuses it with an action of `type: "profile"`:

```json
{
  "type": "profile",
  "profile": "ambush:bogged_balloon_tiny",
  "overrides": {
    "id": "bogged_balloon_tiny_early",
    "structure_key": "bogged_balloon_tiny_early",
    "spawn_distance": 26,
    "minimum_spawn_distance": 20,
    "maximum_clear_player_distance": 30,
    "avoid_line_of_sight": true
  }
}
```

`profile` is a namespaced ID resolved from the `ambush_ship_profiles` registry.
At reload, AMBUSH loads the profile's concrete `action` and deep-merges the
`overrides` object on top of it. The result must be a real action such as
`sable_structure`, `sable_boat`, or `sable_car`; a profile cannot resolve to
another unresolved `profile` action.

Use a profile for the parts that define the craft itself: template, controller,
crew, hardware, weapons, loot, patrol configuration, and other reusable vessel
behavior. Keep encounter-specific identity, spawn distance, formation key, or
other one-off changes in `overrides`. This avoids duplicating a full vessel
action across early-, late-, support-, convoy-, and skirmish encounters.

Custom profiles belong in the external datapack under the same registry path.
A profile referenced from the installed AMBUSH resources does not need to be
copied into your pack.

### Natural vessel spawning

A vessel that should participate in AMBUSH's natural vessel scheduler declares
`natural_airship`. This scheduler is separate from the ordinary encounter
chance/weight explanation earlier in the guide.

The bundled Tiny Bogged Balloon uses:

```json
"natural_airship": {
  "curve": "ambush:default",
  "pool": "undead",
  "threat_tier": "tiny",
  "progression_stage": 0,
  "encounter_weight": 1.1,
  "budget": "normal",
  "attitudes": ["hostile"]
}
```

| Field | Meaning |
|---|---|
| `curve` | Natural-airship curve resource. Bundled encounters use `ambush:default`. |
| `pool` | Candidate pool inside the curve, such as `pillager`, `undead`, `villager`, `piglin`, or `world_battle` in the bundled default curve. |
| `threat_tier` | Tier name selected through the current progression tier weights. The bundled curve uses `tiny`, `small`, `medium`, `heavy`, and `elite`. |
| `progression_stage` | Minimum player progression stage required before this candidate is eligible. Bundled content uses stages `0` through `3`. |
| `encounter_weight` | Relative weight among natural candidates in the selected pool/tier after the curve's other multipliers are applied. This is separate from root `weight`. |
| `budget` | Scheduler timing lane. Bundled values are `normal` and `major`; they select `normal_window` or `major_window`. It is not a mob-count budget. |
| `attitudes` | Allowed faction attitude states for this candidate. Bundled natural content uses `hostile`, `neutral`, `ally`, and `none` where appropriate. |
| `follow_up` | Optional per-encounter follow-up metadata; the curve also has global follow-up timing/chance controls. |

Natural-airship curves are datapack resources at:

```text
data/<namespace>/ambush_natural_airship_curves/<id>.json
```

The bundled `ambush:default` curve defines the timing and selection model. Its
core values are:

```json
{
  "primary": true,
  "normal_window": {
    "distribution": "triangular",
    "min_ticks": 48000,
    "mode_ticks": 72000,
    "max_ticks": 120000
  },
  "major_window": {
    "distribution": "triangular",
    "min_ticks": 96000,
    "mode_ticks": 144000,
    "max_ticks": 192000
  },
  "retry_interval_ticks": 1200,
  "nearby_player_share_radius": 384,
  "progression_kill_thresholds": {"0": 0, "1": 10, "2": 40, "3": 100}
}
```

All window values are **ticks**. At 20 ticks per second, the bundled normal
window is 40 to 100 minutes with a 60-minute mode, and the major window is 80
to 160 minutes with a 120-minute mode before rate scaling and normal runtime
eligibility are considered.

The same curve declares its pools. A pool can have its own selection `weight`,
associate itself with a faction, and choose a `reputation_score_mode`:

```json
"pools": {
  "pillager": {
    "weight": 1.0,
    "faction": "ambush:pillager",
    "reputation_score_mode": "negative_only"
  },
  "villager": {
    "weight": 0.75,
    "faction": "ambush:villager",
    "reputation_score_mode": "full"
  },
  "world_battle": {
    "weight": 0.2,
    "reputation_score_mode": "none"
  }
}
```

`progression_kill_thresholds` determines the player's current stage. A candidate
whose `progression_stage` is above that current stage is not eligible. Then
`progression_tier_weights` chooses which threat tiers are available and how
strongly each is weighted for that stage.

For faction-backed pools, `reputation_bands` can multiply both the pool's
selection weight and particular tier weights. This is why reputation can make a
pillager or allied-faction natural encounter more or less common without
changing that encounter's ordinary root `weight`.

`nearby_player_share_radius` shares the next natural scheduling delay with
nearby players after a spawn, preventing a cluster of players from each
receiving a fully independent vessel timer. `retry_interval_ticks` is the
bounded retry interval when a due opportunity cannot currently produce a valid
encounter.

Use `/ambush rate` to inspect or change the global natural pacing multiplier,
`/ambush admin reputation` to test reputation bands, and `/ambush admin spawning
status` to make sure natural spawning is actually enabled.

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

Excerpt from the bundled definition: **Redsail Fishing Boat (Tiniest)** (`ambush:redsail_tiniest`).

```json
{
  "redstone_activations": [
    {
      "id": "port_autocannon",
      "positions": [
        [
          2,
          5,
          10
        ]
      ],
      "fire_mount_local": [
        2,
        5,
        11
      ],
      "state": "button",
      "signal": 15,
      "button_ticks": 6,
      "after_ticks": 160,
      "repeat_ticks": 80,
      "range": 72,
      "vertical_tolerance": 16,
      "require_living_crew": true,
      "require": "all",
      "player_direction": "left",
      "direction_tolerance_degrees": 40,
      "cannon_alignment": {
        "mount_local": [
          2,
          5,
          11
        ],
        "aim_local": [
          -1,
          0,
          0
        ],
        "arc_degrees": 35
      }
    },
    {
      "id": "starboard_autocannon",
      "positions": [
        [
          8,
          5,
          10
        ]
      ],
      "fire_mount_local": [
        8,
        5,
        11
      ],
      "state": "button",
      "signal": 15,
      "button_ticks": 6,
      "after_ticks": 160,
      "repeat_ticks": 80,
      "range": 72,
      "vertical_tolerance": 16,
      "require_living_crew": true,
      "require": "all",
      "player_direction": "right",
      "direction_tolerance_degrees": 40,
      "cannon_alignment": {
        "mount_local": [
          8,
          5,
          11
        ],
        "aim_local": [
          1,
          0,
          0
        ],
        "arc_degrees": 35
      }
    }
  ]
}
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

`cannon_reloads` is an optional integration for assembled Sable ships. It does nothing when Create, CBC, Sable, or Simulated
is unavailable.

The reload action runs after a successful shot. It requires at least one living
configured crew member.

Excerpt from the bundled definition: **Cast Iron Frigate** (`ambush:cast_iron_frigate`).

```json
{
  "cannon_reloads": [
    {
      "id": "frigate_gun_0_reload",
      "mount_local": [
        5,
        5,
        10
      ],
      "required": true,
      "shell_stack_snbt": "{id:\"createbigcannons:solid_shot\",count:1}",
      "propellant_type": "powder_charge",
      "propellant_stack_snbt": "{id:\"createbigcannons:powder_charge\",count:1}",
      "propellant_blocks": 7,
      "propellant_power": 1,
      "reload_after_ticks": 120,
      "unload_before_load": true
    }
  ]
}
```

| Field | Meaning |
|---|---|
| `id` | Stable name for this reload rule. |
| `mount_local` | Cannon-mount block-entity coordinates, measured from the Sable plot minimum. |
| `reload_after_ticks` | Delay after a successful shot before the reload attempt. |
| `shell_stack_snbt` | Shell item stack in SNBT form. |
| `propellant_stack_snbt` | Propellant item stack in SNBT form. |
| `propellant_type` | Loading mode for the configured propellant. The bundled frigate uses `powder_charge`. |
| `propellant_blocks` | Number of propellant blocks/charges requested for the load. |
| `propellant_power` | Power value supplied to the supported loading bridge when applicable. |
| `unload_before_load` | Unloads the configured cannon before inserting the new load when true. |

AMBUSH checks the compatible CBC loading API at runtime and does not write cannon
NBT. Powder-charge loading is supported, as shown by the bundled Cast Iron
Frigate definition above. Use mutually compatible Create/CBC versions for the
server pack and verify the specific cannon with `/ambush admin check` plus an
in-game test.

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

Every vessel action needs a namespaced `template`. Vessel templates are stored at `data/<namespace>/structure/<path>.nbt`; a template ID such as `ambush:autocannon_pillager_car` resolves to `data/ambush/structure/autocannon_pillager_car.nbt`.

`sable_formation` has a `members` array. Members inherit compatible parent fields and override only what differs. A member may explicitly be `sable_structure`, `sable_boat`, or `sable_car`, so one fleet can mix airships with boats or cars. Keep every member's placement, template, and hardware independently valid.

### Ocean-boat placement

`sable_boat` resolves the exposed water surface at each candidate anchor and
counts consecutive water blocks downward. `minimum_water_depth` rejects shallow
water. After assembly AMBUSH never changes the boat's position or velocity;
Sable/Aeronautics alone handle all motion and buoyancy.

Excerpt from the bundled definition: **Redsail Fishing Boat (Tiniest)** (`ambush:redsail_tiniest`).

```json
{
  "type": "sable_boat",
  "template": "ambush:redsail_tiniest",
  "placement": "water",
  "minimum_water_depth": 4,
  "ship_ai": {
    "mode": "broadside",
    "distance": 14,
    "distance_tolerance": 4,
    "correction_range": 24,
    "target_range": 320,
    "range_holding_thrust_reversal": false,
    "recovery_turn_enabled": true,
    "match_player_y": true,
    "turn_lead_blocks": 48,
    "come_about_enabled": true,
    "come_about_aft_degrees": 100,
    "come_about_release_degrees": 30,
    "come_about_max_ticks": 300,
    "recovery_circle_enabled": true,
    "recovery_circle_ticks": 160,
    "recovery_circle_degrees": 90
  }
}
```

The placement fields `minimum_water_depth` and `water_spawn_height` are optional: the defaults are `minimum_water_depth: 2` and
`water_spawn_height: 2`. Depth must be 1–256 blocks and spawn height 1–16.

### Ground-car definition

Cars are surface-anchored. Use `offset_y` when the schematic needs to spawn above the resolved ground surface. Their `ship_ai` supports broadside or chase selection; `car_controls` maps that intent to the car's actual clutch, reversing, throttle, and steering hardware.

Excerpt from the bundled definition: **Autocannon Pillager Car** (`ambush:autocannon_pillager_car`).

```json
{
  "type": "sable_car",
  "template": "ambush:autocannon_pillager_car",
  "assembly_origin": [
    3,
    1,
    7
  ],
  "placement": "surface",
  "spawn_distance": 450,
  "schematic_front": "north",
  "facing": "player",
  "ship_ai": {
    "mode": "chase",
    "distance": 14,
    "distance_tolerance": 4,
    "correction_range": 16,
    "target_range": 640,
    "recovery_turn_enabled": true,
    "match_player_y": true
  },
  "car_controls": {
    "left_turn_lever": [
      2,
      1,
      4
    ],
    "right_turn_lever": [
      4,
      1,
      4
    ],
    "turn_mode": "direct",
    "forward_lever": [
      4,
      2,
      4
    ],
    "reverse_lever": [
      2,
      2,
      4
    ],
    "drive_signal": 15,
    "turn_inner_signal": 0,
    "idle_signal": 0,
    "stop_distance": 10,
    "target_still_speed": 0.01,
    "reverse_when_behind_degrees": 135,
    "turn_deadband_degrees": 10,
    "update_ticks": 10
  }
}
```

With `ship_ai.mode: "broadside"`, a car keeps the chosen side toward its target and circles within `target_range`; it uses the chase controller when the target is farther away. It brakes only when the target is sufficiently still and inside the configured broadside brake band. `ship_ai.mode: "chase"` is the separate, data-driven direct-approach mode.

Use `/ambush admin inspect` to verify the actual coordinates and block IDs after assembly.

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

Excerpt from the bundled definition: **Pillager Potato Car** (`ambush:pillager_potato_car`).

```json
{
  "component": "button",
  "positions": [
    [
      4,
      0,
      0
    ],
    [
      5,
      0,
      0
    ],
    [
      6,
      0,
      0
    ],
    [
      7,
      0,
      0
    ]
  ],
  "power_positions": [
    [
      4,
      1,
      1
    ],
    [
      5,
      1,
      1
    ],
    [
      6,
      1,
      1
    ],
    [
      7,
      1,
      1
    ]
  ],
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

Excerpt from the bundled definition: **Redsail Fishing Boat (Tiniest)** (`ambush:redsail_tiniest`).

```json
{
  "id": "port_autocannon",
  "positions": [
    [
      2,
      5,
      10
    ]
  ],
  "fire_mount_local": [
    2,
    5,
    11
  ],
  "state": "button",
  "signal": 15,
  "button_ticks": 6,
  "after_ticks": 160,
  "repeat_ticks": 80,
  "range": 72,
  "vertical_tolerance": 16,
  "require_living_crew": true,
  "require": "all",
  "player_direction": "left",
  "direction_tolerance_degrees": 40,
  "cannon_alignment": {
    "mount_local": [
      2,
      5,
      11
    ],
    "aim_local": [
      -1,
      0,
      0
    ],
    "arc_degrees": 35
  }
}
```

`mount_local` is the real cannon/muzzle location in the template and `aim_local` is its real barrel direction (it need not be unit length). Use one activation per weapon when their arcs differ. The same feature applies to straight-firing autocannons: use their barrel origin and a narrow vector, for example `"aim_local": [0, 0, -1]` with `"arc_degrees": 3`. This gate is optional and has no effect on existing activations.

### Airship movement, crew, formations, and lifecycle

For an airship, `envelope_fill` is a one-time initial balloon fill; it is not an altitude controller. Use `altitude_controller` for ongoing lift control, and keep its lever changes gradual enough for the craft's buoyancy to respond. Useful fields include `mode` (`player_offset` or `absolute`), `offset`, `tolerance`, `hover_signal`, `minimum_signal`, `maximum_signal`, `velocity_lookahead_ticks`, and terrain-clearance limits.

`ship_ai` supports `chase`, `broadside`, `orbit`, `boarding`, `tnt_drop`, `overhead`, `flyover`, and `disabled`. `overhead` and `flyover` are airship-only (`sable_structure`) modes and require an `altitude_controller`: `overhead` steers to the player’s X/Z position and holds there, while `flyover` steers through that position without the overhead stop. Author altitude with `ship_ai.match_player_y: false` and an `altitude_controller.offset` (or a player-Y band) to keep the hull above the target. To fire real mounted cannons down or diagonally down, physically aim the schematic’s mount that way and use a per-cannon `redstone_activations.cannon_alignment` vector with suitable range and real `positions`/`power_positions`. Chase uses a `chase_controller`; a meaningful gap between its reverse and resume distances prevents repeated direction changes. `steering_controls` describes the steering hardware. If a craft turns consistently the wrong way, verify `schematic_front` first, then use the steering control's `invert` setting when appropriate.

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

Excerpt from the bundled definition: **Redsail Fishing Boat (Tiniest)** (`ambush:redsail_tiniest`).

```json
{
  "container_loot": [
    {
      "blocks": [
        "minecraft:hopper"
      ],
      "loot_table": "ambush:autocannon_hopper",
      "replace_existing": true
    },
    {
      "blocks": [
        "minecraft:chest"
      ],
      "loot_table": "ambush:chests/pillager_ship_supply",
      "lootr": true
    },
    {
      "blocks": [
        "minecraft:barrel"
      ],
      "loot_table": "ambush:chests/pillager_ship_supply",
      "lootr": true
    }
  ]
}
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

Excerpt from the bundled definition: **Redsail Fishing Boat (Tiniest)** (`ambush:redsail_tiniest`).

```json
{
  "type": "sable_boat",
  "template": "ambush:redsail_tiniest",
  "lootr_compatibility": true,
  "container_loot": [
    {
      "blocks": [
        "minecraft:hopper"
      ],
      "loot_table": "ambush:autocannon_hopper",
      "replace_existing": true
    },
    {
      "blocks": [
        "minecraft:chest"
      ],
      "loot_table": "ambush:chests/pillager_ship_supply",
      "lootr": true
    },
    {
      "blocks": [
        "minecraft:barrel"
      ],
      "loot_table": "ambush:chests/pillager_ship_supply",
      "lootr": true
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

Loot tables live at `data/<namespace>/loot_table/<path>.json`. Include your
custom tables in the external pack; AMBUSH's existing tables are supplied by
the installed mod.

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

1. Confirm the loot tables are supplied by AMBUSH or an active datapack, then run
   `/reload` and `/ambush admin check <namespace:id>`.
2. Spawn the vessel and run `/ambush admin inspect` to confirm the container
   coordinates and block IDs match the rule.
3. Open the container in-game. A converted Lootr container shows Lootr's own
   per-player behavior; an unconverted one is an ordinary chest with generated
   contents.
4. With Lootr installed, check with a second player that each gets an
   independent inventory.
5. If a container did not convert, enable `/ambush admin debug` and check the
   server log for the reason at DEBUG level.

### Hardware requirements and inspection

`hardware_requirements` makes a vessel fail closed when a necessary category is missing. Supported checks include minimum seats, levers, buttons, analog controls, and engines or propellers. Keep these requirements aligned with the actual schematic so a template change cannot silently create an unusable vessel.

After spawning an AMBUSH vessel, run:

```mcfunction
/ambush admin inspect
```

The command selects the nearest active AMBUSH vessel and prints its action and template, schematic-local origin, configured controller and car controls, required hardware, local positions for detected hardware categories, and actual block IDs. Copy those local positions into `positions`, `power_positions`, `car_controls`, `container_loot`, or cannon-related rules. The full inspection report is also written to the server log.

### Final vessel test sequence

1. Confirm the template and all referenced loot tables are supplied by AMBUSH or an active datapack, and that each reference uses the correct namespace.
2. Run `/reload`, then `/ambush admin check` and `/ambush admin check <namespace:id>`.
3. Turn on always mode with `/ambush always`, then start the definition with `/ambush _<namespace:id>`.
4. Run `/ambush admin inspect` and correct any local-coordinate or hardware mismatch before tuning controller timings.
5. Test container contents and every redstone control, including a full sequenced battery cycle.
6. Run `/ambush clear` once to remove the complete AMBUSH-owned batch.
7. Turn always mode back off with a second `/ambush always`, then confirm the definition still runs under its own conditions.

The encounter definitions and labelled excerpts in this guide come from the bundled AMBUSH resources. Validate every definition against the installed AMBUSH build before distributing it.

---



## 15. Factions, reputation, support, and skirmishes

AMBUSH factions are datapack resources rather than hard-coded lists. Faction
definitions live at:

```text
data/<namespace>/ambush_factions/<id>.json
```

AMBUSH 1.1.6 bundles six faction definitions: `illager`, `neutral`, `piglin`,
`pillager`, `undead`, and `villager` in the `ambush` namespace.

### Faction definitions

A faction can declare enemies, how player standing works, and which mob types
belong to it. The bundled villager faction includes:

```json
{
  "hostile_to": [
    "ambush:pillager",
    "ambush:illager",
    "ambush:piglin",
    "ambush:undead"
  ],
  "standing": {
    "mode": "reputation",
    "start": 80,
    "ally_at": 1000,
    "hostile_below": 0,
    "minimum": -2000,
    "maximum": 2000,
    "on_kill_own_mob": -25,
    "on_kill_enemy_mob": 10,
    "on_defeat_enemy_ambush": 50
  },
  "mob_types": {
    "minecraft:villager": -2,
    "minecraft:wandering_trader": -2,
    "minecraft:iron_golem": -3,
    "guardvillagers:guard": -2
  }
}
```

`standing.mode: "reputation"` enables a numeric player standing with an initial
value and configured hostile/ally thresholds. The `minimum` and `maximum` clamp
the faction's stored standing. The `on_kill_*` and
`on_defeat_enemy_ambush` values are automatic standing changes associated with
those events.

A standing object can also declare `events` that present a toast when the
player reaches an `ally`, `neutral`, `hostile`, or numeric `threshold` event.
The bundled villager faction uses weighted alternate ally toasts.

Hostile factions in the bundled data use `standing.mode: "hostile"` where a
player-reputation ladder is not needed. `ambush:neutral` is marked as the
default for otherwise unassigned actions.

Actions and vessel profiles can declare a `faction`. Faction ownership feeds
AMBUSH targeting, friendly-fire behavior, reputation rewards/penalties, faction
responses, patrol interception, and natural-airship pool scoring. Prefer full
namespaced faction IDs in external data even though bundled action data may use
short built-in names in places.

### Reputation conditions

Encounter eligibility can directly require a standing band with
`conditions.faction_reputation`. The bundled Illager Patrol Airship contains:

```json
"faction_reputation": [
  {
    "faction": "pillager",
    "max": -300
  }
]
```

Use `min` and/or `max` to define the accepted standing range. This condition is
separate from natural-airship `reputation_bands`: the condition is a hard
eligibility gate, while the curve bands can change selection weights.

Use `/ambush admin reputation` to inspect current values and
`/ambush admin reputation set <target> <faction> <value>` when testing the
boundaries.

### Faction support definitions

Faction-call support is a second registry:

```text
data/<namespace>/ambush_faction_support/<id>.json
```

AMBUSH 1.1.6 bundles villager and piglin support definitions. The villager
support definition is:

```json
{
  "gift_ambush": "ambush:villager_alliance_support_balloon",
  "call_sound": "minecraft:item.goat_horn.sound.6",
  "sound_volume": 4.0,
  "sound_pitch": 1.0,
  "arrival_min_ticks": 1200,
  "arrival_max_ticks": 2400,
  "cooldown_at_ally_ticks": 36000,
  "cooldown_at_max_ticks": 12000,
  "support_ambushes": {
    "minecraft:overworld": "ambush:villager_faction_support_ship"
  }
}
```

`gift_ambush` is the alliance gift definition. `support_ambushes` maps a
dimension to the encounter that arrives when support is called there. Arrival
and cooldown values are ticks. The cooldown interpolates between the configured
ally standing and the faction's maximum standing, so stronger standing can
produce the shorter configured cooldown.

### Skirmish action

`skirmish` creates opposed faction sides from normal/profile vessel actions.
The bundled Faction Fleet Battle uses real reusable profiles:

```json
{
  "type": "skirmish",
  "spawn_distance": 300,
  "separation": 220,
  "sides": [
    {
      "faction": "illager",
      "count": 3,
      "side_spread_degrees": 10,
      "type": "profile",
      "profile": "ambush:pillager_autocannon_balloon",
      "overrides": {
        "id": "faction_fleet_battle_illager",
        "faction": "illager"
      }
    },
    {
      "faction": "undead",
      "count": 3,
      "side_spread_degrees": 10,
      "type": "profile",
      "profile": "ambush:bogged_balloon_tiny",
      "overrides": {
        "id": "faction_fleet_battle_undead",
        "faction": "undead"
      }
    }
  ]
}
```

`spawn_distance` moves the battle center away from the encounter player;
`separation` keeps the opposing side anchors apart. Each side has its own
`faction`, repeat `count`, and optional angular spread. A side can use a profile
plus overrides, which is preferable to pasting a complete vessel definition.
The resulting faction ownership gives the spawned sides a reason to engage each
other through the normal faction system.

---

## 16. Mob presentation, boss bars, telegraphs, and reinforcements

Easy-format `mobs` and expanded spawn groups both accept presentation and
behavior fields. The bundled **Wandering Hexarch** demonstrates all of the most
important pieces in one encounter.

### Spawn animation

A spawn group may declare `spawn_animation`. Bundled 1.1.6 encounters use the
animation types `ground`, `flung`, `falling`, and `lightning`.

The Hexarch itself uses:

```json
"spawn_animation": {
  "type": "lightning",
  "visual_only": true,
  "count": 2
}
```

Its escort group uses a ground entrance:

```json
"spawn_animation": {
  "type": "ground",
  "duration_ticks": 30,
  "depth": 1.5
}
```

Animation fields depend on the selected type. Bundled definitions use fields
such as `duration_ticks`, `depth`, `height`, velocities/distances, `count`, and
`visual_only`. Animation is presentation around the spawn; it does not replace
the group's actual placement rules.

### Equipment

`equipment` assigns explicit equipment slots on each spawned entity. Common
slots are:

```json
"equipment": {
  "mainhand": "minecraft:crossbow",
  "offhand": "minecraft:shield",
  "helmet": "minecraft:iron_helmet",
  "chestplate": "minecraft:iron_chestplate",
  "leggings": "minecraft:iron_leggings",
  "boots": "minecraft:iron_boots"
}
```

Use only the slots the encounter needs. Equipment is applied to the spawned
mob; it does not imply a particular AI, seat, or faction.

### Boss bars

A spawn group may own a `boss_bar`. The Wandering Hexarch uses:

```json
"boss_bar": {
  "name": "The Wandering Hexarch",
  "color": "purple",
  "overlay": "notched_10",
  "audience": "nearby",
  "range": 128
}
```

`name`, `color`, and `overlay` control presentation. `audience: "nearby"` with
`range` limits who sees it. Other boss-bar presentation fields used by AMBUSH
include the optional boss-music/fog and screen/sky darkening controls where a
definition needs them.

Do not confuse an entity `boss_bar` with a vessel-level fleet or hull health
presentation; those are separate vessel systems.

### Reinforcements

`reinforcements` belongs to a spawned mob/group and runs actions later based on
that mob's state. Two trigger types appear in the bundled encounters:

- `time_alive` with `after_ticks`
- `health_percent` with `at_or_below_percent`

The Hexarch launches a repeated slowing-arrow action after surviving 600 ticks:

```json
"reinforcements": [
  {
    "id": "hexarch_slowing_rain",
    "trigger": {
      "type": "time_alive",
      "after_ticks": 600
    },
    "repeat_ticks": 600,
    "actions": [
      {
        "type": "directional_arrow_rain",
        "arrow": "minecraft:tipped_arrow",
        "potion": "minecraft:strong_slowness",
        "count": 20,
        "source_height": 28,
        "spread": 18,
        "target_spread": 7,
        "velocity": 1.55
      }
    ]
  }
]
```

A health trigger is useful for a one-time second phase. Outpost Reserves, for
example, triggers its second line at or below 60% health and then runs a
`conditional_spawn` plus directional arrow rain.

### Telegraphs

Projectile/rain actions can declare a `telegraph` so the target sees the danger
before impact. The Hexarch's slowing rain uses:

```json
"telegraph": {
  "shape": "path",
  "particle": "minecraft:witch",
  "duration_ticks": 40,
  "interval_ticks": 5,
  "radius": 7,
  "circle_points": 36,
  "path_points": 28,
  "arc_height": 5,
  "follow_target": true
}
```

`duration_ticks` is how long the warning runs and `interval_ticks` is the
particle update interval. Shape-specific fields control the circle/path sample
count and arc. `follow_target: true` keeps the warning tied to the moving target
while it is active.

---

## 17. Advanced vessel behavior: turrets, ropes, patrols, convoys, and capture

These systems operate on assembled vessels and use **schematic-local**
coordinates. Validate them with `/ambush admin check <namespace:id>` and use
`/ambush admin inspect` on the assembled craft before guessing coordinates.

### Auto-aim turrets

`auto_aim_turrets` controls a real turret already built into the schematic. It
does not create a cannon or steering assembly. The bundled Cast Iron Frigate
uses two turret definitions. Its bow turret begins:

```json
{
  "id": "bow_turret",
  "steering_wheel_local": [6, 11, 22],
  "yaw_sign": -1,
  "max_yaw_degrees": 180,
  "fire_tolerance_degrees": 12,
  "range": 160,
  "repeat_ticks": 200,
  "after_ticks": 240,
  "cannons": [
    {
      "mount_local": [5, 10, 21],
      "fire_button_local": [5, 10, 20]
    },
    {
      "mount_local": [7, 10, 21],
      "fire_button_local": [7, 10, 20]
    }
  ],
  "firing_arc_degrees": 320,
  "firing_forward_local": [0, 0, -1]
}
```

`steering_wheel_local` identifies the real yaw control. `yaw_sign` adapts the
schematic's mechanical direction. `max_yaw_degrees` is the permitted yaw
travel, while `fire_tolerance_degrees` is how closely the turret must be aimed
before firing. `range`, `after_ticks`, and `repeat_ticks` gate engagement.
`cannons` maps each real CBC mount to its real fire button.

A turret can also declare a `pitch` controller. The bundled bow turret maps an
engine, gearshift control, gearshift, clutch control, and clutch; it can
calibrate direction polarity and defines pitch tolerance, direction-switch
hysteresis, braking, and stall recovery. Use this only when the schematic
actually contains that mechanism.

### Rope links

`rope_links` recreates authored links between two real local endpoints after
assembly. The bundled `ambush:redsail_tiny` profile contains entries such as:

```json
"rope_links": [
  {"from": [4, 8, 10], "to": [4, 6, 9]},
  {"from": [8, 8, 2], "to": [8, 6, 1]},
  {"from": [12, 8, 10], "to": [12, 6, 9]}
]
```

Both endpoints are `[x, y, z]` coordinates from the saved schematic's minimum
corner. Keep the endpoint blocks compatible with the rope system used by the
installed pack. AMBUSH validates the declaration; it does not invent suitable
anchors when the schematic is wrong.

### Patrols

`patrol` gives a vessel an objective independent of simply chasing the encounter
player. The bundled `ambush:villager_airship_tiny` profile patrols villages:

```json
"patrol": {
  "mode": "orbit",
  "target": {
    "type": "structure",
    "structure": "#minecraft:village",
    "search_radius": 1024
  },
  "fallback_target": {
    "type": "position"
  },
  "patrol_radius": 96,
  "intercept_range": 512,
  "max_pursuit_from_patrol": 768,
  "point_interval_ticks": 400
}
```

`target` selects what is being protected/patrolled. A structure target searches
within `search_radius`; the fishing escort fleet also demonstrates an ambush
vessel target. `patrol_radius` sets the orbit/area size, `intercept_range` lets
the vessel engage a threat, and `max_pursuit_from_patrol` is the leash that
prevents an interceptor from abandoning the patrol indefinitely.
`point_interval_ticks` controls how frequently the patrol objective advances.

### Convoys

A formation may also have a `convoy` controller. The bundled Villager Fishing
Cargo Convoy uses:

```json
"convoy": {
  "navigation": "follow_player",
  "leader_loss": "promote",
  "max_pursuit_from_convoy": 160,
  "formation": {
    "mode": "custom",
    "tolerance": 18,
    "slots": {
      "port_front": {"x": -64, "y": 0, "z": -48},
      "starboard_front": {"x": 64, "y": 0, "z": -48},
      "port_aft": {"x": -64, "y": 0, "z": 48},
      "starboard_aft": {"x": 64, "y": 0, "z": 48}
    }
  }
}
```

Formation members declare roles such as `leader`, `cargo`, or `escort` through
`convoy_role`; custom escorts use `convoy_slot` to bind to a named slot.
`navigation` controls the convoy's overall objective, `leader_loss: "promote"`
allows another suitable member to take over, and `max_pursuit_from_convoy`
keeps escorts from chasing too far away. `formation.tolerance` is the allowed
slot error before correction becomes important.

The bundled cargo convoy includes six named escort slots around one cargo
member. The separate Villager Fishing Escort Fleet demonstrates a held convoy
with a leader and five escorts.

### Capture rules

A vessel is capturable only when its action's capture/lifecycle rules permit it.
The bundled Cast Iron Phantom uses:

```json
"capture": {
  "max_destroyed_percent": 25,
  "actions": [
    {
      "type": "sound",
      "sound": "minecraft:ui.toast.challenge_complete",
      "at": "structure",
      "volume": 1.5
    },
    {
      "type": "message",
      "message": "The ship has been captured."
    }
  ]
}
```

`max_destroyed_percent` is the hull-damage ceiling under which capture is still
allowed. Capture also depends on the runtime crew/capture state; removing a
single mob is not a substitute for satisfying the vessel's confirmed crew
resolution. Keep capture separate from `destroyed_cleanup_percent`, which is a
destruction/cleanup threshold rather than a capture threshold.

Boss vessels demonstrate two additional capture fields:
`captured_reward_ambush`, which names a reward encounter, and
`post_capture_cleanup_ticks`, which schedules cleanup after a successful
capture. Capture `actions` and `rewards` can provide sounds, particles, messages,
reputation changes, or chained encounter behavior as supported by the action
validator.

---

## 18. Unlock and progression authoring

The unlock commands in the command reference are only inspection/test tools.
The datapack actually defines progression with `unlock.progress`, then decides
when that completed progress can activate through `unlock.activation`.

The bundled **Debris Depth Guard** is a compact complete example of the model:

```json
"weight_scaling": {
  "mode": "linear",
  "start_at": 5,
  "per_unit": 12,
  "maximum": 260
},
"unlock": {
  "progress": {
    "event": "mine_block",
    "block": "minecraft:ancient_debris",
    "count": 5,
    "y_relation": "at",
    "y_level": 15,
    "y_tolerance": 5,
    "conditions": {
      "dimensions": ["minecraft:the_nether"]
    }
  },
  "activation": {
    "type": "immediate",
    "delay_ticks": 800
  },
  "consume_on_success": true,
  "repeatable": true
}
```

### `unlock.progress`

`progress.event` selects the tracked event. AMBUSH 1.1.6 validates these event
names:

| Event | Important authoring fields |
|---|---|
| `kill` | `entity` or `entities`; `count`; optional `require_each_entity` / `required_kills`; optional conditions. |
| `kill_at_structure` | Kill fields plus `conditions.structures`. |
| `mine_block` | `block` or `blocks`; `count`; optional Y relation and conditions. |
| `y_level_crossing` | `y_level`; optional `direction`, `hysteresis`, conditions. |
| `travel_distance` | `distance`; optional time window and `maximum_sample_distance`. |
| `portal_use` | Optional `from_dimensions`, `to_dimensions`, count/conditions as appropriate. |
| `biome_time` | Requires `conditions.biomes`; use `duration_ticks` or `duration_seconds`. |
| `boat_time` | Time spent satisfying the boat-time tracker; use duration plus optional conditions. |

Common filters include `conditions`, `min_y`, `max_y`, `y_relation`
(`below`, `above`, or `at`), `y_level`, and `y_tolerance`. Time windows are
available in ticks or seconds. Travel sampling can use
`maximum_sample_distance` to reject implausibly large individual movement
samples.

For multi-entity kill progress, `require_each_entity: true` means the listed
entity requirements are tracked individually rather than treating the list as
one interchangeable pool. `required_kills` can describe per-entity grouped
requirements when a more explicit multi-kill recipe is needed.

### Activation

Finishing progress does not have to launch the encounter immediately.
`unlock.activation.type` supports:

- `immediate`
- `leave_structure`
- `enter_structure`
- `conditions_met`
- `conditions_clear`

All activation types may use `delay_ticks`. Structure activation must provide
`structures` or a suitable `conditions` object. Condition-based activation uses
its activation `conditions` object. This lets progression be earned in one
context and the consequence occur only after the player enters, leaves, meets,
or clears another context.

### Consumption and repeatability

`consume_on_success` consumes the completed unlock state only after the
encounter actually succeeds. `consume_on_attempt` is the alternative for data
that intentionally wants an attempted activation to consume it.

`repeatable: true` allows the progress cycle to be earned again after it has
been consumed. `repeatable: false` is appropriate for one-time unlocks. Do not
use `/ambush admin unlockall` to judge this behavior; that command deliberately
short-circuits progression for testing.

### First-unlock weight and weight scaling

`first_unlock_weight` is an optional ordinary-selection weight used for the
first unlocked opportunity. It requires an `unlock` block and is validated in
the 0..100 range. Bundled underground excavation encounters use
`first_unlock_weight: 90`.

`weight_scaling` also requires unlock progress. It changes ordinary selection
weight as tracked progress grows beyond `start_at`:

| Field | Meaning |
|---|---|
| `mode` | `linear` or `multiplier`. Defaults to `linear`. |
| `start_at` | Progress value at which excess-progress scaling starts. |
| `per_unit` | Amount added per excess unit in `linear`, or multiplier growth per excess unit in `multiplier`. |
| `minimum` / `maximum` | Clamp for the resulting effective weight. |

In `linear` mode the effective weight grows by `per_unit` for each unit beyond
`start_at`. In `multiplier` mode the base weight is multiplied by a factor that
grows with that excess progress. Debris Depth Guard therefore starts scaling at
five tracked debris and adds 12 weight per additional progress unit, capped at
260.

Use `/ambush admin unlocks` to inspect progress and `/ambush admin weights` to
inspect the resulting ordinary effective weight while tuning these fields.

---


### External-pack delivery

Distribute your encounters as an external datapack. A complete pack contains `pack.mcmeta` plus its definitions and every custom structure template,
ship profile, faction resource, natural-airship curve, and loot table it needs
beneath `data/<namespace>/`.

Resource identity comes from namespace plus path. A higher-priority datapack
file at the **same namespace and path** as a bundled encounter replaces that
resource in the datapack stack. For example, an external
`data/ambush/ambushes/normal_night_zombie_horde.json` overrides the bundled
`ambush:normal_night_zombie_horde`. A file at a different namespace/path, such
as `data/<your_namespace>/ambushes/normal_night_zombie_horde.json`, creates the
separate ID `<your_namespace>:normal_night_zombie_horde` instead of modifying
the bundled one.

Resources referenced from the installed AMBUSH mod do not need to be copied
into your pack. Custom resources must be included, or supplied by another
required active datapack. Document any required mods and datapacks.

Install the pack in the world's `datapacks` folder, enable it, then run `/reload`
and `/ambush admin check`.

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

Excerpt from the bundled definition: **Evoker Tiny Broadside Boat** (`ambush:boat_evoker_tiny_broadside`).

```json
{
  "positions": [
    [
      4,
      2,
      7
    ]
  ],
  "fire_mount_local": [
    4,
    2,
    6
  ],
  "state": "button",
  "signal": 15,
  "button_ticks": 10,
  "after_ticks": 220,
  "repeat_ticks": 240,
  "range": 96,
  "vertical_tolerance": 12,
  "require_living_crew": true,
  "require": "all",
  "cannon_alignment": {
    "mount_local": [
      4,
      2,
      6
    ],
    "aim_local": [
      -1,
      0,
      0
    ],
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

An encounter can complete tagged tasks at start, success, or failure with
`ftb_quests.on_start_task_tag`, `on_success_task_tag`, and
`on_failure_task_tag`.


`failure_events` tracks the ordinary mobs and Sable vessels created by an
encounter. It succeeds when `minimum_kill_percent` is met before
`timeout_ticks`; otherwise its actions run once. Supported failure actions
include server commands and chained AMBUSH definitions.


Failure-event commands run as the server at permission level 2. `{player}` is replaced
with the target player's name; chained ambushes still use AMBUSH's normal
chain-safety checks.

### Contract items

`quest_contract` is an action-level object that lets a player start an
encounter by using an item, and choose its target by naming the item. It works
without FTB Quests; any means of giving the item works.

Excerpt from the bundled definition: **Pillager Cannon Raider Fleet** (`ambush:ship_pillager_tiny_cannon_fleet`).

```json
{
  "quest_contract": {
    "item": "minecraft:paper",
    "attempt_interval_ticks": 600
  }
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
give {p} minecraft:paper[minecraft:custom_data={ambush_contract:"ambush:ship_pillager_tiny_cannon_fleet"},minecraft:custom_name='{"text":"Pillager Cannon Raider Fleet","italic":false}'] 1
```

`{p}` is the FTB Quests player placeholder. The item ID, the `ambush_contract`
value, and the corresponding `quest_contract` declaration must all agree.

---

## Compatibility

Create, Create Aeronautics/Simulated, Sable, FTB Quests, Lootr and Create Big Cannons are optional integrations. Integration availability is checked at runtime; authoring should target mutually compatible mod versions used by the server pack.

Ordinary entity, sound, effect, and vanilla encounter definitions work without them. Sable actions fail closed when their required runtime is unavailable. A generic action that references a missing optional entity is skipped safely and reported in the server log rather than crashing the server. Use `/ambush admin check <namespace:id>` to identify missing requirements before testing.
