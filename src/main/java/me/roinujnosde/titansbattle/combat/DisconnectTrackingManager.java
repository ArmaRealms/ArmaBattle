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

import me.roinujnosde.titansbattle.BaseGame;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.types.Warrior;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Service for tracking player disconnections and managing offline timeouts
 *
 * @author RoinujNosde
 */
public class DisconnectTrackingManager {

    private final TitansBattle plugin;
    private final Map<UUID, DisconnectRecord> disconnectRecords = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> timeoutTasks = new ConcurrentHashMap<>();
    private final int maxDisconnections;
    private final long maxOfflineTimeMs;

    public DisconnectTrackingManager(@NotNull final TitansBattle plugin) {
        this.plugin = plugin;
        // Get configuration values
        this.maxDisconnections = plugin.getConfig().getInt("disconnect-protection.maxDisconnections", 1);
        this.maxOfflineTimeMs = plugin.getConfig().getLong("disconnect-protection.maxOfflineTimeMs", 60000L); // 60 seconds default
    }

    /**
     * Track a player disconnection
     *
     * @param playerId the UUID of the disconnecting player
     * @param game the game in which the player disconnected
     * @return true if the player is still allowed to have an NPC proxy, false if they've exceeded the limit
     */
    public boolean trackDisconnection(@NotNull final UUID playerId, @NotNull final BaseGame game) {
        final DisconnectRecord record = disconnectRecords.computeIfAbsent(playerId, k -> new DisconnectRecord(game));
        record.recordDisconnection();

        final int attempts = record.getDisconnectionCount();
        plugin.debugDisconnect(String.format(
                "Recorded disconnect for player %s; attempt=%d/%d",
                playerId, attempts, maxDisconnections));
        if (attempts > maxDisconnections) {
            plugin.debugDisconnect(String.format(
                    "Disconnect limit exceeded for player %s (%d/%d); protection denied",
                    playerId, attempts, maxDisconnections));
            return false;
        }

        plugin.debugDisconnect(String.format(
                "Protection granted for player %s (%d/%d)",
                playerId, attempts, maxDisconnections));
        return true;
    }

    /** Start the offline timer only after the proxy has actually been spawned. */
    public boolean startOfflineTimeout(@NotNull final UUID playerId) {
        if (!disconnectRecords.containsKey(playerId)) {
            plugin.debugDisconnect("Offline timeout not started for player " + playerId
                    + ": missing disconnect record");
            return false;
        }
        scheduleTimeoutTask(playerId);
        return true;
    }

    /**
     * Check if a player is allowed to return to the game
     *
     * @param playerId the UUID of the player
     * @return true if the player can return, false if they've been banned from the event
     */
    public boolean canPlayerReturn(@NotNull final UUID playerId) {
        final DisconnectRecord record = disconnectRecords.get(playerId);
        if (record == null) {
            plugin.debugDisconnect("Return check for player " + playerId
                    + ": no disconnect record; allowed");
            return true; // Player never disconnected
        }

        final boolean canReturn = record.getDisconnectionCount() <= maxDisconnections;
        plugin.debugDisconnect(String.format(
                "Return check for player %s: allowed=%s, disconnections=%d/%d",
                playerId, canReturn, record.getDisconnectionCount(), maxDisconnections));

        return canReturn;
    }

    /**
     * Clear tracking for a player when they successfully reconnect
     *
     * @param playerId the UUID of the player
     */
    public void clearPlayerReconnected(@NotNull final UUID playerId) {
        // Invalidate any already-queued callback, even if cancelling its task is too late.
        final DisconnectRecord record = disconnectRecords.get(playerId);
        if (record != null) {
            record.invalidateTimeout();
        }
        final BukkitTask timeoutTask = timeoutTasks.remove(playerId);
        if (timeoutTask != null) {
            if (!timeoutTask.isCancelled()) {
                timeoutTask.cancel();
            }
        }
        plugin.debugDisconnect(String.format(
                "Player %s reconnected; timeoutCancelled=%s; protectionsUsed=%d/%d",
                playerId, timeoutTask != null, record == null ? 0 : record.getDisconnectionCount(), maxDisconnections));
    }

    public boolean hasPendingTimeout(@NotNull final UUID playerId) {
        return timeoutTasks.containsKey(playerId);
    }

    /**
     * Clear all tracking for a player (used when game ends or player is eliminated)
     *
     * @param playerId the UUID of the player
     */
    public void clearPlayer(@NotNull final UUID playerId) {
        final DisconnectRecord removed = disconnectRecords.remove(playerId);

        final BukkitTask timeoutTask = timeoutTasks.remove(playerId);
        if (timeoutTask != null && !timeoutTask.isCancelled()) {
            timeoutTask.cancel();
        }

        plugin.debugDisconnect(String.format(
                "Cleared tracking for player %s; recordPresent=%s; pendingTimeout=%s",
                playerId, removed != null, timeoutTask != null));
    }

