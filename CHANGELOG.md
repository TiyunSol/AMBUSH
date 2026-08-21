# AMBUSH 1.1.4

## New content

- Added armored pillager balloon encounters, including surround variants.
- Added armored pillager ship encounters and a small armored ship variant.
- Added the Iron Tide boss encounter and Iron Raider ship encounter.
- Added Pale Leviathan boss content.
- Added Storm Breaker boss encounters, including a high-altitude variant.
- Added pillager fishing-boat content.
- Added dedicated pillager autocannon, mortar, and tiny-cannon raid encounters.
- Added Nether piglin auto-aim balloon and platform encounters.
- Added new captured and standard boss-reward balloon encounters.
- Added new ship structures for armored balloons, armored ships, cast-iron airships, Nether platforms, fishing boats, and deep-ocean boats.
- Added pillager mansion and pillager ship supply loot tables.
- Added expanded autocannon hopper ammunition, including tracer, flak, impact-fuze, and proximity-fuze variants.
- Added cannon-polarity state handling.
- Added rope-control support.
- Added Sable split/rebind handling.
- Added vanilla raid-trigger support.

## FTB Quests integration

- Added quest-contract support for encounter definitions.
- Expanded runtime handling for encounter-linked quest progress and follow-up state.
- Added support for new boss-reward and quest-oriented encounter definitions.

## Improvements

- Improved ship steering, chase control, recovery turns, come-about behavior, and propulsion fallback.
- Improved altitude control with analog stepping, altitude hold timing, lever-signal distribution, ground-clearance enforcement, and descent arrest.
- Improved Sable ship runtime integration with new aiming, hover, clearance, mechanical-polarity, and Nether-clearance handling.
- Improved ship split/rebind handling and safety around split-related state.
- Improved cannon control, cannon alignment, cannon polarity, and per-cannon firing behavior.
- Improved autocannon ammunition handling with nested projectile components and fuze variants.
- Improved ship assembly and hardware validation through expanded encounter fields.
- Improved ship cleanup, ship records, operation journaling, and runtime finalization.
- Improved crew look control, mob-goal access, crew retention, targeting, and alliance behavior.
- Improved encounter definitions with explicit health-envelope handling.
- Improved potato-car stuck detection and escape behavior.
- Improved small-skiff approach behavior with propulsion fallback controls.
- Improved Lootr compatibility for the twin-cannon skelly skiff.
- Improved steering damping and turn-lead behavior for the twin-cannon skelly skiff.
- Improved compatibility with Create, Create Big Cannons, Sable, Simulated, Aeronautics, and FTB Quests.
- Added a split-safety heat-map mixin and a mob-goal-selector accessor while retaining optional, non-failing mixin behavior.

## Removed or retired content

- Removed the deprecated balloon pillager mortar encounter.
- Removed deprecated high-altitude phantom-rider content.
- Removed deprecated drowned hunter, ruined escort, deadfall, and drowned-wanderer encounters.
- Removed deprecated sky bone-rider and sky phantom-scout encounters.
- Removed the deprecated 16-cannon pillager broadside encounter.
- Removed the deprecated armored pillager boss-barge encounter.
- Removed the deprecated 120-skellies boss-barge encounter.
- Removed the deprecated Restless Skies altitude-unlock encounter.

## Technical release notes

- Updated the declared AMBUSH version from `1.1.3` to `1.1.4`.
- Added 14 compiled Java classes.
- Added 2 mixin entries.
- Added 16 encounter-definition JSON files.
- Added 10 NBT structures.
- Added 2 loot tables.
- Changed 49 existing compiled classes, 34 existing JSON/configuration files, and 2 existing NBT structures.
- Removed 12 encounter-definition JSON files.

The detailed archive evidence and file-by-file comparison are included below.

---

# AMBUSH 1.1.4 — Detailed JAR-Diff Appendix

The 1.1.4 hash matches the currently deployed AMBUSH Testing and Create Complex - Steamage copies.

## 1. Release summary

AMBUSH 1.1.4 is a substantial feature and runtime expansion, not a maintenance-only version bump. Compared with 1.1.3, it adds new Sable ship-runtime control systems, cannon-polarity handling, split/rebind support, rope control, quest-contract support, vanilla raid triggering, new safety mixins, new encounter definitions, new ship structures, and new loot tables.

