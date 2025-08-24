# NPC Proxy System Manual Test Scenarios

## Prerequisites
- TitansBattle plugin installed
- FancyNpcs plugin installed and working
- At least 2 players for testing
- A configured game/arena

## Test Scenarios

### Scenario 1: Player disconnects during combat - NPC proxy spawned
1. Start a battle with 2+ players
2. Wait for battle to begin (not in lobby phase)
3. Player A attacks Player B (both in combat)
4. Player B disconnects while in combat
5. **Expected Result**: 
   - NPC proxy spawns at Player B's location
   - NPC has Player B's name and skin
   - NPC has Player B's current health from when they disconnected
   - Message broadcasts: "{PlayerB} disconnected during battle! A proxy has taken their place."

### Scenario 2: NPC proxy takes damage and dies
1. Complete Scenario 1 setup
2. Player A attacks the NPC proxy of Player B
3. Deal enough damage to kill the proxy (reduce health to 0)
4. **Expected Result**:
   - NPC proxy dies and despawns
   - Player B is eliminated from the game
   - Player A is credited as the killer
   - Death message appears as if Player A killed Player B
   - Game continues normally

### Scenario 3: Player reconnects while proxy is alive
1. Complete Scenario 1 setup (Player B disconnected, NPC proxy spawned)
2. Player B reconnects before the NPC proxy dies
3. **Expected Result**:
   - NPC proxy immediately despawns
   - Player B is teleported to the proxy's current location
   - Player B's health is set to the proxy's current health
   - Player B is back in the game and can continue fighting

### Scenario 4: Player disconnects outside of combat - no proxy
1. Start a battle with 2+ players
2. Keep players separated (no combat initiated)
3. Player disconnects while not in combat
4. **Expected Result**:
   - No NPC proxy spawned
   - Player is eliminated normally
   - Normal disconnect behavior occurs

### Scenario 5: Game ends with active proxies
1. Start a battle with 3+ players
2. One player disconnects in combat (proxy spawned)
3. Admin cancels the game OR game ends naturally
4. **Expected Result**:
   - All NPC proxies are cleaned up and despawned
   - No NPCs remain in the world
   - Game ends normally

### Scenario 6: Plugin disable/reload with active proxies
1. Complete Scenario 1 setup (have active NPC proxy)
2. Disable/reload the plugin
3. **Expected Result**:
   - All NPC proxies are cleanly despawned
   - No NPCs remain in the world
   - No errors in console

### Scenario 7: Configuration disabled
1. Set `battle.npcProxy.enabled: false` in config
2. Reload plugin
3. Try Scenario 1 (player disconnect in combat)
4. **Expected Result**:
   - No NPC proxy spawned
   - Player is killed normally (original behavior)

### Scenario 8: FancyNpcs not available
1. Stop FancyNpcs plugin
2. Reload TitansBattle
3. Try Scenario 1 (player disconnect in combat)
4. **Expected Result**:
   - Warning in console about NPC provider not available
   - Player is killed normally (fallback behavior)
   - No errors or crashes

## Configuration Testing

### Test different config values:
```yaml
battle:
  npcProxy:
    enabled: true/false
    combatTimeoutMs: 5000  # Test with short timeout
    broadcastMessages: true/false
```

## Expected Log Messages

- On plugin enable: "Using FancyNpcs for NPC proxy system" or "No NPC plugin available..."
- On proxy spawn: "Spawned NPC proxy for player {name} at {location}"
- On proxy despawn: "Despawned NPC proxy for player {uuid} (reason: {reason})"
- On player rejoin: "Restored player {name} from NPC proxy with {health} health at {location}"

## Troubleshooting

If tests fail, check:
1. FancyNpcs plugin version compatibility
2. Console for any errors during NPC creation
3. Player permissions
4. Game configuration settings
5. Network connectivity during NPC skin fetching