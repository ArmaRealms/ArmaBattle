package me.roinujnosde.titansbattle.games;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.combat.DisconnectTrackingManager;
import me.roinujnosde.titansbattle.managers.ConfigManager;
import me.roinujnosde.titansbattle.managers.DatabaseManager;
import me.roinujnosde.titansbattle.managers.GameManager;
import me.roinujnosde.titansbattle.managers.GroupManager;
import me.roinujnosde.titansbattle.npc.VanillaProvider;
import me.roinujnosde.titansbattle.types.GameConfiguration;
import me.roinujnosde.titansbattle.types.Warrior;
import me.roinujnosde.titansbattle.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * A fighter removed by the disconnect/reconnect handling is online but out of the tournament, so it
 * must not be seated in {@code waitingThirdPlace}, must not keep {@code isParticipant} true, and must
 * be reported to the rest of the plugin as having genuinely left the event.
 */
public class EliminationTournamentGameThirdPlaceTest {

    @Test
    public void rejectedReconnectDoesNotQualifyForThirdPlace() throws Exception {
        final Fixture fixture = new Fixture();
        fixture.startSemiFinals();

        // The player rejoined (online) but was rejected and eliminated by the join listener.
        try (ServerStubs ignored = withServerStubs()) {
            fixture.game.eliminateDisconnected(fixture.warrior, "missing-proxy-on-rejoin");
        }

        assertFalse(fixture.game.isParticipant(fixture.warrior));
    }

    @Test
    public void tournamentDeathStillQualifiesForThirdPlace() throws Exception {
        final Fixture fixture = new Fixture();
        fixture.startSemiFinals();

        // An actual in-tournament death keeps the player waiting for the third-place fight.
        fixture.markCasualty();
        fixture.markDead();
        try (ServerStubs ignored = withServerStubs()) {
            fixture.game.eliminate(fixture.warrior, "killed");
        }

        assertTrue(fixture.game.isParticipant(fixture.warrior));
    }

    @Test
    public void rejectedReconnectIsReportedAsHavingLeftTheEvent() throws Exception {
        final Fixture fixture = new Fixture();
        fixture.startSemiFinals();
        // The fighter died, then dropped the connection during the grace period and was rejected on
        // reconnect: they are in casualties and are online, but their run is already over.
        fixture.markCasualty();
        fixture.markDead();
        try (ServerStubs ignored = withServerStubs()) {
            fixture.game.eliminateDisconnected(fixture.warrior, "missing-proxy-on-rejoin");
        }

        // Such a player must be teleported out and fire PlayerExitGameEvent like a normal exit.
        // Otherwise they stay parked in waitingThirdPlace waiting for a fight nobody will start.
        assertTrue(fixture.game.teleportedToExitOnExit);
        assertTrue(fixture.game.firedExitGameEventOnExit);
    }

    @Test
    public void tournamentDeathIsNotReportedAsHavingLeftTheEvent() throws Exception {
        final Fixture fixture = new Fixture();
        fixture.startSemiFinals();
        fixture.markCasualty();
        fixture.markDead();
        try (ServerStubs ignored = withServerStubs()) {
            fixture.game.eliminate(fixture.warrior, "killed");
        }

        // A real semi-final death must not be teleported out nor reported as an exit, otherwise other
        // plugins would yank the player out of the arena while they wait for the third-place fight.
        assertFalse(fixture.game.teleportedToExitOnExit);
        assertFalse(fixture.game.firedExitGameEventOnExit);
    }

    @Test
    public void thirdPlaceEligibilityIsScopedToTheSemiFinalRound() throws Exception {
        final Fixture fixture = new Fixture();
        fixture.startSemiFinals();
        fixture.markCasualty();
        fixture.markDead();
        // Still in the quarter-final round, so nobody qualifies for a third-place fight yet.
        fixture.setDuels(1);

        assertTrue(fixture.game.shouldTeleportToExitOnExit(fixture.warrior));
        assertTrue(fixture.game.shouldFireExitGameEventOnExit(fixture.warrior));
    }

