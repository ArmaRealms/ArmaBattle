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

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.skins.SkinData;
import me.roinujnosde.titansbattle.TitansBattle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * FancyNpcs implementation of NPC provider
 *
 * @author RoinujNosde
 */
public class FancyNpcsProvider implements NpcProvider {

    private final TitansBattle plugin;
    private final Map<UUID, FancyNpcsHandle> proxies = new HashMap<>();
    private final boolean available;

    public FancyNpcsProvider(@NotNull final TitansBattle plugin) {
        this.plugin = plugin;
        this.available = checkAvailability();
    }

    private boolean checkAvailability() {
        try {
            return Bukkit.getPluginManager().isPluginEnabled("FancyNpcs")
                    && FancyNpcsPlugin.get() != null;
        } catch (final Exception e) {
            plugin.getLogger().warning("FancyNpcs plugin found but API not available: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    @NotNull
    public NpcHandle spawnProxy(@NotNull final Player player, @NotNull final Location location, final double health) {
        if (!isAvailable()) {
            throw new IllegalStateException("FancyNpcs provider is not available");
        }

        try {
            // Create NPC data - using entity UUID for 2.7.0 API
            final UUID entityId = UUID.randomUUID();
            final NpcData npcData = new NpcData(
                    UUID.randomUUID().toString(),
                    entityId,
                    location
            );

            // Set display name to player's name
            npcData.setDisplayName(player.getDisplayName());

            // Create the NPC
            final Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(npcData);
            npc.getData().setSkinData(new SkinData(player.getUniqueId().toString(), SkinData.SkinVariant.AUTO));

            // Note: Skin setting disabled for now due to API changes in 2.7.0
            plugin.getLogger().info("Created NPC proxy for " + player.getName() + " without skin (API compatibility)");

            // Create and spawn NPC
            npc.create();
            npc.spawnForAll();
            npc.setSaveToFile(false);

            final FancyNpcsHandle handle = new FancyNpcsHandle(player.getUniqueId(), npc, health);
            proxies.put(player.getUniqueId(), handle);

            plugin.getLogger().info("Spawned NPC proxy for player " + player.getName() + " at " + location);
            return handle;

        } catch (final Exception e) {
            plugin.getLogger().severe("Failed to spawn NPC proxy for player " + player.getName() + ": " + e.getMessage());
            throw new RuntimeException("Failed to spawn NPC proxy", e);
        }
    }

    @Override
    @NotNull
    public Optional<NpcHandle> getProxyByOwner(@NotNull final UUID ownerPlayerId) {
        return Optional.ofNullable(proxies.get(ownerPlayerId));
    }

    @Override
    public boolean isProxyAlive(@NotNull final UUID ownerPlayerId) {
        final FancyNpcsHandle handle = proxies.get(ownerPlayerId);
        return handle != null && handle.isAlive();
    }

    @Override
    public void despawnProxy(@NotNull final UUID ownerPlayerId, @NotNull final String reason) {
        final FancyNpcsHandle handle = proxies.remove(ownerPlayerId);
        if (handle != null) {
            try {
                handle.getNpc().removeForAll();
                plugin.getLogger().info("Despawned NPC proxy for player " + ownerPlayerId + " (reason: " + reason + ")");
            } catch (final Exception e) {
                plugin.getLogger().warning("Failed to despawn NPC proxy for player " + ownerPlayerId + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        for (final UUID ownerId : proxies.keySet().toArray(new UUID[0])) {
            despawnProxy(ownerId, "plugin-disable");
        }
        proxies.clear();
    }

    @Override
    @NotNull
    public String getName() {
        return "FancyNpcs";
    }

    /**
     * Check if an entity UUID belongs to a TitansBattle proxy NPC
     *
     * @param entityUuid the entity UUID to check
     * @return true if it's a proxy NPC
     */
    public boolean isProxyNpc(@NotNull final UUID entityUuid) {
        return proxies.values().stream()
                .anyMatch(handle -> handle.getNpcUniqueId().equals(entityUuid));
    }

    /**
     * Get the owner ID of a proxy NPC
     *
     * @param npcUuid the NPC UUID
     * @return the owner player UUID if found
     */
    @NotNull
    public Optional<UUID> getProxyOwner(@NotNull final UUID npcUuid) {
        return proxies.entrySet().stream()
                .filter(entry -> entry.getValue().getNpcUniqueId().equals(npcUuid))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /**
     * Handle for a FancyNpc proxy
     */
    static class FancyNpcsHandle implements NpcHandle {
        private final UUID ownerId;
        private final Npc npc;
        private double health;
        private boolean alive = true;

        public FancyNpcsHandle(@NotNull final UUID ownerId, @NotNull final Npc npc, final double health) {
            this.ownerId = ownerId;
            this.npc = npc;
            this.health = health;
        }

        @Override
        @NotNull
        public UUID getOwnerPlayerId() {
            return ownerId;
        }

        @Override
        @NotNull
        public UUID getNpcUniqueId() {
            return UUID.fromString(npc.getData().getId());
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public double getHealth() {
            return health;
        }

        @Override
        public void setHealth(final double health) {
            this.health = Math.max(0.0, health);
            if (this.health <= 0) {
                markDead();
            }
        }

        @Override
        @NotNull
        public Location getLocation() {
            return npc.getData().getLocation();
        }

        @Override
        public void markDead() {
            this.alive = false;
        }

        @NotNull
        public Npc getNpc() {
            return npc;
        }
    }
}