package me.roinujnosde.titansbattle.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Conditions;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import co.aikar.commands.annotation.Values;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.challenges.ArenaConfiguration;
import me.roinujnosde.titansbattle.challenges.Challenge;
import me.roinujnosde.titansbattle.challenges.ChallengeRequest;
import me.roinujnosde.titansbattle.challenges.GroupChallengeRequest;
import me.roinujnosde.titansbattle.dao.ConfigurationDao;
import me.roinujnosde.titansbattle.managers.ChallengeManager;
import me.roinujnosde.titansbattle.managers.DatabaseManager;
import me.roinujnosde.titansbattle.managers.SpectateManager;
import me.roinujnosde.titansbattle.types.Group;
import me.roinujnosde.titansbattle.types.Warrior;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@CommandAlias("%x1clan|duelclan|duelarclan")
public class ChallengeCommand extends BaseCommand {

    @Dependency
    private TitansBattle plugin;
    @Dependency
    private ChallengeManager challengeManager;
    @Dependency
    private DatabaseManager databaseManager;
    @Dependency
    private SpectateManager spectateManager;
    @Dependency
    private ConfigurationDao configDao;

    @Subcommand("%challenge|challenge")
    @CommandCompletion("@groups @arenas:group=true")
    @Conditions("can_challenge:group=true")
    @CommandPermission("titansbattle.challenge.group")
    @Description("{@@command.description.challenge.group}")
    @Syntax("{@@command.sintax.challenge.group}")
    public void challengeGroup(Warrior sender, @Conditions("other") Group target,
                               @Conditions("ready:group=true|empty_inventory") ArenaConfiguration arena) {
        Challenge challenge = new Challenge(plugin, arena);
        Group challenger = Objects.requireNonNull(sender.getGroup());
        GroupChallengeRequest request = new GroupChallengeRequest(challenge, challenger, target);

        challengeManager.add(request);

        sender.sendMessage(plugin.getLang("you.challenged.group", challenge, target.getName()));
        String msgRivals = plugin.getLang("challenged.your.group", challenge, challenger.getName(), challenger.getUniqueName());
        //noinspection ConstantConditions
        plugin.getGroupManager().getWarriors(target).forEach(w -> w.sendMessage(msgRivals));
        String msgOwn = plugin.getLang("your.group.challenged", challenge, challenger.getUniqueName(), target.getName());
        Set<Warrior> members = plugin.getGroupManager().getWarriors(challenger);
        members.remove(sender);
        members.forEach(w -> w.sendMessage(msgOwn));
        Player player = sender.toOnlinePlayer();
        if (player != null) player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        challenge.onChallengeJoin(sender);
    }

    @Default
    @Subcommand("%accept|accept")
    @CommandCompletion("@requests")
    @CommandPermission("titansbattle.challenge.accept")
    @Description("{@@command.description.challenge.accept}")
    @Syntax("{@@command.sintax.challenge.accept}")
    public void accept(@Conditions("is_invited") Warrior warrior, @Optional @Values("@requests") ChallengeRequest<?> challenger) {
        if (challenger == null) {
            List<ChallengeRequest<?>> requests = challengeManager.getRequestsByInvited(warrior);
            requests.get(requests.size() - 1).getChallenge().onChallengeJoin(warrior);
            return;
        }
        Player player = warrior.toOnlinePlayer();
        if (player != null) player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        challenger.getChallenge().onChallengeJoin(warrior);
    }

    @Subcommand("%exit|exit|leave")
    @CommandPermission("titansbattle.challenge.exit")
    @Conditions("participant")
    @Description("{@@command.description.challenge.exit}")
    public void leave(Player sender) {
        Warrior warrior = databaseManager.getWarrior(sender);
        sender.getActivePotionEffects().forEach(e -> sender.removePotionEffect(e.getType()));
        plugin.getBaseGameFrom(sender).onLeave(warrior);
    }

    @Subcommand("%spec|spec|spectate")
    @CommandPermission("titansbattle.challenge.watch")
    @CommandCompletion("@arenas:in_use=true")
    @Description("{@@command.description.challenge.watch}")
    public void watch(Player sender, @Conditions("can_spectate") ArenaConfiguration arena) {
        spectateManager.addSpectator(sender, null, arena);
    }

    @CatchUnknown
    @HelpCommand("%help|help")
    @Description("{@@command.description.help}")
    @Syntax("{@@command.sintax.help}")
    public void doHelp(CommandHelp help) {
        help.showHelp();
    }
}
