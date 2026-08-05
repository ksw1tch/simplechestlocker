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

## Configuration

Located at `config/simplechestlocker.json`:
- `protectFromHoppers` - Prevent hoppers from extracting items from locked containers (default: true)
- `protectFromPistons` - Prevent pistons from moving/destroying locked containers (default: true)
- `protectFromExplosions` - Prevent explosions from destroying locked containers (default: true)
