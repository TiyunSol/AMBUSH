# AMBUSH Datapack Guide — 1.1.9

Author encounters for Minecraft 1.21.1 using the current AMBUSH parser and actual bundled resources. The worked examples use resources shipped in AMBUSH 1.1.9. Each excerpt identifies its source and where the fields belong.

## Contents

Start with "Build your first patrol" below. For a field lookup, use the tables in section 12.

1. Pack layout and resource IDs
2. Easy and expanded definitions
3. Commands and testing
4. Triggers, conditions and progression
5. Entities, equipment and armor sets
6. Actions, placement and vessels
7. Controls, weapons and ammunition
8. Lifecycle, health, rewards and inventories
9. Factions, gifts, support and natural encounters
10. Invasions, rendering and integrations
11. Worked datapack examples
12. Field guide for pack authors
13. Field lookup by resource and object

## Build your first patrol

1. Create a folder named `my_pack` in a test world's `datapacks` directory. Save the pack metadata from section 1 as `my_pack/pack.mcmeta`.
2. Inside that folder, create `data/my_pack/ambushes/night_patrol.json` and paste the complete Midnight Horde example under "A first easy encounter" in section 11 into it. The path gives it the ID `my_pack:night_patrol`. Its display name comes from the JSON.
3. Run `/reload`, then `/ambush admin check my_pack:night_patrol`. If the ID is missing from `/ambush list`, check that `pack.mcmeta` and `data` sit side by side. Check the reload log for the filename and any rejected fields.
4. At night in the Overworld, run `/ambush my_pack:night_patrol` near open terrain with some cover. The example requests nine zombies and three husks outside the player's line of sight.
5. Change the zombie entry's `amount` to three, reload and test a fresh patrol. If the cooldown prevents a manual test, an operator can toggle `/ambush admin always` for that test and toggle it off afterward. To check natural spawning, leave the encounter enabled and observe its night, chance and cooldown settings together.

## 1. Pack layout and resource IDs

Install a folder or ZIP in your world's `datapacks` directory. Install AMBUSH on the server and clients for its full synchronized feature set. Optional dependencies are required by the content that uses them.

Minecraft 1.21.1 uses **pack format 48**:

```json
{"pack":{"pack_format":48,"description":"My AMBUSH encounters"}}
```

| Folder under `data/<namespace>/` | Resource |
|---|---|
| `ambushes/` | Encounters |
| `ambush_ship_profiles/` | Reusable vessel actions |
| `ambush_armor_sets/` | Weighted equipment sets |
| `ambush_factions/` | Relations, standing and faction rules |
| `ambush_faction_aliases/` | Faction ID mappings |
| `ambush_faction_support/` | Alliance support |
| `ambush_faction_gifts/` | Periodic reputation-based gifts |
| `ambush_natural_airship_curves/` | Shared ambient scheduling |
| `ambush_invasions/` | Invasion profiles |
| `ambush_ship_render/` | Default tracking/render settings |
| `ambush_loot_aliases/` | Loot table mappings |
| `structure/` | NBT templates; singular folder |
| `loot_table/` | Vanilla-format loot tables; singular folder |

`data/my_pack/ambushes/night_patrol.json` defines `my_pack:night_patrol`. An underscore in a filename is part of its ID: `data/ambush/ambushes/_villager_antiair_car.json` is `ambush:_villager_antiair_car`. It is not a command prefix to put before a namespace.

The same namespace/path overrides a resource according to datapack priority. A new namespace/path creates another resource. Update cross-references when renaming; references to `ambush:` can continue to use the mod's profiles, templates and loot tables.

Run `/reload`, then `/ambush admin check`. JSON syntax alone cannot validate registry IDs, physical hardware or optional integrations. Reload logs distinguish rejected resources from definitions hidden by missing requirements.

## 2. Easy and expanded definitions

Easy definitions must explicitly set `"format":"easy"`. Common inputs are `name`, `description`, `preset`, `trigger`, `check_every_seconds`, `cooldown_seconds`, `cooldown_group`, `chance`, `weight`, `spawn_near_player`, `minimum_distance`, `distance`, `radius`, `attempts`, `mob`, `count`, `mobs`, `conditions`, `banner`, `particles`, `sound`, `effects`, `actions`, `unlock`, `weight_scaling`, `first_unlock_weight`, `kill_entity`, `kill_count`, and `requires_mods`. Entries under `mobs` can use `amount`.

Presets include `ground`, `night`, `sky`, `water`, `portal`, `village`, `outpost`, thunder aliases (`thunder`, `storm`, `stormy`) and rain aliases (`rain`, `rainy`). Presets add conditions or choose a trigger; they are not expanded action names.

**Easy chance is a percentage:** `chance:5` means 5%. For expanded chance, prefer fractions: `trigger.chance.base:0.05` means 5%. The current chance reader interprets values up to and including 1 as fractions and larger values as percentages, so `1` means 100%, not 1%. This compatibility rule is specific to the chance reader; fields such as `selection_chance` and gift probabilities require 0..1 fractions. Easy timers use seconds; expanded `_ticks` fields use game ticks, normally 20 per second at 20 TPS. Lower TPS lengthens elapsed real time.

Expanded definitions require an object-form `trigger`; a root `"trigger":"interval"` is invalid expanded syntax. They can contain waves, actions, conditions, unlocks, faction defaults and scheduling metadata. `requires_mods` is current; `requires` is rejected.

Reusable profiles use `schema_version` and either a base `action` or the supported inherited `extends`/`overrides` form. A `type:"profile"` action selects `profile:"namespace:id"` with optional `overrides`. Objects merge recursively; arrays replace rather than concatenate. Review inherited arrays before overriding them. A `type:"ambush"` reference triggers another encounter and does not accept arbitrary ship overrides.

## 3. Commands and testing

Spawning and administration require operator permissions. Ordinary invasion participation is available to players.

| Command | Behavior |
|---|---|
| `/ambush` | Command help |
| `/ambush list` | Loaded compatible encounter IDs |
| `/ambush <namespace:id> [player]` | Manual spawn using the exact ID |
| `/ambush admin always` | Toggle the command player's manual eligibility bypass |
| `/ambush clear` | Cancel pending work and queue owned-content cleanup |
| `/ambush admin check [namespace:id]` | Registry validation or definition preflight |
| `/ambush admin inspect` | Inspect an active vessel |
| `/ambush admin natural` | Natural curve timing and eligibility |
| `/ambush admin entries` | First-dimension-entry ledger status |
| `/ambush admin rate` | Current natural rate |
| `/ambush admin rate <10–4000>` | Percentage: 100 normal, 50 half speed, 200 double speed |
| `/ambush admin rate up`, `down`, `reset` | Adjust/reset the rate |
| `/ambush admin spawning` | Toggle global natural spawning |
| `/ambush admin spawning status` | Inspect spawning state |
| `/ambush admin spawning <id> [enable\|disable]` | Toggle or explicitly set one encounter |
| `/ambush enable [pack]`, `/ambush disable [pack]` | Namespace spawning controls |
| `/ambush admin weights` | Ordinary encounter chance and weights |
| `/ambush admin reputation` | Player faction standings |
| `/ambush admin reputation set <player> <faction> <value>` | Set standing within faction bounds |
| `/ambush admin unlocks`, `/ambush admin unlockall` | Inspect/unlock encounters for testing |
| `/ambush admin safe_mode [on\|off\|status]` | Persistent cannon terrain protection; no suffix toggles |
| `/ambush admin debug` | Toggle diagnostics |
| `/ambush admin debug positions` | Vessel positions |
| `/ambush invasion [status]` | Invasion state and profiles |
| `/ambush invasion start [profile] [waves]` | Start an invasion; administrator only |
| `/ambush invasion join`, `leave`, `ready` | Player participation |
| `/ambush invasion stop` | Stop an owned invasion; administrators can stop others |
| `/ambush invasion classify` | Inspect invasion candidates; administrator only |

Test the bundled AA car:

```mcfunction
/ambush admin check ambush:_villager_antiair_car
/ambush admin always
/ambush ambush:_villager_antiair_car
/ambush admin inspect
```

Toggle always mode off afterward. This release has no `/ambush player` subtree and no root `/ambush rate` or `/ambush always`. Always mode does not change natural scheduling or make invalid hardware valid.

Safe mode protects terrain from supported AMBUSH-owned cannon projectiles, explosions and fragments. CBC's zero durability mass also disables protected shells' direct-hit entity damage; explosions and fragments retain native entity damage. It is not blanket player invulnerability or protection from unrelated guns.

## 4. Triggers, conditions and progression

Built-in trigger types include `interval`, `portal`, `block_active`, `structure`, `kill`, `vanilla_raid_wave`, `first_dimension_entry`, `villager_trade` and `scorched_guns_raid_start`. `command` is not a root trigger.

- Common trigger fields are `check_every_ticks`, `cooldown_ticks` and `chance`. Failed eligibility, failed placement and successful spawning are distinct outcomes.
- Chance supports `mode`, `base`, `increase_on_failure`, `max`, `reset_on_success` and `after_success`. Chance gates candidates; `weight` chooses among qualifying alternatives. Weight is not a percentage.
- Kill triggers use root `kill_entity` and `kill_count`. Raid-wave options include `chance_per_wave`, `final_wave_chance`, `final_wave_requires_entity`, `maximum_per_raid`, `minimum_omen_level` and `omen_level_chances`.
- First-entry triggers use `to_dimensions` and optional `entry_group`. They persist **once per world**, not per player. A group chooses one weighted alternative and retains it through restart and placement retries. Manual spawning does not consume the slot. The ledger cannot infer visits before installation.

Conditions cover dimension, biome/tag, structures/groups, height, time, weather, moon, inventory, blocks, activity, reputation and progression. Root conditions, action conditions and equipment conditions have different schemas. Section 12 shows where to put common fields. Loaded-chunk probes must not treat unknown terrain as clear terrain.

Unlocks separate `progress` from `activation`, delay, consumption and repeatability. Supported progress events include kills, kills at structures, activity/biome time and configured event types. `consume_on_attempt` and `consume_on_success` act at different points. `repeatable` governs rearming. `first_unlock_weight`, `first_success_weight` and `weight_scaling` belong to unlock probability/selection, not the shared ship timer.

Encounter-specific unlocks and `conditions.progression` are separate. The default stages 0–3 use 0/10/40/100 qualifying kills; stage 4 uses the configured End advancement. Keep stage requirements reachable in a custom curve.

## 5. Entities, equipment and armor sets

Ordinary groups use `wave.groups[].spawns[]`. Vessel crew use `entities[]` and template-local positions. Entity fields belong to that specification, not automatically to its parent action.

Equipment supports explicit slots, item stacks/SNBT, weighted armor sets, enchantments, names, dye and drop chances. `equipment.armor_sets` may be authored on entities or faction equipment rules with conditions and guaranteed entries. A weighted empty slot choice differs from a missing resource. The Artillery Crew example below shows how to define equipment slots.

`combat` configures supported combat stats/ranged behavior; `attack` adds explicit target selectors. `spawn_sound` is a spawn sound; `spawn_animation`, particles, telegraphs and boss bars control presentation. Equipping a weapon alone does not grant every mob a compatible attack goal.

Deck crew generally use `seat:false` and valid `spawn_on_blocks`/surface selection. `seat:true` requires real seat hardware. Living-crew membership is separate from seat occupancy: cargo and rescued entities can use `crew:false`. `crew_glow` and `crew_unseating` are opt-in presentation/lifecycle controls.

Saved Custom NPC clones require the supported `customnpcs:customnpc`, `clone_name` and `clone_tab` integration. Merchant entities use their dedicated offer schema with exact payments, result stacks and stock limits.

## 6. Actions, placement and vessels

Current action types are `raid`, `ambush`, `directional_cbc_shell_rain`, `directional_entity_wave`, `directional_arrow_rain`, `directional_entity_rain`, `directional_potion_rain`, `conditional_spawn`, `micro_structure`, `inline_ambush`, `sound`, `fog`, `fireworks`, `entity_wave`, `structure`, `block_platform`, `potion_rain`, `arrow_rain`, `entity_rain`, `cbc_shell_rain`, `sable_structure`, `sable_boat`, `sable_car`, `sable_formation`, `skirmish`, and `profile`.

Ordinary actions cover grouped waves, directional projectiles, conditions, delays, nested encounters, sounds, fog, fireworks and placement. Fields are action-specific even where the shared validator accepts a broad key set. A recognized name does not prove that every runtime action consumes it.

Microstructures use block offsets and a separate `runtime` object. They support terrain placement, inventories, ownership, patrols, proximity/engine controls, weapons, arrivals and rewards. `runtime.auto_aim_turrets` differs from ship turrets and must address owned placed hardware. Current microstructure bounds include 4096 unique offsets within -48..48; they are not universal vessel limits.

Vessel `template` selects an existing structure. `schematic_front` describes its physical forward direction; `schematic_rotation` is placement rotation. Hardware coordinates stay template-local; fixed `spawn_anchor_position` values are world coordinates.

Placement options include source references, `spawn_anchor`, fixed anchors, offsets, `spawn_height_reference`, `absolute_y`, `orient_on_spawn`, `spawn_clearance`, terrain clearance and activity avoidance. Boat water/depth requirements and car ground-footprint rules are different. Do not transfer them indiscriminately.

Formations use members, inheritance, offsets, shared facing, health and convoy roles. Member arrays can be replaced by profile overrides. Ropes and suspended children need real connectors, valid child ownership and supported winch controls.

Navigation includes `ship_ai`, `patrol`, `convoy`, `tactical_role`, `tactics`, `target_direction`, `rammer`, `unreachable_target_policy`, threat ranges and target priority. Acquisition range, weapon range and minimum engagement range are independent. Faction priority does not bypass ballistic or visibility checks.

## 7. Controls, weapons and ammunition

`analog_controls`, `altitude_controller`, `propulsion_throttle`, `continuous_propulsion`, `steering_controls`, `directional_clutch_drives` and `car_controls` operate native hardware. Coordinates, signal polarity, angle bounds and timers must match the actual template. `unmanaged_analog_control_positions` excludes intentionally independent controls from generic management.

Native lift settings include feedback/signal strategy, lookahead, deadbands, dwell, capture stability and descent arrest. Engine heat/direction and propeller overrides select real machinery; omit overrides to retain its authored settings. Missing engines cannot be repaired through tuning values.

The native wheel accepts 1..360-degree limits; hull steering has its own smaller bound. Reverse polarity and reverse steering inversion must match the drivetrain. Route checks retain support, rise/drop, collision, fluid and loaded-chunk safety. Recovery requests/timers do not prove travel. Unseen righting, downed cleanup and explicit runaway recovery are separate opt-ins.

Cannon stages are assembly, first load, acquisition, measured aiming, readiness/alignment, firing and reload. Match ammunition to actual material, geometry, cartridge power, stress, fuze and barrel space.

- `cannon_assembly` selects mounts and minimum assembled-mount guards.
- `initial_cannon_loads`, `cannon_reloads` and their timers govern native load transactions. Initial and reload rules must agree on the intended ammunition.
- `cannon_swivel_aiming` controls wheel yaw. `cannon_mechanical_aiming` controls measured axes. `auto_aim_turrets` is a separate compiled authoring form.
- `redstone_activations` use real buttons or explicitly declared native mounts. `activation_fire_mode` supplies a vessel default; a per-activation `fire_mode` can override it.
- Tilt, steering, range, sector, settling, crew, alignment and reload gates remain separate. Ballistic velocity/gravity/drag must match the actual shot.
- `cannon_fire_mode:"activations"` uses the authored activation battery. Salvo readiness/alignment settings belong to their selected firing mode.
- `autocannon_ammo_box_refill_ticks` controls the supported ammunition-box refill path; it is not a general infinite-ammo flag.

Verify actual shots, ammunition consumption and repeat fire. Button presses and native requests are not projectiles. Check pitch polarity on your drivetrain by aiming at a target above the barrel.

## 8. Lifecycle, health, rewards and inventories

Hull health uses tracked content and bounded scans. Boss bars, shared fleet health, destruction thresholds, scan intervals, envelope exclusions and grace periods solve different problems.

`sable_events` handles supported lifecycle/health/range events and nested actions. Living-crew gates use actual crew, not seats. `capture`, rescue, `destroyed_rewards`, `success_events`, `failure_events`, `defense_reward`, departure and reputation rewards have different outcome owners. Capture must not be treated as destruction.

Cleanup includes minimum lifetime, wreck/downed policies, split fragments, `shot_down_cleanup` and restart handling. Cleanup is queued; a request is not physical completion. `runaway_recovery_teleport` is a separate explicit recovery feature with distance, unseen-player, timer and concealment gates. Ordinary navigation uses native controls.

Inventories use `container_loot`, hopper contents, explicit stack components and supported Lootr conversion. Hoppers/ammunition containers retain their operational roles. Not every barrel is automatically eligible for Lootr, and a loot-table reference does not fill every inventory type.

## 9. Factions, gifts, support and natural encounters

Factions define relations, classification, standing, equipment and events. Bounds, hostility/alliance thresholds, gains/losses, decay and vessel reputation rewards are independent settings. Bundled standing spans -10000..10000 with alliance at 5000. Fixed-attitude factions can remain hostile regardless of numeric standing.

Support resources configure alliance routes and weighted choices. Periodic gifts use `enabled`, `check_every_ticks`, `cooldown_ticks`, `chance_at_ally`, `chance_at_max`, `enemy_ship_bonus`, `elite_ship_bonus`, `boss_ship_bonus`, `maximum_bonus` and `gifts`. Entries select `ambush`, weight, reputation bounds, player height and dimensions. Probabilities are fractions. Gift intervals must be at least 100 ticks; cooldown must be at least the interval. `gift_defeat_tier` classifies destruction credit rather than directly spawning a reward.

Natural metadata supports `curve`, `pool`, `threat_tier`, `progression_stage`, `encounter_weight`, `selection_chance`, `budget`, `attitudes` and `follow_up`. Curves supply windows, pools, reputation bands, tier weights, progression thresholds/advancements, multiplayer sharing and follow-up policy.

At 1x speed and 20 TPS, the bundled normal window spans 60 to 140 active-play minutes (3 to 7 Minecraft days), with 100 minutes most likely. The major window spans 80 to 140 minutes, with 120 most likely. These are shared opportunities, not independent timers per ship. `encounter_weight` is relative selection; `selection_chance` is a fractional gate. Failed placement retains the opportunity for retry. Nearby players share budget timing.

Nonmanaged ordinary encounters, kills/unlocks, support, gifts, scripted fleets and invasions have separate scheduling owners. The ambient window is not a universal cooldown. New ambient content should join an appropriate pool unless a separate clock is intentional.

## 10. Invasions, rendering and integrations

Invasion profiles combine objective, waves, budget, quality scaling, encounter pools, conditions and overrides. The naval example below is complete. Overrides can remove ordinary capture/reward/lifetime behavior; check null removals. Lives, ready checks, intermission, timeout, wave budgets and boss cadence are independent controls.

Render resources set defaults by vessel action type. `force_render` can select supported tracking/render/release distances and audience. Client tracking does not forceload server terrain or establish collision safety.

`conditional_redstone_sequences` and `conditional_controls` have their own conditions, delay, schedule, target and operation schemas. They are not interchangeable with ordinary activations. Rope-length commands require real compatible winches. Linked typewriters use dedicated targets and key settings.

FTB Quests hooks and contract items have separate event/reward/contract state. Guard Villagers, Lootr, Custom NPCs and CBC add-ons are needed by content that uses them. Toolgun `.excraft` placement uses `schematic_format:"toolgun"` and its supported placement-only setup with ship AI disabled; it does not grant imported vehicles AMBUSH navigation.

## 11. Worked datapack examples

A standalone resource path introduces a complete file. For an **excerpt**, open the named file in the release JAR and edit the fields at the indicated location. Keep the other required fields and the profile, template and loot references your encounter uses.

- A first easy encounter · An unlock consequence · A world-first dimension entry
- A natural vehicle using a profile · Configure an anti-air car profile · Ground-car controls · A merchant convoy · Configure merchant landing
- Villager standing · Periodic gifts · Alliance support · Natural encounter curve
- Naval invasion · Rendering defaults · Faction aliases · Loot aliases · Define an equipment set for artillery crew
- Three-ship Berry fleet · Escort a fishing convoy to the ocean · Reinforce a vanilla raid
- Spawn a roadside field gun · Call a villager artillery boat · Target enemies behind cover

### A first easy encounter

`data/ambush/ambushes/_normal_night_zombie_horde.json`

