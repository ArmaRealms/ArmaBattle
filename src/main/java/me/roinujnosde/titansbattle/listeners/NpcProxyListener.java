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

import me.roinujnosde.titansbattle.BaseGame;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.combat.CombatLogService;
import me.roinujnosde.titansbattle.npc.FancyNpcsProvider;
import me.roinujnosde.titansbattle.npc.VanillaProvider;
import me.roinujnosde.titansbattle.npc.NpcHandle;
import me.roinujnosde.titansbattle.npc.NpcProvider;
import me.roinujnosde.titansbattle.npc.event.NpcProxyDeathEvent;
import me.roinujnosde.titansbattle.types.Warrior;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Listener for NPC proxy damage and death events
 *
 * @author RoinujNosde
 */
public class NpcProxyListener extends TBListener {

    private final CombatLogService combatLogService;

    public NpcProxyListener(@NotNull final TitansBattle plugin) {
        super(plugin);
        this.combatLogService = plugin.getCombatLogService();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNpcProxyDamage(final EntityDamageByEntityEvent event) {
        // Check if the damaged entity is an NPC proxy
        final UUID npcProxyOwner = getNpcProxyOwner(event.getEntity());
        if (npcProxyOwner == null) {
            return;
        }

        // Get the attacking player
        final UUID attackerId = getAttackerPlayerId(event.getDamager());
        if (attackerId == null) {
            plugin.debug("NPC proxy damaged by non-player entity, ignoring");
            return;
        }

        // Verify both players are in the same game
        final Warrior ownerWarrior = plugin.getDatabaseManager().getWarrior(npcProxyOwner);
        final Warrior attackerWarrior = plugin.getDatabaseManager().getWarrior(attackerId);
        final BaseGame ownerGame = plugin.getBaseGameFrom(ownerWarrior);
        final BaseGame attackerGame = plugin.getBaseGameFrom(attackerWarrior);

        if (ownerGame == null || attackerGame != ownerGame) {
            plugin.debug("NPC proxy owner and attacker not in same game, cancelling damage");
            event.setCancelled(true);
            return;
        }

        // Record the damage in combat log
        final double damage = event.getFinalDamage();
        combatLogService.recordDamageToProxy(npcProxyOwner, attackerId, damage);

        // Update NPC health and check for death
        final NpcProvider npcProvider = plugin.getNpcProvider();
        npcProvider.getProxyByOwner(npcProxyOwner).ifPresent(npcHandle -> {
            final double newHealth = npcHandle.getHealth() - damage;
            npcHandle.setHealth(newHealth);

            plugin.debug(String.format("NPC proxy for %s took %.2f damage, health now %.2f", 
                    npcProxyOwner, damage, newHealth));

            // Check if NPC should die
            if (newHealth <= 0) {
                handleNpcProxyDeath(npcProxyOwner, npcHandle, attackerId);
            }
        });
    }

    private void handleNpcProxyDeath(@NotNull final UUID ownerId, @NotNull final NpcHandle npcHandle, @Nullable final UUID killerId) {
        plugin.debug("Handling NPC proxy death for owner " + ownerId);

        // Mark NPC as dead
        npcHandle.markDead();

        // Get the last attacker from combat log (more reliable than direct killer)
        final Optional<UUID> lastAttacker = combatLogService.getLastAttacker(ownerId);
        final UUID finalKillerId = lastAttacker.orElse(killerId);

        // Fire proxy death event
        final NpcProxyDeathEvent event = new NpcProxyDeathEvent(ownerId, npcHandle, finalKillerId);
        Bukkit.getPluginManager().callEvent(event);

        // Find the game and eliminate the owner
        final Warrior ownerWarrior = plugin.getDatabaseManager().getWarrior(ownerId);
        final BaseGame game = plugin.getBaseGameFrom(ownerWarrior);
        if (game != null) {
            final Warrior killerWarrior = finalKillerId != null ? plugin.getDatabaseManager().getWarrior(finalKillerId) : null;
            plugin.debug(String.format("Eliminating proxy owner %s, killer: %s", 
                    ownerWarrior.getName(), killerWarrior != null ? killerWarrior.getName() : "unknown"));
            
            // Eliminate the player through the game's death system
            game.onDeath(ownerWarrior, killerWarrior);
        }

        // Clean up
        plugin.getNpcProvider().despawnProxy(ownerId, "proxy-death");
        combatLogService.clear(ownerId);
    }

    /**
     * Get the owner UUID of an NPC proxy, if the entity is one
     */
    @Nullable
    private UUID getNpcProxyOwner(@NotNull final Entity entity) {
        final NpcProvider provider = plugin.getNpcProvider();
        
        if (provider instanceof final FancyNpcsProvider fancyProvider) {
            if (fancyProvider.isProxyNpc(entity.getUniqueId())) {
                return fancyProvider.getProxyOwner(entity.getUniqueId()).orElse(null);
            }
        } else if (provider instanceof final VanillaProvider vanillaProvider) {
            if (vanillaProvider.isProxyMob(entity.getUniqueId())) {
                return vanillaProvider.getProxyOwner(entity.getUniqueId()).orElse(null);
            }
            // Also check persistent data as fallback
            if (vanillaProvider.isProxyEntity(entity)) {
                return vanillaProvider.getEntityProxyOwner(entity).orElse(null);
            }
        }
        
        return null;
    }

    /**
     * Get the attacking player UUID, resolving through projectiles if needed
     */
    @Nullable
    private UUID getAttackerPlayerId(@NotNull final Entity damager) {
        if (damager instanceof Player) {
            return damager.getUniqueId();
        } else if (damager instanceof final Projectile projectile) {
            if (projectile.getShooter() instanceof final Player shooter) {
                return shooter.getUniqueId();
            }
        }
        return null;
    }
}