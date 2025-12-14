package me.roinujnosde.titansbattle.commands.conditions;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.ConditionContext;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.InvalidCommandArgument;
import me.roinujnosde.titansbattle.BaseGameConfiguration;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.challenges.ArenaConfiguration;
import me.roinujnosde.titansbattle.challenges.Challenge;
import me.roinujnosde.titansbattle.challenges.ChallengeRequest;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class ArenaReadyCondition extends AbstractParameterCondition<ArenaConfiguration> {

    public ArenaReadyCondition(TitansBattle plugin) {
        super(plugin);
    }

    @Override
    public @NotNull Class<ArenaConfiguration> getType() {
        return ArenaConfiguration.class;
    }

    @Override
    public void validateCondition(ConditionContext<BukkitCommandIssuer> cc,
                                  BukkitCommandExecutionContext cec,
                                  ArenaConfiguration value) throws InvalidCommandArgument {
        if (value == null) {
            cc.getIssuer().sendMessage(plugin.getLang("arena.does.not.exist", getArenas()));
            throw new ConditionFailedException();
        }
        boolean matches = getChallengeManager().getRequests().stream().map(ChallengeRequest::getChallenge)
                .map(Challenge::getConfig).anyMatch(config -> config.equals(value));
        if (matches) {
            cec.getIssuer().sendMessage(plugin.getLang("arena.in.use"));
            throw new ConditionFailedException();
        }
        if (!value.locationsSet()) {
            cc.getIssuer().sendMessage(plugin.getLang("this.arena.isnt.ready"));
            throw new ConditionFailedException();
        }
        boolean groupMode = Boolean.parseBoolean(cc.getConfigValue("group", "false"));
        if (groupMode != value.isGroupMode()) {
            cc.getIssuer().sendMessage(plugin.getLang("group.mode.not.supported"));
            throw new ConditionFailedException();
        }
    }

    @Override
    public @NotNull String getId() {
        return "ready";
    }

    @Contract(" -> new")
    private @NotNull String getArenas() {
        return String.join(", ", getConfigurationDao().getConfigurations(ArenaConfiguration.class).stream()
                .filter(BaseGameConfiguration::isGroupMode)
                .map(ArenaConfiguration::getName)
                .toList());
    }
}
