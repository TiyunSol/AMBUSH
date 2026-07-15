# Installation and release verification

## Installation

1. Install Minecraft 1.21.1 and NeoForge 21.1 or newer.
2. Put `ambush-0.1.0.jar` in the server's `mods` folder. Use the same jar on clients when distributing it as part of a modpack.
3. Install only the optional integrations needed by your datapacks. Install physics integrations on both client and server.
4. Back up the test world before spawning physics aircraft.

## First-start verification

1. Start a client and server with the exact release jar.
2. Join a disposable test world.
3. Run `/ambush validate`; expect 18 accepted definitions.
4. Run `/ambush list` and confirm the bundled IDs appear.
5. With the optional Sable aircraft stack installed, run `/ambush ambush:example_air_balloon` in open, already-loaded terrain.
6. Confirm the balloon assembles as one functional Sable family and the crossbow pillager occupies a seat.
7. Run `/ambush ambush:example_barrel_balloon`; verify the six seated pillagers and dungeon loot in barrels.
8. With CBC installed, run `/ambush ambush:example_cannon_balloon`; verify elevated timed-fuze shell waves and seated crew.
9. Restart before the five-minute deadline; confirm cooldowns, delayed actions, and aircraft cleanup survive restart.
10. Destroy at least 30 percent of an Ambush aircraft, including split fragments; confirm the full tracked family is removed with its configured effect.
11. Review `logs/latest.log` for Ambush errors, failed dependency checks, Sable assembly failures, or cleanup failures.

## Core-only verification

1. Create a clean NeoForge 1.21.1 profile with Ambush and no Create, Aeronautics, Simulated, Sable, Discovery, or CBC jars.
2. Start a world and run `/ambush validate`; all definitions should parse even though optional actions are unavailable.
3. Run `/ambush ambush:example_zombie_pair`; generic spawning should work.
4. Run a Sable balloon definition; it should fail closed without crashing or loading optional classes.
5. Check `latest.log` for `NoClassDefFoundError`, `ClassNotFoundException`, or repeated optional-compatibility warnings.

## Automatic-spawn verification

- Non-balloon bundled examples have automatic chance zero.
- Single balloon examples use compact chance `0.1` percent per interval check.
- Fleet, surround, and orbit examples use compact chance `0.05` percent per interval check.
- Manual commands remain available regardless of automatic chance, but normal definition conditions still apply.

## Verification status for this package

The packaging workflow proves JSON parsing, compilation, jar contents, hashes, and a repeated clean NeoForge 21.1.228 dedicated-server startup with only Ambush, Minecraft, and NeoForge loaded. The latest rerun reached `Done (2.489s)` with no `NoClassDefFoundError`, `ClassNotFoundException`, or mod-loading failure.

The exact release jar has not received a fresh client launch or complete aircraft/CBC gameplay pass after the optional-dependency metadata change. Complete the gameplay checks above before changing the file from Beta to Release.
