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
import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import de.oliver.fancynpcs.api.utils.SkinFetcher;
import me.roinujnosde.titansbattle.TitansBattle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * FancyNpcs implementation of NPC provider
 *
 * @author RoinujNosde
 */
public class FancyNpcsProvider implements NpcProvider {

    private static final String TITANS_BATTLE_PROXY_KEY = "titans_battle_proxy";
    private final TitansBattle plugin;
    private final Map<UUID, FancyNpcsHandle> proxies = new HashMap<>();
    private boolean available;

    public FancyNpcsProvider(@NotNull TitansBattle plugin) {
        this.plugin = plugin;
        this.available = checkAvailability();
    }

    private boolean checkAvailability() {
        try {
            return Bukkit.getPluginManager().isPluginEnabled("FancyNpcs")
                    && FancyNpcsPlugin.get() != null;
        } catch (Exception e) {
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
    public NpcHandle spawnProxy(@NotNull Player player, @NotNull Location location, double health) {
        if (!isAvailable()) {
            throw new IllegalStateException("FancyNpcs provider is not available");
        }

        try {
            // Create NPC data
            NpcData npcData = new NpcData(
                    UUID.randomUUID().toString(),
                    EntityType.PLAYER,
                    location
            );

            // Set display name to player's name
            npcData.setDisplayName(player.getDisplayName());

            // Create the NPC
            Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(npcData);

            // Set skin asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    SkinFetcher.fetchSkin(player.getUniqueId()).thenAccept(skin -> {
                        if (skin != null) {
                            npcData.setSkin(skin);
                            npc.updateForAll();
                        }
                    });
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to fetch skin for NPC proxy of " + player.getName() + ": " + e.getMessage());
                }
            });

            // Mark as TitansBattle proxy
            npc.getData().setPersistentData(TITANS_BATTLE_PROXY_KEY, PersistentDataType.STRING, player.getUniqueId().toString());

            // Create and spawn NPC
            npc.create();
            npc.spawnForAll();

            FancyNpcsHandle handle = new FancyNpcsHandle(player.getUniqueId(), npc, health);
            proxies.put(player.getUniqueId(), handle);

            plugin.getLogger().info("Spawned NPC proxy for player " + player.getName() + " at " + location);
            return handle;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to spawn NPC proxy for player " + player.getName() + ": " + e.getMessage());
            throw new RuntimeException("Failed to spawn NPC proxy", e);
        }
    }

    @Override
    @NotNull
    public Optional<NpcHandle> getProxyByOwner(@NotNull UUID ownerPlayerId) {
        return Optional.ofNullable(proxies.get(ownerPlayerId));
    }

    @Override
    public boolean isProxyAlive(@NotNull UUID ownerPlayerId) {
        FancyNpcsHandle handle = proxies.get(ownerPlayerId);
        return handle != null && handle.isAlive();
    }

    @Override
    public void despawnProxy(@NotNull UUID ownerPlayerId, @NotNull String reason) {
        FancyNpcsHandle handle = proxies.remove(ownerPlayerId);
        if (handle != null) {
            try {
                handle.getNpc().removeForAll();
                plugin.getLogger().info("Despawned NPC proxy for player " + ownerPlayerId + " (reason: " + reason + ")");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to despawn NPC proxy for player " + ownerPlayerId + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        for (UUID ownerId : proxies.keySet().toArray(new UUID[0])) {
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
    public boolean isProxyNpc(@NotNull UUID entityUuid) {
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
    public Optional<UUID> getProxyOwner(@NotNull UUID npcUuid) {
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

        public FancyNpcsHandle(@NotNull UUID ownerId, @NotNull Npc npc, double health) {
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
            return npc.getData().getUuid();
        }

        @Override
        public boolean isAlive() {
            return alive && npc.getData().isCreated();
        }

        @Override
        public double getHealth() {
            return health;
        }

        @Override
        public void setHealth(double health) {
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