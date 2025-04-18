package me.roinujnosde.titansbattle.listeners;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.managers.ConfigManager;
import me.roinujnosde.titansbattle.managers.SpectateManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public class PlayerSpawnLocationListener extends TBListener {

    private final ConfigManager configManager;
    private final SpectateManager spectateManager;

    public PlayerSpawnLocationListener(TitansBattle plugin) {
        super(plugin);
        this.configManager = plugin.getConfigManager();
        this.spectateManager = plugin.getSpectateManager();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        final Player player = event.getPlayer();
        if (spectateManager.isSpectating(player)) {
            event.setSpawnLocation(configManager.getGeneralExit());
            spectateManager.remove(player);
            plugin.debug(String.format("Player %s move to lobby.", player.getName()));
        }
    }
}