It also removes twelve older encounter-definition JSON files and substantially migrates many existing encounters to newer runtime-control schemas.

## 3. New Java classes

The following compiled classes are new in 1.1.4. No Java class files were removed.

### Runtime and ship-control classes

- `com/createcomplex/ambush/CannonPolarityStore.class`
  - New persistent/runtime state holder associated with cannon polarity behavior.
- `com/createcomplex/ambush/RopeController.class`
  - New rope-control implementation. DOES NOT WORK YET
- `com/createcomplex/ambush/SableSplitRebind.class`
  - New Sable split/rebind handling.
- `com/createcomplex/ambush/SableSplitRebind$Claim.class`
  - New claim/state helper for split/rebind processing.
- `com/createcomplex/ambush/SableShipRuntimeAdapter$AimAngles.class`
  - New aiming-angle helper.
- `com/createcomplex/ambush/SableShipRuntimeAdapter$ComeAbout.class`
  - New come-about/recovery-turn state helper.
- `com/createcomplex/ambush/SableShipRuntimeAdapter$HoverClearance.class`
  - New hover/ground-clearance helper.
- `com/createcomplex/ambush/SableShipRuntimeAdapter$HoverMotion.class`
  - New hover-motion helper.
- `com/createcomplex/ambush/SableShipRuntimeAdapter$MechanicalPolarity.class`
  - New mechanical-polarity state/helper.
- `com/createcomplex/ambush/SableShipRuntimeAdapter$NetherClearanceBounds.class`
  - New Nether clearance-bounds helper.

### Quest and raid classes

- `com/createcomplex/ambush/AmbushRuntime$QuestContract.class`
  - New quest-contract runtime type.
- `com/createcomplex/ambush/VanillaRaidTrigger.class`
  - New vanilla-raid trigger implementation.

### New mixin/accessor classes

- `com/createcomplex/ambush/mixin/MobGoalSelectorAccessor.class`
- `com/createcomplex/ambush/mixin/AmbushHeatMapSplitSafetyMixin.class`

## 2. Mixin configuration changes

The 1.1.3 mixin list contained ten entries. 1.1.4 retains those entries and adds two more:

- `MobGoalSelectorAccessor`
- `AmbushHeatMapSplitSafetyMixin`

The configuration remains optional and non-failing:

```json
"required": false,
"injectors": {"defaultRequire": 0}
```

The retained mixins are:

- `MountedBigCannonFireMixin`
- `AirshipCrewSensingMixin`
- `AirshipCrewDistanceMixin`
- `AirshipCrewNavigationMixin`
- `AirshipCrewLookControlMixin`
- `AirshipCrewCrossbowAimMixin`
- `AirshipCrewCrossbowRangeMixin`
- `AirshipPillagerGunAimMixin`
- `AmbushCrewSableRetentionMixin`
- `AmbushOwnedAllianceMixin`

## 3. Removed encounter definitions

- `data/ambush/ambushes/balloon_pillager_mortar.json`
- `data/ambush/ambushes/high_altitude_phantom_riders.json`
- `data/ambush/ambushes/normal_ocean_drowned_hunters.json`
- `data/ambush/ambushes/normal_ocean_ruined_escort.json`
- `data/ambush/ambushes/normal_rain_drowned_deadfall.json`
- `data/ambush/ambushes/normal_rain_drowned_wanderers.json`
- `data/ambush/ambushes/normal_sky_bone_riders.json`
- `data/ambush/ambushes/normal_sky_phantom_scouts.json`
- `data/ambush/ambushes/ship_pillager_16cannon_broadside.json`
- `data/ambush/ambushes/ship_pillager_armored_boss_barge.json`
- `data/ambush/ambushes/ship_skelly_boss_barge_120.json`
- `data/ambush/ambushes/unlock_altitude_restless_skies.json`

No compiled Java classes or NBT structures were removed.

## 4. New encounter definitions

The following definitions are new in 1.1.4:

- `armored_pillager_balloon_surround.json`
- `armored_pillager_balloon.json`
- `armored_pillager_ship_small.json`
- `boss_reward_balloon_captured.json`
- `boss_reward_balloon_standard.json`
- `iron_raider_ship.json`
- `iron_tide_boss.json`
- `nether_autoaim_piglin_platform.json`
- `nether_piglin_autoaim_platform_overworld.json`
- `pale_leviathan_boss.json`
- `pillager_fishing_boat.json`
- `ship_pillager_autocannon.json`
- `ship_pillager_mortar.json`
- `ship_pillager_tiny_cannon_raid.json`
- `storm_breaker_boss_high_altitude.json`
- `storm_breaker_boss.json`

