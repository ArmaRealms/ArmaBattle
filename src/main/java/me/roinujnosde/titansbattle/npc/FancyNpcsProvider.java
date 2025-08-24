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
            return Bukkit.getPluginManager().isPluginEnabled("FancyNpcs") && FancyNpcsPlugin.get() != null;
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
    public NpcHandle spawnProxy(@NotNull final Player player, @NotNull final Location location) {
        if (!isAvailable()) {
            throw new IllegalStateException("FancyNpcs provider is not available");
        }

        try {
            // Create NPC data - using entity UUID for 2.7.0 API
            final UUID entityId = UUID.randomUUID();
            final NpcData npcData = new NpcData(UUID.randomUUID().toString(), entityId, location);

            // Set display name to player's name
            npcData.setDisplayName(player.getDisplayName());

            // Create the NPC
            final Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(npcData);
            npc.getData().setSkinData(new SkinData(player.getUniqueId().toString(), SkinData.SkinVariant.AUTO));

            plugin.getLogger().info("Created NPC proxy for " + player.getName() + " with entity ID " + entityId);

            // Create and spawn NPC
            npc.create();
            npc.spawnForAll();
            npc.setSaveToFile(false);

            final FancyNpcsHandle handle = new FancyNpcsHandle(player.getUniqueId(), npc);
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
        return handle != null && handle.npc().getData() != null;
    }

    @Override
    public void despawnProxy(@NotNull final UUID ownerPlayerId, @NotNull final String reason) {
        final FancyNpcsHandle handle = proxies.remove(ownerPlayerId);
        if (handle != null) {
            try {
                handle.npc().removeForAll();
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
     * Handle for a FancyNpc proxy
     */
    record FancyNpcsHandle(UUID ownerId, Npc npc) implements NpcHandle {
        FancyNpcsHandle(@NotNull final UUID ownerId, @NotNull final Npc npc) {
            this.ownerId = ownerId;
            this.npc = npc;
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
        @NotNull
        public Location getLocation() {
            return npc.getData().getLocation();
        }

        @Override
        @NotNull
        public Npc npc() {
            return npc;
        }
    }
}