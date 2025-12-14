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

public class CanSpectateCondition extends AbstractParameterCondition<ArenaConfiguration> {

    public CanSpectateCondition(TitansBattle plugin) {
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
                .map(Challenge::getConfig).noneMatch(config -> config.equals(value));
        if (matches) {
            cec.getIssuer().sendMessage(plugin.getLang("no.challenge.in.arena"));
            throw new ConditionFailedException();
        }
        if (!value.locationsSet()) {
            cc.getIssuer().sendMessage(plugin.getLang("this.arena.isnt.ready"));
            throw new ConditionFailedException();
        }
    }

    @Override
    public @NotNull String getId() {
        return "can_spectate";
    }

    @Contract(" -> new")
    private @NotNull String getArenas() {
        return String.join(", ", getConfigurationDao().getConfigurations(ArenaConfiguration.class).stream()
                .filter(BaseGameConfiguration::isGroupMode)
                .map(ArenaConfiguration::getName)
                .toList());
    }
}
