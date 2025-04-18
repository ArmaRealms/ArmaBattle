package me.roinujnosde.titansbattle.managers;

import co.aikar.commands.annotation.Optional;
import me.roinujnosde.titansbattle.BaseGameConfiguration;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.challenges.ArenaConfiguration;
import me.roinujnosde.titansbattle.games.Game;
import me.roinujnosde.titansbattle.types.Kit;
import me.roinujnosde.titansbattle.types.Warrior;
import me.roinujnosde.titansbattle.utils.SoundUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static me.roinujnosde.titansbattle.utils.SoundUtils.Type.LEAVE_GAME;

public class SpectateManager {
    private final TitansBattle plugin;
    private final ConfigManager configManager;
    private final List<UUID> spectators = new ArrayList<>();

    public SpectateManager(TitansBattle plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void addSpectator(final Set<Warrior> warriors, Game game, ArenaConfiguration arena) {
        warriors.stream()
                .map(Warrior::toOnlinePlayer)
                .forEach(player -> addSpectator(player, game, arena));
    }

    public void addSpectator(final Player player, Game game, @Optional ArenaConfiguration arena) {
        if (Kit.inventoryHasItems(player)) {
            player.sendMessage(plugin.getLang("clear-your-inventory-before-spectating"));
            return;
        }

        BaseGameConfiguration config;
        if (arena == null && game == null) {
            player.sendMessage(plugin.getLang("not-starting-or-started"));
            return;
        }
        config = (arena == null) ? game.getConfig() : arena;

        if (!player.teleport(config.getWatchroom())) {
            player.sendMessage(plugin.getLang("teleport-failed"));
            plugin.debug(String.format("Failed to teleport player %s to watchroom location.", player.getName()));
            return;
        }

        spectators.add(player.getUniqueId());
        SoundUtils.playSound(SoundUtils.Type.WATCH, plugin.getConfig(), player);
        player.hidePlayer(plugin, player);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setCollidable(false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));
        player.sendMessage(plugin.getLang("spectator-enter"));
        plugin.debug(String.format("Player %s has entered spectator mode and was teleported to the watchroom.", player.getName()));
    }

    public void removeSpectator(final Player player) {
        if (player == null) {
            plugin.debug("Player is null, cannot remove spectator.");
            return;
        }
        if (!isSpectating(player)) {
            player.sendMessage(plugin.getLang("not-spectating"));
            plugin.debug(String.format("Player %s is not a spectator, cannot remove.", player.getName()));
            return;
        }
        boolean removed = spectators.remove(player.getUniqueId());
        if (!removed) {
            plugin.debug(String.format("Player %s was not removed from spectators list.", player.getName()));
            return;
        }
        if (!player.teleport(configManager.getGeneralExit(), PlayerTeleportEvent.TeleportCause.PLUGIN)) {
            plugin.debug(String.format("Failed to teleport player %s to exit location after spectating.", player.getName()));
            player.sendMessage(plugin.getLang("teleport-failed"));
            spectators.add(player.getUniqueId());
        }
        SoundUtils.playSound(LEAVE_GAME, plugin.getConfig(), player);

        player.showPlayer(plugin, player);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setCollidable(true);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.sendMessage(plugin.getLang("spectator-exit"));
        plugin.debug(String.format("Player %s has exited spectator mode and was teleported to the exit location.", player.getName()));
    }

    public void removeAllSpectators() {
        List<UUID> copy = new ArrayList<>(spectators);
        for (UUID uuid : copy) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                removeSpectator(player);
            }
        }
    }

    public boolean isSpectating(Player player) {
        return spectators.contains(player.getUniqueId());
    }
}
