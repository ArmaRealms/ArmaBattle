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
import me.roinujnosde.titansbattle.combat.DisconnectTrackingManager;
import me.roinujnosde.titansbattle.hooks.viaversion.ViaVersionHook;
import me.roinujnosde.titansbattle.managers.ConfigManager;
import me.roinujnosde.titansbattle.npc.NpcProvider;
import me.roinujnosde.titansbattle.types.Kit;
import me.roinujnosde.titansbattle.types.Warrior;
import me.roinujnosde.titansbattle.utils.Helper;
import me.roinujnosde.titansbattle.utils.MessageUtils;
import me.roinujnosde.titansbattle.utils.SoundUtils;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * @author RoinujNosde
 */
public class PlayerJoinListener extends TBListener {

    private final ConfigManager cm;

    public PlayerJoinListener(@NotNull final TitansBattle plugin) {
        super(plugin);
        this.cm = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(final @NotNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        handleNpcProxyRestoration(player);
        teleportToExit(player);
        clearInventory(player);
        sendJoinMessage(player);
    }

    private void handleNpcProxyRestoration(final Player player) {
        final String playerName = player.getName();
        try {
            // Check if player is allowed to return first
            final UUID playerId = player.getUniqueId();
            final BaseGame game = plugin.getBaseGameFrom(player);
            if (game == null) {
                return;
            }

            final ViaVersionHook vvh = plugin.getViaVersionHook();
            if (vvh != null && vvh.isPlayerVersionBlocked(player, game.getConfig())) {
                final Warrior warrior = plugin.getDatabaseManager().getWarrior(player);
                game.eliminate(warrior, "incompatible-version");
                return;
            }

            final DisconnectTrackingManager dtm = plugin.getDisconnectTrackingManager();
            if (!dtm.canPlayerReturn(playerId)) {
                final Warrior warrior = plugin.getDatabaseManager().getWarrior(player);
                game.eliminate(warrior, "disconnect-limit-exceeded");
                return;
            }

            // Check if player has an active NPC proxy
            final NpcProvider np = plugin.getNpcProvider();
            np.getProxyByOwner(playerId).ifPresent(npcHandle -> {
                final Location proxyLocation = npcHandle.getLocation();
                player.teleport(proxyLocation);
                np.despawnProxy(playerId, "owner-rejoined");
                dtm.clearPlayerReconnected(playerId);
            });
        } catch (final Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to restore NPC proxy for " + playerName, e);
        }
    }

    private void sendJoinMessage(final Player player) {
        if (Helper.isWinner(player) || Helper.isKiller(player)) {
            final boolean killerJoinMessageEnabled = Helper.isKillerJoinMessageEnabled(player);
            final boolean winnerJoinMessageEnabled = Helper.isWinnerJoinMessageEnabled(player);
            final FileConfiguration config = Helper.getConfigFromWinnerOrKiller(player);
            final String playerName = player.getName();
            if (Helper.isKiller(player) && Helper.isWinner(player)) {
                if (Helper.isKillerPriority(player) && killerJoinMessageEnabled) {
                    MessageUtils.broadcastKey("killer-has-joined", config, playerName);
                    return;
                }
                if (winnerJoinMessageEnabled) {
                    MessageUtils.broadcastKey("winner-has-joined", config, playerName);
                }
                return;
            }
            if (Helper.isKiller(player) && killerJoinMessageEnabled) {
                MessageUtils.broadcastKey("killer-has-joined", config, playerName);
            }
            if (Helper.isWinner(player) && winnerJoinMessageEnabled) {
                MessageUtils.broadcastKey("winner-has-joined", config, playerName);
            }
        }
    }

    private void clearInventory(final @NotNull Player player) {
        final List<UUID> toClear = cm.getClearInventory();
        final UUID playerId = player.getUniqueId();
        if (toClear.contains(playerId)) {
            Kit.clearInventory(player);
            toClear.remove(playerId);
            cm.save();
        }
    }

    private void teleportToExit(final @NotNull Player player) {
        final UUID playerId = player.getUniqueId();
        if (!cm.getRespawn().contains(playerId)) {
            return;
        }
        if (cm.getGeneralExit() != null) {
            SoundUtils.playSound(SoundUtils.Type.TELEPORT, plugin.getConfig(), player);
            player.teleport(cm.getGeneralExit());
        } else {
            final String playerName = player.getName();
            plugin.getLogger().warning(String.format("GENERAL_EXIT is not set, it was not possible to teleport %s", playerName));
        }
        cm.getRespawn().remove(playerId);
        cm.save();
    }
}
