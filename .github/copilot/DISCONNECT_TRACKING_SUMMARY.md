# Disconnect Tracking System Implementation Summary

## Overview

Implemented a comprehensive disconnect tracking system that allows server administrators to configure limits on player disconnections during battle events, along with automatic timeout mechanisms for offline players.

## New Features Added

### 1. **DisconnectTrackingService**
- Tracks the number of disconnections per player per event
- Enforces configurable limits on reconnections
- Manages automatic NPC proxy removal after timeout periods
- Handles proper cleanup when games end or players are eliminated

### 2. **Configuration Options**
```yaml
battle:
  npcProxy:
    # ... existing options ...
    maxDisconnections: 3        # Maximum disconnections allowed per event
    maxOfflineTimeMs: 300000    # 5 minutes timeout before NPC removal
```

### 3. **Integration Points**

#### BaseGame.onDisconnect()
- Checks disconnect limits before creating NPC proxies
- Tracks each disconnection attempt  
- Eliminates players who exceed the limit
- Enhanced logging with disconnect count information

#### PlayerJoinListener.handleNpcProxyRestoration()
- Validates if player is allowed to return before restoration
- Permanently eliminates players who exceeded limits
- Clears reconnection timers when players successfully rejoin

#### BaseGame.eliminate()
- New method to properly eliminate players from games
- Adds to casualties and processes exit logic
- Used for timeout and limit-exceeded scenarios

### 4. **Automatic Timeout System**
- Schedules tasks to remove NPC proxies after configured timeout
- Automatically eliminates timed-out players from the game
- Cancels timeout tasks when players reconnect
- Proper cleanup prevents memory leaks

## Implementation Details

### Disconnect Limit Logic
1. **First disconnect**: Creates NPC proxy (disconnect count: 1/3)
2. **Second disconnect**: Creates NPC proxy (disconnect count: 2/3)  
3. **Third disconnect**: Eliminates player immediately (disconnect count: 3/3)
4. **Reconnection attempts**: Blocked for players who exceeded limits

### Timeout Logic
1. **Player disconnects**: NPC proxy created + 5-minute timer starts
2. **Player reconnects in time**: Timer cancelled, proxy removed, player restored
3. **Timer expires**: NPC proxy removed, player eliminated from game

### State Management
- Per-player disconnect tracking with concurrent HashMap
- Automatic cleanup on game end or plugin disable  
- Timeout task management prevents orphaned schedulers
- Thread-safe operations for multiplayer scenarios

## Testing

Created comprehensive unit tests covering:
- ✅ Within-limit disconnect tracking
- ✅ Exceeding disconnect limits  
- ✅ Player return validation
- ✅ Multiple player independence
- ✅ Cleanup operations
- ✅ Edge cases and boundary conditions

## Benefits

1. **Prevents Combat Abuse**: Players can't repeatedly disconnect to avoid defeat
2. **Fair Play**: Legitimate connection issues are accommodated with reasonable limits
3. **Configurable**: Server admins can adjust limits based on their community needs
4. **Resource Efficient**: Automatic cleanup prevents memory and task leaks
5. **Transparent**: Clear logging and player feedback on disconnect attempts

## Configuration Examples

**Strict limits** (competitive servers):
```yaml
maxDisconnections: 1
maxOfflineTimeMs: 60000  # 1 minute
```

**Lenient limits** (casual servers):
```yaml  
maxDisconnections: 5
maxOfflineTimeMs: 600000  # 10 minutes
```

The system seamlessly integrates with the existing NPC proxy functionality while adding robust disconnect management capabilities.