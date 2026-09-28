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
        this.maxDisconnections = plugin.getConfig().getInt("battle.npcProxy.maxDisconnections", 3);
        this.maxOfflineTimeMs = plugin.getConfig().getLong("battle.npcProxy.maxOfflineTimeMs", 300000L); // 5 minutes default
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

        plugin.debug(String.format("Player %s disconnected %d times (max: %d)",
                playerId, record.getDisconnectionCount(), maxDisconnections));

        if (record.getDisconnectionCount() > maxDisconnections) {
            plugin.debug(String.format("Player %s exceeded max disconnections (%d), no NPC proxy will be created",
                    playerId, maxDisconnections));
            return false;
        }

        return true;
    }

    /** Start the offline timer only after the proxy has actually been spawned. */
    public boolean startOfflineTimeout(@NotNull final UUID playerId) {
        if (!disconnectRecords.containsKey(playerId)) {
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
            return true; // Player never disconnected
        }

        final boolean canReturn = record.getDisconnectionCount() <= maxDisconnections;
        plugin.debug(String.format("Player %s return check: %s (disconnections: %d, max: %d)",
                playerId, canReturn, record.getDisconnectionCount(), maxDisconnections));

        return canReturn;
    }

    /**
     * Clear tracking for a player when they successfully reconnect
     *
     * @param playerId the UUID of the player
     */
    public void clearPlayerReconnected(@NotNull final UUID playerId) {
        // Cancel any pending timeout task
        final BukkitTask timeoutTask = timeoutTasks.remove(playerId);
        if (timeoutTask != null && !timeoutTask.isCancelled()) {
            timeoutTask.cancel();
            plugin.debug("Cancelled timeout task for reconnected player " + playerId);
        }
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
        disconnectRecords.remove(playerId);

        final BukkitTask timeoutTask = timeoutTasks.remove(playerId);
        if (timeoutTask != null && !timeoutTask.isCancelled()) {
            timeoutTask.cancel();
        }

        plugin.debug("Cleared disconnect tracking for player " + playerId);
    }

    /** Clear only the timeouts belonging to the game that is ending. */
    public void clearGame(@NotNull final BaseGame game) {
        for (final Map.Entry<UUID, DisconnectRecord> entry : disconnectRecords.entrySet()) {
            if (entry.getValue().game == game) {
                clearPlayer(entry.getKey());
            }
        }
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
    public int getDisconnectionCount(@NotNull final UUID playerId) {
        final DisconnectRecord record = disconnectRecords.get(playerId);
        return record != null ? record.getDisconnectionCount() : 0;
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
        }

        // Convert milliseconds to ticks (20 ticks per second)
        final long timeoutTicks = Math.max(1L, (Math.max(0L, maxOfflineTimeMs) + 49L) / 50L);

        final BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(plugin,
                () -> handlePlayerTimeout(playerId), timeoutTicks);

        timeoutTasks.put(playerId, timeoutTask);

        plugin.debug(String.format("Scheduled timeout task for player %s in %d ms (%d ticks)",
                playerId, maxOfflineTimeMs, timeoutTicks));
    }

    /**
     * Handle when a player times out (remove their NPC and mark them as eliminated)
     *
     * @param playerId the UUID of the player who timed out
     */
    private void handlePlayerTimeout(@NotNull final UUID playerId) {
        plugin.debug("Player " + playerId + " timed out, removing NPC proxy");
        final DisconnectRecord record = disconnectRecords.get(playerId);
        if (record == null) return;
        try {
            // A reconnect must not eliminate a player even if a stale task still runs.
            final Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                return;
            }
            // Proxy cleanup must not prevent the game from advancing.
            try {
                plugin.getNpcProvider().despawnProxy(playerId, "timeout");
            } catch (final Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to remove proxy for " + playerId, e);
            }
            final Warrior warrior = plugin.getDatabaseManager().getWarrior(playerId);
            if (record.game.isParticipant(warrior)) {
                record.game.eliminateDisconnected(warrior, "timeout");
            }
        } catch (final Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to eliminate timed out player " + playerId, e);
        } finally {
            clearPlayer(playerId);
        }
    }

    /**
     * Record of disconnections for a single player
     */
    private static class DisconnectRecord {
        private final BaseGame game;
        private int disconnectionCount = 0;

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