```json
{
  "format": "easy",
  "name": "Midnight Horde",
  "description": "Spawns 9 zombies and 3 husks.",
  "preset": "night",
  "chance": 0.01,
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
      "avoid_line_of_sight": true,
      "faction": "undead"
    },
    {
      "mob": "minecraft:husk",
      "amount": 3,
      "avoid_line_of_sight": true,
      "faction": "undead"
    }
  ],
  "sound": "minecraft:entity.zombie.ambient",
  "weight": 25
}
```

### An unlock consequence

Excerpt from the encounter root in `data/ambush/ambushes/_unlock_trial_chamber_surface_retribution.json`:

```json
{
  "unlock": {
    "progress": {
      "event": "biome_time",
      "duration_seconds": 1,
      "reset_on_exit": false,
      "once_per_entry": true,
      "conditions": {
        "structures": [
          "minecraft:trial_chambers"
        ],
        "biomes": [
          "#minecraft:is_overworld"
        ],
        "dimensions": [
          "minecraft:overworld"
        ]
      }
    },
    "activation": {
      "type": "conditions_clear",
      "structures": [
        "minecraft:trial_chambers"
      ],
      "delay_ticks": 600
    },
    "consume_on_attempt": true,
    "consume_on_success": true,
    "repeatable": true
  },
  "conditions": {
    "dimensions": [
      "minecraft:overworld"
    ],
    "min_y": 63
  }
}
```

### A world-first dimension entry

Excerpt from the encounter root in `data/ambush/ambushes/_end_patrol_cannon_ship.json`:

```json
{
  "trigger": {
    "type": "first_dimension_entry",
    "entry_group": "ambush:initial_end_patrol",
    "to_dimensions": [
      "minecraft:the_end"
    ],
    "chance": {
      "mode": "flat",
      "base": 1,
      "max": 1
    }
  },
  "conditions": {
    "dimensions": [
      "minecraft:the_end"
    ],
    "progression": {
      "curve": "ambush:default",
      "tier": "end"
    }
  }
}
```

### A natural vehicle using a profile

`data/ambush/ambushes/_villager_antiair_car.json`

```json
{
  "natural_airship": {
    "curve": "ambush:default",
    "pool": "villager",
    "threat_tier": "small",
    "progression_stage": 1,
    "encounter_weight": 0.45,
    "budget": "normal",
    "attitudes": [
      "hostile",
      "neutral",
      "ally"
    ]
  },
  "trigger": {
    "type": "interval",
    "check_every_ticks": 1200,
    "cooldown_ticks": 168000,
    "chance": {
      "mode": "flat",
      "base": 0.001,
      "max": 0.001,
      "reset_on_success": true
    }
  },
  "weight": 1.0,
  "actions": [
    {
      "type": "profile",
      "profile": "ambush:villager_antiair_car",
      "overrides": {
        "crew_glow": {
          "at_or_below_percent": 20
        }
      }
    }
  ],
  "name": "Villager Anti-Air Car",
  "description": "A villager anti-air patrol with twin bronze autocannons.",
  "requires_mods": [
    "create",
    "createbigcannons",
    "sable",
    "simulated",
    "offroad"
  ],
  "cooldown_group": "villager_antiair_car",
  "conditions": {
    "dimensions": [
      "minecraft:overworld"
    ],
    "surface": true
  }
}
```

### Configure an anti-air car profile

Excerpt from the profile action in `data/ambush/ambush_ship_profiles/villager_antiair_car.json`:

```json
{
  "type": "sable_car",
  "template": "ambush:villager_antiair_car",
  "car_controls": {
    "left_turn_lever": [
      5,
      0,
      4
    ],
    "right_turn_lever": [
      1,
      0,
      4
    ],
    "forward_lever": [
      2,
      2,
      6
    ],
    "reverse_lever": [
      4,
      2,
      6
    ],
    "turn_mode": "direct",
    "stop_distance": 30,
    "stop_alignment_degrees": 10,
    "drive_signal": 15,
    "idle_signal": 0,
    "update_ticks": 10,
    "forward_lever_inverted": false,
    "reverse_lever_inverted": false,
    "speed_throttle_local": [
      3,
      3,
      5
    ],
    "minimum_throttle": 4,
    "maximum_throttle": 11,
    "high_throttle_distance": 20,
    "pit_reverse_escape": true,
    "broadside_brake_min_range": 0,
    "broadside_brake_range": 0,
    "reverse_when_behind_degrees": 181,
    "high_throttle_bearing_degrees": 10,
    "commit_turn_until_aligned": true
  }
}
```

### Ground-car controls

Excerpt from the profile action in `data/ambush/ambush_ship_profiles/illager_saw_car_base.json`:

```json
{
  "car_controls": {
    "turn_mode": "direct",
    "stop_alignment_degrees": 20,
    "alignment_drive_signal": 5,
    "left_turn_lever": [
      1,
      1,
      3
    ],
    "right_turn_lever": [
      3,
      1,
      3
    ],
    "forward_lever": [
      1,
      2,
      3
    ],
    "reverse_lever": [
      3,
      2,
      3
    ],
    "forward_lever_inverted": true,
    "reverse_lever_inverted": true,
    "drive_signal": 15,
    "turn_inner_signal": 0,
    "idle_signal": 0,
    "stop_distance": 7,
    "broadside_brake_min_range": 12,
    "broadside_brake_range": 20,
    "target_still_speed": 0.01,
    "reverse_when_behind_degrees": 135,
    "turn_deadband_degrees": 8,
    "update_ticks": 10,
    "pit_reverse_escape": true
  }
}
```

### A merchant convoy

Excerpt from the first ship action in `data/ambush/ambushes/_traveling_merchant_convoy.json`:

```json
{
  "type": "sable_formation",
  "convoy": {
    "navigation_controller": {
      "leader_lookahead_ticks": 30,
      "collision_prediction_ticks": 50,
      "hard_separation": 40,
      "soft_separation": 64,
      "hull_margin": 8,
      "maximum_avoidance_offset": 96,
      "rejoin_distance": 120,
      "rejoin_release_distance": 36,
      "formation_combat_weight": 0.7,
      "reposition_ticks": 80
    },
    "navigation": "follow_player",
    "threat_detection_range": 768,
    "leader_loss": "promote",
    "leader_loss_grace_ticks": 100,
    "max_pursuit_from_convoy": 768,
    "formation": {
      "mode": "custom",
      "tolerance": 18,
      "slots": {
        "port_front": {
          "x": -112.0,
          "y": 40,
          "z": -56
        },
        "starboard_front": {
          "x": 112.0,
          "y": 40,
          "z": -56
        },
        "port_aft": {
          "x": -112.0,
          "y": 40,
          "z": 56
        },
        "starboard_aft": {
          "x": 112.0,
          "y": 40,
          "z": 56
        }
      }
    }
  },
  "members_inherit_parent": false
}
```

### Configure merchant landing

Excerpt from the profile action in `data/ambush/ambush_ship_profiles/traveling_merchant_tradeship.json`:

```json
{
  "landing_controller": {
    "propulsion_control_local": [
      14,
      9,
      57
    ],
    "approach_radius": 64,
    "approach_max_speed": 0.25,
    "approach_resume_speed": 0.1,
    "surface_flatness": {
      "padding": 0,
      "sample_step": 4,
      "max_height_delta": 16,
      "max_neighbor_delta": 6,
      "minimum_support_fraction": 0.9,
      "maximum_fluid_fraction": 0
    },
    "visit_ticks": 24000,
    "approach_timeout_ticks": 9600,
    "approach_lookahead_blocks": 96,
    "door_control_local": [
      17,
      3,
      37
    ],
    "trigger_distance": 24,
    "height_above_ground": 0,
    "max_signal_step": 2,
    "door_engine_local": [
      16,
      2,
      37
    ],
    "door_engine_direction": "reverse",
    "land_in_arrival_area": true,
    "depart_on_threat": false,
    "refuse_hostile_player": true,
    "depart_on_committed_threat": true,
    "arrival_range": 64
  }
}
```

### Villager standing

Set the starting reputation and the points at which a player becomes hostile or allied. Activity rewards let individual player actions raise or lower standing.

Excerpt from `data/ambush/ambush_factions/villager.json`.

```json
{
  "standing": {
    "mode": "reputation",
    "start": 400,
    "ally_at": 5000,
    "hostile_below": 0,
    "on_kill_own_mob": -50,
    "activities": {
      "rewards": {
        "cure_zombie_villager": 25,
        "start_village_raid": -10,
        "win_village_raid": 35
      }
    }
  }
}
```

### Periodic gifts

`data/ambush/ambush_faction_gifts/villager.json`

```json
{
  "enabled": true,
  "check_every_ticks": 12000,
  "cooldown_ticks": 72000,
  "chance_at_ally": 0.02,
  "chance_at_max": 0.1,
  "enemy_ship_bonus": 0.02,
  "elite_ship_bonus": 0.08,
  "boss_ship_bonus": 0.16,
  "maximum_bonus": 0.5,
  "gifts": [
    {
      "ambush": "ambush:_villager_periodic_supply_gift_balloon",
      "weight": 1,
      "minimum_reputation": 5000,
      "maximum_reputation": 7499,
      "maximum_player_y": 200,
      "dimensions": [
        "minecraft:overworld"
      ]
    },
    {
      "ambush": "ambush:_villager_periodic_supply_gift_quality_balloon",
      "weight": 1,
      "minimum_reputation": 7500,
      "maximum_reputation": 8999,
      "maximum_player_y": 200,
      "dimensions": [
        "minecraft:overworld"
      ]
    },
    {
      "ambush": "ambush:_villager_free_gift_balloon",
      "weight": 1,
      "minimum_reputation": 9000,
      "maximum_reputation": 2147483647,
      "maximum_player_y": 200,
      "dimensions": [
        "minecraft:overworld"
      ]
    }
  ]
}
```

### Alliance support

`data/ambush/ambush_faction_support/villager.json`

```json
{
  "gift_ambush": "ambush:_villager_alliance_support_balloon",
  "call_sound": "minecraft:item.goat_horn.sound.6",
  "sound_volume": 4.0,
  "sound_pitch": 1.0,
  "arrival_min_ticks": 1200,
  "arrival_max_ticks": 2400,
  "cooldown_at_ally_ticks": 36000,
  "cooldown_at_max_ticks": 12000,
  "support_ambushes": {
    "minecraft:overworld": "ambush:_villager_faction_support_ship"
  },
  "support_choices": {
    "minecraft:overworld": [
      {
        "ambush": "ambush:_villager_tiny_ship",
        "weight": 1,
        "quality": 0
      },
      {
        "ambush": "ambush:_villager_tiny_autocannon_ship",
        "weight": 1,
        "quality": 0.25
      },
      {
        "ambush": "ambush:_villager_tiny_grapeshot_ship",
        "weight": 1,
        "quality": 0.25
      },
      {
        "ambush": "ambush:_villager_tiny_potato_ship",
        "weight": 1,
        "quality": 0.25
      },
      {
        "ambush": "ambush:_villager_potato_ship",
        "weight": 1,
        "quality": 0.5
      },
      {
        "ambush": "ambush:_villager_faction_support_ship",
        "weight": 1,
        "quality": 0.5
      },
      {
        "ambush": "ambush:_villager_fishing_escort_fleet_ship",
        "weight": 1,
        "quality": 0.8
      },
      {
        "ambush": "ambush:_merchant_escort_patrol",
        "weight": 1,
        "quality": 1
      },
      {
        "ambush": "ambush:_traveling_merchant_convoy",
        "weight": 1,
        "quality": 1
      },
      {
        "ambush": "ambush:_villager_artillery_boat",
        "weight": 0.65,
        "quality": 0.9
      }
    ]
  },
  "gift_regain_margin": 1250
}
```

### Natural encounter curve

To space out ordinary encounters, edit the normal window. The three values give its shortest, most likely and longest interval in ticks. The major window controls its own budget.

Excerpt from `data/ambush/ambush_natural_airship_curves/default.json`.

```json
{
  "normal_window": {
    "distribution": "triangular",
    "min_ticks": 72000,
    "mode_ticks": 120000,
    "max_ticks": 168000
  },
  "major_window": {
    "distribution": "triangular",
    "min_ticks": 96000,
    "mode_ticks": 144000,
    "max_ticks": 168000
  }
}
```

### Naval invasion

To build a shorter invasion, lower the wave count and choose how many waves apart bosses should appear. Keep the pool and rewards in the full invasion file.

Excerpt from `data/ambush/ambush_invasions/naval.json`.

```json
{
  "waves": {
    "count": 15,
    "telegraph_ticks": 60,
    "intermission_ticks": 240,
    "boss_every": 3
  }
}
```

### Rendering defaults

`data/ambush/ambush_ship_render/defaults.json`

```json
{
  "sable_structure": {
    "enabled": true,
    "after_operational_ticks": 300,
    "tracking_distance": 2000,
    "render_distance": 2000,
    "release_distance": 2048,
    "audience": "nearby"
  },
  "sable_boat": {
    "enabled": true,
    "after_operational_ticks": 300,
    "tracking_distance": 1600,
    "render_distance": 1600,
    "release_distance": 1728,
    "audience": "nearby"
  },
  "sable_car": {
    "enabled": true,
    "after_operational_ticks": 300,
    "tracking_distance": 1200,
    "render_distance": 1200,
    "release_distance": 1328,
    "audience": "nearby"
  },
  "sable_formation": {
    "enabled": true,
    "after_operational_ticks": 300,
    "tracking_distance": 2000,
    "render_distance": 2000,
    "release_distance": 2048,
    "audience": "nearby"
  }
}
```

### Faction aliases

`data/ambush/ambush_faction_aliases/defaults.json`

```json
{
  "ambush:pillager": "ambush:illager"
}
```

### Loot aliases

When renaming a loot table, map its previous ID to the new one so existing references can find it. Each entry follows this pattern.

Excerpt from `data/ambush/ambush_loot_aliases/renamed_combined_tables.json`.

```json
{
  "ambush:chests/combined/ancient_city_0d25456871ff": "ambush:chests/combined/ancient_city_loot_table_1"
}
```

### Define an equipment set for artillery crew

`data/ambush/ambush_armor_sets/artillery_crew.json`

```json
{
  "name": "Artillery Detachment",
  "slots": {
    "head": [
      {
        "item": "minecraft:iron_helmet",
        "weight": 2
      },
      {
        "item": "minecraft:chainmail_helmet",
        "weight": 3
      },
      {
        "weight": 1
      }
    ],
    "chest": [
      {
        "item": "minecraft:iron_chestplate",
        "weight": 2
      },
      {
        "item": "minecraft:chainmail_chestplate",
        "weight": 3
      }
    ],
    "mainhand": "minecraft:crossbow"
  }
}
```

### Design an escort, reinforcement or fleet

To adapt a bundled encounter, find its path in this guide, copy the complete resource from the release JAR under your own namespace, and keep the original profile and template references until the copied encounter spawns successfully. Then change one setting at a time. Examples labeled as excerpts show only the fields used for that task.

#### Three-ship Berry fleet

Follow the member definitions to see how the ships share a formation.

Excerpt from the first ship action in `data/ambush/ambushes/_illager_berry_fleet_ship.json`:

```json
{
  "type": "sable_formation",
  "template": "ambush:illager_berry_airship",
  "members_inherit_parent": true,
  "members": [
    {
      "id": "illager_berry_fleet_left",
      "structure_key": "illager_berry_fleet_left",
      "ship_name": "Illager Berry Fleet Left Ship",
      "spawn_distance": 400,
      "minimum_spawn_distance": 300,
      "spawn_bearing_degrees": -30
    },
    {
      "id": "illager_berry_fleet_center",
      "structure_key": "illager_berry_fleet_center",
      "ship_name": "Illager Berry Fleet Center Ship",
      "spawn_distance": 400,
      "minimum_spawn_distance": 300,
      "spawn_bearing_degrees": 0
    },
    {
      "id": "illager_berry_fleet_right",
      "structure_key": "illager_berry_fleet_right",
      "ship_name": "Illager Berry Fleet Right Ship",
      "spawn_distance": 400,
      "minimum_spawn_distance": 300,
      "spawn_bearing_degrees": 30
    }
  ]
}
```

#### Escort a fishing convoy to the ocean

The trigger controls when the offer can appear. The profile overrides define the escort route.

`data/ambush/ambushes/_fishing_convoy_ocean_escort.json`

```json
{
  "name": "Fishing Grounds Escort",
  "description": "Protect the fishing cargo airship on its way to distant ocean fishing grounds.",
  "natural_airship": {
    "curve": "ambush:default",
    "pool": "villager",
    "threat_tier": "small",
    "progression_stage": 1,
    "encounter_weight": 0.15,
    "budget": "major",
    "attitudes": [
      "neutral",
      "ally"
    ],
    "repeat_group": "ambush:_villager_fishing_cargo_convoy_ship"
  },
  "requires_mods": [
    "create",
    "sable",
    "simulated",
    "aeronautics",
    "createbigcannons"
  ],
  "trigger": {
    "type": "interval",
    "check_every_ticks": 1200,
    "cooldown_ticks": 96000,
    "chance": {
      "mode": "flat",
      "base": 0.002,
      "max": 0.002,
      "reset_on_success": true
    }
  },
  "cooldown_group": "villager_fishing_convoy",
  "weight": 1,
  "conditions": {
    "dimensions": [
      "minecraft:overworld"
    ],
    "over_ocean": true
  },
  "actions": [
    {
      "type": "profile",
      "profile": "ambush:convoy_boss_fishing_fleet",
      "overrides": {
        "id": "fishing_grounds_convoy",
        "ship_name": "Fishing Grounds Convoy",
        "sable_events": [],
        "convoy": {
          "navigation": "travel",
          "leader_loss": "fail",
          "escort_route": {
            "rescue": {
              "maximum_survivors": 3,
              "timeout_ticks": 24000,
              "rewards": [
                {
                  "type": "reputation",
                  "faction": "ambush:villager",
                  "amount": 12
                }
              ]
            },
            "flagship": "villager_fishing_cargo",
            "destination": {
              "type": "biome",
              "id": "#minecraft:is_ocean",
              "minimum_distance": 640,
              "search_radius": 1536
            },
            "arrival_radius": 80,
            "escort_radius": 224,
            "required_fraction": 0.5,
            "timeout_ticks": 36000,
            "invitation": "A fishing convoy is seeking an escort. Fly alongside the cargo ship.",
            "arrival_message": "The fishing fleet reached its grounds. The fishers thank you for the escort.",
            "rewards": [
              {
                "type": "reputation",
                "faction": "ambush:villager",
                "amount": 100
              }
            ],
            "raid_at_fraction": 0.2,
            "raid": {
              "type": "ship_raid",
              "id": "fishing_ground_raider",
              "faction": "ambush:illager",
              "spawn_distance": 320,
              "minimum_spawn_distance": 192,
              "max_active": 1,
              "maximum_attacks": 1,
              "choices": [
                {
                  "ambush": "ambush:_pillager_tiny_cannon_ship",
                  "weight": 2
                },
                {
                  "ambush": "ambush:_illager_patrol_ship",
                  "weight": 1
                }
              ]
            },
            "arrival_sequence": "deep_ocean_fishing"
          }
        }
      }
    }
  ]
}
```

#### Reinforce a vanilla raid

Use this trigger in an expanded encounter to roll once per eligible wave. The example gives the reinforcement a 0.25% chance and caps it at one appearance per raid. Define the reinforcement itself in the encounter wave.

Excerpt from `data/ambush/ambushes/_pillager_raid_mixed_pair.json`.

```json
{
  "trigger": {
    "type": "vanilla_raid_wave",
    "spawn_at_raid_center": true,
    "chance_per_wave": 0.0025,
    "maximum_per_raid": 1,
    "cooldown_ticks": 24000,
    "minimum_cooldown_ticks": 24000
  }
}
```

#### Spawn a roadside field gun

This easy-format encounter keeps the patrol and gun together using its mob list and cluster radius.

`data/ambush/ambushes/_roadside_field_gun.json`

