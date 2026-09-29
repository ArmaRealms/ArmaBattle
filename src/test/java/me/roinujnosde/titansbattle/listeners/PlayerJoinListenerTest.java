package me.roinujnosde.titansbattle.listeners;

import me.roinujnosde.titansbattle.BaseGame;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.combat.DisconnectTrackingManager;
import me.roinujnosde.titansbattle.managers.ConfigManager;
import me.roinujnosde.titansbattle.managers.DatabaseManager;
import me.roinujnosde.titansbattle.npc.NpcProxyManager;
import me.roinujnosde.titansbattle.types.Warrior;
import me.roinujnosde.titansbattle.utils.Helper;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerJoinListenerTest {

    @Test
    public void deadProxyWithStaleHandleRejectsReconnect() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final ConfigManager configManager = mock(ConfigManager.class);
        final DatabaseManager database = mock(DatabaseManager.class);
        final DisconnectTrackingManager tracking = mock(DisconnectTrackingManager.class);
        final NpcProxyManager npcProvider = mock(NpcProxyManager.class);
        final BaseGame game = mock(BaseGame.class);
        final Warrior warrior = mock(Warrior.class);
        final Player player = mock(Player.class);
        final UUID playerId = UUID.randomUUID();
        final LivingEntity mob = mock(LivingEntity.class);
        final PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getDatabaseManager()).thenReturn(database);
        when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
        when(plugin.getNpcProxyManager()).thenReturn(npcProvider);
        when(plugin.getBaseGameFrom(player)).thenReturn(game);
        when(database.getWarrior(player)).thenReturn(warrior);
        when(player.getUniqueId()).thenReturn(playerId);
        when(event.getPlayer()).thenReturn(player);
        when(configManager.getRespawn()).thenReturn(new ArrayList<>());
        when(configManager.getClearInventory()).thenReturn(new ArrayList<>());
        when(tracking.canPlayerReturn(playerId)).thenReturn(true);
        when(tracking.hasPendingTimeout(playerId)).thenReturn(true);
        when(npcProvider.isProxyAlive(playerId)).thenReturn(false);
        when(npcProvider.getProxyByOwner(playerId)).thenReturn(Optional.of(mob));

        try (MockedStatic<Helper> ignored = mockStatic(Helper.class)) {
            new PlayerJoinListener(plugin).onJoin(event);
        }

        verify(npcProvider).isProxyAlive(playerId);
        verify(game).eliminateDisconnected(warrior, "missing-proxy-on-rejoin");
        verify(game, never()).notifyPlayerReturned(org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void livingProxyRestoresPlayerAndCancelsTimeout() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final ConfigManager configManager = mock(ConfigManager.class);
        final DisconnectTrackingManager tracking = mock(DisconnectTrackingManager.class);
        final NpcProxyManager npcProvider = mock(NpcProxyManager.class);
        final LivingEntity mob = mock(LivingEntity.class);
        final UUID playerId = UUID.randomUUID();
        final BaseGame game = mock(BaseGame.class);
        final Player player = mock(Player.class);
        final PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        final Location location = mock(Location.class);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
        when(plugin.getNpcProxyManager()).thenReturn(npcProvider);
        when(plugin.getBaseGameFrom(player)).thenReturn(game);
        when(player.getUniqueId()).thenReturn(playerId);
        when(event.getPlayer()).thenReturn(player);
        when(configManager.getRespawn()).thenReturn(new ArrayList<>());
        when(configManager.getClearInventory()).thenReturn(new ArrayList<>());
        when(tracking.canPlayerReturn(playerId)).thenReturn(true);
        when(tracking.hasPendingTimeout(playerId)).thenReturn(true);
        when(npcProvider.isProxyAlive(playerId)).thenReturn(true);
        when(npcProvider.getProxyByOwner(playerId)).thenReturn(Optional.of(mob));
        when(mob.getLocation()).thenReturn(location);
        when(player.teleport(location)).thenReturn(true);

        try (MockedStatic<Helper> ignored = mockStatic(Helper.class)) {
            new PlayerJoinListener(plugin).onJoin(event);
        }

        final InOrder order = inOrder(player, npcProvider, tracking, game);
        order.verify(player).teleport(location);
        order.verify(npcProvider).despawnProxy(playerId, "owner-rejoined");
        order.verify(tracking).clearPlayerReconnected(playerId);
        order.verify(game).notifyPlayerReturned(player);
        verify(game, never()).eliminateDisconnected(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    /**
     * A handle left in the provider map whose entity died externally must never be used to restore
     * the player, even when no timeout is pending.
     */
    @Test
    public void staleHandleIsNotRestoredEvenWithoutPendingTimeout() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final ConfigManager configManager = mock(ConfigManager.class);
        final DisconnectTrackingManager tracking = mock(DisconnectTrackingManager.class);
        final NpcProxyManager npcProvider = mock(NpcProxyManager.class);
        final BaseGame game = mock(BaseGame.class);
        final Warrior warrior = mock(Warrior.class);
        final Player player = mock(Player.class);
        final DatabaseManager database = mock(DatabaseManager.class);
        final UUID playerId = UUID.randomUUID();
        final LivingEntity mob = mock(LivingEntity.class);
        final PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getDatabaseManager()).thenReturn(database);
        when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
        when(plugin.getNpcProxyManager()).thenReturn(npcProvider);
        when(plugin.getBaseGameFrom(player)).thenReturn(game);
        when(database.getWarrior(player)).thenReturn(warrior);
        when(player.getUniqueId()).thenReturn(playerId);
        when(event.getPlayer()).thenReturn(player);
        when(configManager.getRespawn()).thenReturn(new ArrayList<>());
        when(configManager.getClearInventory()).thenReturn(new ArrayList<>());
        when(tracking.canPlayerReturn(playerId)).thenReturn(true);
        when(tracking.hasPendingTimeout(playerId)).thenReturn(false);
        when(npcProvider.isProxyAlive(playerId)).thenReturn(false);
        when(npcProvider.getProxyByOwner(playerId)).thenReturn(Optional.of(mob));
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));

        try (MockedStatic<Helper> ignored = mockStatic(Helper.class)) {
            new PlayerJoinListener(plugin).onJoin(event);
        }

        verify(player, never()).teleport(org.mockito.ArgumentMatchers.any(Location.class));
        verify(npcProvider, never()).despawnProxy(playerId, "owner-rejoined");
        verify(tracking, never()).clearPlayerReconnected(playerId);
        verify(game, never()).notifyPlayerReturned(player);
    }
}
