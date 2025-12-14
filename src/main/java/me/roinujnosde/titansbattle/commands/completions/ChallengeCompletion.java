package me.roinujnosde.titansbattle.commands.completions;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.InvalidCommandArgument;
import me.roinujnosde.titansbattle.TitansBattle;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ChallengeCompletion extends AbstractCompletion {
    public ChallengeCompletion(TitansBattle plugin) {
        super(plugin);
    }

    @Override
    public @NotNull String getId() {
        return "challenge";
    }

    @Override
    public Collection<String> getCompletions(BukkitCommandCompletionContext context) throws InvalidCommandArgument {
        return getChallengeManager().getChallenges().stream()
                .map(cr -> cr.getConfig().getName())
                .toList();
    }
}