These additions expand the bundled content into several recognizable groups:

- armored pillager balloon and ship encounters
- Iron Tide and Iron Raider content
- Nether piglin auto-aim platforms
- Pale Leviathan boss content
- fishing-boat content
- dedicated pillager autocannon and mortar encounters
- tiny cannon raid content
- Storm Breaker boss variants
- captured and standard boss-reward balloon variants

## 5. New NBT structures

The following structures are new in 1.1.4:

- `data/ambush/structure/armoredballoonpillager1_1_4.nbt`
- `data/ambush/structure/armoredcannonbossbargepillagerfixed.nbt`
- `data/ambush/structure/armoredpillagershipsmall1_1_4.nbt`
- `data/ambush/structure/autoaimpiglinballoonnether.nbt`
- `data/ambush/structure/castironpillagerairshipantiground.nbt`
- `data/ambush/structure/netherpiglinautoaimplatform.nbt`
- `data/ambush/structure/piglinautoaimhoverplatform.nbt`
- `data/ambush/structure/pillagerfishingboat1_1_4.nbt`

No NBT structure paths were removed.

The following existing NBT files changed content:

- `16cannonbroadsidepillagership.nbt`
- `armoredcannonbossbargepillager.nbt`

Static archive comparison confirms that those structures changed, but does not establish which individual blocks changed without a separate NBT-level structural decode.

## 6. New loot tables

Added:

- `data/ambush/loot_table/chests/pillager_mansion_supply.json`
- `data/ambush/loot_table/chests/pillager_ship_supply.json`

The existing `data/ambush/loot_table/autocannon_hopper.json` also changed substantially.

1.1.4 changes in that loot table include:

- changed stack-count handling
- additional weighted pools
- additional `createbigcannons:projectile` component data
- tracer ammunition variants
- flak autocannon-round variants
- impact-fuze variants
- proximity-fuze variants
- nested projectile-component definitions on autocannon cartridges

## 7. Existing JSON definitions changed

The following existing JSON resources changed content in 1.1.4:

- `balloon_pillager_tnt_surround.json`
- `balloon_skelly_small_30.json`
- `balloon_skelly_small_surround_45.json`
- `balloon_skelly_tiny_10.json`
- `balloon_skelly_tiny_surround_20.json`
- `boat_evoker_tiny_broadside.json`
- `boat_pillager_forward_autocannon.json`
- `normal_thunder_bogged_crossfire.json`
- `pillager_combined_fleet.json`
- `pillager_potato_car.json`
- `rare_surface_structure_evoker.json`
- `rare_thunder_bone_tempest_raid.json`
- `ship_pillager_airship.json`
- `ship_pillager_grapeshot.json`
- `ship_pillager_tiny_3man_fleet.json`
- `ship_pillager_tiny_3man.json`
- `ship_pillager_tiny_cannon_fleet.json`
- `ship_pillager_tiny_cannon.json`
- `ship_pillager_tiny_logcannon_fleet.json`
- `ship_pillager_tiny_logcannon.json`
- `ship_pillager_tiny_tnt_fleet.json`
- `ship_pillager_tiny_tnt.json`
- `ship_skelly_6cannon_broadside.json`
- `ship_skelly_mixed_surround_35.json`
- `ship_skelly_small_50.json`
- `ship_skelly_small_fleet_50.json`
- `ship_skelly_small_fleet_70.json`
- `ship_skelly_small_surround_60.json`
- `ship_skelly_tiny_35.json`
- `ship_skelly_tiny_fleet_40.json`
- `ship_skelly_twincannon.json`
- `unlock_full_moon_witch_reckoning.json`
- `autocannon_hopper.json`

## 8. Encounter-schema and control changes

### 8.1 Standardized altitude control

Many balloon and ship definitions gain or change:

- `altitude_hold_ticks`
- `analog_step`
- `analog_step_seconds`
- `hold_release_blocks`
- `distribute_lever_signals`
- `minimum_ground_clearance`
- `ground_clearance_enabled`
- `descent_arrest_enabled`
- `max_signal_step`