```json
{
  "format": "easy",
  "name": "Roadside Field Gun",
  "description": "A field gunner and two escorts arrive on foot.",
  "preset": "ground",
  "chance": 0.075,
  "weight": 12,
  "check_every_seconds": 90,
  "cooldown_seconds": 3600,
  "cooldown_group": "field_gun_patrol",
  "minimum_distance": 28,
  "distance": 48,
  "cluster_radius": 6,
  "conditions": {
    "dimensions": [
      "minecraft:overworld"
    ],
    "biomes": [
      "minecraft:plains",
      "minecraft:savanna"
    ]
  },
  "mobs": [
    {
      "mob": "minecraft:vindicator",
      "amount": 1,
      "faction": "illager",
      "persistent": true,
      "avoid_line_of_sight": false,
      "rolling_cannon": {
        "breech": "createbigcannons:cast_iron_autocannon_breech",
        "spring": "createbigcannons:cast_iron_autocannon_recoil_spring",
        "barrel": "createbigcannons:cast_iron_autocannon_barrel",
        "ammunition": {
          "id": "createbigcannons:autocannon_cartridge",
          "count": 1,
          "components": {
            "createbigcannons:projectile": [
              {
                "slot": 0,
                "item": {
                  "id": "createbigcannons:ap_autocannon_round",
                  "count": 1
                }
              }
            ]
          }
        },
        "rounds": 8,
        "range": 64,
        "minimum_range": 4,
        "retreat_range": 12,
        "magazine_size": 4,
        "fire_rate": 15,
        "reload_ticks": 900
      }
    },
    {
      "mob": "minecraft:vindicator",
      "amount": 2,
      "faction": "illager",
      "persistent": true,
      "avoid_line_of_sight": false,
      "equipment": {
        "mainhand": "minecraft:stone_axe"
      }
    }
  ],
  "requires_mods": [
    "createbigcannons"
  ]
}
```

#### Call a villager artillery boat

To reuse the bundled boat with its crew and hardware, reference its profile from your encounter. Put encounter-specific changes under overrides on that action.

Excerpt from `data/ambush/ambushes/_villager_artillery_boat.json`.

```json
{
  "actions": [
    {
      "type": "profile",
      "profile": "ambush:villager_artillery_boat"
    }
  ]
}
```

#### Target enemies behind cover

Both Cast Iron Phantom variants set `"ignore_target_cover": true` on the **ship action**. This lets the ship acquire covered players and underground hostile mobs. Faction eligibility, weapon range, alignment and physical projectile collisions still apply. Omit the field or set it to `false` for normal targeting. Add this field beside the ship action’s `type`, as in `data/ambush/ambushes/_cast_iron_phantom_ship.json`:

```json
{
  "ignore_target_cover": true
}
```

### Turn a captain defeat into a village defense

In Outpost Captain Retaliation, the evoker carries the tag `outpost_captain_evoker`. The objective matches that tag, so killing a guard cannot award the quest. To adapt it, give your chosen mob a unique tag and use the same value in `success_events.mob_objective.tag`.

Excerpt from `data/ambush/ambushes/_outpost_captain_retaliation.json`.

```json
{
  "success_events": {
    "mob_objective": {
      "tag": "outpost_captain_evoker",
      "description": "Defeat the Outpost Captain to learn which village is threatened.",
      "rewards": [
        {
          "type": "travel_quest",
          "title": "Warn the Village",
          "invasion": "ambush:ground_defense",
          "destination": {
            "type": "structure",
            "id": "#minecraft:village",
            "search_radius": 2048
          },
          "guide": "compass",
          "waves": 3,
          "delivery": "objective_drop",
          "glow_drop": true,
          "end_if_destroyed": true,
          "announce": false
        }
      ],
      "announce": false
    }
  }
}
```

The reward searches up to 2,048 blocks for a structure in the village tag. The guide is a compass dropped at the objective. The drop glows, and destroying it ends this quest. Reaching the destination leads into three waves of the named invasion. Change the destination and invasion together when adapting the route.

## 12. Field guide for pack authors

Choose the part of the encounter you want to change. Each table names the object you are editing. A dot means to go inside an object, and `[]` means an entry in a list. For example, to change `mobs[].amount`, edit `amount` inside the entry for that mob.

### Build a patrol

These fields belong in an easy encounter under `data/<namespace>/ambushes/`. See "A first easy encounter" for a complete file.

| Field | What it controls |
|---|---|
| `format` | Set to `"easy"` to use the short patrol format. |
| `name`, `description` | Give the encounter a display name and describe what happens. Its filename and namespace determine its ID. |
| `preset` | Applies a built-in starting setup. For example, `night` adds the nighttime requirement. |
| `conditions.dimensions` | Lists the dimensions where the encounter is eligible. |
| `chance` | Percentage chance on an eligible check. In easy format, use 5 for 5%. |
| `check_every_seconds` | Sets the interval between checks, in seconds. |
| `cooldown_seconds` | Sets the cooldown in seconds. Give variants the same `cooldown_group` when they should share it. |
| `weight` | Changes how often this encounter is selected relative to other eligible choices. |
| `mobs[].mob` | Selects the entity type for this entry, such as `minecraft:husk`. |
| `mobs[].amount` | Sets how many of this entry to request. Change it independently for each mob type. |
| `mobs[].avoid_line_of_sight` | Requests a spawn position outside the player's view. Allow space and cover when testing. |
| `mobs[].faction` | Assigns the spawned mobs in this entry to a faction. |
| `requires_mods` | Lists mod IDs the encounter depends on. Use this when the encounter needs hardware or entities supplied by another mod. |

### Choose when an expanded encounter runs

These paths start at the encounter root. The raid fields apply to a `vanilla_raid_wave` trigger. See "Reinforce a vanilla raid" for the fields together.

| Field | What it controls |
|---|---|
| `trigger.type` | Chooses the event to check, such as an interval, raid wave or first dimension entry. |
| `trigger.check_every_ticks` | Sets the interval between scheduled checks in game ticks. At 20 TPS, 1,200 ticks is one minute. |
| `trigger.cooldown_ticks` | Sets the cooldown in game ticks. |
| `trigger.chance.base` | Starting chance on an eligible check. Use 0.05 for 5% in the expanded chance object. |
| `trigger.chance_per_wave` | Probability for each eligible raid wave. A value of 0.0025 gives a 0.25% chance. |
| `trigger.maximum_per_raid` | Caps how many times this reinforcement can appear during the same raid. |
| `unlock.progress` | Describes what must happen to earn an unlock, such as completing a kill requirement. |
| `unlock.activation` | Controls when earned progress can activate the encounter. Use it when the consequence should happen later, such as after leaving a structure. |

### Add a vessel to the natural rotation

In an expanded encounter, put the vessel under `actions` and the scheduling settings in the root `natural_airship` object. See "A natural vehicle using a profile" for a complete example.

| Field | What it controls |
|---|---|
| `actions[].type` | Selects the action. Use `profile` to spawn a vessel from a reusable ship profile. |
| `actions[].profile` | Names the profile by namespace and ID. Keep the bundled ID to reuse the original hardware. |
| `actions[].overrides` | Changes profile settings for this encounter. Nested objects merge. Supplying an array replaces the inherited array, so include every entry you want to keep. |
| `natural_airship.curve` | Selects the scheduling resource that supplies the shared timing and selection rules. |
| `natural_airship.pool` | Places the encounter in a named selection pool on that curve. |
| `natural_airship.budget` | Selects the timing budget, such as `normal` or `major`. |
| `natural_airship.threat_tier` | Assigns the encounter to a tier used by the curve's tier weights. |
| `natural_airship.encounter_weight` | Adjusts this entry's relative weight among eligible candidates. To change the time between opportunities, edit the curve window instead. |

### Tune faction standing and invasion pacing

These settings live in their own resources. For a faction, edit `standing` in `ambush_factions/`. For an invasion, edit `waves` in `ambush_invasions/`.

| Field | What it controls |
|---|---|
| `standing.start` | Initial reputation with the faction. |
| `standing.ally_at`, `standing.hostile_below` | Set the thresholds for becoming an ally and falling into hostility. |
| `standing.on_kill_own_mob` | Reputation change when the player kills a member of this faction. Use a negative value for a penalty. |
| `standing.activities.rewards` | Maps supported activities to reputation changes. The villager example awards standing for curing a zombie villager. |
| `waves.count` | Number of waves in the invasion. |
| `waves.telegraph_ticks` | Length of the wave warning, in ticks. |
| `waves.intermission_ticks` | Time between waves, in ticks. |
| `waves.boss_every` | Spacing of boss waves. The bundled naval value of 3 schedules one every third wave. |


### Optional fields and what happens when you leave them out

An optional object enables a feature or customizes an existing one. Its children can still be required. For example, a patrol can omit a travel quest entirely. Once a reward uses `type: "travel_quest"`, it needs a destination and exactly one follow-up encounter or invasion. These tables use the defaults from the reader. The examples in section 11 show the choices the bundled author made.

**Travel orders.** Edit the reward under `success_events.mob_objective.rewards[]` in Outpost Captain Retaliation. The same travel-order rules apply when another supported reward list grants a travel quest.

| Field | Purpose and omission behavior |
|---|---|
| `title` | Names the order. The fallback is "Encounter orders". An authored title can contain 1 to 96 characters. |
| `destination.type` | Use `area` for an exact horizontal position, `biome` for terrain, or `structure` for a generated site. An area requires `x` and `z`. A biome or structure uses `id`. Its optional `search_radius` defaults to 1,024 blocks and accepts 32 to 2,048. |
| `destination.dimension` | Specifies the dimension for the destination. |
| `guide` | Selects a compass or map. The default is `compass`. |
| `delivery` | Places the guide in the inventory, on the hotbar, or at the defeated objective. Use `inventory` (default), `hotbar`, or `objective_drop`. |
| `glow_drop`, `end_if_destroyed`, `announce` | Independent switches for highlighting the drop, ending the quest if the guide is destroyed, and announcing the order. Each defaults to false. |
| `arrival_radius` | Distance in blocks that counts as reaching the destination. Default 32, allowed 1 to 256. |
| `ambush`, `invasion`, `waves` | Choose one follow-up ID. An invasion can also specify its wave count. The captain uses `ambush:ground_defense` with three waves. |

**Mob combat.** On an individual mob, use the `combat` object to tune attacks. Use the separate `attack` object to choose which entities the mob tries to engage.

| Field | Effect |
|---|---|
| `combat.damage`, `combat.damage_always` | Set attack damage from 0 to 2,048. By default this override applies to an unarmed mob. Enable `damage_always` to apply it while armed. |
| `combat.arrow_damage` | Sets the mob's arrow damage from 0 to 1,024. |
| `combat.knockback`, `combat.knockback_resistance` | Set outgoing knockback from 0 to 5 and resistance to incoming knockback from 0 to 1. |
| `combat.bow_attack` | Enables AMBUSH's bow attack goal for eligible mobs. Mobs with native ranged attack behavior keep their existing goal. |
| `combat.bow_range`, `combat.bow_interval_ticks`, `combat.bow_speed` | Set range in blocks (default 20, allowed 4 to 64), attack interval in ticks (40, allowed 4 to 200), and movement speed for the bow goal (1, allowed 0.1 to 4). |
| `attack.entities`, `attack.require_line_of_sight`, `attack.recheck_ticks` | The object form requires an entity selector list. It can also require visibility and change how often targets are checked. The recheck interval defaults to 10 ticks. |

**Faction standing.** Add these to a faction resource when a pack needs behavior beyond the villager standing example.

| Field | Effect |
|---|---|
| `standing.decay.enabled` | Enables periodic movement toward a target reputation. Decay is disabled by default. |
| `standing.decay.interval_ticks`, `standing.decay.amount`, `standing.decay.target` | Set the time between adjustments, points per adjustment, and destination value. Defaults are 11,520 ticks, 20 points, and 0. The target must be inside the faction's standing bounds. |
| `standing.on_kill_mob` | Maps individual entity IDs to reputation adjustments. |
| `standing.activities.gold_armor_ticks` | Sets the gold-armor activity interval. Default 6,000 ticks, minimum 20. |
| `standing.activities.restitution` | Enables merchant restitution terms. If supplied, `loss` and `refund` each take 1 to 1,000 points, and `payment` names the item. Optional `count` defaults to 1 and `timeout_ticks` to 24,000. Omitting the terms leaves restitution inactive. |

**Merchant trades.** Put `merchant` on the merchant's spawn entry. The traveling merchant tradeship profile is a useful starting point when changing stock aboard a vessel.

| Field | Effect |
|---|---|
| `profession`, `villager_type`, `level`, `experience` | Set a villager's job, type, trading level and XP. These fields apply to villagers. |
| `despawn_delay_ticks`, `disable_night_invisibility` | Customize the wandering trader's despawn timer and nighttime invisibility. |
| `trades_mode`, `trades`, `trade_count` | Choose whether to replace or append offers, provide the offer list, and control how many are selected. The default mode is `replace`. |
| `trades[].cost`, `second_cost`, `result` | Define the first payment, optional second payment, and item received. Each stack has an item ID and can specify a count. A result can also specify SNBT. |
| `trades[].max_uses`, `uses` | Set stock and how much is already used. The defaults are 16 and 0. Starting uses must be within the stock limit. |
| `trades[].villager_xp`, `price_multiplier` | Set the experience granted to the merchant and the offer's price multiplier. Defaults are 1 and 0.05. |
| `trades[].optional_result` | Skips this offer if the result item is absent. This lets a trade pool include an item from an optional mod. |

**Equipment variants.** In the Artillery Detachment example, each entry in a slot can be an item ID or a choice object. The equipment-set root accepts `name`, `description`, `slots`, and `combat`.

| Choice field | Effect |
|---|---|
| `item`, `snbt`, `count` | Supply the item ID or stack data and optionally set the count from 1 to 64. |
| `weight`, `conditions` | Use conditions to restrict when a choice applies, and relative weight to favor it among eligible choices. |
| `name`, `dye` | Give the item a display name or supply a dye color for a dyeable item. |
| `enchantments`, `glint` | Map enchantment IDs to levels for gameplay effects. Use the boolean glint option to control the item's visible shine. |
| `drop_chance`, `show_held_on_evoker` | Set the equipment drop chance or request that the evoker show the held item. |
| `rocket_color` | Set a firework rocket's RGB color as a whole number from 0 to 16,777,215. This form requires the firework rocket item and excludes a separate SNBT stack. |

**Typewriter encounter hooks.** The root `typewriter` object is optional. Its `mode` accepts `fire` (default) or `trigger`. Set any of `on_start`, `on_success`, and `on_failure` to invoke the corresponding Typewriter entries at that point in the encounter. Each supplied hook must identify between 1 and 16 entries. These are encounter lifecycle hooks. Vessel key controls are configured on the vessel.

**Vessel controls.** Edit these on the vessel action or in its profile. Hardware positions are local to the template.

| Field | Effect |
|---|---|
| `landing_controller.cruise_signal` | Sets a cruise signal for a landing visit. Use an integer from 0 to 15 within the altitude controller's own signal limits. |
| `landing_controller.approach_dwell_ticks` | Sets the interval used when deciding whether to change the approach drive. The fallback is 40 ticks. |
| `auto_aim_turrets[].fire_mode` | Overrides the vessel's `activation_fire_mode` for one turret. The fallback is the vessel setting, then `button`. A turret using `native` needs its actual mount. |
| `conditional_controls.interval_ticks` | Time between rule evaluations. Default 10 ticks, allowed 1 to 200. The object requires a nonempty `rules` list. |
| `conditional_controls.rules[].outputs[].operation` | Selects the physical operation. Use `set_analog` with `signal`, `set_powered` with `powered`, `pulse` with `pulse_ticks`, or `set_rope_length` with `length`. The rope length accepts 1 to 512. |
| `conditional_redstone_sequences[].release_controls_on_completion` | Releases claimed controls after a one-shot sequence finishes. A repeating sequence cannot enable it. |

## 13. Field lookup by resource and object

Start with the resource you are editing. Each row names a JSON object and the fields that belong inside it. A root row describes the top of the file. In a path, `[]` means one element of a list. In a map, `<key>` stands for an ID, tier, component or name supplied by the pack. For loot and faction aliases, the entire file is a map from an old ID to its replacement ID.

The value labels describe forms used in shipped resources. A field labeled **validator** also has an explicit entry in the reader, but this lookup has no bundled value of that shape. A null in a profile override can remove an inherited setting. Check the concrete action before using that value directly. The tables combine alternatives. Use the fields for the selected trigger, action, reward or objective type. Minecraft defines the contents of block properties, item components and NBT.

### Easy encounter

Start with Midnight Horde. Its mob list defines the patrol, its preset selects the nighttime condition, and its timers use seconds.

