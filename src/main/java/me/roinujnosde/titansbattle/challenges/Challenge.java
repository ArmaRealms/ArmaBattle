package me.roinujnosde.titansbattle.challenges;

import me.roinujnosde.titansbattle.BaseGame;
import me.roinujnosde.titansbattle.BaseGameConfiguration;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.events.GroupWinEvent;
import me.roinujnosde.titansbattle.events.PlayerWinEvent;
import me.roinujnosde.titansbattle.types.Group;
import me.roinujnosde.titansbattle.types.Warrior;
import me.roinujnosde.titansbattle.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Challenge extends BaseGame {

    private Group winnerGroup;
    private List<Warrior> winners = new ArrayList<>();
    private Warrior lastCasualty;

    public Challenge(@NotNull final TitansBattle plugin, @NotNull final ArenaConfiguration config) {
        super(plugin, config);
    }

    @Override
    public @NotNull String getLang(@NotNull final String key, final Object... args) {
        String lang = null;
        if (!key.startsWith("challenge_")) {
            lang = super.getLang("challenge_" + key, args);
        }
        if (lang == null || lang.startsWith("<MISSING KEY:")) {
            lang = super.getLang(key, args);
        }
        return lang;
    }

    @Override
    protected void onLobbyEnd() {
        broadcastKey("game_started", getConfig().getPreparationTime(), getConfig().getName());
        teleportToArena(getParticipants());
        startPreparation();
    }

    @Override
    protected void processRemainingPlayers(@NotNull final Warrior warrior) {
        lastCasualty = warrior;
        if (getConfig().isGroupMode()) {
            if (getGroupParticipants().size() == 1) {
                getGroupParticipants().keySet().stream().findAny().ifPresent(g -> {
                    winnerGroup = g;
                    getParticipants().stream().filter(p -> g.isMember(p.getUniqueId())).forEach(winners::add);
                });
                finish(false);
            }
        } else if (participants.size() == 1) {
            winners = getParticipants();
            finish(false);
        }
    }

    @NotNull
    @Override
    public ArenaConfiguration getConfig() {
        return (ArenaConfiguration) config;
    }

    @Override
    public boolean shouldClearDropsOnDeath(@NotNull final Warrior warrior) {
        return isParticipant(warrior) && config.isClearItemsOnDeath();
    }

    @Override
    public boolean shouldKeepInventoryOnDeath(@NotNull final Warrior warrior) {
        return false;
    }

    @Override
    public @NotNull Collection<Warrior> getCurrentFighters() {
        return participants;
    }

    @Override
    public void finish(final boolean cancelled) {
        super.finish(cancelled);
        plugin.getChallengeManager().remove(this);
    }

    @Override
    protected void processWinners() {
        if (winnerGroup != null) {
            Bukkit.getPluginManager().callEvent(new GroupWinEvent(winnerGroup));
            getCasualties().stream().filter(p -> winnerGroup.isMember(p.getUniqueId())).forEach(winners::add);
        }
        final PlayerWinEvent event = new PlayerWinEvent(this, winners);
        Bukkit.getPluginManager().callEvent(event);
        final String winnerName = getConfig().isGroupMode() ? winnerGroup.getName() : winners.getFirst().getName();
        SoundUtils.playSound(SoundUtils.Type.VICTORY, plugin.getConfig(), winners);
        givePrizes(BaseGameConfiguration.Prize.FIRST, winnerGroup, winners);
        broadcastKey("who_won", winnerName, getLoserName());
        discordAnnounce("discord_who_won", winnerName, getLoserName());
    }

    @Override
    public void setWinner(@NotNull final Warrior warrior) {
        if (!isParticipant(warrior)) {
            return;
        }
        if (getConfig().isGroupMode()) {
            winnerGroup = getGroup(warrior);
            winners = getParticipants().stream().filter(p -> winnerGroup.isMember(p.getUniqueId())).collect(Collectors.toCollection(ArrayList::new));
        } else {
            winners.add(warrior);
        }
        finish(false);
    }

    @Override
    public boolean isInBattle(@NotNull final Warrior warrior) {
        return battle && participants.contains(warrior);
    }

    private String getLoserName() {
        if (!getConfig().isGroupMode()) {
            return lastCasualty.getName();
        }

        return Objects.requireNonNull(getGroup(lastCasualty)).getName();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final Challenge challenge = (Challenge) o;

        if (!Objects.equals(winnerGroup, challenge.winnerGroup))
            return false;
        if (!Objects.equals(winners, challenge.winners)) return false;
        return Objects.equals(lastCasualty, challenge.lastCasualty);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (winnerGroup != null ? winnerGroup.hashCode() : 0);
        result = 31 * result + (winners != null ? winners.hashCode() : 0);
        result = 31 * result + (lastCasualty != null ? lastCasualty.hashCode() : 0);
        return result;
    }
}
