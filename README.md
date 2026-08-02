# SimpleChestLocker

A Minecraft Fabric mod that allows players to lock chests and grant access to other players.

## Commands

- `/scl version` - Displays the mod version
- `/scl lock` - Lock the chest you're looking at
- `/scl unlock` - Unlock the chest you're looking at (if you own it)
- `/scl label <label>` - Label the locked chest you're looking at
- `/scl rename <label>` - Rename the label of the locked chest you're looking at
- `/scl unlabel` - Remove the label of the locked chest you're looking at
- `/scl trust <player>` - Trust a player with the chest you're looking at
- `/scl trust <player> <label>` - Trust a player with a labeled chest
- `/scl untrust <player>` - Untrust a player from the chest you're looking at
- `/scl untrust <player> <label>` - Untrust a player from a labeled chest
- `/scl players` - List trusted players for the chest you're looking at
- `/scl players <label>` - List trusted players for a labeled chest
- `/scl list` - List all your locked chests with their positions, types, and labels
- `/scl list <label>` - List trusted players for a labeled chest

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