| Object | Fields by value form |
|---|---|
| **Root** | list: `actions`, `mobs`, `requires_mods` |
| **Root** | validator: `attempts`, `default_faction_behavior`, `effects`, `max_y`, `min_y`, `radius`, `sounds`, `wave` |
| **Root** | text: `banner`, `cooldown_group`, `default_faction`, `description`, `format`, `kill_entity`, `mob`, `name`, `preset`, `sound`, `trigger` |
| **Root** | number: `chance`, `check_every_seconds`, `cluster_radius`, `cooldown_seconds`, `count`, `distance`, `first_success_weight`, `first_unlock_weight`, `kill_count`, `minimum_distance`, `weight` |
| **Root** | object: `conditions`, `particles`, `success_events`, `unlock`, `weight_scaling` |
| **Root** | boolean: `spawn_near_player` |
| `actions[]` | boolean: `adapt_terrain`, `avoid_line_of_sight`, `managed`, `required`, `show_boss_bar` |
| `actions[]` | number: `after_ticks`, `attempts`, `between_waves_ticks`, `build_clearance`, `completion_delay_ticks`, `count`, `horn_delay_ticks`, `lifetime_seconds`, `lifetime_ticks`, `min_radius`, `offset_y`, `player_build_chunk_radius` |
| `actions[]` | number: `player_build_inhabited_ticks`, `radius`, `source_height`, `spread`, `target_spread`, `velocity` |
| `actions[]` | text: `arrow`, `completion_sound`, `faction`, `id`, `pickup`, `replace_mode`, `type`, `victory_name` |
| `actions[]` | list: `blocks`, `completion_actions`, `completion_rewards`, `entities`, `waves` |
| `actions[]` | object: `boss_bar`, `horn` |
| `actions[].blocks[]` | text: `block`, `replace` / list: `offset` / object: `properties` |
| `actions[].boss_bar` | text: `audience`, `color`, `name`, `overlay` / boolean: `create_world_fog`, `darken_sky`, `play_boss_music` / number: `range` |
| `actions[].completion_actions[]` | number: `count`, `height`, `spread`, `velocity` / text: `entity`, `type` |
| `actions[].completion_rewards[]` | text: `track`, `type` |
| `actions[].entities[]` | number: `aggro_range`, `count`, `deaggro_range`, `extra_damage`, `extra_health`, `nearby_player_range`, `target_range` / boolean: `aggro_through_walls`, `avoid_line_of_sight`, `friendly_fire`, `persistent` / text: `entity`, `target` / list: `offsets`, `tags` / object: `particles`, `spawn_animation` |
| `actions[].entities[].particles` | number: `count`, `interval_ticks` / text: `particle` |
| `actions[].entities[].spawn_animation` | number: `depth`, `duration_ticks` / text: `type` |
| `actions[].horn` | number: `pitch`, `volume` / text: `sound` |
| `actions[].waves[]` | list: `groups` / text: `name` |
| `actions[].waves[].groups[]` | number: `attempts`, `min_radius`, `radius` / text: `id` / list: `on_spawn_actions`, `spawns` |
| `actions[].waves[].groups[].on_spawn_actions[]` | number: `after_ticks`, `count`, `gravity`, `height`, `source_height`, `spread`, `target_height_offset`, `target_spread`, `velocity` / text: `arrow`, `item`, `pickup`, `potion`, `type` / boolean: `ballistic`, `per_spawn_origin` / object: `telegraph` |
| `actions[].waves[].groups[].on_spawn_actions[].telegraph` | number: `arc_height`, `circle_points`, `duration_ticks`, `interval_ticks`, `path_points`, `radius` / boolean: `follow_target` / text: `particle`, `shape` |
| `actions[].waves[].groups[].spawns[]` | number: `aggro_range`, `count`, `deaggro_range`, `extra_damage`, `extra_health`, `target_range` / boolean: `avoid_line_of_sight`, `count_toward_wave`, `friendly_fire`, `persistent` / object: `banner`, `equipment` / list: `effects`, `passengers`, `tags` / text: `entity`, `placement`, `target` / object or text: `particles`, `spawn_animation` |
| `actions[].waves[].groups[].spawns[].banner` | text: `color`, `name` |
| `actions[].waves[].groups[].spawns[].equipment` | text: `mainhand` |
| `actions[].waves[].groups[].spawns[].particles` | number: `count`, `interval_ticks`, `spread` / text: `particle` |
| `actions[].waves[].groups[].spawns[].passengers[]` | number: `aggro_range`, `count`, `deaggro_range`, `target_range` / boolean: `avoid_line_of_sight`, `friendly_fire`, `persistent` / text: `entity`, `particles`, `target` / object: `equipment` / list: `reinforcements`, `tags` |
| `actions[].waves[].groups[].spawns[].passengers[].equipment` | text: `mainhand` |
| `actions[].waves[].groups[].spawns[].passengers[].reinforcements[]` | list: `actions` / text: `id` / number: `repeat_ticks` / object: `trigger` |
| `actions[].waves[].groups[].spawns[].passengers[].reinforcements[].actions[]` | text: `arrow`, `item`, `pickup`, `type` / boolean: `ballistic` / number: `count`, `gravity`, `source_height`, `spread`, `target_height_offset`, `target_spread`, `velocity` / list: `potions` / object: `telegraph` |
| `actions[].waves[].groups[].spawns[].passengers[].reinforcements[].actions[].telegraph` | number: `arc_height`, `circle_points`, `duration_ticks`, `interval_ticks`, `path_points`, `radius` / boolean: `follow_target` / text: `particle`, `shape` |
| `actions[].waves[].groups[].spawns[].passengers[].reinforcements[].trigger` | number: `after_ticks` / text: `type` |
| `actions[].waves[].groups[].spawns[].spawn_animation` | number: `count` / text: `type` / boolean: `visual_only` |
| `conditions` | list: `biomes`, `dimensions`, `faction_reputation`, `structures`, `use_structure_groups` / object: `height`, `progression`, `structure_groups` / number: `max_y`, `min_y` / text: `moon`, `time`, `weather` / boolean: `surface`, `underground` |
| `conditions.faction_reputation[]` | text: `attitude`, `faction` / number: `max` |
| `conditions.height` | number: `max`, `min` |
| `conditions.progression` | text: `curve`, `faction`, `tier` |
| `conditions.structure_groups` | list: `surface_structures` |
| `mobs[]` | number: `aggro_range`, `amount`, `crossbow_range`, `deaggro_range`, `extra_damage`, `extra_health`, `follow_distance`, `glow_after_ticks`, `target_range` |
| `mobs[]` | boolean: `aggro_through_walls`, `avoid_line_of_sight`, `custom_name_visible`, `persistent` |
| `mobs[]` | object or text: `banner`, `particles`, `spawn_animation` |
| `mobs[]` | object: `boss_bar`, `equipment`, `rolling_cannon`, `scout_signal` |
| `mobs[]` | validator: `count`, `entity`, `friendly_fire` |
| `mobs[]` | text: `custom_name`, `faction`, `mob`, `placement`, `target` |
| `mobs[]` | list: `effects`, `passengers`, `reinforcements`, `tags` |
| `mobs[].banner` | text: `color`, `name` |
| `mobs[].boss_bar` | text: `audience`, `color`, `name`, `overlay` / number: `range` |
| `mobs[].entity_choices[]` | validator: `entity`, `merchant`, `required_mods`, `weight` |
| `mobs[].equipment` | list: `armor_sets` / text: `boots`, `chestplate`, `helmet`, `leggings`, `mainhand`, `offhand` |
| `mobs[].equipment.armor_sets[]` | boolean: `guaranteed` / text: `set` |
| `mobs[].particles` | number: `count`, `interval_ticks`, `spread` / text: `particle` |
| `mobs[].passengers[]` | boolean: `avoid_line_of_sight`, `friendly_fire`, `persistent` / number: `count` / text: `entity`, `target` / object: `equipment` |
| `mobs[].passengers[].equipment` | text: `mainhand` |
| `mobs[].reinforcements[]` | list: `actions` / text: `id` / number: `repeat_ticks` / object: `trigger` |
| `mobs[].reinforcements[].actions[]` | text: `arrow`, `pickup`, `potion`, `type` / boolean: `ballistic` / number: `count`, `gravity`, `min_radius`, `radius`, `source_height`, `spread`, `target_spread`, `velocity` / list: `spawns`, `waves` / object: `telegraph` |
| `mobs[].reinforcements[].actions[].spawns[]` | boolean: `avoid_line_of_sight`, `friendly_fire`, `persistent` / number: `count` / text: `entity`, `target` / object: `equipment` |
| `mobs[].reinforcements[].actions[].spawns[].equipment` | list: `armor_sets` |
| `mobs[].reinforcements[].actions[].spawns[].equipment.armor_sets[]` | boolean: `guaranteed` / text: `set` |
| `mobs[].reinforcements[].actions[].telegraph` | number: `arc_height`, `circle_points`, `duration_ticks`, `interval_ticks`, `path_points`, `radius` / boolean: `follow_target` / text: `particle`, `shape` |
| `mobs[].reinforcements[].actions[].waves[]` | number: `after_ticks`, `min_radius`, `radius` / list: `spawns` |
| `mobs[].reinforcements[].actions[].waves[].spawns[]` | boolean: `avoid_line_of_sight`, `friendly_fire`, `persistent` / number: `count` / text: `entity`, `target` / object: `equipment` |
| `mobs[].reinforcements[].actions[].waves[].spawns[].equipment` | list: `armor_sets` |
| `mobs[].reinforcements[].actions[].waves[].spawns[].equipment.armor_sets[]` | boolean: `guaranteed` / text: `set` |
| `mobs[].reinforcements[].trigger` | number: `after_ticks`, `at_or_below_percent` / text: `type` |
| `mobs[].rolling_cannon` | object: `ammunition`, `propellant` / text: `barrel`, `breech`, `chamber`, `spring`, `type` / number: `fire_rate`, `magazine_size`, `minimum_range`, `range`, `reload_ticks`, `retreat_range`, `rounds` / list: `target_vessel_templates` |
| `mobs[].rolling_cannon.ammunition` | object: `components` / number: `count` / text: `id` |
| `mobs[].rolling_cannon.propellant` | number: `count` / text: `id` |
| `mobs[].scout_signal` | number: `delay_ticks`, `range`, `speed` / object: `order` |
| `mobs[].scout_signal.order` | boolean: `announce` / object: `destination` / text: `guide`, `invasion`, `title`, `type` / number: `waves` |
| `mobs[].scout_signal.order.destination` | text: `id`, `type` / number: `search_radius` |
| `mobs[].spawn_animation` | number: `count`, `depth`, `downward_velocity`, `duration_ticks`, `height`, `horizontal_distance`, `max_duration_ticks`, `upward_velocity`, `velocity` / boolean: `safe_fall`, `visual_only` / object: `telegraph` / text: `type` |
| `mobs[].spawn_animation.telegraph` | number: `arc_height`, `circle_points`, `duration_ticks`, `interval_ticks`, `path_points`, `radius` / text: `particle`, `shape` |
| `particles` | number: `count`, `interval_ticks` / text: `particle` |
| `success_events` | number: `glow_after_killed_percent`, `timeout_ticks` / object: `mob_objective` / list: `rewards` |
| `success_events.mob_objective` | boolean: `announce` / text: `description`, `tag` / list: `rewards` |
| `success_events.mob_objective.rewards[]` | text: `ambush`, `delivery`, `guide`, `invasion`, `title`, `type` / boolean: `announce`, `end_if_destroyed`, `glow_drop` / object: `destination` / number: `waves` |
| `success_events.mob_objective.rewards[].destination` | text: `id`, `type` / number: `search_radius` |
| `success_events.rewards[]` | text: `track`, `type` |
| `unlock` | object: `activation`, `progress` / boolean: `consume_on_attempt`, `consume_on_success`, `repeatable` |
| `unlock.activation` | object: `conditions` / number: `delay_ticks` / list: `structures` / text: `type` |
| `unlock.activation.conditions` | list: `biomes`, `dimensions` / number: `min_y` / text: `moon`, `time`, `weather` / boolean: `surface` |
| `unlock.progress` | text: `block`, `entity`, `event`, `y_relation` / list: `blocks`, `entities` / object: `conditions` / number: `count`, `distance`, `duration_seconds`, `maximum_sample_distance`, `time_window_seconds`, `y_level`, `y_tolerance` / boolean: `once_per_entry`, `reset_on_exit` |
| `unlock.progress.conditions` | list: `biomes`, `dimensions`, `structures` |
| `weight_scaling` | number: `maximum`, `per_unit`, `start_at` / text: `mode` |

### Encounter

Use the mixed-cart raid reinforcement for a raid-wave trigger, or the fishing convoy escort for a scheduled encounter that starts a route. Conditions gate admission, unlocks track progress, and actions describe what happens after admission.

| Object | Fields by value form |
|---|---|
| **Root** | text: `_display_name`, `cooldown_group`, `default_faction`, `description`, `name` |
| **Root** | list: `actions`, `requires_mods`, `sounds` |
| **Root** | boolean: `allow_peaceful` |
| **Root** | object: `conditions`, `default_faction_behavior`, `natural_airship`, `success_events`, `trigger`, `unlock`, `wave`, `weight_scaling` |
| **Root** | validator: `effects`, `failure_events`, `first_success_weight`, `first_unlock_weight`, `ftb_quests`, `kill_count`, `kill_entity`, `typewriter` |
| **Root** | number: `weight` |
| `conditions` | list: `biomes`, `dimensions`, `faction_reputation`, `structures` / validator: `block_range`, `blocks`, `max_y`, `min_y`, `underground` / object: `height`, `progression`, `time` / boolean: `over_ocean`, `surface` / text: `weather` |
| `conditions.faction_reputation[]` | text: `faction` / number: `max` |
| `conditions.height` | number: `max`, `min` |
| `conditions.progression` | text: `curve`, `faction`, `tier` |
| `conditions.time` | number: `max`, `min` |
| `default_faction_behavior` | validator: `default_hostile_response`, `defend_range`, `flee_from_mobs`, `responses` / text: `hostile_response` |
| `failure_events` | validator: `actions`, `minimum_kill_percent`, `timeout_ticks` |
| `failure_events.actions[]` | validator: `ambush`, `command`, `type` |
| `ftb_quests` | validator: `on_failure_task_tag`, `on_start_task_tag`, `on_success_task_tag` |
| `natural_airship` | list: `attitudes` / text: `budget`, `curve`, `pool`, `repeat_group`, `threat_tier` / object: `conditions`, `follow_up` / number: `encounter_weight`, `progression_stage`, `selection_chance` / boolean: `respect_definition_cooldown` |
| `natural_airship.conditions` | list: `faction_reputation` |
| `natural_airship.conditions.faction_reputation` | validator: `attitude`, `faction`, `max`, `min` |
| `natural_airship.conditions.faction_reputation[]` | text: `faction` / number: `max` |
| `natural_airship.follow_up` | validator: `allow_same_encounter` / number: `chance_percent`, `max_chain_depth` / object: `delay_window` |
| `natural_airship.follow_up.delay_window` | text: `distribution` / number: `max_ticks`, `min_ticks`, `mode_ticks` |
| `success_events` | validator: `credited_rewards`, `glow_after_killed_percent`, `mob_objective` / list: `destroy_ships`, `protect_ships`, `rewards` / number: `timeout_ticks` |
| `success_events.mob_objective` | validator: `announce`, `description`, `rewards`, `tag` |
| `success_events.rewards[]` | number: `amount`, `amplifier`, `duration_ticks` / text: `effect`, `faction`, `track`, `type` |
| `trigger` | object: `chance` / number: `chance_per_wave`, `check_every_ticks`, `cooldown_ticks`, `final_wave_chance`, `maximum_per_raid`, `minimum_cooldown_ticks` / text: `entry_group`, `final_wave_requires_entity`, `type` / validator: `minimum_omen_level`, `raid_ids` / list: `omen_level_chances`, `to_dimensions` / boolean: `retain_successful_roll`, `spawn_at_raid_center` |
| `trigger.chance` | number: `after_success`, `base`, `increase_on_failure`, `max` / text: `mode` / boolean: `reset_on_success` |
| `trigger.omen_level_chances[]` | number: `chance_per_wave`, `final_wave_chance`, `minimum_level` |
| `unlock` | object: `activation`, `progress` / validator: `consume_on_attempt` / boolean: `consume_on_success`, `repeatable` |
| `unlock.activation` | object: `conditions` / number: `delay_ticks` / validator: `structures` / text: `type` |
| `unlock.activation.conditions` | number: `max_y`, `min_y` / text: `time` |
| `unlock.progress` | validator: `block`, `direction`, `distance`, `entities`, `from_dimensions`, `hysteresis`, `max_y`, `maximum_sample_distance`, `min_y`, `once_per_entry`, `require_each_entity`, `time_window_seconds` |
| `unlock.progress` | validator: `time_window_ticks`, `y_level`, `y_relation`, `y_tolerance` |
| `unlock.progress` | list: `blocks`, `to_dimensions` |
| `unlock.progress` | object: `conditions`, `required_kills` |
| `unlock.progress` | number: `count`, `duration_seconds`, `duration_ticks` |
| `unlock.progress` | text: `entity`, `event` |
| `unlock.progress` | boolean: `reset_on_exit` |
| `unlock.progress.conditions` | list: `biomes`, `dimensions`, `faction_reputation`, `structures` |
| `unlock.progress.conditions.faction_reputation[]` | text: `faction` / number: `max` |
| `unlock.progress.required_kills` | number: `count` / validator: `entities` / text: `entity` |
| `wave` | number: `cluster_radius`, `concealment_radius`, `maximum_attempts_per_member`, `minimum_radius`, `radius` / list: `groups` |
| `wave.groups[]` | number: `aggro_range`, `count`, `deaggro_range`, `nearby_player_range` / boolean: `avoid_line_of_sight`, `persistent` / text: `entity`, `faction`, `placement` / object: `equipment`, `rolling_cannon` / list: `tags` |
| `wave.groups[].equipment` | text: `mainhand` |
| `wave.groups[].rolling_cannon` | object: `ammunition`, `propellant` / boolean: `approach_raid` / text: `barrel`, `breech`, `chamber`, `spring`, `type` / number: `fire_rate`, `magazine_size`, `minimum_range`, `range`, `reload_ticks`, `retreat_range`, `rounds` |
| `wave.groups[].rolling_cannon.ammunition` | object: `components` / number: `count` / text: `id` |
| `wave.groups[].rolling_cannon.propellant` | number: `count` / text: `id` |
| `weight_scaling` | number: `maximum`, `per_unit`, `start_at` / validator: `minimum` / text: `mode` |

### Vessel profile

The Villager Anti-Air Car separates the encounter schedule from the machine. Put a reusable machine in the profile action. Use extends and overrides to vary a parent profile.

| Object | Fields by value form |
|---|---|
| **Root** | object: `action`, `overrides` / text: `extends` / number: `schema_version` |

### Vessel or encounter action

These settings live on an action or within the named child object. The field set depends on the action type. For a vehicle, use the profile with the same physical controls. The Berry fleet demonstrates formation members, the merchant tradeship demonstrates landing, and the Phantom demonstrates target-cover policy.

