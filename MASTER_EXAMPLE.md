# Ambush master example

The executable reference definition is:

```text
data/ambush/ambushes/example_master.json
```

It is intentionally disabled for automatic selection with expanded trigger chance `0.0`. The descriptive `documentation_only` field is ignored by the runtime; chance zero is the actual automatic-spawn safeguard. Operators can invoke it explicitly with:

```text
/ambush ambush:example_master
```

This is a stress-test and API reference, not a balanced encounter. A full invocation may queue three Sable cannon balloons with 24 seated pillagers, schedule 18 CBC shells, produce several rain actions, and conditionally add ground, air, ocean, or storm groups. Use a disposable test world and face open, already-loaded terrain.

## What the file demonstrates

### Definition and trigger fields

- Expanded `trigger` object with `type`, `check_every_ticks`, `cooldown_ticks`, and object-form chance.
- Named `cooldown_group`.
- Expanded height/time conditions plus biome, dimension, and active-block arrays.
- Expanded `wave` object with radius, bounded placement attempts, and groups.
- Player effects and immediate registry sounds.
- An empty base group list, allowing successful Sable queuing without a dummy mob.

Only one trigger type can control a definition. Alternatives for copied definitions are:

- `interval`: normal periodic evaluation.
- `portal`: additionally requires a Nether portal within three blocks.
- `block_active`: requires a configured block within four blocks or a nearby block entity with positive `progress` NBT.
- `kill`: uses `kill_entity` and `kill_count`; the counter persists in `ambush_state`.

Compact top-level timing fields and expanded trigger fields are alternatives. Do not mix them unless you intentionally want the expanded fields to take precedence.

### Sable formation

The master formation demonstrates:

- A shared `sable_formation` definition with three member overrides.
- Named origins through `structure_key`.
- Player-relative distance and `spawn_bearing_degrees`.
- `player`, `orbit_clockwise`, and `orbit_counterclockwise` facing modes for a north-authored schematic.
- Clear-air placement, bounded search, assembly retries, lifetime, 30% damage cleanup, and a safe data-defined explosion.
- Fixed initial child sublevels, restart-safe cleanup, envelope fill, portable-engine burn time, and Y-selected throttle signal.
- Stronghold loot on barrel child sublevels with explicit seed and replacement policy.
- Object-form entity NBT, local coordinates, seating, persistence, owner targeting, custom tags, and plot-aware crossbow controls.

`local: [x,y,z]` takes precedence over `local_x`, `local_y`, and `local_z`; both forms are present to show their spelling. Normally choose one. `engine_burn_seconds` is the alternative to `engine_burn_ticks`. `lifetime_ticks` may be `null`, `"none"`, or `"permanent"` when no timer is wanted. `despawn_effect` currently supports `explosion` or `none`.

Orbit facing supplies only an initial tangent. It does not create centripetal steering, so Sable/Aeronautics physics determine whether the craft actually curves.

### Restart-safe modern actions

- `directional_cbc_shell_rain`: all three named structures, burst timing, HE block/item, timed fuze, velocity, inaccuracy, elevated/forward source, lateral spread, target spread, safe radius, and target height.
- `directional_arrow_rain`: a named structure source with day, height, dimension, delay, spread, and velocity controls.
- `directional_entity_rain`: generic registry entity from another named source, gated to night.
- `conditional_spawn`: delayed land groups, front cone, minimum/maximum radius, bounded attempts, random count, visibility rejection, persistence, owner target, wall aggro, equipment, crossbow range, effects, and tags.
- Conditional air passengers, recursive passengers, water placement in the player's forward view, ocean checks, and storm-only mobs.
- Delayed `sound` action with location, volume, and pitch.

These actions use `AmbushScheduleState` and retain their due time, dimension, owner, action JSON, and resolved structure origin across world/server restarts. If the owner is offline or in another dimension when an action becomes due, it waits.

Action conditions can use `time` (`day` or `night`), `weather` (`clear`, `rain`, or `stormy`), `over_ocean`, `min_y`, `max_y`, and `dimensions`. Opposite time/weather alternatives cannot execute in the same world state, but the master contains separate branches so each syntax is represented.

### Immediate and legacy actions

The master also includes one of each older action form:

