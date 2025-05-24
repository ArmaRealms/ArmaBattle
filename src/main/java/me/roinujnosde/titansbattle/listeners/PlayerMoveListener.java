package me.roinujnosde.titansbattle.listeners;

import me.roinujnosde.titansbattle.BaseGame;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.games.Sumo;
import me.roinujnosde.titansbattle.managers.DatabaseManager;
import me.roinujnosde.titansbattle.types.Warrior;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerMoveListener extends TBListener {
    private final DatabaseManager dm;

    public PlayerMoveListener(@NotNull TitansBattle plugin) {
        super(plugin);
        this.dm = plugin.getDatabaseManager();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        if (event.hasChangedBlock()) {
            Player player = event.getPlayer();
            BaseGame game = plugin.getBaseGameFrom(player);
            if (game == null) return;
            Warrior warrior = dm.getWarrior(player);
            if (game.getCurrentFighters().contains(warrior)) {
                if (game.isInBattle(warrior) && game instanceof Sumo && event.getTo().getY() <= game.getConfig().getMinimumYHeight()) {
                    player.setHealth(0);
                }
                if (game.isPreparation()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
