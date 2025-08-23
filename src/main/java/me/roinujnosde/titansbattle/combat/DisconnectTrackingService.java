/*
 * The MIT License
 *
 * Copyright 2024 Edson Passos - edsonpassosjr@outlook.com.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.roinujnosde.titansbattle.combat;

import me.roinujnosde.titansbattle.TitansBattle;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking player disconnections and managing offline timeouts
 *
 * @author RoinujNosde
 */
public class DisconnectTrackingService {

    private final TitansBattle plugin;
    private final Map<UUID, DisconnectRecord> disconnectRecords = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> timeoutTasks = new ConcurrentHashMap<>();
    private final int maxDisconnections;
    private final long maxOfflineTimeMs;

    public DisconnectTrackingService(@NotNull TitansBattle plugin) {
        this.plugin = plugin;
        // Get configuration values
        this.maxDisconnections = plugin.getConfig().getInt("battle.npcProxy.maxDisconnections", 3);
        this.maxOfflineTimeMs = plugin.getConfig().getLong("battle.npcProxy.maxOfflineTimeMs", 300000L); // 5 minutes default
    }

    /**
     * Track a player disconnection
     *
     * @param playerId the UUID of the disconnecting player
     * @return true if the player is still allowed to have an NPC proxy, false if they've exceeded the limit
     */
    public boolean trackDisconnection(@NotNull UUID playerId) {
        DisconnectRecord record = disconnectRecords.computeIfAbsent(playerId, DisconnectRecord::new);
        record.recordDisconnection();
        
        plugin.debug(String.format("Player %s disconnected %d times (max: %d)", 
                playerId, record.getDisconnectionCount(), maxDisconnections));
        
        if (record.getDisconnectionCount() > maxDisconnections) {
            plugin.debug(String.format("Player %s exceeded max disconnections (%d), no NPC proxy will be created", 
                    playerId, maxDisconnections));
            return false;
        }
        
        // Schedule timeout task for NPC removal
        scheduleTimeoutTask(playerId);
        return true;
    }

    /**
     * Check if a player is allowed to return to the game
     *
     * @param playerId the UUID of the player
     * @return true if the player can return, false if they've been banned from the event
     */
    public boolean canPlayerReturn(@NotNull UUID playerId) {
        DisconnectRecord record = disconnectRecords.get(playerId);
        if (record == null) {
            return true; // Player never disconnected
        }
        
        boolean canReturn = record.getDisconnectionCount() <= maxDisconnections;
        plugin.debug(String.format("Player %s return check: %s (disconnections: %d, max: %d)", 
                playerId, canReturn, record.getDisconnectionCount(), maxDisconnections));
        
        return canReturn;
    }

    /**
     * Clear tracking for a player when they successfully reconnect
     *
     * @param playerId the UUID of the player
     */
    public void clearPlayerReconnected(@NotNull UUID playerId) {
        // Cancel any pending timeout task
        BukkitTask timeoutTask = timeoutTasks.remove(playerId);
        if (timeoutTask != null && !timeoutTask.isCancelled()) {
            timeoutTask.cancel();
            plugin.debug("Cancelled timeout task for reconnected player " + playerId);
        }
        
        // Keep the disconnect record but mark as reconnected for this session
        DisconnectRecord record = disconnectRecords.get(playerId);
        if (record != null) {
            record.markReconnected();
        }
    }

    /**
     * Clear all tracking for a player (used when game ends or player is eliminated)
     *
     * @param playerId the UUID of the player
     */
    public void clearPlayer(@NotNull UUID playerId) {
        disconnectRecords.remove(playerId);
        
        BukkitTask timeoutTask = timeoutTasks.remove(playerId);
        if (timeoutTask != null && !timeoutTask.isCancelled()) {
            timeoutTask.cancel();
        }
        
        plugin.debug("Cleared disconnect tracking for player " + playerId);
    }

    /**
     * Clear all tracking (used when plugin disables)
     */
    public void clearAll() {
        // Cancel all pending timeout tasks
        timeoutTasks.values().forEach(task -> {
            if (!task.isCancelled()) {
                task.cancel();
            }
        });
        
        disconnectRecords.clear();
        timeoutTasks.clear();
        plugin.debug("Cleared all disconnect tracking");
    }

    /**
     * Get the disconnect count for a player
     *
     * @param playerId the UUID of the player
     * @return the number of disconnections
     */
    public int getDisconnectionCount(@NotNull UUID playerId) {
        DisconnectRecord record = disconnectRecords.get(playerId);
        return record != null ? record.getDisconnectionCount() : 0;
    }

    /**
     * Schedule a timeout task to remove the NPC after the configured time
     *
     * @param playerId the UUID of the player whose NPC should be removed
     */
    private void scheduleTimeoutTask(@NotNull UUID playerId) {
        // Cancel any existing timeout task
        BukkitTask existingTask = timeoutTasks.get(playerId);
        if (existingTask != null && !existingTask.isCancelled()) {
            existingTask.cancel();
        }
        
        // Convert milliseconds to ticks (20 ticks per second)
        long timeoutTicks = maxOfflineTimeMs / 50L;
        
        BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            handlePlayerTimeout(playerId);
        }, timeoutTicks);
        
        timeoutTasks.put(playerId, timeoutTask);
        
        plugin.debug(String.format("Scheduled timeout task for player %s in %d ms (%d ticks)", 
                playerId, maxOfflineTimeMs, timeoutTicks));
    }

    /**
     * Handle when a player times out (remove their NPC and mark them as eliminated)
     *
     * @param playerId the UUID of the player who timed out
     */
    private void handlePlayerTimeout(@NotNull UUID playerId) {
        plugin.debug("Player " + playerId + " timed out, removing NPC proxy");
        
        // Remove timeout task reference
        timeoutTasks.remove(playerId);
        
        // Remove the NPC proxy if it exists
        if (plugin.getNpcProvider().isProxyAlive(playerId)) {
            plugin.getNpcProvider().despawnProxy(playerId, "timeout");
            plugin.debug("Despawned NPC proxy for timed out player " + playerId);
        }
        
        // Mark player as eliminated due to timeout
        // Find the player's game and eliminate them
        try {
            me.roinujnosde.titansbattle.types.Warrior warrior = plugin.getDatabaseManager().getWarrior(playerId);
            if (warrior != null) {
                me.roinujnosde.titansbattle.BaseGame game = plugin.getBaseGameFrom(warrior);
                if (game != null) {
                    game.eliminate(warrior, "timeout");
                    plugin.debug("Eliminated player " + playerId + " due to timeout");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to eliminate timed out player " + playerId + ": " + e.getMessage());
        }
        
        // Clear tracking for this player
        clearPlayer(playerId);
    }

    /**
     * Record of disconnections for a single player
     */
    private static class DisconnectRecord {
        private final UUID playerId;
        private int disconnectionCount = 0;
        private long lastDisconnectTime = 0;
        private boolean currentlyReconnected = false;

        public DisconnectRecord(@NotNull UUID playerId) {
            this.playerId = playerId;
        }

        public void recordDisconnection() {
            this.disconnectionCount++;
            this.lastDisconnectTime = System.currentTimeMillis();
            this.currentlyReconnected = false;
        }

        public void markReconnected() {
            this.currentlyReconnected = true;
        }

        public int getDisconnectionCount() {
            return disconnectionCount;
        }

        public long getLastDisconnectTime() {
            return lastDisconnectTime;
        }

        public boolean isCurrentlyReconnected() {
            return currentlyReconnected;
        }
    }
}