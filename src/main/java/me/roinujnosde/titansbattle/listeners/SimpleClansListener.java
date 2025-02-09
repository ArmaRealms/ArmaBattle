package me.roinujnosde.titansbattle.listeners;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.managers.GameManager;
import me.roinujnosde.titansbattle.types.Group;
import me.roinujnosde.titansbattle.types.Warrior;
import net.sacredlabyrinth.phaed.simpleclans.events.PreDisbandClanEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.PrePlayerKickedClanEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.PrePlayerLeaveClanEvent;
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
        plugin.getLogger().info("PrePlayerKickedClanEvent");
        gm.getCurrentGame().ifPresent(game -> {
            if (game.getConfig().isGroupMode()) {
                plugin.getLogger().info("Group mode");
                List<UUID> participants = game.getParticipants().stream().map(Warrior::getUniqueId).toList();
                UUID target = event.getClanPlayer().getUniqueId();
                plugin.getLogger().info("Target: " + target);
                if (participants.contains(target)) {
                    plugin.getLogger().info("Player is in a group event");
                    event.setCancelled(true);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrePlayerLeaveClan(PrePlayerLeaveClanEvent event) {
        plugin.getLogger().info("PrePlayerLeaveClanEvent");
        gm.getCurrentGame().ifPresent(game -> {
            if (game.getConfig().isGroupMode()) {
                plugin.getLogger().info("Group mode");
                List<UUID> participants = game.getParticipants().stream().map(Warrior::getUniqueId).toList();
                UUID target = event.getPlayer().getUniqueId();
                plugin.getLogger().info("Target: " + target);
                if (participants.contains(target)) {
                    plugin.getLogger().info("Player is in a group event");
                    event.setCancelled(true);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreDisbandClan(PreDisbandClanEvent event) {
        plugin.getLogger().info("PreDisbandClanEvent");
        gm.getCurrentGame().ifPresent(game -> {
            if (game.getConfig().isGroupMode()) {
                plugin.getLogger().info("Group mode");
                String clanTag = event.getClan().getTag();
                plugin.getLogger().info("Clan tag: " + clanTag);
                for (Map.Entry<Group, Integer> entry : game.getGroupParticipants().entrySet()) {
                    if (entry.getKey().getId().equalsIgnoreCase(clanTag)) {
                        plugin.getLogger().info("Clan is in a group event");
                        event.setCancelled(true);
                        break;
                    }
                }
            }
        });
    }

}
