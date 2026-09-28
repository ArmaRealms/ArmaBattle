package me.roinujnosde.titansbattle.combat;

import me.roinujnosde.titansbattle.BaseGame;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.managers.DatabaseManager;
import me.roinujnosde.titansbattle.npc.VanillaProvider;
import me.roinujnosde.titansbattle.types.Warrior;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
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
        final VanillaProvider provider = mock(VanillaProvider.class);
        final Warrior warrior = mock(Warrior.class);
        when(plugin.getDatabaseManager()).thenReturn(database);
        when(plugin.getNpcProvider()).thenReturn(provider);
        when(database.getWarrior(playerId)).thenReturn(warrior);
        when(game.isParticipant(warrior)).thenReturn(true);

        assertTrue(manager.trackDisconnection(playerId, game));
        assertFalse(manager.hasPendingTimeout(playerId));
        manager.startOfflineTimeout(playerId);
        assertTrue(manager.hasPendingTimeout(playerId));

        timeout.run();

        verify(provider).despawnProxy(playerId, "timeout");
        verify(game).eliminateDisconnected(warrior, "timeout");
        assertFalse(manager.hasPendingTimeout(playerId));
    }

    @Test
    public void successfulRejoinCancelsTheTimerWithoutResettingDisconnectLimit() {
        final BaseGame game = mock(BaseGame.class);
        assertTrue(manager.trackDisconnection(playerId, game));
        manager.startOfflineTimeout(playerId);

        manager.clearPlayerReconnected(playerId);

        verify(scheduledTask).cancel();
        assertFalse(manager.hasPendingTimeout(playerId));
        assertTrue(manager.trackDisconnection(playerId, game));
        assertTrue(manager.trackDisconnection(playerId, game));
        assertFalse(manager.trackDisconnection(playerId, game));
    }
}