| Object | Fields by value form |
|---|---|
| **Root** | null or number: `absolute_y`, `air_search_attempts`, `air_step`, `cannon_fire_vertical_tolerance`, `envelope_fill`, `health_scan_budget`, `lifetime_ticks`, `maintenance_interval_ticks`, `ship_settle_delay_ticks`, `ship_stage_delay_ticks`, `spawn_distance` |
| **Root** | text: `activation_fire_mode`, `ambush`, `arrow`, `at`, `block`, `boss_health_mode`, `cannon_alignment_mode`, `cannon_broadside_mode`, `cannon_fire_mode`, `cannon_salvo_alignment`, `color`, `combat_target_priority` |
| **Root** | text: `convoy_role`, `convoy_slot`, `direction`, `fuze`, `item`, `message`, `message_channel`, `particle`, `pickup`, `potion`, `profile`, `propeller_direction` |
| **Root** | text: `replace_mode`, `schematic_front`, `shape`, `ship_name`, `sound`, `source_fallback`, `source_structure`, `spawn_anchor`, `spawn_height_reference`, `spawn_reference_action`, `structure_key`, `tactical_role` |
| **Root** | text: `target`, `template`, `type`, `unreachable_target_policy`, `victory_name` |
| **Root** | boolean: `adapt_terrain`, `airworthiness_cleanup_enabled`, `allow_engine_direction_override`, `allow_navigation_reverse`, `allow_nether_roof_spawn`, `allow_propulsion_reversal`, `allow_stuck_engine_direction_override`, `assemble_next_tick`, `assemble_swivel_bearings`, `ballistic`, `cannon_broadside_split`, `clear_natural_overgrowth` |
| **Root** | boolean: `clutch_when_above`, `continuous_propulsion`, `formation_shared_facing`, `health_ignore_envelope`, `ignore_target_cover`, `include_air`, `include_clutch_levers`, `inherit_entities`, `keep_propulsion_when_unattended`, `lootr_compatibility`, `managed`, `members_inherit_parent` |
| **Root** | boolean: `orient_on_spawn`, `preserve_clear_spawn_position`, `preserve_initial_propulsion_direction`, `protect_intact_hull_from_split`, `reanchor_before_assembly`, `require_clear_path`, `require_flat_ground`, `required`, `show_boss_bar`, `steering_follows_propulsion_direction`, `stuck_escape_enabled`, `unattended_force_forward_thrust` |
| **Root** | list: `additional_glue`, `assembly_origin`, `auto_aim_turrets`, `cannon_mechanical_aiming`, `choices`, `colors`, `combat_target_mob_types`, `conditional_redstone_sequences`, `crew_unseating`, `defense_rewards`, `destroyed_rewards`, `engine_direction_overrides` |
| **Root** | list: `engine_heat_overrides`, `entities`, `initial_hopper_contents`, `members`, `propeller_direction_overrides`, `propeller_swivel_positions`, `redstone_activations`, `required_mods`, `rope_links`, `sable_events`, `sides`, `spawn_anchor_position` |
| **Root** | list: `spawns`, `steering_controls`, `throttle_signal_by_y`, `unmanaged_analog_control_positions`, `waves` |
| **Root** | number: `after_ticks`, `arc_degrees`, `assembly_split_quiet_ms`, `attempts`, `autocannon_ammo_box_refill_ticks`, `behind_offset`, `block_break_reputation`, `build_clearance`, `cannon_alignment_tolerance_degrees`, `cannon_button_rest_ticks`, `cannon_button_ticks`, `cannon_fire_interval_seconds` |
| **Root** | number: `cannon_fire_range`, `cannon_hull_aim_fraction`, `cannon_load_batch_size`, `cannon_max_ship_tilt_degrees`, `cannon_mechanical_aim_update_ticks`, `cannon_player_safety_range`, `cannon_reload_delay_ticks`, `cannon_reload_minimum_ticks`, `cannon_salvo_min_ready`, `cannon_swivel_aim_update_ticks`, `cannon_swivel_settle_ticks`, `clutch_above_range` |
| **Root** | number: `clutch_align_degrees`, `clutch_align_range`, `clutch_touch_range`, `clutch_vertical_tolerance`, `combat_player_over_mobs_range`, `control_target_range`, `count`, `destroyed_cleanup_percent`, `direction_tolerance_degrees`, `duration_ticks`, `engine_burn_seconds`, `engine_burn_ticks` |
| **Root** | number: `fade_in_ticks`, `fade_out_ticks`, `far_distance`, `finalization_missing_sublevel_limit`, `flight`, `fog_occlusion_distance`, `fuze_ticks`, `gravity`, `health_scan_interval_ticks`, `height`, `initial_cannon_load_after_ticks`, `max_active` |
| **Root** | number: `max_generation_depth`, `maximum_attacks`, `maximum_clear_player_distance`, `min_radius`, `minimum_cannon_fire_interval_ticks`, `minimum_clear_player_distance`, `minimum_lifetime_ticks`, `minimum_spawn_distance`, `minimum_water_depth`, `near_distance`, `nether_max_hull_y`, `offset_y` |
| **Root** | number: `operational_split_quiet_ms`, `physics_wake_interval_ticks`, `pitch`, `player_build_chunk_radius`, `player_build_inhabited_ticks`, `player_engage_range`, `radius`, `recovery_abandon_ticks`, `recovery_total_turn_ticks`, `rope_suspended_child_delay_ticks`, `rope_suspended_child_hold_ticks`, `separation` |
| **Root** | number: `source_forward_offset`, `source_height`, `spawn_anchor_separation`, `spawn_bearing_degrees`, `spawn_hold_max_ticks`, `spawn_hold_settle_ticks`, `spawn_hold_ticks`, `spawn_terrain_clearance_blocks`, `split_guard_release_destroyed_percent`, `spread`, `stuck_clear_above_ground`, `stuck_climb_blocks` |
| **Root** | number: `stuck_climb_ticks`, `stuck_detect_ticks`, `stuck_forward_ticks`, `stuck_hold_ticks`, `stuck_lookahead_blocks`, `stuck_reverse_ticks`, `surface_analog_signal`, `surface_layer_y`, `target_height_offset`, `target_safe_radius`, `target_spread`, `throttle_signal` |
| **Root** | number: `velocity`, `vertical_offset`, `volume`, `water_clearance_radius`, `yaw_degrees` |
| **Root** | validator: `aggro_range`, `airworthiness_grace_ticks`, `airworthiness_shortfall_blocks`, `allow_partial_actions`, `altitude_hold_ticks`, `analog_controls`, `analog_hysteresis`, `analog_step`, `analog_step_seconds`, `approach_propulsion_fallback`, `autocannon_flak_range_margin`, `between_waves_ticks` |
| **Root** | validator: `block_percent`, `blocks`, `blue`, `bursts`, `cannon_fire_interval_ticks`, `cannon_max_steering_angle_degrees`, `cannon_mount_aim_update_ticks`, `cannon_mount_aiming`, `chance`, `child_cleanup_linger_ticks`, `cleanup_at_health_percent`, `clear` |
| **Root** | validator: `clearance_signal_margin`, `clutch_local`, `clutch_target_speed`, `combat_hostile_distance_advantage`, `combat_hostile_distance_ratio`, `completion_actions`, `completion_delay_ticks`, `completion_rewards`, `completion_sound`, `conditional_controls`, `conditions`, `contraption_fire_radius` |
| **Root** | validator: `control_lease_ticks`, `crossbow_range`, `deaggro_range`, `definition`, `delay_ticks`, `dimensions`, `direct_pitch`, `distribute_lever_signals`, `drives`, `duration_seconds`, `effects`, `engine_direction` |
| **Root** | validator: `engine_direction_by_player_direction`, `engine_superheated`, `entity`, `equipment`, `face_player`, `fire_mount_local`, `fog_start`, `follow_distance`, `friendly_fire`, `fuze_detonation_distance`, `gift_defeat_tier`, `glue` |
| **Root** | validator: `glue_watch_ticks`, `green`, `hold_release_blocks`, `hopper_contents`, `horn`, `horn_delay_ticks`, `hostile_vessel_first`, `integral_gain`, `integral_limit`, `interval`, `invert`, `kill_entity` |
| **Root** | validator: `lateral_offset`, `lifetime_seconds`, `local_direction`, `long_distance`, `loot`, `max_destroyed_percent`, `max_signal_step`, `maximum_signal`, `minimum_age_ticks`, `minimum_range`, `nearby_player_range`, `offset_x` |
| **Root** | validator: `offset_z`, `origin_offset_x`, `origin_offset_z`, `override_fluid_fog`, `passenger`, `persist_after_parent_cleanup`, `persistent`, `physics_wake_enabled`, `positions`, `potions`, `power_mounts_directly`, `propellant_blocks` |
| **Root** | validator: `propellant_power`, `propellant_stack_snbt`, `rammer`, `range`, `recover_beyond_distance`, `recover_cooldown_ticks`, `recover_runaway_enabled`, `recover_unseen_distance`, `recovery_abandon_distance`, `recovery_turn_max_ticks`, `red`, `render_distance` |
| **Root** | validator: `repeat_ticks`, `restart_policy`, `rope_link_to_parent`, `rotation`, `schematic_format`, `schematic_rotation`, `schematic_rotation_degrees`, `seat_predicates`, `shared_boss_id`, `shared_boss_leader`, `shell`, `shell_stack_snbt` |
| **Root** | validator: `ship_destroy_reputation`, `signal`, `source_delay_ticks`, `source_structures`, `spawn_angle_degrees`, `spawn_bearing_jitter_degrees`, `spawn_max_descent_blocks`, `split_off_despawn`, `split_off_despawn_ticks`, `stuck_lookahead_step`, `stuck_move_tolerance`, `surface` |
| **Root** | validator: `tags`, `target_direction`, `target_onboard_entities`, `target_range`, `target_underground_mobs`, `threat_detection_range`, `turn_altitude_boost`, `unattended_altitude_hold`, `unattended_despawn`, `velocity_lookahead_ticks`, `width`, `yaw` |
| **Root** | object: `altitude_controller`, `approach_effects`, `boss_bar`, `capture`, `car_controls`, `chase_controller`, `convoy`, `cowardice`, `crew_glow`, `defense_reward`, `directional_clutch_drives`, `disabled_vessel_targets` |
| **Root** | object: `downed_despawn`, `engine_stress_management`, `faction_behavior`, `fallback`, `force_render`, `ground_mob_target_condition`, `ground_patrol`, `hardware_requirements`, `landing_controller`, `nether_clearance`, `overrides`, `patrol` |
| **Root** | object: `propeller_direction_by_player_direction`, `propulsion_throttle`, `quest_contract`, `rope_suspended_child`, `rope_winch_controller`, `runaway_recovery_teleport`, `runtime`, `ship_ai`, `shot_down_cleanup`, `source_origin`, `spawn_clearance`, `suppress_when_player_within` |
| **Root** | object: `surface_flatness`, `tactics`, `telegraph`, `unseen_righting`, `wreck_cleanup` |
| **Root** | boolean or null: `attach_child_sublevels`, `avoid_line_of_sight`, `bearing_never_place`, `clutch_when_aligned` |
| **Root** | null or object: `cannon_assembly`, `depart_then_despawn`, `despawn_effect`, `split_fragment_cleanup` |
| **Root** | list or null: `cannon_reloads`, `cannon_swivel_aiming`, `initial_cannon_loads` |
| **Root** | list or object: `container_loot` |
| **Root** | null or text: `facing`, `faction`, `id`, `placement` |
| **Root** | boolean or object: `fleet_health` |
| `additional_drive_lever_pairs` | validator: `forward_lever`, `forward_lever_inverted`, `reverse_lever`, `reverse_lever_inverted` |
| `additional_glue[]` | list: `from`, `to` |
| `altitude_controller` | list: `analog_controls`, `control_positions`, `player_y_offsets`, `reference_local` |
| `altitude_controller` | number: `analog_hysteresis`, `analog_step_seconds`, `buoyancy_feedback_limit`, `calibration_settle_ticks`, `clearance_minimum_signal`, `close_offset`, `damage_recalibration_percent`, `damage_recalibration_ticks`, `descent_arrest_engage_velocity`, `descent_arrest_margin`, `descent_arrest_release_velocity`, `descent_arrest_signal` |
| `altitude_controller` | number: `descent_signal`, `failsafe_signal`, `follow_signal_step`, `full_scale_blocks`, `gain`, `ground_hull_bottom_offset`, `ground_hull_clearance`, `ground_mode_airborne_height`, `ground_overshoot_hard`, `ground_overshoot_soft`, `hard_descent_engage_above`, `height_map_deadband` |
| `altitude_controller` | number: `height_map_signal_per_block`, `hold_release_blocks`, `hover_signal`, `hull_bottom_offset`, `integral_gain`, `integral_limit`, `lever_changes_per_update`, `lever_update_ticks`, `max_signal_step`, `maximum_player_offset`, `maximum_signal`, `minimum_ground_clearance` |
| `altitude_controller` | number: `minimum_player_offset`, `minimum_signal`, `offset`, `path_lookahead_blocks`, `path_lookahead_clearance`, `path_lookahead_half_width`, `path_lookahead_max_signal`, `path_lookahead_step`, `player_above_deadband`, `player_above_minimum_signal`, `retarget_deadband`, `spawn_approach_height` |
| `altitude_controller` | number: `stable_capture_ticks`, `stable_velocity_tolerance`, `target_y`, `tolerance`, `turn_altitude_boost`, `velocity_lookahead_ticks`, `vessel_offset` |
| `altitude_controller` | object: `balanced_lift`, `sequence_surface_offsets` |
| `altitude_controller` | boolean: `calibrate_levers`, `distribute_lever_signals`, `ground_clearance_enabled`, `ground_clearance_is_minimum`, `hard_descent_enabled`, `initialize_at_spawn`, `match_cannon_height`, `path_collision_volume_enabled`, `path_lookahead_enabled`, `path_surface_preview`, `physical_height_signal_map`, `respect_world_height` |
| `altitude_controller` | boolean: `spawn_at_controller_target` |
| `altitude_controller` | boolean or null: `descent_arrest_enabled` |
| `altitude_controller` | text: `mode`, `signal_strategy` |
| `altitude_controller.analog_controls[]` | text: `block` |
| `altitude_controller.balanced_lift` | list: `center`, `pairs` |
| `altitude_controller.player_y_offsets[]` | number: `max_y`, `min_y`, `offset` |
| `altitude_controller.sequence_surface_offsets` | number: `deep_ocean_fishing` |
| `approach_effects` | validator: `firework`, `message`, `particle` / number: `range`, `stage_interval_ticks` / object: `sound`, `text` / list: `stages` |
| `approach_effects.firework` | validator: `colors`, `count`, `fade_colors`, `flight`, `height`, `shape`, `spread` |
| `approach_effects.particle` | validator: `count`, `particle`, `spread` |
| `approach_effects.sound` | number: `pitch`, `volume` / text: `sound` |
| `approach_effects.stages[]` | validator: `firework`, `message` / text: `id` / object: `particle`, `sound`, `text` / number: `range` |
| `approach_effects.stages[].particle` | number: `count`, `speed`, `spread_x`, `spread_y`, `spread_z` / text: `particle` / validator: `spread` |
| `approach_effects.stages[].sound` | number: `pitch`, `volume` / text: `sound` |
| `approach_effects.stages[].text` | text: `mode`, `text` / number: `stay_ticks` |
| `approach_effects.text` | number: `fade_in_ticks`, `fade_out_ticks`, `stay_ticks` / text: `mode`, `text` |
| `assembly_origin` | validator: `x`, `y`, `z` |
| `auto_aim_turrets[]` | number: `after_ticks`, `button_ticks`, `direct_fire_max_range`, `drag`, `fire_tolerance_degrees`, `firing_arc_degrees`, `gravity`, `max_pitch_degrees`, `max_yaw_degrees`, `maximum_miss_distance`, `min_pitch_degrees`, `minimum_cannon_fire_interval_ticks` |
| `auto_aim_turrets[]` | number: `pitch_target_sign`, `range`, `repeat_ticks`, `velocity`, `vertical_tolerance`, `wheel_limit_degrees`, `yaw_sign` |
| `auto_aim_turrets[]` | boolean: `auto_calibrate_yaw`, `ballistic`, `use_native_muzzle_offset` |
| `auto_aim_turrets[]` | list: `cannons`, `firing_forward_local`, `steering_wheel_local` |
| `auto_aim_turrets[]` | text: `fire_mode`, `id`, `salvo_group`, `trajectory` |
| `auto_aim_turrets[]` | validator: `high_trajectory_min_range` |
| `auto_aim_turrets[]` | object: `pitch` |
| `auto_aim_turrets[].cannons[]` | list: `fire_button_local`, `mount_local` |
| `auto_aim_turrets[].pitch` | boolean: `auto_calibrate_polarity`, `brake_on_target_crossing`, `neutral_when_aligned`, `positive_powered`, `reverse_on_stall` |
| `auto_aim_turrets[].pitch` | list: `clutch_control_local`, `clutch_local`, `directional_gearshift_local`, `engine_local`, `gearshift_control_local`, `gearshift_local`, `left_control_local`, `right_control_local` |
| `auto_aim_turrets[].pitch` | number: `direction_calibration_min_degrees`, `direction_calibration_ticks`, `direction_switch_hysteresis_degrees`, `gearshift_update_ticks`, `stall_min_progress_degrees`, `stall_reversal_ticks`, `target_crossing_brake_ticks`, `tolerance_degrees` |
| `auto_aim_turrets[].pitch` | text: `direction_control`, `positive_control` |
| `blocks[]` | validator: `blend`, `block`, `nbt`, `offset`, `properties`, `replace` |
| `boss_bar` | text: `audience`, `color`, `name`, `overlay` / boolean: `create_world_fog`, `darken_screen`, `play_boss_music`, `progress_until_despawn` / validator: `darken_sky`, `range` / number: `visible_range` |
| `cannon_assembly` | number: `after_ticks`, `assembly_verify_timeout_ticks`, `preassembly_clutch_ticks`, `signal` |
| `cannon_assembly` | text: `block`, `component`, `state` |
| `cannon_assembly` | boolean: `force_redstone_edge`, `include_clutch_levers`, `kinetic_mount_reassemble`, `power_mounts_directly`, `preassemble_with_clutches`, `preserve_mount_rotation`, `require_loaded_cbc`, `required` |
| `cannon_assembly` | null or number: `minimum_assembled_mounts` |
| `cannon_assembly` | list: `mount_positions`, `positions` |
| `cannon_mechanical_aiming[]` | boolean: `ballistic`, `require_reachable_firing_solution`, `use_native_muzzle_offset` |
| `cannon_mechanical_aiming[]` | number: `direct_fire_max_range`, `drag`, `fire_tolerance_degrees`, `firing_arc_degrees`, `gravity`, `max_pitch_degrees`, `max_yaw_degrees`, `min_pitch_degrees`, `pitch_target_sign`, `rear_arc_degrees`, `rear_min_pitch_degrees`, `velocity` |
| `cannon_mechanical_aiming[]` | number: `yaw_target_sign` |
| `cannon_mechanical_aiming[]` | list: `firing_forward_local`, `mount_local` |
| `cannon_mechanical_aiming[]` | text: `id`, `trajectory` |
| `cannon_mechanical_aiming[]` | object: `pitch`, `yaw` |
| `cannon_mechanical_aiming[].pitch` | boolean: `auto_calibrate_polarity`, `brake_on_target_crossing`, `neutral_when_aligned`, `positive_powered`, `reverse_on_stall` |
| `cannon_mechanical_aiming[].pitch` | list: `clutch_control_local`, `clutch_local`, `directional_gearshift_local`, `engine_local`, `gearshift_control_local`, `gearshift_local`, `left_control_local`, `right_control_local` |
| `cannon_mechanical_aiming[].pitch` | number: `direction_calibration_min_degrees`, `direction_calibration_ticks`, `direction_switch_hysteresis_degrees`, `engine_burn_ticks`, `gearshift_update_ticks`, `stall_min_progress_degrees`, `stall_reversal_ticks`, `target_crossing_brake_ticks`, `tolerance_degrees` |
| `cannon_mechanical_aiming[].pitch` | text: `direction_control`, `positive_control` |
| `cannon_mechanical_aiming[].yaw` | boolean: `auto_calibrate_polarity`, `brake_on_target_crossing`, `positive_powered`, `reverse_on_stall` |
| `cannon_mechanical_aiming[].yaw` | list: `clutch_control_local`, `clutch_local`, `engine_local`, `gearshift_control_local`, `gearshift_local` |
| `cannon_mechanical_aiming[].yaw` | number: `direction_calibration_min_degrees`, `direction_calibration_ticks`, `direction_switch_hysteresis_degrees`, `gearshift_update_ticks`, `stall_min_progress_degrees`, `stall_reversal_ticks`, `target_crossing_brake_ticks`, `tolerance_degrees` |
| `cannon_mechanical_aiming[].yaw` | text: `direction_control` |
| `cannon_reloads[]` | text: `fuze`, `id`, `propellant_stack_snbt`, `propellant_type`, `shell_stack_snbt` / number: `fuze_detonation_distance`, `fuze_ticks`, `propellant_blocks`, `propellant_power`, `reload_after_ticks` / list: `mount_local` / boolean: `required`, `unload_before_load` / object: `shell` |
| `cannon_reloads[].shell` | text: `fill`, `potion`, `type` |
| `cannon_swivel_aiming` | validator: `auto_calibrate_yaw`, `ballistic`, `drag`, `fire_requires_pitch`, `fire_tolerance_degrees`, `firing_arc_degrees`, `firing_forward_local`, `gravity`, `id`, `max_yaw_degrees`, `mount_local`, `steering_wheel_local`, `trajectory`, `use_native_muzzle_offset`, `velocity`, `wheel_limit_degrees`, `yaw_offset_degrees`, `yaw_sign` |
| `cannon_swivel_aiming[]` | boolean: `auto_calibrate_yaw`, `fire_requires_pitch` / number: `fire_tolerance_degrees`, `firing_arc_degrees`, `max_yaw_degrees`, `wheel_limit_degrees`, `yaw_sign` / list: `firing_forward_local`, `mount_local`, `steering_wheel_local` / text: `id` |
| `capture` | list: `actions`, `rewards` / text: `captured_reward_ambush` / number: `max_destroyed_percent`, `post_capture_cleanup_ticks` / object: `rescue` |
| `capture.actions[]` | text: `at`, `message`, `particle`, `sound`, `type` / number: `count`, `pitch`, `spread`, `volume` / boolean: `long_distance` |
| `capture.rescue` | list: `ambushes`, `rewards` / text: `entity`, `message`, `release_faction`, `tag` / number: `minimum_survivors` |
| `capture.rescue.rewards[]` | number: `amount` / text: `faction`, `type` |
| `capture.rewards[]` | number: `amount` / text: `faction`, `type` |
| `car_controls` | list: `additional_drive_lever_pairs`, `clutch_lever`, `forward_lever`, `left_turn_lever`, `reverse_lever`, `right_turn_lever`, `speed_throttle_local` |
| `car_controls` | number: `alignment_drive_signal`, `broadside_brake_min_range`, `broadside_brake_range`, `broadside_stop_alignment_degrees`, `drive_signal`, `fixed_drive_throttle`, `high_throttle_bearing_degrees`, `high_throttle_distance`, `idle_signal`, `maximum_throttle`, `minimum_throttle`, `pit_max_drop_blocks` |
| `car_controls` | number: `ram_range`, `recoil_clearance_blocks`, `retreat_below_range`, `retreat_min_range`, `reverse_when_behind_degrees`, `stop_alignment_degrees`, `stop_distance`, `target_still_speed`, `turn_deadband_degrees`, `turn_inner_signal`, `update_ticks` |
| `car_controls` | boolean: `commit_turn_until_aligned`, `forward_lever_inverted`, `invert_steering`, `invert_steering_when_reversing`, `pit_reverse_escape`, `position_for_fixed_cannons`, `reverse_lever_inverted` |
| `car_controls` | text: `turn_mode` |
| `car_controls.additional_drive_lever_pairs[]` | list: `forward_lever`, `reverse_lever` / boolean: `forward_lever_inverted`, `reverse_lever_inverted` |
| `chase_controller` | number: `idle_signal`, `max_angle`, `maximum_signal`, `resume_distance`, `reverse_at_distance`, `spread_correction_range`, `spread_degrees`, `stop_distance`, `target_range`, `throttle_signal`, `turn_lead_blocks`, `update_ticks` |
| `choices[]` | text: `ambush` / number: `weight` |
| `conditional_redstone_sequences[]` | number: `activation_delay_ticks`, `priority` / text: `id` / boolean: `lock_analog_controls`, `release_controls_on_completion` / validator: `repeat_ticks` / list: `steps` / object: `when` |
| `conditional_redstone_sequences[].steps[]` | validator: `block`, `component`, `include_clutch_levers`, `positions`, `pulse_ticks`, `rope_length`, `when` / number: `delay_ticks`, `signal` / text: `operation` / list: `position` / boolean: `powered` |
| `conditional_redstone_sequences[].when` | validator: `age_ticks`, `all`, `analog`, `combat_target`, `convoy_sequence`, `crew_aboard`, `day_time`, `destroyed_percent`, `dimension`, `hull_remaining_percent`, `player_aboard`, `ship_y`, `structure_collision`, `typewriter_key`, `visible_to_players`, `weather` / list: `any` / text: `biome` / object: `nearby`, `not` / boolean: `water_below` |
| `conditional_redstone_sequences[].when.analog` | validator: `block`, `component`, `match`, `max`, `min`, `position`, `positions`, `signal` |
| `conditional_redstone_sequences[].when.any[]` | object: `combat_target`, `nearby`, `ship_y` / boolean: `structure_collision` |
| `conditional_redstone_sequences[].when.any[].combat_target` | number: `half_angle_degrees`, `max_range` / text: `relative_sector` |
| `conditional_redstone_sequences[].when.any[].nearby` | number: `half_angle_degrees`, `min_count`, `range` / list: `relations`, `targets` / text: `relative_sector` |
| `conditional_redstone_sequences[].when.any[].ship_y` | number: `min` |
| `conditional_redstone_sequences[].when.convoy_sequence` | validator: `member_id`, `sequence` |
| `conditional_redstone_sequences[].when.crew_aboard` | validator: `entity`, `present` |
| `conditional_redstone_sequences[].when.nearby` | validator: `factions`, `max_count`, `min_count` / number: `half_angle_degrees`, `range` / boolean: `horizontal_only` / list: `origin_local`, `relations`, `targets` / text: `relative_sector` |
| `conditional_redstone_sequences[].when.not` | list: `any` / object: `nearby` |
| `conditional_redstone_sequences[].when.not.any[]` | object: `combat_target`, `nearby` |
| `conditional_redstone_sequences[].when.not.any[].combat_target` | number: `half_angle_degrees`, `max_range` / text: `relative_sector` |
| `conditional_redstone_sequences[].when.not.any[].nearby` | number: `half_angle_degrees`, `range` / list: `relations`, `targets` / text: `relative_sector` |
| `conditional_redstone_sequences[].when.not.nearby` | number: `half_angle_degrees`, `range` / boolean: `horizontal_only` / list: `origin_local`, `relations`, `targets` / text: `relative_sector` |
| `container_loot` | list: `blocks`, `loot_tables` / boolean: `combine`, `lootr` |
| `container_loot[]` | text: `block`, `loot_table` / list: `blocks`, `loot_tables`, `position`, `positions`, `slots` / boolean: `combine`, `lootr`, `replace_existing` |
| `container_loot[].slots[]` | number: `count`, `slot` / text: `item` |
| `convoy` | object: `escort_route`, `formation`, `navigation_controller` / text: `leader_loss`, `navigation` / number: `leader_loss_grace_ticks`, `max_pursuit_from_convoy`, `threat_detection_range` |
| `convoy.escort_route` | text: `arrival_message`, `arrival_sequence`, `flagship`, `invitation`, `start` / number: `arrival_radius`, `departure_height`, `escort_radius`, `raid_at_fraction`, `required_fraction`, `timeout_ticks` / object: `destination`, `note`, `raid`, `rescue` / list: `rewards` |
| `convoy.escort_route.destination` | text: `id`, `type` / number: `minimum_distance`, `search_radius` |
| `convoy.escort_route.note` | text: `text`, `title` |
| `convoy.escort_route.raid` | list: `choices` / text: `faction`, `id`, `type` / number: `max_active`, `maximum_attacks`, `minimum_spawn_distance`, `spawn_distance` |
| `convoy.escort_route.raid.choices[]` | text: `ambush` / number: `weight` |
| `convoy.escort_route.rescue` | number: `maximum_survivors`, `timeout_ticks` / list: `rewards` |
| `convoy.escort_route.rescue.rewards[]` | number: `amount` / text: `faction`, `type` |
| `convoy.escort_route.rewards[]` | number: `amount` / text: `faction`, `type` |
| `convoy.formation` | text: `mode` / object: `slots` / number: `spacing`, `tolerance` |
| `convoy.formation.slots` | object: `center_aft`, `escort_1`, `escort_2`, `escort_3`, `escort_4`, `escort_5`, `escort_6`, `escort_7`, `escort_8`, `escort_9`, `port_aft`, `port_beam`, `port_flank`, `port_front`, `starboard_aft`, `starboard_beam`, `starboard_flank`, `starboard_front` / validator: `x`, `y`, `z` |
| `convoy.formation.slots.center_aft` | number: `x`, `y`, `z` |
| `convoy.formation.slots.escort_1` | number: `x`, `y`, `z` |
| `convoy.formation.slots.escort_2` | number: `x`, `y`, `z` |
| `convoy.formation.slots.escort_3` | number: `x`, `y`, `z` |
| `convoy.formation.slots.escort_4` | number: `x`, `y`, `z` |
| `convoy.formation.slots.escort_5` | number: `x`, `y`, `z` |
| `convoy.formation.slots.escort_6` | number: `x`, `y`, `z` |
| `convoy.formation.slots.escort_7` | number: `x`, `y`, `z` |
| `convoy.formation.slots.escort_8` | number: `x`, `y`, `z` |
| `convoy.formation.slots.escort_9` | number: `x`, `y`, `z` |
| `convoy.formation.slots.port_aft` | number: `x`, `y`, `z` |
| `convoy.formation.slots.port_beam` | number: `x`, `y`, `z` |
| `convoy.formation.slots.port_flank` | number: `x`, `y`, `z` |
| `convoy.formation.slots.port_front` | number: `x`, `y`, `z` |
| `convoy.formation.slots.starboard_aft` | number: `x`, `y`, `z` |
| `convoy.formation.slots.starboard_beam` | number: `x`, `y`, `z` |
| `convoy.formation.slots.starboard_flank` | number: `x`, `y`, `z` |
| `convoy.formation.slots.starboard_front` | number: `x`, `y`, `z` |
| `convoy.navigation_controller` | number: `collision_prediction_ticks`, `formation_combat_weight`, `hard_separation`, `hull_margin`, `leader_lookahead_ticks`, `maximum_avoidance_offset`, `rejoin_distance`, `rejoin_release_distance`, `reposition_ticks`, `soft_separation` |
| `cowardice` | number: `at_or_below_percent`, `despawn_after_ticks`, `speed` / boolean: `enabled`, `include_shared_boss_members`, `ships_run_away` |
| `crew_glow` | number: `at_or_below_percent` |
| `crew_unseating[]` | list: `crew_tags` / text: `id` / number: `max_count` / boolean: `once` / object: `trigger` |
| `crew_unseating[].trigger` | number: `clearance`, `range` / text: `type` |
| `defense_reward` | number: `range` / list: `rewards` |
| `defense_reward.rewards[]` | number: `amount` / text: `faction`, `type` |
| `defense_rewards[]` | number: `amount` / text: `faction`, `type` |
| `depart_then_despawn` | number: `after_ticks`, `minimum_age_ticks`, `minimum_distance`, `minimum_player_distance`, `timeout_ticks` / text: `direction`, `trigger` |
| `despawn_effect` | text: `particle`, `reward_ambush`, `secondary_particle`, `sound`, `type` |
| `destroyed_rewards[]` | number: `amount` / text: `faction`, `type` |
| `directional_clutch_drives` | list: `drives` / number: `target_range`, `update_ticks` |
| `directional_clutch_drives.drives[]` | list: `clutch_local`, `local_direction` / text: `id` |
| `disabled_vessel_targets` | number: `maximum_ground_clearance`, `maximum_speed`, `minimum_missing_blocks` |
| `downed_despawn` | boolean: `enabled` / number: `maximum_angular_speed`, `maximum_speed`, `minimum_missing_blocks`, `stationary_ticks`, `unseen_ticks` |
| `engine_direction_overrides[]` | text: `direction` / list: `position` |
| `engine_heat_overrides[]` | list: `position` / boolean: `superheated` |
| `engine_stress_management` | number: `maximum_signal` / text: `mode` |
| `entities[]` | boolean: `adult`, `aggro_through_walls`, `crew`, `custom_name_visible`, `fill_all`, `fill_all_seats`, `friendly_fire`, `no_ai`, `persistent`, `pillager_crossbow_ai`, `seat`, `share_spawn_marker` |
| `entities[]` | number: `aggro_range`, `count`, `deaggro_range`, `health`, `nearby_player_range`, `target_range` |
| `entities[]` | text: `banner`, `custom_name`, `entity`, `faction`, `fallback_entity`, `placement`, `target` |
| `entities[]` | list: `effects`, `entity_choices`, `local`, `offsets`, `reinforcements`, `seat_predicates`, `spawn_on_blocks`, `tags` |
| `entities[]` | object: `equipment`, `kill_reputation`, `merchant`, `nbt`, `particles` |
| `entities[].entity_choices[]` | text: `entity` / object: `merchant` / list: `required_mods` / number: `weight` |
| `entities[].entity_choices[].merchant` | number: `experience`, `level`, `trade_count` / text: `profession`, `trades_mode` / list: `trades` |
| `entities[].entity_choices[].merchant.trades[]` | object: `cost`, `result` / number: `max_uses`, `price_multiplier`, `villager_xp` |
| `entities[].entity_choices[].merchant.trades[].cost` | number: `count` / text: `item` |
| `entities[].entity_choices[].merchant.trades[].result` | number: `count` / text: `item` |
| `entities[].equipment` | list: `armor_sets` / text: `helmet`, `mainhand` |
| `entities[].equipment.armor_sets[]` | boolean: `guaranteed` / text: `set` / number: `weight` |
| `entities[].kill_reputation` | number: `<key>` |
| `entities[].merchant` | number: `despawn_delay_ticks`, `experience`, `level`, `trade_count` / boolean: `disable_night_invisibility` / text: `profession`, `trades_mode` / list: `trades` |
| `entities[].merchant.trades[]` | object: `cost`, `result` / number: `max_uses`, `price_multiplier`, `villager_xp` / boolean: `optional_result` |
| `entities[].merchant.trades[].cost` | number: `count` / text: `item` |
| `entities[].merchant.trades[].result` | number: `count` / text: `item` |
| `entities[].particles` | number: `count`, `interval_ticks` / text: `particle` |
| `entities[].reinforcements[]` | list: `actions` / text: `id` / number: `repeat_ticks` / object: `trigger` |
| `entities[].reinforcements[].actions[]` | boolean: `require_clear_path` / number: `source_height`, `spread`, `target_spread` / object: `telegraph` / text: `type` / list: `waves` |
| `entities[].reinforcements[].actions[].telegraph` | number: `duration_ticks`, `interval_ticks`, `radius` / text: `particle`, `shape` |
| `entities[].reinforcements[].actions[].waves[]` | number: `after_ticks` / list: `entries` |
| `entities[].reinforcements[].actions[].waves[].entries[]` | text: `arrow`, `kind`, `potion` / number: `count` |
| `entities[].reinforcements[].trigger` | number: `after_ticks`, `max_distance` / boolean: `require_line_of_sight` / text: `type` |
| `entities[].seat_predicates[]` | text: `block` / list: `position` |
| `entities[].spawn_on_blocks[]` | text: `block` / list: `position` |
| `faction_behavior` | boolean: `flee_from_mobs` / text: `hostile_response` |
| `fleet_health` | boolean: `boss_bar` / list: `events` |
| `fleet_health.events[]` | list: `actions` / text: `id` / number: `repeat_ticks` / object: `trigger` |
| `fleet_health.events[].actions[]` | text: `at`, `sound`, `type` / number: `pitch`, `source_delay_ticks`, `source_height`, `spread`, `target_spread`, `velocity`, `volume` / list: `source_structures`, `waves` |
| `fleet_health.events[].actions[].waves[]` | number: `after_ticks` / list: `entries` |
| `fleet_health.events[].actions[].waves[].entries[]` | text: `arrow`, `kind` / number: `count` |
| `fleet_health.events[].trigger` | boolean: `horizontal_only` / number: `range` / text: `type` |
| `force_render` | number: `after_operational_ticks`, `release_distance`, `render_distance`, `tracking_distance` / text: `audience` / boolean: `enabled` |
| `ground_mob_target_condition` | object: `convoy_sequence` |
| `ground_mob_target_condition.convoy_sequence` | text: `member_id`, `sequence` |
| `ground_patrol` | number: `aggro_range`, `combat_leash_radius`, `deaggro_range`, `leash_radius` / list: `entities` |
| `ground_patrol.entities[]` | number: `count` / text: `entity` / object: `equipment` |
| `ground_patrol.entities[].equipment` | list: `armor_sets` / text: `helmet`, `mainhand` |
| `ground_patrol.entities[].equipment.armor_sets[]` | boolean: `guaranteed` / text: `set` / number: `weight` |
| `hardware_requirements` | number: `minimum_analog_controls`, `minimum_balloon_components`, `minimum_buttons`, `minimum_cannon_mounts`, `minimum_containers`, `minimum_engines_or_propellers`, `minimum_levers`, `minimum_seats`, `minimum_steering_controls` / null or number: `minimum_hoppers` |
| `initial_cannon_loads[]` | boolean: `allow_unsafe_propellant`, `required`, `unload_before_load` / text: `fuze`, `id`, `propellant_stack_snbt`, `propellant_type`, `shell_stack_snbt` / number: `fuze_detonation_distance`, `fuze_ticks`, `propellant_blocks`, `propellant_power` / list: `mount_local` / object: `shell` |
| `initial_cannon_loads[].shell` | text: `fill`, `potion`, `type` |
| `initial_hopper_contents[]` | number: `count`, `slot` / text: `item`, `snbt` |
| `landing_controller` | number: `approach_lookahead_blocks`, `approach_max_speed`, `approach_radius`, `approach_resume_speed`, `approach_timeout_ticks`, `arrival_range`, `height_above_ground`, `max_signal_step`, `trigger_distance`, `visit_ticks` |
| `landing_controller` | boolean: `depart_on_committed_threat`, `depart_on_threat`, `land_in_arrival_area`, `refuse_hostile_player` |
| `landing_controller` | list: `door_control_local`, `door_engine_local`, `propulsion_control_local` |
| `landing_controller` | text: `door_engine_direction` |
| `landing_controller` | object: `surface_flatness` |
| `landing_controller.surface_flatness` | number: `max_height_delta`, `max_neighbor_delta`, `maximum_fluid_fraction`, `minimum_support_fraction`, `padding`, `sample_step` |
| `nether_clearance` | number: `floor_clearance_max`, `floor_clearance_min`, `floor_emergency_margin`, `hull_bottom_offset`, `hull_top_offset`, `roof_clearance_max`, `roof_clearance_min`, `roof_emergency_margin`, `scan_radius` |
| `patrol` | validator: `altitude_offset`, `hostile_response`, `on_target_lost`, `radius` / object: `fallback_target`, `target` / number: `intercept_range`, `max_pursuit_from_patrol`, `orbit_points`, `patrol_radius`, `point_interval_ticks` / text: `mode`, `route_end` / list: `route` |
| `patrol.fallback_target` | object: `position` / text: `type` / boolean: `use_spawn_anchor` |
| `patrol.fallback_target.position` | number: `x`, `y`, `z` |
| `patrol.route[]` | number: `x`, `y`, `z` |
| `patrol.target` | text: `biome`, `faction`, `structure`, `template`, `type` / validator: `convoy_role`, `use_spawn_anchor`, `x`, `y`, `z` / object: `position` / number: `search_radius` / boolean: `use_micro_structure` |
| `patrol.target.position` | number: `x`, `y`, `z` |
| `propeller_direction_by_player_direction` | text: `back_left`, `back_right`, `behind`, `default` |
| `propeller_direction_overrides[]` | text: `direction` / list: `position` |
| `propulsion_throttle` | number: `high_beyond_distance`, `high_recovery_signal`, `high_signal`, `low_signal`, `near_signal`, `recovery_signal`, `stop_signal`, `stop_within_distance` / boolean: `interpolate` / list: `position` |
| `quest_contract` | number: `attempt_interval_ticks` / text: `item` |
| `redstone_activations[]` | number: `after_ticks`, `alignment_hold_ticks`, `button_ticks`, `direction_tolerance_degrees`, `direction_vertical_tolerance_degrees`, `formation_stagger_ticks`, `minimum_cannon_fire_interval_ticks`, `minimum_height_above_player`, `minimum_range`, `range`, `repeat_ticks`, `sequence_interval_ticks` |
| `redstone_activations[]` | number: `signal`, `vertical_alignment_blocks`, `vertical_tolerance` |
| `redstone_activations[]` | text: `block`, `cannon_broadside_side`, `cannon_mechanical_aiming`, `cannon_salvo_group`, `cannon_swivel_aiming`, `component`, `fire_mode`, `id`, `player_direction`, `require`, `state`, `target_direction` |
| `redstone_activations[]` | list: `blocks`, `fire_mount_local`, `positions`, `power_positions` |
| `redstone_activations[]` | object: `cannon_alignment`, `firing_door` |
| `redstone_activations[]` | boolean: `horizontal_only`, `require_living_crew` |
| `redstone_activations[].cannon_alignment` | list: `aim_local`, `mount_local` / number: `arc_degrees`, `drag`, `gravity`, `maximum_miss_distance`, `velocity` / boolean: `ballistic`, `horizontal_only`, `use_native_muzzle_offset` |
| `redstone_activations[].firing_door` | list: `control_local` / boolean: `open_powered` |
| `rescue` | validator: `ambushes`, `entity`, `message`, `minimum_survivors`, `release_faction`, `rewards`, `tag` |
| `rope_links[]` | list: `from`, `to` |
| `rope_winch_controller` | object: `cycle` / list: `drivetrains` / number: `engine_burn_ticks`, `maximum_rope_length`, `minimum_rope_length`, `update_ticks` / boolean: `reverse_pays_out`, `sequence_controlled` |
| `rope_winch_controller.cycle` | number: `lower_hold_ticks`, `lowered_length`, `maximum_skew`, `raised_hold_ticks`, `raised_length`, `required_winches` |
| `rope_winch_controller.drivetrains[]` | list: `clutch_local`, `engine_local`, `gearshift_local` |
| `runaway_recovery_teleport` | number: `beyond_chunks`, `beyond_distance`, `cooldown_ticks`, `hidden_settle_ticks`, `hide_after_departure_blocks`, `maximum_target_distance`, `return_distance`, `reveal_after_travel_blocks`, `reveal_timeout_ticks`, `unseen_chunks` / boolean: `conceal_teleport`, `enabled` / text: `return_height_mode`, `target_selection` |
| `runtime` | list: `auto_aim_turrets`, `cannon_reloads`, `engine_activations`, `initial_cannon_loads`, `proximity_levers`, `vessel_arrivals` / number: `autocannon_ammo_box_refill_ticks`, `minimum_crew` / object: `cannon_assembly`, `weapons` / validator: `despawn_effect`, `redstone_activations` / boolean: `requires_crew`, `warning_firework` |
| `runtime.auto_aim_turrets[]` | list: `additional_mounts`, `fire_buttons`, `firing_forward_local`, `mount_local` |
| `runtime.auto_aim_turrets[]` | validator: `ammunition_sources`, `autocannon_flak_range_margin`, `high_trajectory_min_range` |
| `runtime.auto_aim_turrets[]` | boolean: `ballistic`, `shared_aim`, `use_native_muzzle_offset` |
| `runtime.auto_aim_turrets[]` | number: `direct_fire_max_range`, `drag`, `fire_tolerance_degrees`, `firing_arc_degrees`, `gravity`, `max_pitch_degrees`, `min_pitch_degrees`, `range`, `repeat_ticks`, `velocity` |
| `runtime.auto_aim_turrets[]` | text: `id`, `name`, `trajectory`, `weapon_type` |
| `runtime.auto_aim_turrets[]` | object: `pitch`, `yaw` |
| `runtime.auto_aim_turrets[].pitch` | boolean: `auto_calibrate_polarity` / validator: `clutch_local`, `gearshift_local`, `positive_direction_powered`, `steering_wheel_local`, `swivel_bearing_local` / list: `directional_gearshift_local`, `left_control_local`, `right_control_local` / number: `tolerance_degrees` |
| `runtime.auto_aim_turrets[].yaw` | boolean: `auto_calibrate_polarity` / validator: `clutch_local`, `gearshift_local`, `positive_direction_powered` / list: `directional_gearshift_local`, `left_control_local`, `right_control_local`, `steering_wheel_local`, `swivel_bearing_local` / number: `tolerance_degrees` |
| `runtime.cannon_assembly` | number: `assembly_tick_delay`, `lever_delay_ticks`, `lever_gap_ticks`, `lever_signal` / list: `levers`, `mount_positions` |
| `runtime.cannon_reloads[]` | text: `fuze`, `id`, `propellant_stack_snbt`, `propellant_type`, `shell_stack_snbt` / number: `fuze_detonation_distance`, `propellant_blocks`, `propellant_power`, `reload_after_ticks` / list: `mount_local` / boolean: `required`, `unload_before_load` |
| `runtime.engine_activations[]` | number: `burn_ticks`, `signal` / list: `offset` |
| `runtime.initial_cannon_loads[]` | text: `fuze`, `id`, `propellant_stack_snbt`, `propellant_type`, `shell_stack_snbt` / number: `fuze_detonation_distance`, `propellant_blocks`, `propellant_power` / list: `mount_local` / boolean: `required`, `unload_before_load` |
| `runtime.proximity_levers[]` | validator: `local` / list: `offset` / boolean: `one_shot`, `owner_only`, `powered_when_near` / number: `range` |
| `runtime.vessel_arrivals[]` | number: `chance`, `first_after_ticks`, `interval_ticks`, `max_count` / object: `definition` / list: `definitions` / text: `id` |
| `runtime.vessel_arrivals[].definition` | list: `actions` / object: `conditions` |
| `runtime.vessel_arrivals[].definition.actions[]` | object: `overrides` / text: `profile`, `type` |
| `runtime.vessel_arrivals[].definition.actions[].overrides` | number: `absolute_y`, `engine_burn_seconds`, `lifetime_ticks`, `minimum_spawn_distance`, `offset_y`, `spawn_distance` / boolean: `avoid_line_of_sight`, `required` / text: `facing`, `id`, `spawn_anchor`, `structure_key` / object: `patrol` |
| `runtime.vessel_arrivals[].definition.actions[].overrides.patrol` | number: `intercept_range`, `max_pursuit_from_patrol`, `patrol_radius`, `point_interval_ticks` / text: `mode` / object: `target` |
| `runtime.vessel_arrivals[].definition.actions[].overrides.patrol.target` | text: `type` / boolean: `use_micro_structure` |
| `runtime.vessel_arrivals[].definition.conditions` | object: `progression` |
| `runtime.vessel_arrivals[].definition.conditions.progression` | text: `curve`, `tier` |
| `runtime.vessel_arrivals[].definitions[]` | list: `actions` / object: `conditions` |
| `runtime.vessel_arrivals[].definitions[].actions[]` | object: `overrides` / text: `profile`, `type` |
| `runtime.vessel_arrivals[].definitions[].actions[].overrides` | boolean: `avoid_line_of_sight` / number: `lifetime_ticks`, `minimum_spawn_distance`, `spawn_distance` / object: `patrol` |
| `runtime.vessel_arrivals[].definitions[].actions[].overrides.patrol` | number: `intercept_range`, `max_pursuit_from_patrol`, `patrol_radius` / object: `target` |
| `runtime.vessel_arrivals[].definitions[].actions[].overrides.patrol.target` | text: `type` / boolean: `use_micro_structure` |
| `runtime.vessel_arrivals[].definitions[].conditions` | object: `progression` |
| `runtime.vessel_arrivals[].definitions[].conditions.progression` | text: `curve`, `tier` |
| `runtime.weapons` | number: `alignment_tolerance_degrees`, `fire_interval_ticks`, `fire_pulse_ticks`, `mob_target_range`, `range`, `reload_delay_ticks` / boolean: `enabled`, `require_clear_path`, `target_encounter_owner`, `target_mobs`, `target_players`, `target_underground_mobs` / list: `fire_controls` / text: `priority`, `reload_mode` |
| `runtime.weapons.fire_controls[]` | list: `aim_local`, `button_local`, `mount_local` / validator: `local`, `offset` |
| `sable_events[]` | list: `actions` / number: `chance`, `repeat_ticks` / text: `id` / boolean: `require_living_crew` / object: `trigger` |
| `sable_events[].trigger` | number: `after_ticks`, `at_or_below_percent`, `minimum_age_ticks`, `range`, `repeat_ticks` / boolean: `horizontal_only` / text: `type` |
| `ship_ai` | object: `air_to_air` |
| `ship_ai` | number: `approach_bearing_degrees`, `broadside_switch_ticks`, `clearance`, `close_distance`, `close_switch_airborne_height`, `close_switch_range`, `come_about_aft_degrees`, `come_about_max_ticks`, `come_about_release_degrees`, `control_distance`, `correction_range`, `depart_ticks` |
| `ship_ai` | number: `distance`, `distance_tolerance`, `emergency_margin`, `ground_broadside_distance`, `hard_recovery_distance`, `height_offset`, `horizontal_deadband`, `movement_margin`, `pass_distance`, `recovery_release_degrees`, `recovery_turn_behind_degrees`, `recovery_turn_degrees` |
| `ship_ai` | number: `recovery_turn_max_ticks`, `resume_forward_at_distance`, `reverse_at_distance`, `sample_step`, `scan_distance`, `standoff_distance`, `surface_distance`, `target_range`, `turn_degrees`, `turn_ticks`, `update_ticks`, `vertical_deadband` |
| `ship_ai` | text: `broadside_side`, `mode`, `recovery_turn_side`, `side` |
| `ship_ai` | boolean: `come_about_enabled`, `come_about_flip_side`, `commit_recovery_turn_side`, `enabled`, `preserve_broadside_recovery`, `preserve_orbit_recovery`, `vertical_priority` |
| `ship_ai` | list: `hull_max_offset`, `hull_min_offset`, `hull_origin_local`, `lift_propellers`, `motion_clutches` |
| `ship_ai` | boolean or null: `match_player_y`, `range_holding_thrust_reversal`, `recovery_circle_enabled`, `recovery_turn_enabled` |
| `ship_ai` | null or number: `recovery_circle_degrees`, `recovery_circle_ticks`, `turn_lead_blocks` |
| `ship_ai.air_to_air` | number: `close_distance`, `close_switch_airborne_height`, `close_switch_range`, `come_about_aft_degrees`, `come_about_max_ticks`, `come_about_release_degrees`, `correction_range`, `distance`, `distance_tolerance`, `hard_recovery_distance`, `height_offset`, `recovery_release_degrees` |
| `ship_ai.air_to_air` | number: `resume_forward_at_distance`, `reverse_at_distance`, `target_range`, `turn_lead_blocks` |
| `ship_ai.air_to_air` | boolean: `come_about_enabled`, `come_about_flip_side`, `commit_recovery_turn_side`, `match_player_y`, `range_holding_thrust_reversal`, `recovery_turn_enabled` |
| `ship_ai.air_to_air` | text: `mode`, `recovery_turn_side`, `side` |
| `ship_ai.motion_clutches[]` | list: `clutch_local`, `engine_local`, `gearshift_lever_local`, `gearshift_local`, `lever_local`, `propellers` / text: `direction` / boolean: `gearshift_powered`, `reverse_engine` |
| `shot_down_cleanup` | number: `destroyed_percent`, `minimum_below_target`, `out_of_view_ticks`, `viewer_range` / boolean: `enabled` |
| `sides[]` | number: `air_search_attempts`, `air_step`, `cannon_button_ticks`, `cannon_fire_interval_seconds`, `cannon_fire_range`, `cannon_fire_vertical_tolerance`, `cannon_player_safety_range`, `cannon_reload_delay_ticks`, `count`, `destroyed_cleanup_percent`, `engine_burn_seconds`, `engine_burn_ticks` |
| `sides[]` | number: `envelope_fill`, `health_scan_budget`, `initial_cannon_load_after_ticks`, `lifetime_ticks`, `maintenance_interval_ticks`, `minimum_cannon_fire_interval_ticks`, `minimum_spawn_distance`, `offset_y`, `recovery_abandon_ticks`, `ship_settle_delay_ticks`, `ship_stage_delay_ticks`, `side_spread_degrees` |
| `sides[]` | number: `spawn_bearing_degrees`, `spawn_distance`, `spawn_hold_settle_ticks`, `spawn_terrain_clearance_blocks`, `stuck_clear_above_ground`, `stuck_climb_blocks`, `throttle_signal` |
| `sides[]` | boolean: `allow_propulsion_reversal`, `attach_child_sublevels`, `avoid_line_of_sight`, `bearing_never_place`, `cannon_broadside_split`, `clutch_when_aligned`, `health_ignore_envelope`, `lootr_compatibility`, `required` |
| `sides[]` | object: `altitude_controller`, `cannon_assembly`, `capture`, `chase_controller`, `crew_glow`, `despawn_effect`, `directional_clutch_drives`, `downed_despawn`, `engine_stress_management`, `faction_behavior`, `force_render`, `hardware_requirements` |
| `sides[]` | object: `overrides`, `ship_ai`, `spawn_clearance` |
| `sides[]` | list: `assembly_origin`, `cannon_reloads`, `container_loot`, `entities`, `initial_cannon_loads`, `redstone_activations`, `sable_events`, `steering_controls` |
| `sides[]` | text: `cannon_alignment_mode`, `combat_target_priority`, `facing`, `faction`, `id`, `placement`, `profile`, `propeller_direction`, `schematic_front`, `ship_name`, `structure_key`, `template` |
| `sides[]` | text: `type` |
| `sides[].altitude_controller` | number: `analog_hysteresis`, `analog_step_seconds`, `buoyancy_feedback_limit`, `follow_signal_step`, `gain`, `hold_release_blocks`, `hover_signal`, `lever_changes_per_update`, `max_signal_step`, `maximum_signal`, `minimum_ground_clearance`, `minimum_signal` |
| `sides[].altitude_controller` | number: `offset`, `path_lookahead_blocks`, `path_lookahead_clearance`, `path_lookahead_half_width`, `path_lookahead_step`, `retarget_deadband`, `stable_capture_ticks`, `stable_velocity_tolerance`, `tolerance` |
| `sides[].altitude_controller` | boolean: `descent_arrest_enabled`, `distribute_lever_signals`, `ground_clearance_enabled`, `initialize_at_spawn`, `path_lookahead_enabled`, `spawn_at_controller_target` |
| `sides[].altitude_controller` | text: `mode`, `signal_strategy` |
| `sides[].cannon_assembly` | text: `component` / number: `minimum_assembled_mounts`, `signal` / boolean: `power_mounts_directly`, `preserve_mount_rotation`, `required` |
| `sides[].cannon_reloads[]` | text: `fuze`, `id`, `propellant_stack_snbt`, `propellant_type`, `shell_stack_snbt` / number: `fuze_detonation_distance`, `propellant_blocks`, `propellant_power`, `reload_after_ticks` / list: `mount_local` / boolean: `required`, `unload_before_load` |
| `sides[].capture` | list: `actions` / number: `max_destroyed_percent` |
| `sides[].capture.actions[]` | text: `at`, `message`, `sound`, `type` / number: `volume` |
| `sides[].chase_controller` | number: `idle_signal`, `max_angle`, `maximum_signal`, `resume_distance`, `stop_distance`, `target_range`, `throttle_signal`, `update_ticks` |
| `sides[].container_loot[]` | list: `blocks`, `loot_tables`, `position`, `positions`, `slots` / boolean: `combine`, `lootr`, `replace_existing` / text: `loot_table` |
| `sides[].container_loot[].slots[]` | number: `count`, `slot` / text: `item` |
| `sides[].crew_glow` | number: `at_or_below_percent` |
| `sides[].despawn_effect` | text: `type` |
| `sides[].directional_clutch_drives` | list: `drives` / number: `target_range`, `update_ticks` |
| `sides[].directional_clutch_drives.drives[]` | list: `clutch_local`, `local_direction` / text: `id` |
| `sides[].downed_despawn` | boolean: `enabled` / number: `maximum_angular_speed`, `maximum_speed`, `minimum_missing_blocks`, `stationary_ticks`, `unseen_ticks` |
| `sides[].engine_stress_management` | number: `maximum_signal` / text: `mode` |
| `sides[].entities[]` | number: `aggro_range`, `count`, `deaggro_range`, `nearby_player_range`, `target_range` / boolean: `aggro_through_walls`, `crew`, `fill_all`, `friendly_fire`, `persistent`, `seat` / text: `banner`, `entity`, `target` / object: `equipment` / list: `reinforcements`, `seat_predicates`, `tags` |
| `sides[].entities[].equipment` | text: `helmet`, `mainhand` |
| `sides[].entities[].seat_predicates[]` | text: `block` |
| `sides[].faction_behavior` | text: `hostile_response` |
| `sides[].force_render` | number: `after_operational_ticks`, `release_distance`, `render_distance`, `tracking_distance` / text: `audience` / boolean: `enabled` |
| `sides[].hardware_requirements` | number: `minimum_analog_controls`, `minimum_balloon_components`, `minimum_buttons`, `minimum_containers`, `minimum_engines_or_propellers`, `minimum_seats`, `minimum_steering_controls` |
| `sides[].initial_cannon_loads[]` | text: `fuze`, `id`, `propellant_stack_snbt`, `propellant_type`, `shell_stack_snbt` / number: `fuze_detonation_distance`, `propellant_blocks`, `propellant_power` / list: `mount_local` / boolean: `required` |
| `sides[].overrides` | object: `chase_controller`, `crew_glow`, `patrol`, `ship_ai`, `spawn_clearance` / text: `combat_target_priority`, `faction`, `id`, `structure_key` / list: `destroyed_rewards`, `entities` / number: `maximum_clear_player_distance`, `minimum_spawn_distance`, `player_engage_range`, `spawn_anchor_separation`, `spawn_distance` / boolean: `required` |
| `sides[].overrides.chase_controller` | number: `target_range` |
| `sides[].overrides.crew_glow` | number: `at_or_below_percent` |
| `sides[].overrides.destroyed_rewards[]` | number: `amount` / text: `faction`, `type` |
| `sides[].overrides.entities[]` | number: `count` / boolean: `crew`, `fill_all`, `persistent` / text: `entity` |
| `sides[].overrides.patrol` | object: `target` |
| `sides[].overrides.patrol.target` | text: `type` / boolean: `use_spawn_anchor` |
| `sides[].overrides.ship_ai` | number: `target_range` |
| `sides[].overrides.spawn_clearance` | object: `dimension_minimum_origin_y` |
| `sides[].overrides.spawn_clearance.dimension_minimum_origin_y` | number: `<key>` |
| `sides[].redstone_activations[]` | number: `after_ticks`, `button_ticks`, `direction_tolerance_degrees`, `range`, `repeat_ticks`, `signal`, `vertical_tolerance` / object: `cannon_alignment` / list: `fire_mount_local`, `positions` / text: `id`, `player_direction`, `state` / boolean: `require_living_crew` |
| `sides[].redstone_activations[].cannon_alignment` | list: `aim_local`, `mount_local` / number: `arc_degrees` |
| `sides[].sable_events[]` | list: `actions` / text: `id` / boolean: `require_living_crew` / object: `trigger` |
| `sides[].sable_events[].actions[]` | text: `at`, `sound`, `type` / number: `pitch`, `volume` |
| `sides[].sable_events[].trigger` | number: `after_ticks` / text: `type` |
| `sides[].ship_ai` | text: `broadside_side`, `mode`, `recovery_turn_side`, `side` |
| `sides[].ship_ai` | number: `broadside_switch_ticks`, `control_distance`, `correction_range`, `distance`, `distance_tolerance`, `height_offset`, `recovery_release_degrees`, `recovery_turn_degrees`, `recovery_turn_max_ticks`, `target_range`, `turn_lead_blocks`, `update_ticks` |
| `sides[].ship_ai` | boolean: `commit_recovery_turn_side`, `match_player_y`, `preserve_broadside_recovery`, `range_holding_thrust_reversal`, `recovery_turn_enabled` |
| `sides[].spawn_clearance` | object: `dimension_maximum_origin_y`, `dimension_minimum_origin_y` / boolean: `fit_world_height` / number: `maximum_origin_y` |
| `sides[].spawn_clearance.dimension_maximum_origin_y` | number: `<key>` |
| `sides[].spawn_clearance.dimension_minimum_origin_y` | number: `<key>` |
| `sides[].steering_controls[]` | text: `behind_direction`, `block`, `mode` / boolean: `horizontal_only`, `invert`, `require_living_crew` / number: `max_angle`, `range`, `update_ticks` |
| `source_origin` | text: `entity_tag`, `mode` |
| `spawn_clearance` | number: `activity_chunk_radius`, `activity_inhabited_ticks`, `chunk_load_budget`, `horizontal`, `horizontal_search`, `maximum_origin_y`, `search_step`, `vertical`, `vertical_search` / boolean: `avoid_player_activity`, `fit_world_height` / object: `dimension_maximum_origin_y`, `dimension_minimum_origin_y` |
| `spawn_clearance.dimension_maximum_origin_y` | number: `<key>` |
| `spawn_clearance.dimension_minimum_origin_y` | number: `<key>` |
| `spawns[]` | number: `aggro_range`, `count`, `deaggro_range`, `nearby_player_range` / boolean: `aggro_through_walls`, `avoid_line_of_sight`, `friendly_fire`, `persistent` / text: `banner`, `entity`, `faction`, `placement`, `target` / list: `effects`, `tags` / object: `equipment`, `spawn_animation` |
| `spawns[].equipment` | list: `armor_sets` / text: `mainhand` |
| `spawns[].equipment.armor_sets[]` | boolean: `guaranteed` / text: `set` |
| `spawns[].spawn_animation` | number: `count`, `height`, `horizontal_distance`, `upward_velocity`, `velocity` / text: `type` / boolean: `visual_only` |
| `split_fragment_cleanup` | number: `despawn_after_ticks`, `max_blocks` / text: `mode` |
| `steering_controls[]` | text: `behind_direction`, `block`, `mode` / boolean: `center_before_reverse`, `horizontal_only`, `invert`, `require_living_crew` / number: `damping_ticks`, `deadband_degrees`, `max_angle`, `max_angle_step`, `range`, `recovery_max_angle`, `update_ticks` / list: `left_control_local`, `positions`, `right_control_local` |
| `suppress_when_player_within` | number: `horizontal`, `vertical` |
| `surface_flatness` | number: `max_height_delta`, `max_neighbor_delta`, `maximum_fluid_fraction`, `minimum_support_fraction`, `padding`, `sample_step` |
| `tactics` | number: `defend_radius`, `disengage_hull_percent`, `flank_degrees`, `formation_weight`, `maximum_distance`, `minimum_distance`, `preferred_distance` / boolean: `fire_while_retreating` / validator: `reengage_hull_percent`, `retreat_distance`, `retreat_ticks` |
| `telegraph` | number: `circle_points`, `duration_ticks`, `interval_ticks`, `radius` / boolean: `follow_target` / text: `particle`, `shape` |
| `throttle_signal_by_y[]` | number: `max_y`, `min_y`, `signal` |
| `unseen_righting` | boolean: `enabled` / number: `max_lift_blocks`, `unseen_ticks`, `upright_tilt_degrees` |
| `waves[]` | number: `after_ticks`, `attempts`, `min_radius`, `radius` / list: `spawns` |
| `waves[].spawns[]` | number: `aggro_range`, `count`, `deaggro_range`, `nearby_player_range` / boolean: `aggro_through_walls`, `avoid_line_of_sight`, `friendly_fire`, `persistent` / text: `entity`, `placement`, `target` / list: `passengers`, `tags` |
| `waves[].spawns[].passengers[]` | number: `count` / text: `entity`, `target` / object: `equipment` / boolean: `friendly_fire`, `persistent` / list: `tags` |
| `waves[].spawns[].passengers[].equipment` | text: `mainhand` |
| `wreck_cleanup` | number: `delay_ticks`, `maximum_up_y`, `submerged_depth` / boolean: `enabled` |

