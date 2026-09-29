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

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import me.roinujnosde.titansbattle.TitansBattle;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages the vanilla Minecraft entity proxies that stand in for disconnected players.
 *
 * <p>This is the only NPC proxy implementation. Proxies are ordinary living entities tagged with
 * persistent data, which lets the plugin recognise them and protect them from unwanted
 * interactions such as damage and knockback.
 *
 * @author RoinujNosde
 */
public final class NpcProxyManager {

    private static final String TITANS_BATTLE_PROXY_KEY = "titans_battle_proxy";
    private static final String DEFAULT_MOB_TYPE = "MANNEQUIN";

    private final TitansBattle plugin;
    private final Map<UUID, LivingEntity> proxies = new HashMap<>();
    private final NamespacedKey proxyKey;

    public NpcProxyManager(@NotNull final TitansBattle plugin) {
        this.plugin = plugin;
        this.proxyKey = new NamespacedKey(plugin, TITANS_BATTLE_PROXY_KEY);
    }

    /**
     * Spawn a vanilla proxy mob for the given player.
     *
     * @param player   the player to create a proxy for
     * @param location the location to spawn the proxy at
     * @return the spawned proxy entity
     */
    @NotNull
    public LivingEntity spawnProxy(@NotNull final Player player, @NotNull final Location location) {
        final String mobTypeString = plugin.getConfig().getString("disconnect-protection.mobType", DEFAULT_MOB_TYPE);
        final EntityType mobType = resolveMobType(mobTypeString);

        try {
            final Entity entity = location.getWorld().spawnEntity(location, mobType);
            if (!(entity instanceof final LivingEntity mob)) {
                entity.remove();
                throw new IllegalStateException("Spawned entity is not a LivingEntity");
            }

            configureProxy(mob, player);
            proxies.put(player.getUniqueId(), mob);

            plugin.getLogger().info("Spawned vanilla mob proxy (" + mobType + ") for player "
                    + player.getName() + " at " + location);
            return mob;
        } catch (final Exception e) {
            plugin.getLogger().severe("Failed to spawn vanilla mob proxy for player "
                    + player.getName() + ": " + e.getMessage());
            throw new RuntimeException("Failed to spawn vanilla mob proxy", e);
        }
    }

    private EntityType resolveMobType(@NotNull final String mobTypeString) {
        try {
            final EntityType mobType = EntityType.valueOf(mobTypeString.toUpperCase(Locale.ROOT));
            final Class<? extends Entity> entityClass = mobType.getEntityClass();
            if (entityClass == null || !LivingEntity.class.isAssignableFrom(entityClass)) {
                plugin.getLogger().warning("Mob type '" + mobTypeString
                        + "' has no living entity class, using " + DEFAULT_MOB_TYPE + " as fallback");
                return EntityType.MANNEQUIN;
            }
            return mobType;
        } catch (final IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid mob type '" + mobTypeString + "' in config, using "
                    + DEFAULT_MOB_TYPE + " as fallback");
            return EntityType.MANNEQUIN;
        }
    }

    private void configureProxy(@NotNull final LivingEntity mob, @NotNull final Player player) {
        mob.customName(player.displayName());
        mob.setCustomNameVisible(true);
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(true);
        mob.setInvulnerable(true);
        mob.setAI(false);
        mob.setCollidable(false);

        if (mob instanceof final Mannequin mannequin) {
            mannequin.setDescription(Component.empty());
            mannequin.setImmovable(true);
            mannequin.setProfile(ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
        }

        mob.getPersistentDataContainer().set(proxyKey, PersistentDataType.STRING, player.getUniqueId().toString());
    }

    /**
     * Get the proxy entity for a player if it exists.
     *
     * @param ownerPlayerId the UUID of the player who owns the proxy
     * @return the proxy entity if it exists
     */
    @NotNull
    public Optional<LivingEntity> getProxyByOwner(@NotNull final UUID ownerPlayerId) {
        return Optional.ofNullable(proxies.get(ownerPlayerId));
    }

    /**
     * Check if a proxy is alive for the given player.
     *
     * @param ownerPlayerId the UUID of the player who owns the proxy
     * @return true if the proxy exists and is alive
     */
    public boolean isProxyAlive(@NotNull final UUID ownerPlayerId) {
        final LivingEntity proxy = proxies.get(ownerPlayerId);
        return proxy != null && !proxy.isDead();
    }

    /**
     * Despawn the proxy for a player.
     *
     * @param ownerPlayerId the UUID of the player who owns the proxy
     * @param reason        the reason for despawning
     */
    public void despawnProxy(@NotNull final UUID ownerPlayerId, @NotNull final String reason) {
        final LivingEntity proxy = proxies.remove(ownerPlayerId);
        if (proxy == null) {
            return;
        }
        try {
            if (!proxy.isDead()) {
                proxy.remove();
            }
            plugin.getLogger().info("Despawned vanilla mob proxy for player " + ownerPlayerId
                    + " (reason: " + reason + ")");
        } catch (final Exception e) {
            plugin.getLogger().warning("Failed to despawn vanilla mob proxy for player "
                    + ownerPlayerId + ": " + e.getMessage());
        }
    }

    /**
     * Whether the given entity is one of the plugin's proxies.
     *
     * @param entity the entity to check
     * @return true if the entity is a proxy
     */
    public boolean isProxy(@NotNull final Entity entity) {
        return entity.getPersistentDataContainer().has(proxyKey, PersistentDataType.STRING);
    }

    /**
     * Clean up all proxies when the plugin is disabled.
     */
    public void onDisable() {
        for (final UUID ownerId : proxies.keySet().toArray(new UUID[0])) {
            despawnProxy(ownerId, "plugin-disable");
        }
        proxies.clear();
    }
}
