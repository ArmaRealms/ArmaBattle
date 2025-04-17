package me.roinujnosde.titansbattle.managers;

import co.aikar.commands.annotation.Optional;
import me.roinujnosde.titansbattle.BaseGameConfiguration;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.challenges.ArenaConfiguration;
import me.roinujnosde.titansbattle.games.Game;
import me.roinujnosde.titansbattle.types.Kit;
import me.roinujnosde.titansbattle.utils.SoundUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpectateManager {
    private final TitansBattle plugin;
    private final ConfigManager configManager;
    private final List<UUID> spectators = new ArrayList<>();

    public SpectateManager(TitansBattle plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void addSpectator(final Player player, Game game, @Optional ArenaConfiguration arena) {
        if (Kit.inventoryHasItems(player)) {
            player.sendMessage(plugin.getLang("clear-your-inventory"));
            return;
        }

        BaseGameConfiguration config;
        if (arena == null && game == null) {
            player.sendMessage(plugin.getLang("not-starting-or-started"));
            return;
        }
        config = (arena == null) ? game.getConfig() : arena;

        Location watchroom = config.getWatchroom();
        spectators.add(player.getUniqueId());
        if (player.teleport(watchroom)) {
            SoundUtils.playSound(SoundUtils.Type.WATCH, plugin.getConfig(), player);
            player.hidePlayer(plugin, player);
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));
        } else {
            player.sendMessage(plugin.getLang("teleport-failed"));
            plugin.debug(String.format("Failed to teleport player %s to watchroom location.", player.getName()));
        }
    }

    public void removeSpectator(final Player player) {
        if (!isSpectating(player)) {
            player.sendMessage(plugin.getLang("not-spectating"));
            return;
        }

        Location generalExit = configManager.getGeneralExit();
        spectators.remove(player.getUniqueId());
        if (player.teleport(generalExit)) {
            player.showPlayer(plugin, player);
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        } else {
            player.sendMessage(plugin.getLang("teleport-failed"));
            plugin.debug(String.format("Failed to teleport player %s to exit location after spectating.", player.getName()));
        }
    }

    public void removeAllSpectators() {
        for (UUID uuid : spectators) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                removeSpectator(player);
            }
        }
        spectators.clear();
    }

    public boolean isSpectating(Player player) {
        return spectators.contains(player.getUniqueId());
    }
}
