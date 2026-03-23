package me.roinujnosde.titansbattle.listeners;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.managers.DatabaseManager;
import me.roinujnosde.titansbattle.managers.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.jetbrains.annotations.NotNull;

public class ProjectileLaunchListener extends TBListener {
    private final GameManager gm;
    private final DatabaseManager dm;

    public ProjectileLaunchListener(@NotNull TitansBattle plugin) {
        super(plugin);
        this.gm = plugin.getGameManager();
        this.dm = plugin.getDatabaseManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player) {
            cancel(player, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity().getShooter() instanceof Player player) {
            cancel(player, event);
        }
    }

    private void cancel(Player player, Cancellable event) {
        gm.getCurrentGame().ifPresent(game -> {
            if ((game.isLobby() || game.isPreparation()) && game.isParticipant(dm.getWarrior(player))) {
                event.setCancelled(true);
            }
        });
    }

}
