# Dependencies and compatibility

## Required

- Minecraft 1.21.1
- NeoForge 21.1.228 or newer
- Java 21

No third-party content mod is required for ordinary mob encounters, vanilla projectile waves, sounds, effects, conditions, cooldowns, or commands. Install Ambush on the server and connecting clients.

## Optional integrations

### Create

Supported metadata range: 6.0.10 up to, but not including, 6.1. Create seats and compatible block entities may be used by moving-structure encounters.

### Create Aeronautics and Simulated

Supported metadata range: 1.3.x. These provide aircraft blocks, balloon systems, portable engines, throttle components, steering wheels, and related content used by bundled aircraft schematics. Ambush remains loadable when they are absent.

### Sable

Supported metadata range: 2.0.3 up to, but not including, 2.1. Sable enables moving structures, fleets, structure health, split-fragment cleanup, seated crews, redstone, steering, propulsion direction, and lifecycle events. Ambush contains its own guarded assembly adapter; no separate assembly-helper mod is required.

### Create Big Cannons

Supported metadata range: 5.11.7 up to, but not including, 6. This enables validated CBC shell rain, including shell blocks/items, fuzes, burst timing, ballistic aiming, safe targeting, and directional sources. Install CBC's own dependencies normally.

## Failure behavior

- Missing optional registry content is rejected or skipped with bounded diagnostics.
- Optional classes are loaded only when their feature is requested.
- A failed required action prevents dependent encounter parts from continuing unless partial execution is explicitly allowed.
- Generic encounters continue working with every optional integration absent.

Datapack authors should reference only registry content available in their selected mod set.
# AMBUSH 1.1.2 optional ship stack

Ship actions require the following tested API line:

- Minecraft 1.21.1
- NeoForge 21.1.228 or compatible 21.1 release
- Create 6.0.10
- Sable 2.0.3
- Create Simulated / Create Aeronautics 1.3.0
- Sable Companion 1.6.0 (normally provided by the installed Sable stack)

Generic AMBUSH encounters remain usable when the optional ship stack is absent.

