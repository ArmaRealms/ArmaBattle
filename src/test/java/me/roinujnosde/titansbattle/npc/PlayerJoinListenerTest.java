package me.roinujnosde.titansbattle.npc;

import me.roinujnosde.titansbattle.BaseGame;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.combat.DisconnectTrackingManager;
import me.roinujnosde.titansbattle.listeners.PlayerJoinListener;
import me.roinujnosde.titansbattle.managers.ConfigManager;
import me.roinujnosde.titansbattle.managers.DatabaseManager;
import me.roinujnosde.titansbattle.types.Warrior;
import me.roinujnosde.titansbattle.utils.Helper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerJoinListenerTest {

    @Test
    public void deadProxyWithStaleHandleRejectsReconnect() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final ConfigManager configManager = mock(ConfigManager.class);
        final DatabaseManager database = mock(DatabaseManager.class);
        final DisconnectTrackingManager tracking = mock(DisconnectTrackingManager.class);
        final VanillaProvider npcProvider = mock(VanillaProvider.class);
        final BaseGame game = mock(BaseGame.class);
        final Warrior warrior = mock(Warrior.class);
        final Player player = mock(Player.class);
        final LivingEntity mob = mock(LivingEntity.class);
        final PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        final UUID playerId = UUID.randomUUID();
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getDatabaseManager()).thenReturn(database);
        when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
        when(plugin.getNpcProvider()).thenReturn(npcProvider);
        when(plugin.getBaseGameFrom(player)).thenReturn(game);
        when(database.getWarrior(player)).thenReturn(warrior);
        when(player.getUniqueId()).thenReturn(playerId);
        when(event.getPlayer()).thenReturn(player);
        when(configManager.getRespawn()).thenReturn(new ArrayList<>());
        when(configManager.getClearInventory()).thenReturn(new ArrayList<>());
        when(tracking.canPlayerReturn(playerId)).thenReturn(true);
        when(tracking.hasPendingTimeout(playerId)).thenReturn(true);
        when(npcProvider.getProxyByOwner(playerId)).thenReturn(Optional.of(
                new VanillaProvider.VanillaNpcHandle(playerId, mob)));

        try (MockedStatic<Helper> ignored = mockStatic(Helper.class)) {
            new PlayerJoinListener(plugin).onJoin(event);
        }

        verify(npcProvider).isProxyAlive(playerId);
        verify(game).eliminateDisconnected(warrior, "missing-proxy-on-rejoin");
    }
}
