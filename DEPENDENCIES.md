# Dependency relations

Configure these in the CurseForge file's Related Projects section.

## Required

No third-party mod project is required for generic Ambush datapacks. Minecraft 1.21.1 and NeoForge are represented by the uploaded file's game-version and loader selections.

## Optional integrations

1. [Create](https://www.curseforge.com/minecraft/mc-mods/create)
   - Tested: 6.0.10 for Minecraft 1.21.1 NeoForge.
2. [Create Aeronautics](https://www.curseforge.com/minecraft/mc-mods/create-aeronautics)
   - Tested: bundled 1.3.0 for Minecraft 1.21.1 NeoForge.
   - Includes the Simulated content used by the bundled schematics.
3. [Sable](https://www.curseforge.com/minecraft/mc-mods/sable)
   - If present, jar metadata accepts: >=2.0.3 and <2.1.
4. [Create Aeronautics Discovery](https://www.curseforge.com/minecraft/mc-mods/create-aeronautics-discovery)
   - If present, jar metadata accepts: >=1.4.4 and <1.5.

1. [Create Big Cannons](https://www.curseforge.com/minecraft/mc-mods/create-big-cannons)
   - Tested: 5.11.7.
   - Enables CBC shell-rain actions used by cannon-balloon definitions.
2. [Ritchie's Projectile Library](https://www.curseforge.com/minecraft/mc-mods/ritchies-projectile-library)
   - Tested: 2.1.2.
   - Required transitively when Create Big Cannons is installed.

## Discovery availability note

The local aircraft test stack uses `aeronauticsdiscovery-1.4.4.jar`. At packaging time, CurseForge's indexed public files page exposed only up through 1.4.1. This does not block publishing core Ambush because Discovery is optional, but CurseForge users cannot use Sable aircraft actions until a compatible Discovery release is publicly available through a permitted route. Do not bundle or redistribute Discovery's All Rights Reserved jar inside Ambush.