#### Flight and navigation fallback values

These optional fields are read inside the named controller. The fallback is the literal used by the control routine when it evaluates that option. Operating mode, hardware and parent settings determine which routine runs. Signals use redstone strength, angles use degrees and tick intervals use game ticks.

| Controller | Field | Value form | Fallback |
|---|---|---|---|
| `altitude_controller` | `blend_ground_height` | boolean | `false` |
| `altitude_controller` | `buoyancy_feedback_limit` | number | `1` |
| `altitude_controller` | `calibration_settle_ticks` | integer | `60` |
| `altitude_controller` | `close_offset` | number | `0` |
| `altitude_controller` | `fallback_target_y` | number | `180` |
| `altitude_controller` | `force_descent_signal` | boolean | `false` |
| `altitude_controller` | `ground_clearance_enabled` | boolean | `true` |
| `altitude_controller` | `ground_clearance_is_minimum` | boolean | `false` |
| `altitude_controller` | `ground_hull_bottom_offset` | number | `0` |
| `altitude_controller` | `ground_hull_clearance` | number | `0` |
| `altitude_controller` | `ground_overshoot_hard` | number | `15` |
| `altitude_controller` | `ground_overshoot_soft` | number | `6` |
| `altitude_controller` | `hard_descent_enabled` | boolean | `false` |
| `altitude_controller` | `hold_release_blocks` | number | `6` |
| `altitude_controller` | `hover_floor_clearance` | number | `20` |
| `altitude_controller` | `hover_signal_range` | integer | `1` |
| `altitude_controller` | `hull_bottom_offset` | number | `0` |
| `altitude_controller` | `initialize_at_spawn` | boolean | `true` |
| `altitude_controller` | `integral_gain` | number | `.001` |
| `altitude_controller` | `integral_limit` | number | `3` |
| `altitude_controller` | `lever_update_ticks` | integer | `80` |
| `altitude_controller` | `match_cannon_height` | boolean | `false` |
| `altitude_controller` | `maximum_signal` | integer | `15` |
| `altitude_controller` | `minimum_ground_clearance` | number | `20` |
| `altitude_controller` | `minimum_signal` | integer | `0` |
| `altitude_controller` | `mode` | text | `"player_offset"` |
| `altitude_controller` | `neutral_capture_height_tolerance` | number | `2` |
| `altitude_controller` | `neutral_capture_ticks` | integer | `80` |
| `altitude_controller` | `neutral_capture_velocity` | number | `.02` |
| `altitude_controller` | `path_collision_volume_enabled` | boolean | `false` |
| `altitude_controller` | `path_lookahead_blocks` | number | `64` |
| `altitude_controller` | `path_lookahead_clearance` | number | `16` |
| `altitude_controller` | `path_lookahead_enabled` | boolean | `false` |
| `altitude_controller` | `path_lookahead_half_width` | number | `6` |
| `altitude_controller` | `path_lookahead_step` | number | `8` |
| `altitude_controller` | `path_surface_preview` | boolean | `false` |
| `altitude_controller` | `physical_height_signal_map` | boolean | `false` |
| `altitude_controller` | `player_above_deadband` | number | `0` |
| `altitude_controller` | `player_follow_deadband` | number | `20` |
| `altitude_controller` | `player_follow_signal_step` | integer | `1` |
| `altitude_controller` | `respect_world_height` | boolean | `false` |
| `altitude_controller` | `signal_strategy` | text | `"responsive"` |
| `altitude_controller` | `spawn_approach_height` | number | `0` |
| `altitude_controller` | `spawn_at_controller_target` | boolean | `true` |
| `altitude_controller` | `spawn_target_mode` | text | `"preserve"` |
| `ship_ai` | `air_prediction_ticks` | number | `40` |
| `ship_ai` | `approach_bearing_degrees` | number | `0` |
| `ship_ai` | `avoid_water` | boolean | `true` |
| `ship_ai` | `broadside_switch_ticks` | integer | `40` |
| `ship_ai` | `clearance` | integer | `6` |
| `ship_ai` | `close_switch_airborne_height` | number | `0` |
| `ship_ai` | `close_switch_range` | number | `0` |
| `ship_ai` | `combat_ship_separation` | number | `48.0` |
| `ship_ai` | `come_about_aft_degrees` | number | `115` |
| `ship_ai` | `come_about_enabled` | boolean | `false` |
| `ship_ai` | `come_about_flip_side` | boolean | `true` |
| `ship_ai` | `come_about_max_ticks` | integer | `300` |
| `ship_ai` | `come_about_release_degrees` | number | `30` |
| `ship_ai` | `commit_recovery_turn_side` | boolean | `true` |
| `ship_ai` | `convoy_ship_separation` | number | `40.0` |
| `ship_ai` | `depart_ticks` | integer | `60` |
| `ship_ai` | `emergency_margin` | integer | `2` |
| `ship_ai` | `enabled` | boolean | `true` |
| `ship_ai` | `engine_reverse_interval_ticks` | integer | `20` |
| `ship_ai` | `fleet_spacing` | number | `18` |
| `ship_ai` | `horizontal_deadband` | number | `3` |
| `ship_ai` | `match_player_y` | boolean | `false` |
| `ship_ai` | `minimum_ship_separation` | number | `96.0` |
| `ship_ai` | `mode` | text | `"chase"` |
| `ship_ai` | `movement_margin` | integer | `2` |
| `ship_ai` | `overshoot_distance_increase` | number | `24` |
| `ship_ai` | `overshoot_increasing_ticks` | integer | `40` |
| `ship_ai` | `overshoot_release_degrees` | number | `30` |
| `ship_ai` | `pass_distance` | number | `16` |
| `ship_ai` | `preserve_broadside_recovery` | boolean | `false` |
| `ship_ai` | `preserve_orbit_recovery` | boolean | `false` |
| `ship_ai` | `range_holding_thrust_reversal` | boolean | `false` |
| `ship_ai` | `recovery_circle_degrees` | number | `90` |
| `ship_ai` | `recovery_circle_enabled` | boolean | `false` |
| `ship_ai` | `recovery_circle_ticks` | integer | `160` |
| `ship_ai` | `recovery_release_degrees` | number | `30` |
| `ship_ai` | `recovery_turn_behind_degrees` | number | `90` |
| `ship_ai` | `recovery_turn_degrees` | number | `90` |
| `ship_ai` | `recovery_turn_enabled` | boolean | `false` |
| `ship_ai` | `recovery_turn_side` | text | `"auto"` |
| `ship_ai` | `require_living_crew` | boolean | `true` |
| `ship_ai` | `sample_step` | integer | `3` |
| `ship_ai` | `separation_vertical_tolerance` | number | `16` |
| `ship_ai` | `side` | text | `"nearest"` |
| `ship_ai` | `standoff_distance` | number | `0` |
| `ship_ai` | `turn_degrees` | number | `150` |
| `ship_ai` | `turn_ticks` | integer | `100` |
| `ship_ai` | `vertical_deadband` | number | `2` |
| `ship_ai` | `vertical_priority` | boolean | `false` |

