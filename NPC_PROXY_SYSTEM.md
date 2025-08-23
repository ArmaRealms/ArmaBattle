# NPC Proxy System

## Overview

The NPC Proxy System automatically creates proxy NPCs to replace players who disconnect during active battle events. This ensures fair gameplay by preventing players from escaping combat through disconnection while still allowing legitimate reconnections.

## Features

- **Automatic Proxy Creation**: When a player disconnects during combat, an NPC proxy is spawned in their place
- **Player State Preservation**: The NPC proxy maintains the disconnected player's name, skin, and current health
- **Combat Continuity**: The proxy can take damage and be eliminated just like a real player
- **Seamless Reconnection**: If the player reconnects while their proxy is alive, they resume where the proxy left off
- **Kill Attribution**: Proper kill tracking when proxies are eliminated
- **Multi-Provider Support**: Supports FancyNpcs with extensible architecture for Citizens and vanilla fallbacks

## Supported NPC Providers

### FancyNpcs (Primary)
- Full feature support including player skins and names
- Automatic skin fetching from player UUID
- Proper hitbox and interaction support

### Citizens (Planned)
- Placeholder implementation ready for future development
- Will provide similar functionality to FancyNpcs

### Vanilla Fallback
- Logs warnings when no NPC provider is available
- Falls back to original behavior (immediate elimination)

## Configuration

Add the following to your `config.yml`:

```yaml
battle:
  npcProxy:
    enabled: true                    # Enable/disable NPC proxy system
    provider: auto                   # auto | fancynpcs | citizens | vanilla
    onlyWhenInCombat: true          # Only create proxies for players in combat
    combatTimeoutMs: 15000          # Time in ms to track last attacker (15 seconds)
    dropItemsOnProxyDeath: false    # Whether proxy death drops items
    broadcastMessages: true         # Broadcast proxy spawn/death messages
```

## How It Works

### Player Disconnection
1. Player disconnects during an active battle event
2. System checks if player is currently fighting (not in lobby)
3. If conditions are met, creates an NPC proxy at the player's location
4. Proxy inherits player's current health, name, and skin
5. Broadcasts notification to other players

### Proxy Combat
1. Other players can attack the NPC proxy normally
2. Combat damage is tracked in the Combat Log Service
3. Proxy health decreases with damage taken
4. When proxy health reaches zero, it's eliminated from the game

### Player Reconnection
1. System detects when the original player reconnects
2. If their proxy is still alive, it's immediately despawned
3. Player is teleported to the proxy's current location
4. Player's health is set to match the proxy's current health
5. Player rejoins the battle seamlessly

### Game End Cleanup
1. When games are cancelled or ended, all remaining proxies are cleaned up
2. Plugin disable also triggers proxy cleanup to prevent orphaned NPCs

## Events

The system fires several custom events for integration:

- `NpcProxySpawnEvent`: When a proxy is created
- `NpcProxyDeathEvent`: When a proxy is eliminated  
- `NpcProxyDespawnEvent`: When a proxy is removed (player rejoined)

## Dependencies

### Required
- **TitansBattle**: The main plugin
- **FancyNpcs**: For NPC creation and management

### Optional
- **Citizens**: Future alternative NPC provider

## Installation

1. Install FancyNpcs plugin
2. Update TitansBattle to a version with NPC proxy support
3. Configure the system in `config.yml`
4. Restart the server

## Troubleshooting

### Common Issues

**NPCs not spawning:**
- Check that FancyNpcs is installed and enabled
- Verify `battle.npcProxy.enabled` is set to `true`
- Check console for NPC provider availability messages

**NPCs have wrong skin:**
- Ensure server has internet connectivity for skin fetching
- Check FancyNpcs configuration for skin settings

**Players not rejoining properly:**
- Verify the player UUID matches exactly
- Check for any permission or world-related issues

### Log Messages

Enable debug mode to see detailed proxy system messages:
- Proxy creation attempts and results
- Combat damage tracking
- Player reconnection handling
- Cleanup operations

## Architecture

The system is built with a modular architecture:

- **NpcProvider Interface**: Abstracts NPC creation across different plugins
- **NpcHandle Interface**: Represents individual NPC proxies
- **CombatLogService**: Tracks damage and determines kill attribution
- **Provider Resolver**: Automatically selects the best available NPC provider

This design allows for easy extension with additional NPC providers in the future.