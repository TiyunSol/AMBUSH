# Ambush 0.1.3

## Data and authoring

- Expanded validated datapack schema and clearer JSON-path validation errors.
- Added embedded examples for night skeletons, high-altitude phantoms, ocean guardians, outpost defenders, portal piglins, underground silverfish, village zombies, and directional arrow rain.
- Added updated full documentation, master example, AI authoring guide, and copy-paste authoring reference.
- Fixed night-condition parsing for embedded examples.

## Runtime and performance

- Added bounded placement-attempt safety for hidden spawning.
- Added hard encounter limits for ships, entities, projectiles, scheduled actions, chunk distance, and nested formations.
- Added transactional moving-structure assembly and runtime hardware verification.
- Added persistent encounter state, scheduled actions, owned-entity indexing, cleanup generation tracking, and safe optional integrations.
- Capped debug entity output so large projectile waves cannot flood the server log.
- Added dry-run and debug inspection for templates, crew, hardware, schedules, wave sources, loot, and estimated resource usage.
- Vindicator and evoker Ambush entities aggro through walls by default.

## Compatibility

- Generic encounters remain usable when optional integration mods are absent.
- Moving-structure and artillery actions fail closed when their required registry content is unavailable.
- Reload, command execution, and required-action failures now report actionable reasons.
