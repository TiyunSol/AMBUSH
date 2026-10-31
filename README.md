# Ambush

(TEMPORARY) - I am creating a modpack that will be an example for this mod. If you have ambush ideas, schematics, or suggestions please join the discord! [https://discord.gg/BHEQthrSSD](https://discord.gg/BHEQthrSSD)

Ambush is a NeoForge 1.21.1 mod for configurable, per-player hostile encounters. Install it on the server and connecting clients because the full feature set includes client fog/audio payloads. Encounter definitions are data-driven JSON files loaded from every server datapack under:

```text
data/<namespace>/ambushes/<id>.json
```

Definitions enter the runtime through a Mojang `Codec` boundary and recursive action validation. Unknown trigger/action modes and invalid bounded fields are rejected during datapack reload instead of being silently ignored.

Advanced definitions use the object-form `trigger`, and easy definitions must explicitly set `"format": "easy"`. Older datapacks may need updating before they will reload. Supported compatibility mappings are documented in the authoring guide; removed fields should not be copied into new definitions.

Create, Create Aeronautics/Simulated, Sable, and Create Big Cannons are optional integrations. Generic entity, sound, effect, and vanilla projectile encounters work without them. Individual vehicle encounters need the mods used by their hardware. Ambush contains its own guarded Sable assembly adapter and does not require an external assembly helper mod.

## Installation

Install `ambush-1.1.7.jar` on the server and connecting clients. It is intended to be distributed with a normal CurseForge modpack. Definitions may be bundled inside the mod or supplied by a separate datapack. Put a datapack in a world's `datapacks` directory, run `/reload`, and the definitions become available without rebuilding the mod.

Minecraft 1.21.1 uses datapack `pack_format: 48`. Java 21 is required.

The mod does not edit other mods' files or require a scripting platform. Ordinary encounters use loaded-chunk placement checks. Ship assembly and other integrations have their own bounded staging requirements.

Ships with cannons can damage terrain. Back up your world before testing large encounters, or use `/ambush admin safe_mode on` for AMBUSH cannon terrain protection.

## Making your own encounters

**Start here: [AMBUSH Datapack Guide](docs/AMBUSH_DATAPACK_GUIDE_1.1.7.md).** It walks through the datapack layout, easy and expanded definitions, mobs, equipment, unlocks, factions, vehicles, invasions, commands, and troubleshooting. No Java, no rebuilding — write a JSON file, run `/reload`, and test your encounter.

The guide includes complete examples copied from the bundled 1.1.7 datapack, along with a source-backed field reference. You can also download the [bundled JSON example library](docs/AMBUSH_1.1.7_BUNDLED_JSON_EXAMPLES.zip) and copy the resources you want to change. These resources already ship with Ambush; the library is for reference rather than an extra pack to install.

For a shorter introduction, see the [Easy Datapack Guide](EASY_DATAPACK_GUIDE.md). Additional topics are covered in [Full Documentation](FULL_DOCUMENTATION.md), the [Cannon Authoring Guide](CANNON_AUTHORING_GUIDE.md), and [Faction Gifts](FACTION_GIFTS.md).

## Commands

Help and listing are available to players. Starting or clearing encounters and the admin tools require permission level 2. Invasion participation commands are available to players; starting an invasion requires operator permissions.

```text
/ambush
/ambush list
/ambush <id> [player]
/ambush clear
/ambush enable [pack]
/ambush disable [pack]
/ambush admin always
/ambush admin check [id]
/ambush admin inspect
/ambush admin natural
/ambush admin entries
/ambush admin rate
/ambush admin rate <10–4000>
/ambush admin rate up
/ambush admin rate down
/ambush admin rate reset
/ambush admin safe_mode [on|off|status]
/ambush admin debug
/ambush admin debug positions
/ambush admin weights
/ambush admin reputation
/ambush admin reputation set <player> <faction> <value>
/ambush admin unlocks
/ambush admin unlockall
/ambush admin spawning
/ambush admin spawning status
/ambush admin spawning <id> [enable|disable]
/ambush invasion [status]
/ambush invasion start [profile] [waves]
/ambush invasion join
/ambush invasion leave
/ambush invasion ready
/ambush invasion stop
/ambush invasion classify
```

Use the exact resource ID from `/ambush list` or autocomplete. An underscore in a bundled filename is part of its ID. For example:

```text
/ambush admin check ambush:_villager_antiair_car
/ambush admin always
/ambush ambush:_villager_antiair_car
```

An external definition at `data/my_pack/ambushes/night_patrol.json` is run with `/ambush my_pack:night_patrol`. Do not add an underscore before the namespace.

`/ambush admin always` toggles persistent always mode for the executing player. While on, a manual encounter skips the normal eligibility check. Toggle it on, run your encounter, then run `/ambush admin always` again to turn it off. It does not change natural encounter timing or bypass invalid definitions and missing hardware.

Running a definition by name does not roll its natural chance. `/ambush <id> <player>` targets another player through the manual command path. There is no separate `/ambush player` subtree.

`/ambush admin check` reports loaded, dependency-hidden, and rejected definitions. Adding an ID runs its read-only preflight. `/ambush admin inspect` reports active vessel hardware and controls, which is useful when creating ship, boat, and car definitions.

`/ambush clear` cancels pending encounter work and queues cleanup of AMBUSH-owned content. Cleanup is processed over time rather than requiring a large fleet to disappear in one server tick.

### Natural-spawn controls and rate

`/ambush admin spawning` toggles natural spawning for the server. Manual encounters remain available. The `status` command reports the global toggle and individual encounter disables. Supplying an ID alone toggles that encounter; `enable` or `disable` sets it explicitly.

`/ambush enable [pack]` and `/ambush disable [pack]` operate on a datapack namespace; a blank argument targets `ambush`.

`/ambush admin rate` uses percentages. **100 is normal, 50 is half speed, and 200 is double speed.** The numeric command accepts 10 through 4000. `up`, `down`, and `reset` are convenient shortcuts. Changing the rate adjusts natural pacing without rewriting the authored selection weights.

`/ambush admin natural` shows shared-curve timing, eligible pools and rejection reasons. `/ambush admin weights` reports the ordinary encounter weights and chance state. These are different selection layers.

### Cannon safe mode

`/ambush admin safe_mode on` protects terrain from supported AMBUSH-owned cannon shots, explosions and inherited fragments. The setting persists with the world. Use `off` to disable it or `status` to inspect it; omitting the suffix toggles it.

CBC's protected projectile mass also disables direct-hit entity damage from those shells. Explosions and fragments retain their native entity damage. Safe mode is not general player invulnerability and does not change unrelated cannons.

### Invasions

Invasions provide cooperative wave battles with configurable objectives, budgets, intermissions, boss waves and scaling. Bundled profiles include standard, endless, ground-defense and naval modes.

```text
/ambush invasion start ambush:naval
/ambush invasion join
/ambush invasion ready
```

An invasion owner can stop their invasion; administrators can stop other invasions. Use `/ambush invasion status` to see the active state and available profiles.

## Factions, reputation, and natural encounters

Factions define relationships, mob membership, equipment, reputation and support behavior. Depending on the faction, standing can make it hostile, neutral or allied. Fixed-attitude factions can remain hostile independently of numeric standing.

The bundled reputation range is -10000 through 10000, with alliance at 5000. Reputation can change through kills, enemy vessel defeats, block damage and configured events. Hostile standing can affect encounter selection and threat level.

Allied villagers can provide support and periodic supply gifts. Gift quality depends on reputation, and enemy vessel defeats can improve the gift chance. Faction support and periodic gifts use separate datapack resources and scheduling rules.

Natural ships share a curve-based encounter budget. At the normal rate, the bundled normal window is 40–140 active-play minutes, averaging 80 minutes; the major window is 80–160 minutes. Each listed ship does not have its own independent copy of that timer. Nearby players can share the next encounter deadline.

Ordinary mob encounters, unlock consequences, faction support, gifts and invasions have their own gates. Progression stages and encounter-specific unlocks are separate requirements. First-dimension-entry encounters use a persistent world ledger and can choose one weighted patrol when a dimension is first entered after installation.

## Vehicles, boats, and potato cannons

Ambush supports ground vehicles through `sable_car`. Cars use surface placement and explicit `car_controls` positions for steering, clutch, reverse and throttle hardware. Controllers support chase and broadside behavior, with configurable stopping and range holding.

Ground navigation checks the rotated hull footprint, wheel support, steps, drops, fluids and obstacles. Speed-dependent lookahead and checked reverse recovery help the controller choose its next route. The vehicle still needs a working native drivetrain and suitable physical clearance.

`sable_boat` places vessels on eligible open water using authored depth and placement checks. Boats use the crew, cannon, steering, propulsion, event, cleanup and container systems, with native physics handling their motion and buoyancy.

A `sable_formation` can mix airships, boats and cars. Each member needs appropriate placement settings and its own valid hardware. Reusable profiles let encounters share a vessel definition while overriding selected behavior.

The bundled encounters include potato cars, saw and drill vehicles, anti-air cars, broadside boats, fishing fleets, merchant convoys, artillery emplacements and larger fleet bosses.

Vessel tracking and render distances can be configured through `force_render` and the shared render resources. These settings control client visibility; they do not force distant server terrain to load.

## Runtime safety and transactional fleets

Server-authoritative limits bound entities, projectiles, scheduled actions, nested fleets and other expensive work. Some ceilings are configurable through `ambush-common.toml`; others are fixed safety limits. Validation reports unsupported definitions and exceeded limits rather than silently accepting them.

Formations stage their members and verify assembly, crew and required hardware. Failed placement or finalization cancels dependent work. A required action can fail the encounter instead of reporting success with no accepted content.

`hardware_requirements` can require minimum seats, carpets, levers, analog controls, engines or propellers, balloon components and steering hardware. Requested controls and inventory operations use native readback where available.

Assembly, splitting and removal retain ownership tracking so later controls and cleanup address the live vessel family. Heavy scans and lifecycle operations use bounded processing. Debug mode adds control, aiming, placement and lifecycle diagnostics; normal gameplay does not require it.

## Definition format

Easy definitions explicitly declare `"format": "easy"`. Expanded definitions use an object-form `trigger` and can combine conditions, waves, actions, unlocks, faction defaults and rewards.

Easy `chance` is a percentage: `5` means 5%. For expanded chance, use fractions such as `0.05` for 5%. In the current chance reader, `1` means 100%. Easy timers use seconds; expanded `_ticks` fields use game ticks.

Root trigger types are `interval`, `portal`, `block_active`, `structure`, `kill`, `vanilla_raid_wave`, and `first_dimension_entry`. Conditions can use dimensions, biomes, structures, height, time, weather, inventory, nearby blocks, faction reputation and progression.

Ordinary entity encounters use spawn groups; actions add directional waves, projectiles, sound, fog, fireworks, conditional spawns, nested encounters and structures. Equipment can use explicit items or weighted armor sets, with attributes, enchantments, drop chances and target rules.

The datapack guide contains complete current examples and the field reference. Copy those rather than older compact-format examples.

### Reusable profiles and resources

Profiles live in `data/<namespace>/ambush_ship_profiles/`. A profile can contain an action or inherit another profile with overrides. Objects merge recursively; arrays replace rather than concatenate.

Other resource folders include `ambush_armor_sets`, `ambush_factions`, `ambush_faction_support`, `ambush_faction_gifts`, `ambush_natural_airship_curves`, `ambush_invasions`, and `ambush_ship_render`.

Minecraft 1.21.1 uses singular `structure/` and `loot_table/` folders. Resource IDs follow the namespace and filename, so moving or renaming a resource also requires updating its references.

## Crew, controls, and cannons

Crew can use real seats or supported deck spawn surfaces. Seat occupancy and living-crew membership are separate: cargo or rescued entities can be excluded from the crew that keeps weapons operational.

Analog, steering, propulsion, altitude, clutch and car controls address native hardware at template-local positions. Signal polarity, steering bounds and timing must match the actual schematic. Template-local coordinates must not be confused with world coordinates after assembly.

Conditional controls and redstone sequences can react to authored conditions, operate machinery, and control compatible rope winches. Linked Simulated typewriters use their own input settings; optional Typewriter event hooks use a separate integration.

Mounted cannons support assembly, initial loading, mechanical or swivel aiming, firing gates and reload rules. Initial and reload ammunition must match the actual cannon material, charge limits, tube space and fuze requirements.

Per-gun activations can declare real buttons or supported native firing mounts. Physical barrel direction, range, crew, settling, ship tilt and ammunition readiness remain separate gates. Supported autocannon ammunition boxes can use configurable refill timing.

A button pulse or native firing request is not confirmation of a projectile. Test visible shots, ammunition consumption and a second shot after reload. See the [Cannon Authoring Guide](CANNON_AUTHORING_GUIDE.md) before changing a gun's authored load.

## Health, lifecycle, rewards, and loot

Vessels can use tracked hull content as health, show boss bars, share fleet health, and run health-based events. Events can call reinforcements, activate controls, play sounds, apply fog, issue rewards or begin another encounter.

Capture, rescue, destruction, defense success and departure have separate outcomes. Cleanup settings cover lifetime, wrecks, downed vessels and split fragments. A captured ship should not automatically count as a destroyed enemy.

Container loot supports normal loot tables, combined tables and supported Lootr conversion. Hoppers and ammunition containers retain their operational roles. Merchant encounters can carry native trades with authored payments, results and stock limits.

FTB Quests hooks can connect encounter events to tagged tasks. Contract items can select a target and schedule an encounter without requiring FTB Quests. These integrations and their data formats are covered in the datapack guide.

## Optional integrations

Generic encounters work without the vehicle integrations. Install the dependencies used by the encounters you want to enable:

- **Create, Create Aeronautics, Simulated and Sable** for compatible moving vessels and machinery.
- **Create Big Cannons** and supported add-ons for their weapons and ammunition.
- **Offroad** for compatible wheeled vehicle encounters.
- **Lootr** for supported per-player container loot.
- **Guard Villagers** for compatible guard crews and support encounters.
- **FTB Quests** for quest hooks.
- **Custom NPCs** for explicitly configured saved-clone spawns.

Optional integrations do not make every schematic compatible automatically. The selected encounter must still name valid content and provide the required physical controls.

## Reloading and troubleshooting

1. Run `/reload` and review rejected or dependency-hidden definitions.
2. Use `/ambush admin check <id>` before spawning a complicated encounter.
3. Test a fresh spawn, then use `/ambush admin inspect` for live hardware information. Existing vessels can retain serialized settings from their original definition.
4. Use `/ambush admin natural`, `/ambush admin weights`, reputation and unlock commands to inspect eligibility.
5. Enable debug logging when you need detailed control or lifecycle evidence, then turn it off afterward.

Test physical movement, barrel direction, visible shots, inventory changes and cleanup rather than relying only on requested control values. Check valid routes as well as unsafe ones, and test reload/restart behavior when persistent state is involved.

Ambush can validate its own resources and supported integration boundaries, but it cannot infer a schematic's intended mechanics or force a shader to honor vanilla fog. Resource-pack sounds, external templates and optional mod content must be installed and tested with the target pack.
