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
package me.roinujnosde.titansbattle.npc;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Interface for NPC providers that can create proxy NPCs for disconnected players
 *
 * @author RoinujNosde
 */
public interface NpcProvider {

    /**
     * Check if this provider is available and can create NPCs
     *
     * @return true if the provider is available
     */
    boolean isAvailable();

    /**
     * Spawn a proxy NPC for the given player
     *
     * @param player   the player to create a proxy for
     * @param location the location to spawn the NPC
     * @return the NPC handle
     */
    @NotNull NpcHandle spawnProxy(@NotNull Player player, @NotNull Location location);

    /**
     * Get the proxy NPC for a player if it exists
     *
     * @param ownerPlayerId the UUID of the player who owns the proxy
     * @return the NPC handle if it exists
     */
    @NotNull Optional<NpcHandle> getProxyByOwner(@NotNull UUID ownerPlayerId);

    /**
     * Check if a proxy NPC is alive for the given player
     *
     * @param ownerPlayerId the UUID of the player who owns the proxy
     * @return true if the proxy exists and is alive
     */
    boolean isProxyAlive(@NotNull UUID ownerPlayerId);

    /**
     * Despawn the proxy NPC for a player
     *
     * @param ownerPlayerId the UUID of the player who owns the proxy
     * @param reason        the reason for despawning
     */
    void despawnProxy(@NotNull UUID ownerPlayerId, @NotNull String reason);

    /**
     * Clean up all proxy NPCs when the plugin is disabled
     */
    void onDisable();

    /**
     * Get the name of this provider
     *
     * @return the provider name
     */
    @NotNull String getName();
}