    /** Clear only the timeouts belonging to the game that is ending. */
    public void clearGame(@NotNull final BaseGame game) {
        int cleared = 0;
        for (final Map.Entry<UUID, DisconnectRecord> entry : disconnectRecords.entrySet()) {
            if (entry.getValue().game == game) {
                clearPlayer(entry.getKey());
                cleared++;
            }
        }
        plugin.debugDisconnect("Finished event cleanup; cleared disconnect records=" + cleared);
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
        plugin.debugDisconnect("Plugin shutdown cleanup; cleared all disconnect tracking and timeouts");
    }

    /**
     * Get the disconnect count for a player
     *
     * @param playerId the UUID of the player
     * @return the number of disconnections
     */
    public int getDisconnectionCount(@NotNull final UUID playerId) {
        final DisconnectRecord record = disconnectRecords.get(playerId);
        return record != null ? record.getDisconnectionCount() : 0;
    }

    /**
     * The maximum number of protected disconnections allowed per event.
     *
     * @return the configured maximum
     */
    public int getMaxDisconnections() {
        return maxDisconnections;
    }

    /**
     * The effective offline protection time, in seconds, players have to return.
     *
     * @return the configured offline time in seconds
     */
    public long getMaxOfflineTimeSeconds() {
        return Math.max(0L, maxOfflineTimeMs) / 1000L;
    }

    /**
     * Schedule a timeout task to remove the NPC after the configured time
     *
     * @param playerId the UUID of the player whose NPC should be removed
     */
    protected void scheduleTimeoutTask(@NotNull final UUID playerId) {
        // Cancel any existing timeout task
        final BukkitTask existingTask = timeoutTasks.get(playerId);
        if (existingTask != null && !existingTask.isCancelled()) {
            existingTask.cancel();
            plugin.debugDisconnect("Replaced existing offline timeout for player " + playerId);
        }

        final DisconnectRecord record = disconnectRecords.get(playerId);
        if (record == null) {
            throw new IllegalStateException("No disconnect record for " + playerId);
        }
        final long generation = record.nextTimeoutGeneration();

        // Convert milliseconds to ticks (20 ticks per second).
        final long timeoutTicks = Math.max(1L, (Math.max(0L, maxOfflineTimeMs) + 49L) / 50L);
        final BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(plugin,
                () -> handlePlayerTimeout(playerId, record, generation), timeoutTicks);
        timeoutTasks.put(playerId, timeoutTask);

        plugin.debugDisconnect(String.format(
                "Offline timeout scheduled for player %s; durationMs=%d, ticks=%d, generation=%d",
                playerId, maxOfflineTimeMs, timeoutTicks, generation));
    }

    /**
     * Handle when a player times out (remove their NPC and mark them as eliminated)
     *
     * @param playerId the UUID of the player who timed out
     */
    private void handlePlayerTimeout(@NotNull final UUID playerId,
                                     @NotNull final DisconnectRecord record, final long generation) {
        // Ignore cancelled timers, older disconnects and records from games that have ended.
        if (disconnectRecords.get(playerId) != record || record.timeoutGeneration != generation) {
            plugin.debugDisconnect(String.format(
                    "Ignored stale timeout for player %s; expectedGeneration=%d, currentGeneration=%d, recordActive=%s",
                    playerId, generation, record.timeoutGeneration, disconnectRecords.get(playerId) == record));
            return;
        }
        // A reconnect must not eliminate the player or reset their used protection count.
        final Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            plugin.debugDisconnect("Timeout fired for online player " + playerId
                    + "; cancelling timer without resetting disconnect count");
            clearPlayerReconnected(playerId);
            return;
        }

        plugin.debugDisconnect(String.format(
                "Offline timeout fired for player %s after %d ms; checking elimination",
                playerId, maxOfflineTimeMs));
        try {
            // Proxy cleanup must not prevent the game from advancing.
            try {
                plugin.getNpcProxyManager().despawnProxy(playerId, "timeout");
            } catch (final Exception e) {
                plugin.debugDisconnect(Level.WARNING, "Failed to remove proxy for player " + playerId, e);
            }
            final Warrior warrior = plugin.getDatabaseManager().getWarrior(playerId);
            if (record.game.isParticipant(warrior)) {
                plugin.debugDisconnect("Player " + playerId + " is still a participant; eliminating on timeout");
                record.game.eliminateDisconnected(warrior, BaseGame.ELIMINATION_REASON_TIMEOUT);
            } else {
                plugin.debugDisconnect("Ignoring timeout elimination for player " + playerId
                        + ": no longer an event participant");
            }
        } catch (final Exception e) {
            plugin.debugDisconnect(Level.SEVERE, "Failed to eliminate timed out player " + playerId, e);
        } finally {
            if (disconnectRecords.get(playerId) == record && record.timeoutGeneration == generation) {
                clearPlayer(playerId);
            }
        }
    }

    /**
     * Record of disconnections for a single player
     */
    private static class DisconnectRecord {
        private final BaseGame game;
        private int disconnectionCount = 0;
        private long timeoutGeneration = 0;

        private long nextTimeoutGeneration() {
            return ++timeoutGeneration;
        }

        private void invalidateTimeout() {
            timeoutGeneration++;
        }

        private DisconnectRecord(final BaseGame game) {
            this.game = game;
        }

        public void recordDisconnection() {
            this.disconnectionCount++;
        }

        public int getDisconnectionCount() {
            return disconnectionCount;
        }
    }
}
