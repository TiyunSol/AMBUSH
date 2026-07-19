# Ambush

Ambush is a data-driven encounter framework for Minecraft 1.21.1 on NeoForge. Datapack authors can create per-player hostile encounters without writing Java or maintaining server scripts.

Definitions can combine mobs, formations, delayed waves, directional arrows and potions, validated modded projectiles, sounds, fog, structures, environmental conditions, persistent cooldowns, and optional moving Sable aircraft. Every encounter is owned by the player who triggered it, and commands are restricted to operators.

## Highlights

- Validated JSON loaded from `data/<namespace>/ambushes/<id>.json`.
- Per-player cooldowns, cooldown groups, chance buildup, ownership, targeting, and cleanup.
- Ground, water, underwater, air, ring, cluster, surround, formation, and hidden placement.
- Immediate, delayed, interval, death-gated, and chained waves.
- Vanilla, spectral, tipped, and registry-defined modded arrows.
- Directional entity, arrow, potion, projectile, and CBC shell rains.
- Structure ID, structure tag, and author-defined structure-group triggers.
- Biome, weather, time, altitude-duration, inventory, kill, trade, portal, active-block, and structure-history conditions.
- Optional Sable structures and heterogeneous fleets imported from structure-template NBT files.
- Data-driven structure health, shared fleet boss bars, percentage events, rewards, redstone, steering, propulsion, sounds, fog, and cleanup.
- No hard dependency on optional content integrations; unavailable actions fail closed.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.228 or newer
- Java 21

Optional integrations are listed in [DEPENDENCIES.md](DEPENDENCIES.md).

## Commands

```text
/ambush <type> [player]
/ambush always <type> [player]
/ambush list
/ambush validate
/ambush weights
/ambush state
/ambush debug
```

Commands require permission level 2. `always` bypasses chance, trigger restrictions, and cooldowns for testing.

## Documentation

- [Full datapack and feature documentation](FULL_DOCUMENTATION.md)
- [Executable master-example guide](MASTER_EXAMPLE.md)
- [Dependencies and compatibility](DEPENDENCIES.md)
- [Changelog](CHANGELOG.md)
- [CurseForge description](CURSEFORGE_DESCRIPTION.md)

The mod embeds five command-only examples, all with natural chance zero:

```text
ambush:example_boss
ambush:example_boss_fleet
ambush:example_cannon_balloon_fleet
ambush:example_directional_arrow_rain
ambush:example_master
```

## Building

```powershell
.\gradlew.bat clean build
```

The output jar is written to `build/libs/`.

## License

Ambush is released under the [MIT License](LICENSE).
