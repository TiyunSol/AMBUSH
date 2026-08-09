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