This affects the small balloon, balloon-surround, small skelly ship, tiny skelly ship, mixed fleet, tiny skiff, cannon skiff, log-cannon skiff, TNT skiff, and related fleet definitions.

### 8.2 Health and envelope handling

Multiple definitions add:

```json
"health_ignore_envelope": false
```

This appears in the revised ship/balloon definitions as a new explicit health-envelope behavior field.

### 8.3 Propulsion and stuck recovery

`pillager_potato_car.json` gains explicit stuck-recovery controls:

- `stuck_escape_enabled`
- `stuck_detect_ticks`
- `stuck_forward_ticks`
- `stuck_reverse_ticks`
- `stuck_lookahead_blocks`
- `stuck_climb_blocks`
- `stuck_climb_ticks`

Several skiff definitions gain `approach_propulsion_fallback`.

### 8.4 Chase-controller changes

Several ship encounters change:

- `target_range`
- `stop_distance`
- `resume_distance`
- `distance`
- `distance_tolerance`
- `correction_range`
- `maximum_signal`
- `throttle_signal`

These changes appear across the balloon, boat, tiny skiff, cannon skiff, log-cannon skiff, TNT skiff, and skelly ship definitions.

### 8.5 Recovery-turn and come-about behavior

New fields appear in relevant ship definitions:

- `recovery_circle_enabled`
- `recovery_circle_degrees`
- `come_about_enabled`
- `come_about_aft_degrees`
- `come_about_release_degrees`
- `come_about_max_ticks`

These are especially visible in `ship_pillager_grapeshot.json` and `ship_skelly_6cannon_broadside.json`.

### 8.6 Cannon alignment and loot configuration

`ship_pillager_grapeshot.json` gains `cannon_alignment_mode` and changes its container-loot representation.

The following definitions migrate from a plural loot-table field to a singular loot-table field in at least one container entry:

- `ship_pillager_grapeshot.json`
- `ship_skelly_6cannon_broadside.json`

### 8.7 Entity aggro and tag changes

Several existing definitions change:

- entity `aggro_range`
- entity `deaggro_range`
- entity `spawn_distance`
- entity tag arrays

In several small-skiff and fleet definitions, old entity tags are removed and a reduced/new tag arrangement is introduced.

### 8.8 Lootr compatibility

`ship_skelly_twincannon.json` gains a `lootr_compatibility` section.

### 8.9 Steering and damping

`ship_skelly_twincannon.json` changes steering behavior, including:

- `turn_lead_blocks`
- `damping_ticks`
- `max_angle_step`

### 8.10 Boss-bar and player-facing text

The following definitions change boss-bar names or related player-facing text:

- `normal_thunder_bogged_crossfire.json`
- `rare_surface_structure_evoker.json`
- `rare_thunder_bone_tempest_raid.json`
- `unlock_full_moon_witch_reckoning.json`



## AMBUSH 1.1.3

### New content

- Added the Evoker Tiny Broadside Boat encounter, featuring an evoker captain, pillager crew, HE broadside cannons, a forward autocannon, and slowness-arrow attacks.
- Added ocean-capable surface boat encounters.
- Removed the deprecated Boat Autocannon and Pillager Autocannon encounters.

### FTB Quests integration

- Added optional FTB Quests support for tracking ambush outcomes.
- Encounters can now award quest progress on success and support failure outcomes such as timeouts or insufficient enemy defeats.
- Added optional follow-up actions for completed or failed encounters.

### Improvements

- Improved ship steering, propulsion, cannon assembly, cannon reloads, and firing reliability.
- Improved per-cannon broadside firing behavior and HE impact-fuze support.
- Improved ship crew behavior, targeting, and encounter cleanup.
- Improved compatibility with Create, Create Big Cannons, Sable, Simulated, and Aeronautics.



# Ambush 1.1.2

## Pillager Potato Car and authoring tools

- Added the rare plains-spawning Pillager Potato Car as a data-driven
  `sable_car` encounter with broadside and front potato-cannon banks.
- Added individual five-slot potato-cannon hopper tables plus a weighted mixed
  hopper table; the car's side hoppers use baked potatoes and its front hopper
  uses the mixed table.
