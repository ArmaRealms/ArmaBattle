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
import org.bukkit.Bukkit;
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

    public VanillaProvider(@NotNull TitansBattle plugin) {
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
    public NpcHandle spawnProxy(@NotNull Player player, @NotNull Location location, double health) {
        try {
            // Get mob type from configuration
            String mobTypeString = plugin.getConfig().getString("battle.npcProxy.vanilla.mobType", "ZOMBIE");
            EntityType mobType;
            
            try {
                mobType = EntityType.valueOf(mobTypeString.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid mob type '" + mobTypeString + "' in config, using ZOMBIE as fallback");
                mobType = EntityType.ZOMBIE;
            }

            // Validate mob type is a living entity
            if (!LivingEntity.class.isAssignableFrom(mobType.getEntityClass())) {
                plugin.getLogger().warning("Mob type '" + mobTypeString + "' is not a living entity, using ZOMBIE as fallback");
                mobType = EntityType.ZOMBIE;
            }

            // Spawn the mob
            Entity entity = location.getWorld().spawnEntity(location, mobType);
            
            if (!(entity instanceof LivingEntity)) {
                entity.remove();
                throw new RuntimeException("Spawned entity is not a LivingEntity");
            }

            LivingEntity mob = (LivingEntity) entity;
            
            // Configure the mob
            mob.setCustomName(player.getDisplayName());
            mob.setCustomNameVisible(true);
            mob.setRemoveWhenFarAway(false);
            mob.setPersistent(true);
            
            // Set health
            if (mob.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                double maxHealth = Math.max(health, 1.0); // Ensure at least 1 HP
                mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
                mob.setHealth(health);
            }

            // Mark as TitansBattle proxy
            mob.getPersistentDataContainer().set(proxyKey, PersistentDataType.STRING, player.getUniqueId().toString());

            VanillaNpcHandle handle = new VanillaNpcHandle(player.getUniqueId(), mob, health);
            proxies.put(player.getUniqueId(), handle);

            plugin.getLogger().info("Spawned vanilla mob proxy (" + mobType + ") for player " + player.getName() + " at " + location);
            return handle;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to spawn vanilla mob proxy for player " + player.getName() + ": " + e.getMessage());
            throw new RuntimeException("Failed to spawn vanilla mob proxy", e);
        }
    }

    @Override
    @NotNull
    public Optional<NpcHandle> getProxyByOwner(@NotNull UUID ownerPlayerId) {
        return Optional.ofNullable(proxies.get(ownerPlayerId));
    }

    @Override
    public boolean isProxyAlive(@NotNull UUID ownerPlayerId) {
        VanillaNpcHandle handle = proxies.get(ownerPlayerId);
        return handle != null && handle.isAlive();
    }

    @Override
    public void despawnProxy(@NotNull UUID ownerPlayerId, @NotNull String reason) {
        VanillaNpcHandle handle = proxies.remove(ownerPlayerId);
        if (handle != null) {
            try {
                LivingEntity mob = handle.getMob();
                if (mob != null && !mob.isDead()) {
                    mob.remove();
                }
                plugin.getLogger().info("Despawned vanilla mob proxy for player " + ownerPlayerId + " (reason: " + reason + ")");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to despawn vanilla mob proxy for player " + ownerPlayerId + ": " + e.getMessage());
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
        return "Vanilla";
    }

    /**
     * Check if an entity UUID belongs to a TitansBattle proxy mob
     *
     * @param entityUuid the entity UUID to check
     * @return true if it's a proxy mob
     */
    public boolean isProxyMob(@NotNull UUID entityUuid) {
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
    public Optional<UUID> getProxyOwner(@NotNull UUID mobUuid) {
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
    public boolean isProxyEntity(@NotNull Entity entity) {
        return entity.getPersistentDataContainer().has(proxyKey, PersistentDataType.STRING);
    }

    /**
     * Get the proxy owner UUID from entity persistent data
     *
     * @param entity the entity to check
     * @return the owner UUID if found
     */
    @NotNull
    public Optional<UUID> getEntityProxyOwner(@NotNull Entity entity) {
        String ownerString = entity.getPersistentDataContainer().get(proxyKey, PersistentDataType.STRING);
        if (ownerString != null) {
            try {
                return Optional.of(UUID.fromString(ownerString));
            } catch (IllegalArgumentException e) {
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

        public VanillaNpcHandle(@NotNull UUID ownerId, @NotNull LivingEntity mob, double health) {
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
        public void setHealth(double health) {
            this.health = Math.max(0.0, health);
            if (mob.isValid() && !mob.isDead()) {
                // Update actual mob health
                if (mob.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                    double maxHealth = Math.max(this.health, 1.0);
                    mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
                }
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