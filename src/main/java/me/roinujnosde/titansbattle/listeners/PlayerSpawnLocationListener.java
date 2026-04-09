package me.roinujnosde.titansbattle.listeners;

import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.managers.ConfigManager;
import me.roinujnosde.titansbattle.managers.SpectateManager;
import net.kyori.adventure.identity.Identity;
import org.bukkit.event.EventHandler;

import java.util.Optional;
import java.util.UUID;

public class PlayerSpawnLocationListener extends TBListener {

    private final ConfigManager configManager;
    private final SpectateManager spectateManager;

    public PlayerSpawnLocationListener(final TitansBattle plugin) {
        super(plugin);
        this.configManager = plugin.getConfigManager();
        this.spectateManager = plugin.getSpectateManager();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSpawnLocation(final AsyncPlayerSpawnLocationEvent event) {
        final Optional<UUID> optionalUUID = event.getConnection().getAudience().get(Identity.UUID);
        if (optionalUUID.isEmpty()) {
            plugin.debug("Player UUID not found in spawn location event, skipping.");
            return;
        }
        final UUID uuid = optionalUUID.get();
        if (spectateManager.isSpectating(uuid)) {
            event.setSpawnLocation(configManager.getGeneralExit());
            spectateManager.remove(uuid);
            plugin.debug(String.format("Player %s move to lobby.", uuid));
        }
    }
}
