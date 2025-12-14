package me.roinujnosde.titansbattle.commands.contexts;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.challenges.Challenge;
import org.jetbrains.annotations.NotNull;

public class ChallengeContext extends AbstractContextResolver<Challenge> {

    public ChallengeContext(@NotNull TitansBattle plugin) {
        super(plugin);
    }

    @Override
    public @NotNull Class<Challenge> getType() {
        return Challenge.class;
    }

    @Override
    public Challenge getContext(BukkitCommandExecutionContext context) throws InvalidCommandArgument {
        return getChallengeManager().getChallenge(context.popFirstArg());
    }
}