- Added data-driven paired redstone receiver positions, allowing each visible
  button press to power its matching moving-assembly machine directly.
- Side potato-cannon buttons hold for 20 ticks and fire one at a time with a
  10-tick gap between presses; cannons receive their paired power pulse.
- Car controls support broadside orbit behavior, stationary-target braking in
  the configured firing band, out-of-range chase fallback, and a separate
  data-driven `ship_ai.mode: "chase"` mode for future vehicles.
- Ground cars honor `offset_y` during surface placement; the Potato Car spawns
  two blocks above terrain.
- `/ambush clear` now snapshots owned entities before discarding them so a
  single invocation does not skip entities while iterating the live collection.
- Added `/ambush admin inspect` for permission-level-2 vessel authoring: it
  reports the nearest active AMBUSH vessel's action/template, local origin,
  controls, hardware buckets, real block IDs, and schematic-local positions.

## Sable compatibility fix

- Stopped globally retaining vanilla pillagers, vindicators, evokers, and
  villagers inside Sable shipyard plots. Spawn-egg illagers and mobs seated on
  player-built sublevels now stay in world coordinates, so TaCZ hit detection,
  knockback, dismounts, and Aeronautics physics use the correct frame.
- AMBUSH's own registered airship crew remains retained through a targeted
  runtime hook, preserving its ship-only AI and aiming adapters without
  affecting unrelated mobs.

## Surface vessels

- Added data-driven `sable_boat` actions: water-only placement, normal
  horizontal ship AI, and a locked waterline with no vertical lift control.
- Bundled disabled pillager examples and their exact test-instance templates:
  a forward-autocannon motorboat and a twin-broadside cannon boat.


# Ambush 1.1.1

## Summary

This development release expands encounter content and improves ship, artillery, crew, compatibility, performance, and administrative behavior relative to Ambush 1.1.0.

## New content

- Added the Pillager Combined Fleet encounter.
- Added rare high-explosive shell barrage content with impact-fuze behavior.
- Added underground mining encounters with armored zombies and weapon-carrying skeletons.
- Added invisible-spider and speed-skeleton underground encounters.
- Added Trial Chamber encounters with telegraphed attacks.
- Added full-moon witch coven encounters with potion effects and potion-rain behavior.
- Added Nether piglin and piglin-brute encounters.
- Added blaze and breeze cohort encounters with telegraphed firecharge and windcharge attacks.
- Added mixed axolotl, vex, arrow, and potion storm encounters.
- Added additional ship fleet formations, reinforcements, and boss-barge encounter content.
- Added additional ship container loot where applicable.

## Ship systems

- Improved broadside formation spacing.
- Improved ship positioning and station-keeping behavior.
- Improved recovery from stuck or runaway movement.
- Improved spawn positioning across varied terrain and structures.
- Improved reinforcement pacing for larger ship encounters.

## Cannons and artillery

- Improved independent reload and firing behavior for broadside cannon groups.
- Improved cannon-battery assembly and firing reliability.
- Added distinct autocannon button-pulse behavior.
- Added dropmortar support for timed impact-fuze shells.
- Improved reload and firing consistency after the initial volley.
- Preserved button-controlled cannon behavior where required by an encounter definition.

## Crew and weapon compatibility

- Preserved native crossbow behavior for seated pillagers.
- Improved compatibility between ship crew, vanilla crossbows, and TaCZ weapons.
- Improved crew visibility, range, and aiming calculations on Sable ships.
- Improved crew initialization after ship assembly and propulsion setup.
- Preserved normal vanilla mob behavior where AMBUSH-specific control is unnecessary.

## Administration

- Added administrative controls for enabling and disabling datapack content.
- Improved administrative cleanup and persistent encounter-state handling.
- Improved recovery of persisted encounter state after restart.

## Performance and reliability

- Reduced unnecessary entity, schedule, and presentation scans.
- Added rotating work budgets for large encounters.
- Reduced idle server work when encounter processing is unnecessary.
- Improved cleanup and persistence for large encounters and ship formations.
- Improved guarded compatibility handling for optional Sable, Aeronautics and Create Big Cannons integrations

## Compatibility

- Continued support for data-driven encounter definitions supplied through server datapacks.
- Maintained fail-closed behavior for unavailable optional integrations.
- Improved validation of encounter definitions and bounded runtime settings.

