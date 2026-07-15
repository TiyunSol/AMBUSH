# Ambush 0.1.0 - Initial beta

- Added datapack-driven, per-player ambush definitions for NeoForge 1.21.1.
- Added interval, portal, active-block, and kill-count trigger support.
- Added player-specific cooldowns and persisted world/server restart state.
- Added bounded land, air, and water entity placement with visibility controls.
- Added equipment, effects, sounds, passengers, conditional waves, and projectile-rain actions.
- Added Sable aircraft assembly through Aeronautics Discovery's command pipeline.
- Added aircraft directionality, player-facing orientation, fleet, surround, and tangent/orbit formations.
- Added seated aircraft crews and moving-sublevel-aware crossbow aiming.
- Added balloon fill, engine burn, altitude-selected throttle, container loot, and child-sublevel attachment.
- Added restart-safe timed cleanup and configurable damage-percentage cleanup across parent, child, and split Sable structures.
- Added configurable cleanup explosions and force-load/constraint cleanup.
- Added optional Create Big Cannons shell waves with timed fuzes and source/target safety controls.
- Made Create, Create Aeronautics/Simulated, Sable, Aeronautics Discovery, and Create Big Cannons optional integrations; unavailable actions fail closed while generic datapacks remain functional.
- Added four bundled balloon schematics and a disabled-by-default master documentation encounter.
- Disabled automatic chance for every bundled non-balloon example; balloon examples retain low automatic chances.

Known limits:

- Automatic orbit steering is not included; orbit facing is only an initial tangent.
- Create Big Cannons actions require CBC and Ritchie's Projectile Library.
- Sable aircraft actions require Aeronautics Discovery 1.4.4 or newer within the 1.4.x line when that optional integration is used.
