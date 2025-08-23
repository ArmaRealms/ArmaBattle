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
package me.roinujnosde.titansbattle.listeners;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.npc.NpcProvider;
import me.roinujnosde.titansbattle.npc.VanillaProvider;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Listener for NPC proxy damage and death events
 *
 * @author RoinujNosde
 */
public class NpcProxyListener extends TBListener {

    public NpcProxyListener(@NotNull final TitansBattle plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNpcProxyDamage(final EntityDamageByEntityEvent event) {
        getNpcProxyOwner(event.getEntity())
                .flatMap(npcProxyOwner -> getAttackerPlayerId(event.getDamager())
                        .flatMap(attackerId -> plugin.getNpcProvider().getProxyByOwner(npcProxyOwner)))
                .ifPresent(handle -> event.setCancelled(true));
    }

    /**
     * Get the owner UUID of an NPC proxy, if the entity is one
     */
    private Optional<UUID> getNpcProxyOwner(@NotNull final Entity entity) {
        final NpcProvider provider = plugin.getNpcProvider();
        if (provider instanceof final VanillaProvider vanillaProvider) {
            if (vanillaProvider.isProxyMob(entity.getUniqueId())) {
                return vanillaProvider.getProxyOwner(entity.getUniqueId());
            }
            // Also check persistent data as fallback
            if (vanillaProvider.isProxyEntity(entity)) {
                return vanillaProvider.getEntityProxyOwner(entity);
            }
        }

        return Optional.empty();
    }

    /**
     * Get the attacking player UUID, resolving through projectiles if needed
     */
    private Optional<UUID> getAttackerPlayerId(@NotNull final Entity damager) {
        if (damager instanceof Player) {
            return Optional.of(damager.getUniqueId());
        } else if (damager instanceof final Projectile projectile) {
            if (projectile.getShooter() instanceof final Player shooter) {
                return Optional.of(shooter.getUniqueId());
            }
        }
        return Optional.empty();
    }
}