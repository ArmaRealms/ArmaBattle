package me.roinujnosde.titansbattle.combat;

import me.roinujnosde.titansbattle.BaseGame;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.managers.DatabaseManager;
import me.roinujnosde.titansbattle.npc.NpcProxyManager;
import me.roinujnosde.titansbattle.types.Warrior;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DisconnectTrackingManagerTest {
    private final UUID playerId = UUID.randomUUID();
    private TitansBattle plugin;
    private BukkitScheduler scheduler;
    private BukkitTask scheduledTask;
    private MockedStatic<Bukkit> bukkit;
    private DisconnectTrackingManager manager;
    private Runnable timeout;

    @Before
    public void setUp() {
        plugin = mock(TitansBattle.class);
        scheduler = mock(BukkitScheduler.class);
        scheduledTask = mock(BukkitTask.class);
        final FileConfiguration config = mock(FileConfiguration.class);
        when(config.getInt("disconnect-protection.maxDisconnections", 1)).thenReturn(1);
        when(config.getLong("disconnect-protection.maxOfflineTimeMs", 60000L)).thenReturn(60000L);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("DisconnectTrackingManagerTest"));
        bukkit = org.mockito.Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong())).thenAnswer(invocation -> {
            timeout = invocation.getArgument(1);
            return scheduledTask;
        });
        manager = new DisconnectTrackingManager(plugin);
    }

    @After
    public void tearDown() {
        bukkit.close();
    }

    @Test
    public void offlineFighterIsEliminatedAfterTimeoutEvenWhenProxyIsMissing() {
        final BaseGame game = mock(BaseGame.class);
        final DatabaseManager database = mock(DatabaseManager.class);
        final NpcProxyManager provider = mock(NpcProxyManager.class);
        final Warrior warrior = mock(Warrior.class);
        when(plugin.getDatabaseManager()).thenReturn(database);
        when(plugin.getNpcProxyManager()).thenReturn(provider);
        when(database.getWarrior(playerId)).thenReturn(warrior);
        when(game.isParticipant(warrior)).thenReturn(true);

        assertTrue(manager.trackDisconnection(playerId, game));
        assertFalse(manager.hasPendingTimeout(playerId));
        assertTrue(manager.startOfflineTimeout(playerId));
        assertTrue(manager.hasPendingTimeout(playerId));
        assertEquals(1, manager.getMaxDisconnections());
        assertEquals(60, manager.getMaxOfflineTimeSeconds());
        verify(scheduler).runTaskLater(eq(plugin), any(Runnable.class), eq(1200L));

        timeout.run();

        verify(provider).despawnProxy(playerId, "timeout");
        verify(game).eliminateDisconnected(warrior, "timeout");
        assertFalse(manager.hasPendingTimeout(playerId));
    }

    @Test
    public void successfulRejoinCancelsTheTimerWithoutResettingDisconnectLimit() {
        final BaseGame game = mock(BaseGame.class);
        assertTrue(manager.trackDisconnection(playerId, game));
        assertTrue(manager.startOfflineTimeout(playerId));

        manager.clearPlayerReconnected(playerId);

        verify(scheduledTask).cancel();
        assertFalse(manager.hasPendingTimeout(playerId));
        assertEquals(1, manager.getDisconnectionCount(playerId));
        assertFalse(manager.trackDisconnection(playerId, game));
        assertEquals(2, manager.getDisconnectionCount(playerId));
    }

    @Test
    public void cancelledTimeoutCannotEliminateAReconnectedPlayerOrResetTheirLimit() {
        final BaseGame game = mock(BaseGame.class);
        final DatabaseManager database = mock(DatabaseManager.class);
        when(plugin.getDatabaseManager()).thenReturn(database);
        assertTrue(manager.trackDisconnection(playerId, game));
        assertTrue(manager.startOfflineTimeout(playerId));

        final Runnable cancelledTimeout = timeout;
        manager.clearPlayerReconnected(playerId);
        cancelledTimeout.run();

        verify(database, never()).getWarrior(playerId);
        assertEquals(1, manager.getDisconnectionCount(playerId));
        assertFalse(manager.trackDisconnection(playerId, game));
    }

    @Test
    public void timeoutForOnlinePlayerRetainsTheUsedAttempt() {
        final BaseGame game = mock(BaseGame.class);
        final org.bukkit.entity.Player player = mock(org.bukkit.entity.Player.class);
        when(player.isOnline()).thenReturn(true);
        bukkit.when(() -> Bukkit.getPlayer(playerId)).thenReturn(player);
        assertTrue(manager.trackDisconnection(playerId, game));
        assertTrue(manager.startOfflineTimeout(playerId));

        timeout.run();

        assertFalse(manager.hasPendingTimeout(playerId));
        assertEquals(1, manager.getDisconnectionCount(playerId));
        assertFalse(manager.trackDisconnection(playerId, game));
    }

    @Test
    public void timeoutCannotStartWithoutDisconnectionRecord() {
        assertFalse(manager.startOfflineTimeout(playerId));
        assertFalse(manager.hasPendingTimeout(playerId));
        verify(scheduler, org.mockito.Mockito.never()).runTaskLater(eq(plugin), any(Runnable.class), anyLong());
    }
}
