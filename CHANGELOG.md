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