    /**
     * Eliminating a fighter fires a {@code PlayerQuitEvent} and schedules a delayed cleanup, both of
     * which reach out to the running server. The scheduler is left idle so the deferred third-place
     * {@code removeIf} has not fired yet when the assertions run.
     */
    private static ServerStubs withServerStubs() {
        final BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTaskLater(any(), any(Runnable.class), anyLong())).thenReturn(mock(BukkitTask.class));
        final MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        return new ServerStubs(bukkit, mockStatic(SoundUtils.class));
    }

    private record ServerStubs(MockedStatic<Bukkit> bukkit, MockedStatic<SoundUtils> sounds) implements AutoCloseable {
        @Override
        public void close() {
            sounds.close();
            bukkit.close();
        }
    }

    /**
     * Records the exit hooks, which are only meaningful while {@code processPlayerExit} is running:
     * the {@code removeDuelist} call afterwards shrinks the duel list and changes what the round
     * looks like to a later assertion.
     */
    private static final class RecordingGame extends EliminationTournamentGame {
        private boolean teleportedToExitOnExit;
        private boolean firedExitGameEventOnExit;
        private Warrior recordedWarrior;

        private RecordingGame(final TitansBattle plugin, final GameConfiguration config) {
            super(plugin, config);
        }

        @Override
        protected boolean shouldTeleportToExitOnExit(final Warrior warrior) {
            final boolean result = super.shouldTeleportToExitOnExit(warrior);
            if (warrior.equals(recordedWarrior)) {
                teleportedToExitOnExit = result;
            }
            return result;
        }

        @Override
        protected boolean shouldFireExitGameEventOnExit(final Warrior warrior) {
            final boolean result = super.shouldFireExitGameEventOnExit(warrior);
            if (warrior.equals(recordedWarrior)) {
                firedExitGameEventOnExit = result;
            }
            return result;
        }
    }

    /** Drives a 4-player game into the semifinal round with {@code warrior} as a current duelist. */
    private static final class Fixture {
        private final TitansBattle plugin = mock(TitansBattle.class);
        private final GameConfiguration config = mock(GameConfiguration.class);
        private final RecordingGame game;
        private final Warrior warrior = mock(Warrior.class);
        private final Player player = mock(Player.class);
        private final UUID playerId = UUID.randomUUID();

        private Fixture() {
            final ConfigManager configManager = mock(ConfigManager.class);
            final DatabaseManager database = mock(DatabaseManager.class);
            final GroupManager groupManager = mock(GroupManager.class);
            final VanillaProvider npcProvider = mock(VanillaProvider.class);
            final DisconnectTrackingManager tracking = mock(DisconnectTrackingManager.class);
            final FileConfiguration pluginConfig = mock(FileConfiguration.class);
            when(plugin.getConfigManager()).thenReturn(configManager);
            when(plugin.getDatabaseManager()).thenReturn(database);
            when(plugin.getGroupManager()).thenReturn(groupManager);
            when(plugin.getGameManager()).thenReturn(mock(GameManager.class));
            when(plugin.getNpcProvider()).thenReturn(npcProvider);
            when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
            when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
            when(plugin.getConfig()).thenReturn(pluginConfig);
            when(pluginConfig.getString(anyString(), anyString())).thenReturn("message");
            when(configManager.getClearInventory()).thenReturn(new ArrayList<>());
            when(configManager.getRespawn()).thenReturn(new ArrayList<>());
            when(config.isGroupMode()).thenReturn(false);
            when(config.isUseKits()).thenReturn(false);
            when(warrior.getUniqueId()).thenReturn(playerId);
            when(warrior.getName()).thenReturn("duelist");
            when(warrior.toOnlinePlayer()).thenReturn(player);
            when(player.isOnline()).thenReturn(true);
            when(player.isDead()).thenReturn(false);
            when(npcProvider.getProxyByOwner(playerId)).thenReturn(Optional.empty());
            this.game = new RecordingGame(plugin, config);
            this.game.recordedWarrior = warrior;
        }

        @SuppressWarnings("unchecked")
        private void startSemiFinals() throws Exception {
            setDuels(2);
            // The lobby is over and the battle is running, so deaths reach the third-place logic.
            writeField("lobby", false);
            writeField("battle", true);
            final List<Warrior> participants = (List<Warrior>) readField("participants");
            participants.add(warrior);
        }

        /**
         * Fills the duel list. Only the first duel contains {@code warrior}, so eliminating them
         * lowers the count by exactly one, as it would in a real round.
         */
        @SuppressWarnings("unchecked")
        private void setDuels(final int count) throws Exception {
            final List<Duel<Warrior>> duels = (List<Duel<Warrior>>) readField("playerDuelists");
            duels.clear();
            for (int i = 0; i < count; i++) {
                duels.add(new Duel<>(i == 0 ? warrior : mock(Warrior.class), mock(Warrior.class)));
            }
        }

        private Object readField(final String name) throws Exception {
            return findField(name).get(game);
        }

        @SuppressWarnings("unchecked")
        private void markCasualty() throws Exception {
            ((Set<Warrior>) readField("casualties")).add(warrior);
        }

        /** A fighter that just lost a semi-final is lying dead on the ground. */
        private void markDead() {
            when(player.isDead()).thenReturn(true);
        }

        private void writeField(final String name, final Object value) throws Exception {
            findField(name).set(game, value);
        }

        /** {@code lobby} and {@code battle} live on BaseGame, so walk up the hierarchy. */
        private static Field findField(final String name) throws NoSuchFieldException {
            Class<?> type = EliminationTournamentGame.class;
            while (type != null) {
                try {
                    final Field field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (final NoSuchFieldException e) {
                    type = type.getSuperclass();
                }
            }
            throw new NoSuchFieldException(name);
        }
    }
}
