package me.roinujnosde.titansbattle.listeners;

import me.roinujnosde.titansbattle.BaseGame;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.managers.DatabaseManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerRespawnListener extends TBListener {
    private final DatabaseManager dm;

    public PlayerRespawnListener(@NotNull TitansBattle plugin) {
        super(plugin);
        this.dm = plugin.getDatabaseManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.debug("PlayerRespawnEvent for " + event.getPlayer().getName());
        BaseGame game = plugin.getBaseGameFrom(event.getPlayer());
        if (game != null) {
            game.onRespawn(event, dm.getWarrior(event.getPlayer()));
        }
    }
}