- `entity_wave`
- `arrow_rain`
- `entity_rain`
- `shell_rain`
- `potion_rain`
- `cbc_shell_rain`
- `block_platform`
- `structure`
- `sable_substructure`

`entity_wave` is now dispatched correctly, but it and old `cbc_shell_rain` use the in-memory legacy scheduler and do not survive a restart. Immediate rain actions also do not persist because they execute at encounter start.

The final three static-world actions are intentionally gated behind the impossible dimension `ambush:documentation_only`. Their syntax remains visible without leaving permanent blocks during a normal master test. Remove those action-level conditions only when specifically testing them:

- `block_platform` fills ordinary world blocks and has no structure lifecycle.
- `structure` runs vanilla `place template`; it does not create a Sable craft or automatic cleanup.
- Despite its historical name, `sable_substructure` is static template placement plus a pillager and is not physics assembly. Use `sable_structure` or `sable_formation` for production aircraft.

A direct `sable_structure` action is also gated behind that documentation dimension because the active formation already exercises the same assembly queue and lifecycle with three members. `sable_sublevel` and `sable_sublevel_direct` are accepted compatibility aliases for `sable_structure`; prefer the explicit production name in new data.

## Spawn-group notes and alternatives

- `count` accepts an integer or `{ "min": n, "max": n }`.
- `passenger` adds one shorthand rider. `passengers` accepts recursive full spawn specifications. Using both adds both.
- `placement` supports normal land behavior, `air`, and `water`.
- `avoid_line_of_sight` defaults to true.
- `target` accepts `owner` or `none`.
- `aggro_through_walls` repeatedly restores the owner target.
- Positive `follow_distance` tags an air entity for bounded owner-follow motion; do not apply it to ordinary ground mobs because it directly adjusts velocity.
- `equipment.mainhand` and `equipment.offhand` accept registered item IDs. Pillagers with crossbows can use `crossbow_range`.
- `effects` use `namespace:id:seconds:amplifier`; amplifier `2` means level III.
- All spawned entities receive Ambush ownership tags in addition to custom `tags`.

## Dependencies and portability

- Sable assembly requires a compatible Sable and Aeronautics runtime. Ambush supplies its own guarded adapter and has no external assembly-helper dependency.
- Engine and throttle post-processing require the installed Simulated blocks to exist in the schematic. If absent, only those requested post-process operations fail.
- CBC rain requires Create Big Cannons and valid block, item, projectile, and fuze IDs. The master uses IDs verified against CBC 5.11.7.
- Structure templates are resources at `data/<namespace>/structure/<path>.nbt`.
- Definitions and referenced loot tables may come from any datapack namespace.
- Sound and entity IDs must exist in the active registries. Missing generic entities are skipped; dependency-specific behavior should be tested in the target pack.

## Other possible designs

- Add more formation members with unique keys and reference all or a subset through `source_structures`.
- Use fixed world angles with `spawn_angle_degrees`, or player-relative angles with `spawn_bearing_degrees`.
- Use cardinal facing, player facing, clockwise tangent, counterclockwise tangent, or raw `yaw_degrees` when no direction field is present.
- Give each ship different templates, elevations, throttle rules, loot tables, crew NBT, lifetimes, or damage thresholds by using separate `sable_structure` actions instead of one shared formation.
- Use custom datapack loot tables rather than vanilla chest tables.
- Use `despawn_effect: "none"` for silent removal or tune explosion power, fire, and block damage explicitly.
- Put mutually exclusive trigger/condition variants in separate definitions sharing one cooldown group to create a family of encounters without simultaneous activation.

The runtime does not provide automatic orbital steering, arbitrary particle-script execution, claims integration, chunk generation, or a general quest system. Those require a dedicated compatibility layer rather than additional JSON fields.

## Verification

1. Run `/ambush validate`; the master should count as one valid definition.
2. Run `/ambush ambush:example_master` in a disposable Overworld test area.
3. Check `latest.log` for three keyed Sable queue entries and persisted-action result counts.
4. Test day, night, ocean, and thunder separately to exercise mutually exclusive conditional branches.
5. Restart before a delayed modern action or structure cleanup to verify persistence.
6. Do not enable a nonzero automatic chance until the master has been copied and reduced to the intended production behavior.