### Equipment set

Artillery Detachment shows how to put weighted choices in equipment slots. Each slot selects its own item.

| Object | Fields by value form |
|---|---|
| **Root** | text: `name` / object: `slots` |
| `slots` | list or object or text: `chest`, `head`, `mainhand` / object: `legs`, `offhand` |
| `slots.chest` | number: `drop_chance` / object: `enchantments` / boolean: `glint` / text: `item` |
| `slots.chest[]` | number: `drop_chance`, `weight` / text: `dye`, `item` |
| `slots.head` | number: `drop_chance` / text: `dye`, `item`, `name` / object: `enchantments` / boolean: `glint` |
| `slots.head[]` | number: `drop_chance`, `weight` / text: `item` |
| `slots.legs` | number: `drop_chance` / object: `enchantments` / boolean: `glint` / text: `item` |
| `slots.mainhand` | number: `drop_chance` / object: `enchantments` / boolean: `glint`, `show_held_on_evoker` / text: `item`, `name` |
| `slots.mainhand[]` | number: `drop_chance`, `weight` / object: `enchantments` / boolean: `glint` / text: `item`, `name` |
| `slots.offhand` | number: `count`, `drop_chance`, `rocket_color` / text: `item` |

### Faction

Use the villager faction to set relations, standing thresholds, and rewards for activities. A standing event can announce a threshold crossing.

