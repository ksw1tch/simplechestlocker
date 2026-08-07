# SimpleChestLocker
Does what it's supposed to.

A Minecraft Fabric mod that allows players to lock containers and grant access to other players.

[![Modrinth](https://github.com/intergrav/devins-badges/blob/v3/assets/cozy/available/modrinth_64h.png?raw=true)](https://modrinth.com/mod/simplechestlocker)
[![CurseForge](https://github.com/intergrav/devins-badges/blob/v3/assets/cozy/available/curseforge_64h.png?raw=true)](https://www.curseforge.com/minecraft/mc-mods/simplechestlocker)

## Features
- Lock any container (chests, dispensers, modded containers, etc.)
- Trust specific players to access your locked containers
- Label chests for easier identification
- Multi-dimension support (chests across different dimensions work correctly)
- All data saved properly and persists between server restarts
- Configurable protections for hoppers, pistons, and explosions
- Optional client-side overlay (padlock icons + labels)

## Configuration

Located at `config/simplechestlocker.json`:
- `protectFromHoppers` - Prevent hoppers from extracting items from locked containers (default: true)
- `protectFromPistons` - Prevent pistons from moving/destroying locked containers (default: true)
- `protectFromExplosions` - Prevent explosions from destroying locked containers (default: true)
- `blockBlacklist` - List of block IDs (e.g. `minecraft:dispenser`) that cannot be locked. Default contains two `example:*` placeholders; remove or replace them with the blocks you want disallowed.

## Client-side overlay

The mod works fully server-side, but installing it on your **client** too adds a visual overlay (added in 2.2.0):

- A padlock icon appears in front of every locked container, so locked chests and barrels are easy to spot.
- The icon shows an unlocked padlock when you are the owner or a trusted player.
- When you aim at the padlock, the container's label floats in front of it (owner/trusted players only).
- Works for any locked container, including modded ones, and is completely optional.
