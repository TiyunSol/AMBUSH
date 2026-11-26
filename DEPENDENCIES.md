# AMBUSH 1.1.9 dependencies and compatibility

AMBUSH can run on its own. Ordinary mob encounters, vanilla projectile waves, sounds, effects, conditions, cooldowns, and commands work with just Minecraft and NeoForge. The optional mods below provide the vehicles, weapons, crews, and loot systems used by specific encounters.

## Requirements

- Minecraft **1.21.1**
- NeoForge **21.1** for Minecraft 1.21.1. The development build used **21.1.228**.
- Java **21**
- AMBUSH installed on the server and connecting clients

## Airships, boats, and ground vehicles

| Optional mod | Version declared by AMBUSH | What it adds |
|---|---|---|
| **Create** | 6.0.10 or newer in the 6.0 series | Seats for crew, redstone controls, and machinery used aboard vessels. |
| **Sable** | 2.0.3 or newer in the 2.0 series | Moving hulls that players can board, fight, damage, and capture. AMBUSH handles assembly and cleanup for its own vessels. |
| **Create Aeronautics** and **Create Simulated** | 1.3.x for both | Balloon lift, engines, propellers, throttles, and steering. Bundled encounters such as the fishing convoy and merchant fleet use this machinery to travel through the world. |
| **Create Offroad** | Used by individual encounters | Wheels and vehicle hardware for ground encounters, including the pillager autocannon car, Cast Iron Twincannon Car, and drill car. |

**Create Aeronautics: Burner Fuel** lets AMBUSH stock adjustable burners through the burner's own fuel reserve. A timed ship receives fuel for its remaining lifetime at full output. A ship without a time limit receives two hours of fuel at full output. Lower throttle can stretch that supply. Existing fuel and inventory are preserved, and parking, capturing, or reloading the ship does not refill it. The integration targets the **Burner Fuel 1.0.2 API**.

**Create Aeronautics Toolgun** supports encounters authored with Toolgun `.excraft` schematics. Whether a pack needs it depends on the schematic format its author chose.

## Cannons and ammunition

**Create Big Cannons** supplies the weapons for armed ships, artillery boats, cannon carts, and roadside field guns. It also enables shell-rain encounters that use CBC projectiles and fuzes. AMBUSH declares support for **5.11.7 up to, but excluding, 6.0**.

Cannon add-ons expand the equipment that encounter authors can use:

| Optional add-on | AMBUSH compatibility |
|---|---|
| **CBC AT** | Heavy and twin autocannons, rocket pods, and medium rocket pods using their native ammunition feeds. |
| **CBC Neo Warfare** | Medium and rotary cannons. The bundled Cast Iron Twincannon Car and cast-iron raid reinforcement reference this add-on. Whole-contraption launchers are outside the supported loading path. |
| **CBC Enhanced Shells** | Compatible projectile blocks can be loaded through CBC’s big-cannon loading system. |
| **CBC Military Supplement** | HE and APHE cannon rockets for big cannons, plus HE, APHE, dual, and loitering rocket items for the steel projectile rack. |

## Crews and NPCs

- **Guard Villagers** adds guards to crew selections in the villager fishing convoy, fishing escorts, and faction support ship. Crew entries can specify a fallback for packs without guards.
- **Villagers Reborn** (`slimpatch`) lets compatible villager, wandering trader, pillager, vindicator, and evoker spawns use their replacement entity types while retaining their encounter and crew assignments.
- **Custom NPCs** lets a datapack spawn a saved NPC clone by name and clone tab. The matching clone must be available on the server.

## Lootr

With **Lootr** installed, supported ship chests and barrels can give each player their own loot. Datapack authors can opt out for one container rule with `lootr: false`, or for the entire vessel with `lootr_compatibility: false`.

Hoppers keep their normal inventory behavior. Rules that combine multiple loot tables also use ordinary containers. This lets a ship carry personal treasure alongside working supplies, such as ammunition or blaze cakes.

## Quests and event hooks

- **FTB Quests** can start an AMBUSH encounter when a tagged quest begins. An encounter can also complete tagged FTB tasks when it starts, succeeds, or fails. AMBUSH declares support for **2101.1.30 or newer**. A quest reward can give a player an AMBUSH contract item. The player renames it in an anvil to an online player’s exact name, then uses it to summon the configured encounter on that player. Contracts can also be awarded without FTB Quests.
- **Scorched Guns** raid starts can trigger a datapack-defined reinforcement. Authors can restrict the response to particular raid IDs.
- **Typewriter** can run configured entries when an encounter starts, succeeds, or fails on a server that provides its command integration. **Create Simulated’s linked typewriter** serves a separate purpose: it is an in-world control block that vessel rules can read.

## Optional mods in datapacks

An encounter can declare the mods it needs with `requires_mods`. For example, a pack can require Offroad for a cannon car while keeping its night patrol available to everyone. Missing requirements hide the affected encounter. Invalid definitions are reported during reload, and `/ambush list` shows which encounters loaded for the installed mod set.