| Object | Fields by value form |
|---|---|
| **Root** | validator: `allied_with`, `equipment` / boolean: `default_for_unassigned_actions`, `hostile_to_all` / list: `hostile_to` / object: `mob_types`, `standing` |
| `mob_types` | number: `<key>` |
| `standing` | object: `activities`, `decay`, `on_kill_mob` / number: `ally_at`, `hostile_below`, `maximum`, `minimum`, `on_break_own_ship_block`, `on_defeat_enemy_ambush`, `on_kill_enemy_claimed_mob`, `on_kill_enemy_mob`, `on_kill_own_mob`, `reputation_scale`, `start` / list: `events` / text: `mode` |
| `standing.activities` | boolean: `gold_armor_pauses_loss` / number: `gold_armor_ticks` / validator: `restitution` / object: `rewards` |
| `standing.activities.restitution` | validator: `count`, `loss`, `payment`, `refund`, `timeout_ticks` |
| `standing.activities.rewards` | number: `cure_zombie_villager`, `nether_gold_armor`, `piglin_barter`, `start_village_raid`, `win_village_raid`, `witnessed_piglin_mining` |
| `standing.decay` | number: `amount`, `interval_ticks`, `target` / boolean: `enabled` |
| `standing.events[]` | validator: `ambush`, `unlock` / text: `on` / number: `threshold`, `weight` / object: `toast` |
| `standing.events[].toast` | text: `description`, `icon`, `title` |
| `standing.on_kill_mob` | number: `<key>` |

### Faction gifts

The villager resource controls when a gift check runs, the chance at each standing level, and which gifts can be chosen.

| Object | Fields by value form |
|---|---|
| **Root** | number: `boss_ship_bonus`, `chance_at_ally`, `chance_at_max`, `check_every_ticks`, `cooldown_ticks`, `elite_ship_bonus`, `enemy_ship_bonus`, `maximum_bonus` / boolean: `enabled` / list: `gifts` |
| `gifts[]` | text: `ambush` / list: `dimensions` / number: `maximum_player_y`, `maximum_reputation`, `minimum_reputation`, `weight` |

### Faction support

The villager support resource sets the time before help arrives, the cooldown before another call, and the eligible support encounters.

| Object | Fields by value form |
|---|---|
| **Root** | number: `arrival_max_ticks`, `arrival_min_ticks`, `cooldown_at_ally_ticks`, `cooldown_at_max_ticks`, `gift_regain_margin`, `sound_pitch`, `sound_volume` / text: `call_sound`, `gift_ambush` / object: `support_ambushes`, `support_choices` |
| `support_ambushes` | text: `<key>` |
| `support_choices` | list: `<key>` |
| `support_choices.<key>[]` | text: `ambush` / number: `quality`, `weight` |
| `support_choices[]` | validator: `ambush`, `quality`, `weight` |

### Natural encounter curve

Use the bundled default curve to vary timing, pools or tier weights across encounters. The normal and major windows govern separate budgets. Tier and pool names are map keys chosen by the author.

| Object | Fields by value form |
|---|---|
| **Root** | boolean: `avoid_immediate_repeats`, `primary` / object: `encounter_tiers`, `follow_up`, `major_window`, `normal_window`, `pools`, `progression_kill_thresholds`, `progression_stage_advancements`, `progression_tier_weights` / number: `nearby_player_share_radius`, `retry_interval_ticks`, `ship_defeat_kill_credit` / list: `reputation_bands` |
| `encounter_tiers` | object: `<key>` |
| `encounter_tiers.<key>` | number: `maximum_reputation`, `minimum_stage`, `minimum_victories` / text: `victory_track` |
| `follow_up` | number: `chance_percent`, `max_chain_depth` / object: `delay_window` |
| `follow_up.delay_window` | text: `distribution` / number: `max_ticks`, `min_ticks`, `mode_ticks` |
| `major_window` | text: `distribution` / number: `max_ticks`, `min_ticks`, `mode_ticks` |
| `normal_window` | text: `distribution` / number: `max_ticks`, `min_ticks`, `mode_ticks` |
| `pools` | object: `piglin`, `pillager`, `undead`, `villager`, `world_battle` |
| `pools.piglin` | text: `faction`, `reputation_score_mode` / number: `weight` |
| `pools.pillager` | text: `faction`, `reputation_score_mode` / number: `weight` |
| `pools.undead` | text: `faction`, `reputation_score_mode` / number: `weight` |
| `pools.villager` | text: `faction`, `reputation_score_mode` / number: `weight` |
| `pools.world_battle` | text: `reputation_score_mode` / number: `weight` |
| `progression_kill_thresholds` | number: `<key>` |
| `progression_stage_advancements` | list: `<key>` |
| `progression_tier_weights` | object: `<key>` |
| `progression_tier_weights.<key>` | number: `elite`, `heavy`, `medium`, `small`, `tiny` |
| `reputation_bands[]` | number: `max`, `min`, `selection_multiplier` / object: `tier_multipliers` |
| `reputation_bands[].tier_multipliers` | number: `elite`, `heavy`, `medium`, `small`, `tiny` |

### Invasion

The naval invasion separates the defense objective, wave timing, encounter pool, budget and rewards. For example, pool[].cost spends wave budget while pool[].weight changes selection within the eligible pool.

| Object | Fields by value form |
|---|---|
| **Root** | object: `boss_bar`, `budget`, `encounter_overrides`, `entity_overrides`, `objective`, `quality_scaling`, `rewards`, `waves` / text: `name` / list: `pool` |
| `boss_bar` | text: `color`, `overlay`, `progress`, `title` / validator: `phase_messages`, `phase_names`, `show_ships`, `show_timer` / boolean: `show_percent`, `show_phase`, `show_wave` |
| `boss_bar.phase_messages` | validator: `fight`, `intermission`, `objective`, `spawn`, `telegraph` |
| `boss_bar.phase_names` | validator: `fight`, `intermission`, `objective`, `spawn`, `telegraph` |
| `budget` | number: `base`, `carry_fraction`, `cost_bias_exponent`, `per_participant` / object: `scaling` |
| `budget.scaling` | number: `maximum`, `per_unit` / validator: `minimum`, `start_at` / text: `mode` |
| `encounter_overrides` | null: `<key>` |
| `entity_overrides` | boolean: `<key>` |
| `objective` | object: `action_overrides` / text: `ambush`, `faction`, `type` / validator: `count_deaths_outside_radius`, `count_non_combat_deaths`, `duration_ticks`, `quota_per_wave` / number: `fail_at_damage_percent`, `lives_per_player`, `respawn_grace_ticks` / boolean: `lives_are_fatal` |
| `objective.action_overrides` | null: `absolute_y`, `altitude_controller`, `capture`, `cleanup_at_health_percent`, `lifetime_seconds`, `sable_events`, `shared_boss_id`, `source_structure`, `source_structures`, `spawn_anchor`, `spawn_height_reference` / boolean: `airworthiness_cleanup_enabled`, `avoid_line_of_sight`, `show_boss_bar` / number: `destroyed_cleanup_percent`, `lifetime_ticks` / object: `ship_ai` |
| `objective.action_overrides.ship_ai` | boolean: `enabled` |
| `pool[]` | text: `ambush`, `battle_group`, `biome_tag`, `role` / number: `cost`, `max_per_wave`, `min_wave`, `weight` / object: `hostile_reputation`, `spawn_distances` |
| `pool[].hostile_reputation` | text: `faction` / number: `maximum` |
| `pool[].spawn_distances` | boolean: `avoid_line_of_sight` / number: `height`, `maximum`, `minimum`, `preferred` |
| `quality_scaling` | number: `extra_damage_per_wave`, `extra_health_per_wave`, `maximum_extra_damage`, `maximum_extra_health`, `starts_at_wave` |
| `reward item` | validator: `count`, `delivery`, `item` |
| `rewards` | list: `per_wave`, `victory` |
| `rewards.per_wave[]` | number: `amount`, `count` / text: `delivery`, `faction`, `item`, `type` |
| `rewards.per_wave[] / rewards.victory[]` | validator: `ambush`, `amount`, `amplifier`, `delivery`, `duration_ticks`, `faction`, `items`, `type` |
| `rewards.victory[]` | text: `ambush`, `delivery`, `faction`, `item`, `type` / number: `amount`, `amplifier`, `count`, `duration_ticks` / list: `items` |
| `rewards.victory[].items[]` | number: `count` / text: `item` |
| `waves` | object: `action_overrides` |
| `waves` | boolean: `allow_faction_battles`, `change_faction_between_waves`, `enemy_health_bar`, `ready_check`, `ship_actions_only`, `single_faction` |
| `waves` | number: `boss_every`, `count`, `downed_cleanup_ticks`, `empty_wave_retries`, `intermission_ticks`, `maximum_standoff_distance`, `opening_encounters`, `radius`, `repeat_weight`, `retry_ticks`, `spawn_distance`, `spawn_offset_y` |
| `waves` | number: `stranded_car_cleanup_ticks`, `telegraph_ticks`, `wave_timeout_ticks`, `waves_per_extra_encounter` |
| `waves` | validator: `highlight_remaining_percent`, `show_enemy_direction` |
| `waves.action_overrides` | null: `absolute_y`, `capture`, `cleanup_at_health_percent`, `container_loot`, `lifetime_seconds`, `sable_events`, `shared_boss_id`, `source_structure`, `source_structures`, `spawn_anchor`, `spawn_height_reference` |
| `waves.action_overrides` | boolean: `airworthiness_cleanup_enabled`, `show_boss_bar` |
| `waves.action_overrides` | text: `combat_target_priority` |
| `waves.action_overrides` | number: `destroyed_cleanup_percent`, `engine_burn_ticks`, `lifetime_ticks`, `maximum_clear_player_distance`, `minimum_clear_player_distance`, `minimum_spawn_distance` |
| `waves.action_overrides` | object: `faction_behavior` |
| `waves.action_overrides.faction_behavior` | text: `hostile_response` |

#### Invasion tuning ranges

Use these limits when editing a field in its matching objective, wave, pool or reward type. The fallback applies when the reader evaluates that setting. A bundled invasion can supply a different value.

| Field | Fallback | Allowed range |
|---|---:|---|
| `objective.fail_at_damage_percent` | 80 | 1 to 100 |
| `objective.lives_per_player` | 3 | 1 to 100 |
| `objective.respawn_grace_ticks` | 200 | 0 to 1200 |
| `objective.duration_ticks` | 24000 | 200 to 1728000 |
| `objective.quota_per_wave` | 20 | 1 to 4096 |
| `waves.repeat_weight` | 1 | .01 to 1 |
| `waves.opening_encounters` | 64 | 1 to 64 |
| `waves.waves_per_extra_encounter` | 1 | 1 to 10000 |
| `waves.highlight_remaining_percent` | 30 | 0 to 100 |
| `waves.maximum_standoff_distance` | 0 | 0 to 1024 |
| `waves.downed_cleanup_ticks` | 600 | 0 to 72000 |
| `waves.stranded_car_cleanup_ticks` | 600 | 0 to 72000 |
| `waves.count` | 15 | 0 to 10000 |
| `waves.radius` | 192 | 32 to 512 |
| `waves.telegraph_ticks` | 100 | 20 to 12000 |
| `waves.intermission_ticks` | 600 | 20 to 12000 |
| `waves.wave_timeout_ticks` | 12000 | 1200 to 72000 |
| `waves.retry_ticks` | 100 | 20 to 1200 |
| `waves.empty_wave_retries` | 6 | 1 to 30 |
| `waves.boss_every` | 5 | 0 to 100 |
| `waves.spawn_distance` | 100 | 24 to 2048 |
| `waves.spawn_offset_y` | 24 | 0 to 96 |
| `budget.base` | 40 | 1 to 4000 |
| `budget.per_participant` | .6 | 0 to 4 |
| `budget.carry_fraction` | .5 | 0 to 1 |
| `budget.cost_bias_exponent` | 1.5 | 0 to 4 |
| `budget.scaling.per_unit` | 10 | 0 to 4000 |
| `budget.scaling.start_at` | 1 | 0 to 10000 |
| `budget.scaling.minimum` | 0 | 0 to 4000 |
| `quality_scaling.starts_at_wave` | 10 | 1 to 10000 |
| `quality_scaling.extra_health_per_wave` | 4 | 0 to 2048 |
| `quality_scaling.extra_damage_per_wave` | .5 | 0 to 1024 |
| `quality_scaling.maximum_extra_health` | 400 | 0 to 2048 |
| `quality_scaling.maximum_extra_damage` | 30 | 0 to 1024 |
| `pool[].spawn_distances.height` | 24 | 0 to 96 |
| `pool[].spawn_distances.minimum` | 160 | 24 to 2048 |
| `pool[].hostile_reputation.maximum` | -5000 | -1000000 to 1000000 |
| `pool[].cost` | 40 | 1 to 4000 |
| `pool[].weight` | 1 | .001 to 10000 |
| `pool[].min_wave` | 1 | 1 to 10000 |
| `pool[].max_per_wave` | 2 | 1 to 64 |
| `reward.duration_ticks` | 24000 | 20 to 1728000 |
| `reward.amplifier` | 0 | 0 to 4 |
| `reward.amount` | 0 | -10000 to 10000 |
| `reward item.count` | 1 | 1 to 64 |

### Rendering defaults

Choose the vessel class first. Change its tracking and release distances together so a vessel stays visible at the intended range.

| Object | Fields by value form |
|---|---|
| **Root** | object: `sable_boat`, `sable_car`, `sable_formation`, `sable_structure` |
| `sable_boat` | number: `after_operational_ticks`, `release_distance`, `render_distance`, `tracking_distance` / text: `audience` / boolean: `enabled` |
| `sable_car` | number: `after_operational_ticks`, `release_distance`, `render_distance`, `tracking_distance` / text: `audience` / boolean: `enabled` |
| `sable_formation` | number: `after_operational_ticks`, `release_distance`, `render_distance`, `tracking_distance` / text: `audience` / boolean: `enabled` |
| `sable_structure` | number: `after_operational_ticks`, `release_distance`, `render_distance`, `tracking_distance` / text: `audience` / boolean: `enabled` |

### Final validation checklist

1. Parse JSON; verify exact IDs, paths, namespaces and required mods.
2. Reload and review rejected/hidden resource messages.
3. Run `/ambush admin check <id>` and test a fresh spawn. Existing vessels may retain serialized settings.
4. Inspect native hardware, inventory, crew and coordinate frames.
5. Verify physical travel, barrel direction, actual projectiles, repeat fire and cleanup as applicable. Test both viable and unsafe conditions.
6. Check reload/restart and multiplayer ownership.

This document describes source support and bundled data. It does not certify every configuration in gameplay.
AI was used to review documentation (not to fully generate). It's pretty hard for me to write in a professional manner, type clearly, and to not forget to put anything. Please reach out to me in the discord if you have any data pack questions!
