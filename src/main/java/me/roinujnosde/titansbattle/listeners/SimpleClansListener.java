package me.roinujnosde.titansbattle.listeners;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.managers.GameManager;
import me.roinujnosde.titansbattle.types.Group;
import me.roinujnosde.titansbattle.types.Warrior;
import net.sacredlabyrinth.phaed.simpleclans.events.PreDisbandClanEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.PrePlayerKickedClanEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SimpleClansListener extends TBListener {
    private final GameManager gm;

    public SimpleClansListener(TitansBattle plugin) {
        super(plugin);
        this.gm = plugin.getGameManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrePlayerKickedClan(PrePlayerKickedClanEvent event) {
        gm.getCurrentGame().ifPresent(game -> {
            if (game.getConfig().isGroupMode()) {
                List<UUID> participants = game.getParticipants().stream().map(Warrior::getUniqueId).toList();
                UUID target = event.getClanPlayer().getUniqueId();
                if (participants.contains(target)) {
                    event.setCancelled(true);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreDisbandClan(PreDisbandClanEvent event) {
        gm.getCurrentGame().ifPresent(game -> {
            if (game.getConfig().isGroupMode()) {
                String clanTag = event.getClan().getTag();
                for (Map.Entry<Group, Integer> entry : game.getGroupParticipants().entrySet()) {
                    if (entry.getKey().getId().equalsIgnoreCase(clanTag)) {
                        event.setCancelled(true);
                        break;
                    }
                }
            }
        });
    }

}
