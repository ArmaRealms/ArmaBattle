package me.roinujnosde.titansbattle.listeners;

import me.roinujnosde.titansbattle.BaseGame;
import me.roinujnosde.titansbattle.BaseGameConfiguration;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.games.EliminationTournamentGame;
import me.roinujnosde.titansbattle.managers.DatabaseManager;
import me.roinujnosde.titansbattle.managers.GroupManager;
import me.roinujnosde.titansbattle.types.Warrior;
import me.roinujnosde.titansbattle.utils.Helper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

public class EntityDamageListener extends TBListener {
    private final DatabaseManager dm;
    private final GroupManager gm;

    public EntityDamageListener(@NotNull TitansBattle plugin) {
        super(plugin);
        this.dm = plugin.getDatabaseManager();
        this.gm = plugin.getGroupManager();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamageLowest(EntityDamageEvent event) {
        boolean disableFfMessages = plugin.getConfig().getBoolean("disable-ff-messages", true);
        if (disableFfMessages && isParticipant(event.getEntity())) {
            // Cancelling so other plugins don't display messages such as "can't hit an ally" during the game
            event.setCancelled(true);
        }
    }

    //mcMMO's listener is on HIGHEST and ignoreCancelled = true, this will run before
    //Aurellium / Auraskills is on HIGH
    @EventHandler(priority = EventPriority.NORMAL)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player defender)) {
            return;
        }

        BaseGame game = plugin.getBaseGameFrom(defender);
        if (game == null) {
            return;
        }

        if (!game.isInBattle(dm.getWarrior(defender))) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(false);
        if (event instanceof EntityDamageByEntityEvent) {
            processEntityDamageByEntityEvent(event, defender, game);
        }
    }

    private void processEntityDamageByEntityEvent(EntityDamageEvent event, Player defender, BaseGame game) {
        EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
        Player attacker = Helper.getPlayerAttackerOrKiller(subEvent.getDamager());
        if (!isDamageTypeAllowed(subEvent, game)) {
            event.setCancelled(true);
            return;
        }

        if (attacker != null) {
            Warrior warrior = dm.getWarrior(attacker);
            if (!game.getConfig().isPvP() || !game.isInBattle(warrior)) {
                event.setCancelled(true);
                return;
            }
        }

        if (attacker == null) return;

        if (game instanceof EliminationTournamentGame tournament && tournament.getConfig().isBoxing()) {
            if (tournament.onHit(attacker, defender)) {
                event.setDamage(0.0);
            } else {
                event.setDamage(1000.0);
            }
        }

        if (game.getConfig().isGroupMode() && gm != null) {
            event.setCancelled(gm.sameGroup(defender.getUniqueId(), attacker.getUniqueId()));
        }
    }

    private boolean isDamageTypeAllowed(EntityDamageByEntityEvent event, BaseGame game) {
        BaseGameConfiguration config = game.getConfig();
        if (event.getDamager() instanceof Projectile) {
            return config.isRangedDamage();
        } else {
            return config.isMeleeDamage();
        }
    }

    private boolean isParticipant(Entity entity) {
        if (entity instanceof Player player) {
            return plugin.getBaseGameFrom(player) != null;
        }
        return false;
    }

}
