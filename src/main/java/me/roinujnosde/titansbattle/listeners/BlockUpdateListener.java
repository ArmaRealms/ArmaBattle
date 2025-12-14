package me.roinujnosde.titansbattle.listeners;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.managers.DatabaseManager;
import me.roinujnosde.titansbattle.managers.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

public class BlockUpdateListener extends TBListener {
    private final GameManager gm;
    private final DatabaseManager dm;

    public BlockUpdateListener(@NotNull TitansBattle plugin) {
        super(plugin);
        this.gm = plugin.getGameManager();
        this.dm = plugin.getDatabaseManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {
        cancel(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent event) {
        cancel(event.getPlayer(), event);
    }

    private void cancel(Player player, Cancellable event) {
        gm.getCurrentGame().ifPresent(game -> {
            if (game.getConfig().isCancelBlockInteract() && game.isInBattle(dm.getWarrior(player))) {
                event.setCancelled(true);
            }
        });
    }

}
