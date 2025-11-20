package me.roinujnosde.titansbattle.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.roinujnosde.titansbattle.TitansBattle;
import org.bukkit.entity.Player;

/**
 * Command to toggle event messages for players
 */
@CommandAlias("%titansbattle|tb")
public class MessagesCommand extends BaseCommand {

    private final TitansBattle plugin = TitansBattle.getInstance();

    @Subcommand("%messages|messages")
    @CommandPermission("titansbattle.messages")
    @Description("{@@command.description.messages}")
    public void toggleMessages(Player player) {
        boolean currentState = plugin.getDatabaseManager().hasMessagesEnabled(player.getUniqueId());
        boolean newState = !currentState;
        
        plugin.getDatabaseManager().setMessagesEnabled(player.getUniqueId(), newState);
        
        String message = newState ? 
            plugin.getLang("messages_enabled") : 
            plugin.getLang("messages_disabled");
        player.sendMessage(message);
    }

    @Subcommand("%messages|messages on")
    @CommandPermission("titansbattle.messages")
    @Description("{@@command.description.messages.on}")
    public void enableMessages(Player player) {
        plugin.getDatabaseManager().setMessagesEnabled(player.getUniqueId(), true);
        player.sendMessage(plugin.getLang("messages_enabled"));
    }

    @Subcommand("%messages|messages off")
    @CommandPermission("titansbattle.messages")
    @Description("{@@command.description.messages.off}")
    public void disableMessages(Player player) {
        plugin.getDatabaseManager().setMessagesEnabled(player.getUniqueId(), false);
        player.sendMessage(plugin.getLang("messages_disabled"));
    }
}
