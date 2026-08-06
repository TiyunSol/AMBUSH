Ambush 1.1.0
Ships and balloons
Ships no longer fall on spawn. A newly assembled hull is now held motionless from the moment it becomes physics-active until it has finished loading in — every finalization stage complete, plus a short settle — and only then released. Previously a ship became physics-active with no buoyancy yet and simply dropped, landing before it could orient or fly.
Orientation now happens while the hull is held, as the last finalization stage, so a ship is already facing the right way before the player ever sees it move. Ships previously oriented after landing, which baked in a heading taken on the ground.
envelope_fill is now explicitly a one-shot spawn initialiser that gives a balloon its starting gas. It is not an altitude mechanism; height is controlled by altitude_controller driving the burners and analog levers.
Added lead compensation to the altitude controller. It now corrects for where the ship is heading, not only where it currently is, via velocity_lookahead_ticks. A purely proportional controller driving an actuator limited to one signal step per four-second dwell could not arrest a descent in time and flew ships into terrain.
Added descent_arrest_enabled / descent_arrest_margin: when a descent cannot be stopped before reaching terrain, lift goes to maximum and the normal dwell and slew limits are bypassed.
Fixed range-holding thrust reversal never running for chase and tnt_drop ships. It only ran in the broadside/orbit branch, which a chase ship never reaches — it returns early once inside stop_distance, precisely where going astern matters.
Fixed a chase ship centring its rudder and coasting once inside stop_distance. Centring the rudder does not slow a ship, it only stops it turning, so it flew past the player and continued in a straight line indefinitely. Chase ships now keep steering at close range and come about.
Fixed glue (Create super_glue, Simulated honey_glue) never holding propeller assemblies together. The entities were kept during placement, but Sable's block-only assembly moved a ship's blocks into its plot without carrying the glue, orphaning it at the staging position.
Fixed ships arriving with clutches already engaged, cutting the driveline.
Fixed formation members merging into a single hull. A formation parent carrying its own structure_key made every member resolve to the same assembly slot, so they were built on top of each other into one hull holding every member's blocks. Formation parents must not define structure_key.
Ships now spawn further out, and spawn bearings are randomised. The candidate bearings were previously scanned in a fixed order, so with line-of-sight avoidance every ship reliably ended up at the same spot behind the player. Added spawn_bearing_jitter_degrees.
Escort and reinforcement ships now linger after their parent is destroyed (child_cleanup_linger_ticks, default two minutes) and leave with an explosion, staggered so a large fleet does not tear down in a single tick.
Structures
micro_structure cleanup no longer leaves parts of a structure behind. Restoration required an exact blockstate match, but connecting blocks (walls, fences, panes) rewrite their own connection properties as neighbours are removed, so the last few pieces never matched and were skipped. It now matches on block type, while still refusing to touch a position the player has replaced with something else.
The structure action gained surface and spawn_distance. It previously emitted only player-relative coordinates, placing structures at whatever Y the player stood at and directly on top of them.
Redstone and cannons
Fixed range being inert on redstone activations. Operator precedence in the readiness expression meant an activation with the default require: "any" evaluated as always-ready, so ship weapons fired regardless of how far away or how far above the player was. An explicitly configured range is now always enforced.
Activations that resolve to zero targets several times in a row now retire themselves and stop re-resolving, which normally means the player destroyed the component they drove. Previously they retried forever and re-logged.
Added vertical_tolerance and honoured player_direction and repeat_ticks, which were documented and accepted but never actually consumed.
Persistence
Fixed ship state silently failing to save. The encounter definition was stored as an NBT string, which is hard-capped at 65,535 bytes; a large definition exceeded it and the entire journal write failed. It is now stored as a byte array, and journals written by earlier versions still load.
Logging
Per-encounter lifecycle logging is now off by default and toggled with /ambush admin debug. It previously wrote thousands of lines per session at INFO, burying genuine warnings. Warnings and errors are never suppressed.
Documentation
Added DATAPACK_GUIDE.md for players writing their own encounters, and SHIP_AUTHORING.md for airships.
Ambush 1.0.1
Replaced single-seed connectivity assembly with Sable's public explicit-block assembly API. AMBUSH now passes the complete bounded placement receipt and template bounds, so ships assemble every placed schematic block exactly once without depending on glue or adjacency discovered from the seed block.
Fixed the shared ship-finalization failure that left only ghost block-entity renderers visible. Sable returns global plot bounds while its embedded accessor accepts plot-local positions; AMBUSH now translates every bounded global scan coordinate before access and rebuilds its block receipt from the verified ship blocks.
Restored ordinary placement synchronization so clients receive the complete transition from staging blocks to the assembled Sable ship.
Deferred player-facing orientation until the final ship setup stage, after loot, hoppers, cannons, crew, propulsion, and envelope initialization.
Added temporary bounded ship diagnostics containing the plot center, global bounds, loaded plot chunks, recorded and verified block totals, hardware counts, and small coordinate/state samples.
Fixed a Cannon Barge crash during post-assembly hardware scanning. AMBUSH now translates recorded global plot positions into Sable accessor-local positions for block reads, block-entity access, writes, and scheduled button ticks, preventing the plot center from being applied twice.
Corrected ship spawning to prepare at most one unloaded footprint chunk per turn, place and assemble immediately when the footprint is ready, issue the Sable force-load ticket immediately, and access hardware through the assembled plot rather than parent-world coordinates. Removed distant pose parking, which caused enormous collision bounds and client failure.
Replaced repeated Sable reassembly with one-pass bounded chunk preparation: AMBUSH loads the destination footprint and new Sable plot before hardware scanning, then finalizes crew and ship state before the first visible snapshot or cleans the incomplete ship without respawning it.
Fixed /ambush clear tolerating a transient null entity returned by the live entity iterable.
Defaulted mobs to nearby-player targeting, 40–56 block placement, and land-without-water unless a datapack explicitly overrides those choices.
Added raid count_toward_wave; Bone Tempest skeleton horses are now non-counting passive mounts and its later directional volleys telegraph longer and more clearly.
Rebuilt Projected Skeleton Volley as a persistent one-wave raid whose arrows originate from each successfully spawned archer. Fixed all three rejected spawn-animation aliases and validated all 77 maintained definitions.
Added bounded, data-driven, non-Sable micro_structure actions with block states, replacement rules, shared-anchor mob offsets, persistent restoration, loaded-chunk-only placement, and /ambush clear cleanup.
Micro structures accept author-friendly lifetime_seconds as well as lifetime_ticks; the bundled sculk breach restores itself after five minutes.
Added the rare ambush:surface_sculk_breach: a hidden temporary sculk patch with sensors, a summoning shrieker, a catalyst, and two empowered wardens.
Made /ambush always toggle-only. It no longer accepts or suggests an encounter argument; enable the toggle and then use /ambush <id>.
Renamed the bundled ambush:sol_2_fleet encounter to ambush:airship_fleet, including its display name, boss bar, cooldown group, crew tags, and structure keys. The internal schematic resource ID is unchanged.
Overhauled Sable ship spawning to suppress temporary staging-block packets, finish assembly and all setup before the first completed snapshot, and remove incomplete sublevels before further processing. Ships no longer remain visible while waiting for crew, loot, cannon, propulsion, or envelope finalization.
Reduced ship tick spikes with one heavy transition per dimension tick, UUID-staggered maintenance, once-per-second split discovery, indexed cannon routing, one fewer full staging-volume pass, and health scans capped at 1,024 checks per ship turn with a 256 default.
Reset the datapack API to one current schema. Advanced definitions now require an object-form trigger; easy definitions require explicit format: "easy". Removed old field aliases, old action names, implicit easy-format detection, and runtime hotpatching of maintained external ship definitions. All bundled and maintained external definitions were migrated to canonical fields.
Removed pre-journal ship-state conversion. Saved ship journals whose schema is not the current version are intentionally discarded instead of translated; optional Sable, Aeronautics, Simulated, and CBC dependency handling remains.
Replaced the serialized ship phase machine with an AMBUSH operation journal composed of placement receipts, public ship-handle records, and an exact owned-resource ledger.
Added required, deterministic assembly_origin coordinates and removed the staged-volume assembly-block scan. Updated all bundled ships, nested ship actions, and the two maintained external grapeshot/autocannon definitions.
Failed staging now rolls back only block positions recorded as changed by that AMBUSH placement request. Finalization, operational polling, and cleanup are derived from receipts instead of serialized lifecycle phases.
Made placement and assembly_origin rotation-safe by using the template's exact rotated bounding box and coordinate transform, including rotations whose footprint extends toward negative world coordinates.
Removed load_chunks_for_placement; bounded footprint preparation and post-assembly Sable force loading are automatic. All rotated-footprint chunks be loaded; otherwise the journal waits safely.
Successful assembly now consumes and compacts its placement receipt, while failed staging retains the exact changed-block receipt needed for rollback.
Added a persisted, datapack-driven vanilla-style raid action with shared wave boss bars, configurable horns, named waves/groups, wave-clear gating, victory presentation, restart recovery, and bounded cleanup.
Raid groups can run directional projectile actions from their successful spawn positions. Added the rare five-wave thunder-night Bone Tempest raid, including hidden archers, lightning sword troops, skeleton cavalry, arrow barrages, and a strengthened wither-skeleton final champion.
Fixed natural trigger: "kill" encounters so their environmental conditions, configured chance policy, cooldown, and failure buildup are honored before spawning.
Added persistent, datapack-defined unlock detectors for structure-specific kills, mined blocks, mining above/below/at a configured Y level, Y-level crossings, timed travel distance, timed dimension transitions, biome residence, and boat residence.
Added linear or multiplicative weight_scaling from qualifying progress, including configurable start, per-unit growth, minimum, and maximum weight.
Added nine activity-consequence encounters, upgraded all seven structure-revenge encounters to explicit structure-kill detection and scaling weights, and replaced all demonstration-prefixed encounter IDs with production names.
Added per-mob bonus health, bonus melee/projectile damage, persistent mob boss bars, and health- or age-triggered reinforcement actions.
Mob reinforcement actions can spawn mobs, call named or inline encounters, queue compatible ships/formations, and launch entity/projectile/CBC rains.
Added persisted one-shot/repeat state and accepted-action cursors, owner-aware execution, boss-bar cleanup, schema caps, fair rotating mob scans, and per-tick lifecycle budgets.
Updated the outpost, Pillager Airship, and Pillager Cannon Ship examples with test captains covering stats, bars, mob waves, ship calls, and rains.
Replaced the large command surface with /ambush list, direct triggering, persistent /ambush always, /ambush clear, and one grouped admin branch.
Added a beginner format: "easy" compiler, seven encounter presets, a ready-to-copy datapack template, display names, banners, and mob particles.
Added explicit/inferred mod requirements so incompatible encounters are absent from listing, suggestions, direct triggering, and automatic checks.
Split ship setup into a one-scan, persistent checkpoint pipeline for requirements, loot, hoppers, cannon assembly/loading, crew, and propulsion.
Added CBC 5.11.7 native cannon assembly and first-load integration through its public hand-loading and mounted-contraption synchronization APIs.
Updated all eight generic examples and both ship encounters for the new authoring and compatibility behavior.
Added closed-loop player-relative/absolute altitude control with deadband, proportional analog output, slew limiting, inverted-lever readback, and a configurable loss-of-target failsafe.
Added broadside and boarding ship AI modes with persistent left/right side selection, player-relative height, range bands, and inward/outward steering.
Updated pillagerairship, pillagercannonship, its Sol 4 reinforcement, and the two external autocannon/grapeshot ship definitions for the new controls.
Replaced the former ship implementation with an independently designed clean-room Sable 2.0.3 / Simulated 1.3.0 adapter.
Added persistent AMBUSH action, owner, member, entity, parent, and sublevel identity with bounded attempt delays and restart recovery.
Added bounded template placement, public-API assembly, hardware inspection, crew creation, redstone/analog activation, hopper initialization, block health tracking, lifecycle scheduling, and ownership-checked family cleanup.
Added clean-room provenance and preserved the former ship source only under quarantine/, outside the build.
Added the command-only ambush:pillagerairship integration test encounter, derived from sol_s_e_3.nbt, with explicit crew, hardware, redstone, propulsion, lifecycle, persistence, split, damage, and cleanup coverage.
Added missing schema allow-list entries for clean-room ship lifecycle and hopper fields. CBC reloads and boss bars remain outside pillagerairship.
Added the command-only ambush:pillagercannonship encounter using the unmodified cannon-barge structure, color-specific crew placement, combined outpost/mansion container loot, fixed-cannon redstone tests, and the health-driven The Ashen Broadside boss bar.
Added real Create seat mounting, non-crew passenger tracking, combined loot table insertion, and the optional ambush:autocannon_hopper loot table.
Ambush 0.1.3
Data and authoring
Expanded validated datapack schema and clearer JSON-path validation errors.
Added embedded examples for night skeletons, high-altitude phantoms, ocean guardians, outpost defenders, portal piglins, underground silverfish, village zombies, and directional arrow rain.
Added updated full documentation, master example, AI authoring guide, and copy-paste authoring reference.
Fixed night-condition parsing for embedded examples.
Runtime and performance
Added bounded placement-attempt safety for hidden spawning.
Added hard encounter limits for ships, entities, projectiles, scheduled actions, chunk distance, and nested formations.
Added transactional moving-structure assembly and runtime hardware verification.
Added persistent encounter state, scheduled actions, owned-entity indexing, cleanup generation tracking, and safe optional integrations.
Capped debug entity output so large projectile waves cannot flood the server log.
Added dry-run and debug inspection for templates, crew, hardware, schedules, wave sources, loot, and estimated resource usage.
Vindicator and evoker Ambush entities aggro through walls by default.
Compatibility
Generic encounters remain usable when optional integration mods are absent.
Moving-structure and artillery actions fail closed when their required registry content is unavailable.
Reload, command execution, and required-action failures now report actionable reasons.
