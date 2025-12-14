package me.roinujnosde.titansbattle.commands.conditions;

import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.ConditionContext;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.InvalidCommandArgument;
import me.roinujnosde.titansbattle.TitansBattle;
import org.jetbrains.annotations.NotNull;

public class ChallengeCondition extends AbstractCommandCondition {
    public ChallengeCondition(TitansBattle plugin) {
        super(plugin);
    }

    @Override
    public void validateCondition(ConditionContext<BukkitCommandIssuer> context) throws InvalidCommandArgument {
        if (getChallengeManager().getChallenges().isEmpty()) {
            context.getIssuer().sendMessage(plugin.getLang("challenge-not-starting-or-started"));
            throw new ConditionFailedException();
        }
    }

    @Override
    public @NotNull String getId() {
        return "challenge";
    }
}
