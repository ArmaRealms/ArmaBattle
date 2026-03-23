package me.roinujnosde.titansbattle.commands.completions;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.InvalidCommandArgument;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.challenges.ArenaConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ArenasCompletion extends AbstractCompletion {
    public ArenasCompletion(TitansBattle plugin) {
        super(plugin);
    }

    @Override
    public @NotNull String getId() {
        return "arenas";
    }

    @Override
    public Collection<String> getCompletions(BukkitCommandCompletionContext context) throws InvalidCommandArgument {
        List<String> inUse = getChallengeManager().getRequests().stream()
                .map(cr -> cr.getChallenge().getConfig().getName())
                .toList();

        if (context.hasConfig("in_use")) {
            return inUse;
        }

        List<String> arenas = getArenaList();
        arenas.removeAll(inUse);
        return arenas;
    }

    public List<String> getArenaList() {
        return new ArrayList<>(getConfigurationDao().getConfigurations(ArenaConfiguration.class).stream()
                .filter(Objects::nonNull)
                .map(ArenaConfiguration::getName)
                .toList());
    }
}
