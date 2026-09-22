package me.roinujnosde.titansbattle.hooks.papi;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.managers.DatabaseManager;
import me.roinujnosde.titansbattle.types.Warrior;
import me.roinujnosde.titansbattle.types.Winners;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TBExpansionTest {

    private static final String GAME = "torneio_2x2";
    private static final String PLACEHOLDER = "last_winner_players_" + GAME;
    private DatabaseManager database;
    private YamlConfiguration config;
    private List<Winners> history;
    private TBExpansion expansion;

    @Before
    public void setUp() {
        TitansBattle plugin = mock(TitansBattle.class);
        database = mock(DatabaseManager.class);
        config = new YamlConfiguration();
        history = new ArrayList<>();
        when(plugin.getDatabaseManager()).thenReturn(database);
        when(plugin.getConfig()).thenReturn(config);
        when(database.getWinners()).thenReturn(history);
        expansion = new TBExpansion(plugin);
    }

    @Test
    public void formatsAnyTeamSizeWithoutPlayerContext() {
        assertEquals("", expansion.onRequest(null, PLACEHOLDER));
        record(1, GAME);
        assertEquals("", expansion.onRequest(null, PLACEHOLDER));
        record(2, GAME, "Alice");
        assertEquals("Alice", expansion.onRequest(null, PLACEHOLDER));
        record(3, GAME, "Alice", "Bob");
        assertEquals("Alice e Bob", expansion.onRequest(null, PLACEHOLDER));
        record(4, GAME, "Alice", "Bob", "Carol", "Dave");
        assertEquals("Alice, Bob, Carol e Dave", expansion.onRequest(null, PLACEHOLDER));
    }

    @Test
    public void usesConfiguredSeparatorsAndReflectsConfigChanges() {
        record(1, GAME, "Alice", "Bob", "Carol");
        config.set("placeholders.winner-players.separator", " / ");
        config.set("placeholders.winner-players.last-separator", " & ");
        assertEquals("Alice / Bob & Carol", expansion.onRequest(null, PLACEHOLDER));
        config.set("placeholders.winner-players.separator", "");
        config.set("placeholders.winner-players.last-separator", "");
        assertEquals("AliceBobCarol", expansion.onRequest(null, PLACEHOLDER));
    }

    @Test
    public void findsLatestWinnersForRequestedEventRegardlessOfHistoryOrder() {
        record(3, GAME, "Alice", "Bob");
        record(1, GAME, "OldWinner");
        record(4, "another_event", "OtherWinner");
        assertEquals("Alice e Bob", expansion.onRequest(null, PLACEHOLDER));
        assertEquals("", expansion.onRequest(null, "last_winner_players_unknown"));
        assertEquals("Alice e Bob", expansion.onRequest(null, "last_winner_players_TORNEIO_2X2"));
    }

    @Test
    public void keepsLegacyWinnerFormattingIndependentOfConfiguredSeparators() {
        Winners winners = record(1, GAME, "Alice", "Bob");
        winners.setWinnerGroup(GAME, "Champions");
        config.set("placeholders.winner-players.last-separator", " & ");
        assertEquals("Alice e Bob", expansion.onRequest(null, "last_winner_" + GAME));
        assertEquals("Champions", expansion.onRequest(null, "last_winner_group_" + GAME));
    }

    private Winners record(long time, String game, String... names) {
        Winners winners = new Winners(new Date(time));
        List<UUID> ids = new ArrayList<>();
        for (String name : names) {
            UUID id = UUID.randomUUID();
            Warrior warrior = mock(Warrior.class);
            when(warrior.getName()).thenReturn(name);
            when(database.getWarrior(id)).thenReturn(warrior);
            ids.add(id);
        }
        winners.setWinners(game, ids);
        history.add(winners);
        return winners;
    }
}
