# Ambush

Ambush is a data-driven encounter framework for Minecraft 1.21.1 NeoForge.

Create hostile encounters with datapack JSON files—without writing Java, maintaining scripts, or rebuilding the mod. Encounters are per-player and can be as simple as two zombies appearing out of sight, or as large as a boss event with multiple waves, structures, fog, sounds, rewards, and moving craft.

Features

- Per-player encounters with ownership, targeting, cooldowns, cleanup, and persistence.
- Datapack JSON definitions with validated fields and detailed reload errors.
- Flat chance, weighted rarity, cooldown groups, and chance buildup after failed rolls.
- Conditions for height, time, weather, biomes, biome tags, dimensions, structures, portals, active blocks, inventory, kills, trades, and time spent in an environment.
- Land, air, water, underwater, ring, cluster, surround, and out-of-sight spawning.
- Mobs with equipment, effects, attributes, NBT, passengers, tags, wall aggro, aggro/de-aggro ranges, and friendly-fire protection.
- Delayed, repeating, death-gated, health-gated, and conditional waves.
- Vanilla arrow, potion, spectral-arrow, tipped-arrow, entity, and optional artillery rains.
- Directional attacks that originate from a named structure, formation member, or configured bearing.
- Optional moving-structure support with fleets, formations, seats, crew, engines, fill, steering, redstone controls, structure health, boss bars, rewards, and cleanup.
- Optional Create Big Cannons shell attacks with validated fuze and propellant handling.
- Spawn and despawn sounds, particles, effects, fog, fireworks, and encounter audio.
- Transactional fleet assembly, bounded placement, hard safety limits, dry-run inspection, and runtime hardware verification.

Optional integrations are detected safely. Generic mob encounters and vanilla actions work without moving-structure or artillery integrations.

Commands

Commands require permission level 2:

/ambush list
/ambush validate
/ambush debug
/ambush debug <namespace:id>
/ambush dry-run <namespace:id>
/ambush weights
/ambush state
/ambush clear
/ambush exampletoggle
/ambush <id>
/ambush <id> <player>
/ambush always <id>
/ambush always <id> <player>

Definitions are loaded from:

data/<namespace>/ambushes/<id>.json

Structure templates are loaded from:

data/<namespace>/structure/<path>.nbt

See FULL_DOCUMENTATION.md, MASTER_EXAMPLE.md, AI_DATAPACK_AUTHORING_GUIDE.txt, and paste.txt for the complete schema and examples. Test large encounters in a disposable or backed-up world.
