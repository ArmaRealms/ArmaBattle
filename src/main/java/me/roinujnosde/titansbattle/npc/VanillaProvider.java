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

import me.roinujnosde.titansbattle.TitansBattle;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Vanilla NPC provider that spawns custom mobs as proxies
 *
 * @author RoinujNosde
 */
public class VanillaProvider implements NpcProvider {

    private static final String TITANS_BATTLE_PROXY_KEY = "titans_battle_proxy";
    private final TitansBattle plugin;
    private final Map<UUID, VanillaNpcHandle> proxies = new HashMap<>();
    private final NamespacedKey proxyKey;

    public VanillaProvider(@NotNull final TitansBattle plugin) {
        this.plugin = plugin;
        this.proxyKey = new NamespacedKey(plugin, TITANS_BATTLE_PROXY_KEY);
    }

    @Override
    public boolean isAvailable() {
        // Always available as fallback
        return true;
    }

    @Override
    @NotNull
    public NpcHandle spawnProxy(@NotNull final Player player, @NotNull final Location location, final double health) {
        try {
            // Get mob type from configuration
            final String mobTypeString = plugin.getConfig().getString("battle.npcProxy.vanilla.mobType", "ZOMBIE");
            EntityType mobType;

            try {
                mobType = EntityType.valueOf(mobTypeString.toUpperCase());
            } catch (final IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid mob type '" + mobTypeString + "' in config, using ZOMBIE as fallback");
                mobType = EntityType.ZOMBIE;
            }

            // Validate mob type is a living entity
            final Class<? extends Entity> entityClass = mobType.getEntityClass();
            if (entityClass == null || !LivingEntity.class.isAssignableFrom(entityClass)) {
                plugin.getLogger().warning("Mob type '" + mobTypeString + "' has no associated entity class, using ZOMBIE as fallback");
                mobType = EntityType.ZOMBIE;
            }

            // Spawn the mob
            final Entity entity = location.getWorld().spawnEntity(location, mobType);

            if (!(entity instanceof final LivingEntity mob)) {
                entity.remove();
                throw new RuntimeException("Spawned entity is not a LivingEntity");
            }

            // Configure the mob
            mob.setCustomName(player.getDisplayName());
            mob.setCustomNameVisible(true);
            mob.setRemoveWhenFarAway(false);
            mob.setPersistent(true);

            // Disable AI to prevent the mob from targeting players
            mob.setAI(false);

            // Set health
            mob.setHealth(health);

            // Mark as TitansBattle proxy
            mob.getPersistentDataContainer().set(proxyKey, PersistentDataType.STRING, player.getUniqueId().toString());

            final VanillaNpcHandle handle = new VanillaNpcHandle(player.getUniqueId(), mob, health);
            proxies.put(player.getUniqueId(), handle);

            plugin.getLogger().info("Spawned vanilla mob proxy (" + mobType + ") for player " + player.getName() + " at " + location);
            return handle;

        } catch (final Exception e) {
            plugin.getLogger().severe("Failed to spawn vanilla mob proxy for player " + player.getName() + ": " + e.getMessage());
            throw new RuntimeException("Failed to spawn vanilla mob proxy", e);
        }
    }

    @Override
    @NotNull
    public Optional<NpcHandle> getProxyByOwner(@NotNull final UUID ownerPlayerId) {
        return Optional.ofNullable(proxies.get(ownerPlayerId));
    }

    @Override
    public boolean isProxyAlive(@NotNull final UUID ownerPlayerId) {
        final VanillaNpcHandle handle = proxies.get(ownerPlayerId);
        return handle != null && handle.isAlive();
    }

    @Override
    public void despawnProxy(@NotNull final UUID ownerPlayerId, @NotNull final String reason) {
        final VanillaNpcHandle handle = proxies.remove(ownerPlayerId);
        if (handle != null) {
            try {
                final LivingEntity mob = handle.getMob();
                if (mob != null && !mob.isDead()) {
                    mob.remove();
                }
                plugin.getLogger().info("Despawned vanilla mob proxy for player " + ownerPlayerId + " (reason: " + reason + ")");
            } catch (final Exception e) {
                plugin.getLogger().warning("Failed to despawn vanilla mob proxy for player " + ownerPlayerId + ": " + e.getMessage());
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
        return "Vanilla";
    }

    /**
     * Check if an entity UUID belongs to a TitansBattle proxy mob
     *
     * @param entityUuid the entity UUID to check
     * @return true if it's a proxy mob
     */
    public boolean isProxyMob(@NotNull final UUID entityUuid) {
        return proxies.values().stream()
                .anyMatch(handle -> handle.getNpcUniqueId().equals(entityUuid));
    }

    /**
     * Get the owner ID of a proxy mob
     *
     * @param mobUuid the mob UUID
     * @return the owner player UUID if found
     */
    @NotNull
    public Optional<UUID> getProxyOwner(@NotNull final UUID mobUuid) {
        return proxies.entrySet().stream()
                .filter(entry -> entry.getValue().getNpcUniqueId().equals(mobUuid))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /**
     * Check if an entity is a TitansBattle proxy using persistent data
     *
     * @param entity the entity to check
     * @return true if it's a proxy mob
     */
    public boolean isProxyEntity(@NotNull final Entity entity) {
        return entity.getPersistentDataContainer().has(proxyKey, PersistentDataType.STRING);
    }

    /**
     * Get the proxy owner UUID from entity persistent data
     *
     * @param entity the entity to check
     * @return the owner UUID if found
     */
    @NotNull
    public Optional<UUID> getEntityProxyOwner(@NotNull final Entity entity) {
        final String ownerString = entity.getPersistentDataContainer().get(proxyKey, PersistentDataType.STRING);
        if (ownerString != null) {
            try {
                return Optional.of(UUID.fromString(ownerString));
            } catch (final IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in proxy entity data: " + ownerString);
            }
        }
        return Optional.empty();
    }

    /**
     * Handle for a vanilla mob proxy
     */
    static class VanillaNpcHandle implements NpcHandle {
        private final UUID ownerId;
        private final LivingEntity mob;
        private double health;
        private boolean alive = true;

        public VanillaNpcHandle(@NotNull final UUID ownerId, @NotNull final LivingEntity mob, final double health) {
            this.ownerId = ownerId;
            this.mob = mob;
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
            return mob.getUniqueId();
        }

        @Override
        public boolean isAlive() {
            return alive && mob.isValid() && !mob.isDead();
        }

        @Override
        public double getHealth() {
            return health;
        }

        @Override
        public void setHealth(final double health) {
            this.health = Math.max(0.0, health);
            if (mob.isValid() && !mob.isDead()) {
                mob.setHealth(Math.max(this.health, 0.1)); // Prevent 0 health which would kill the mob
            }

            if (this.health <= 0) {
                markDead();
            }
        }

        @Override
        @NotNull
        public Location getLocation() {
            return mob.getLocation();
        }

        @Override
        public void markDead() {
            this.alive = false;
        }

        @NotNull
        public LivingEntity getMob() {
            return mob;
        }
    }
}