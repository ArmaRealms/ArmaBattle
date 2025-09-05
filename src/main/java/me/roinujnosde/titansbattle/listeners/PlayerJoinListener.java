/*
 * The MIT License
 *
 * Copyright 2017 Edson Passos - edsonpassosjr@outlook.com.
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
import me.roinujnosde.titansbattle.managers.ConfigManager;
import me.roinujnosde.titansbattle.types.Kit;
import me.roinujnosde.titansbattle.types.Warrior;
import me.roinujnosde.titansbattle.utils.Helper;
import me.roinujnosde.titansbattle.utils.MessageUtils;
import me.roinujnosde.titansbattle.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * @author RoinujNosde
 */
public class PlayerJoinListener extends TBListener {

    private final ConfigManager cm;

    public PlayerJoinListener(@NotNull final TitansBattle plugin) {
        super(plugin);
        cm = plugin.getConfigManager();
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        handleNpcProxyRestoration(player);
        teleportToExit(player);
        clearInventory(player);
        sendJoinMessage(player);
    }

    private void handleNpcProxyRestoration(final Player player) {
        try {
            // Check if player is allowed to return first
            if (!plugin.getDisconnectTrackingService().canPlayerReturn(player.getUniqueId())) {
                plugin.debug("Player " + player.getName() + " exceeded disconnect limit, cannot return to game");

                final BaseGame game = plugin.getBaseGameFrom(player);
                if (game != null) {
                    final Warrior warrior = plugin.getDatabaseManager().getWarrior(player);
                    game.eliminate(warrior, "disconnect-limit-exceeded");
                    plugin.debug("Permanently eliminated player " + player.getName() + " due to disconnect limit");
                }
                return;
            }

            // Check if player has an active NPC proxy
            plugin.getNpcProvider().getProxyByOwner(player.getUniqueId()).ifPresent(npcHandle -> {
                plugin.debug("Restoring player " + player.getName() + " from NPC proxy");

                final Location proxyLocation = npcHandle.getLocation();

                player.teleport(proxyLocation);

                plugin.getNpcProvider().despawnProxy(player.getUniqueId(), "owner-rejoined");

                plugin.getDisconnectTrackingService().clearPlayerReconnected(player.getUniqueId());

                plugin.debug("Restored player " + player.getName() + " from NPC proxy at " + proxyLocation);

                // Find the active game and reinstate the player
                final BaseGame game = plugin.getBaseGameFrom(player);
                if (game != null) {
                    // Player should still be in participants list, just need to clear them from casualties
                    final Warrior warrior = plugin.getDatabaseManager().getWarrior(player);
                    game.getCasualties().remove(warrior);
                    plugin.debug("Reinstated player " + player.getName() + " in game");
                    game.setKit(warrior);
                }
            });
        } catch (final Exception e) {
            plugin.getLogger().warning("Failed to restore NPC proxy for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void sendJoinMessage(final Player player) {
        if (Helper.isWinner(player) || Helper.isKiller(player)) {
            final boolean killerJoinMessageEnabled = Helper.isKillerJoinMessageEnabled(player);
            final boolean winnerJoinMessageEnabled = Helper.isWinnerJoinMessageEnabled(player);
            final FileConfiguration config = Helper.getConfigFromWinnerOrKiller(player);
            if (Helper.isKiller(player) && Helper.isWinner(player)) {
                if (Helper.isKillerPriority(player) && killerJoinMessageEnabled) {
                    MessageUtils.broadcastKey("killer-has-joined", config, player.getName());
                    return;
                }
                if (winnerJoinMessageEnabled) {
                    MessageUtils.broadcastKey("winner-has-joined", config, player.getName());
                }
                return;
            }
            if (Helper.isKiller(player) && killerJoinMessageEnabled) {
                MessageUtils.broadcastKey("killer-has-joined", config, player.getName());
            }
            if (Helper.isWinner(player) && winnerJoinMessageEnabled) {
                MessageUtils.broadcastKey("winner-has-joined", config, player.getName());
            }
        }
    }

    private void clearInventory(final @NotNull Player player) {
        final List<UUID> toClear = cm.getClearInventory();
        if (toClear.contains(player.getUniqueId())) {
            Kit.clearInventory(player);
            toClear.remove(player.getUniqueId());
            cm.save();
        }
    }

    private void teleportToExit(final @NotNull Player player) {
        if (!cm.getRespawn().contains(player.getUniqueId())) {
            return;
        }
        if (cm.getGeneralExit() != null) {
            SoundUtils.playSound(SoundUtils.Type.TELEPORT, plugin.getConfig(), player);
            player.teleport(cm.getGeneralExit());
        } else {
            plugin.getLogger().warning(String.format("GENERAL_EXIT is not set, it was not possible to teleport %s",
                    player.getName()));
        }
        cm.getRespawn().remove(player.getUniqueId());
        cm.save();
    }
}
