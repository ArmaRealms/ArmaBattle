package me.roinujnosde.titansbattle.managers;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.games.Game;
import me.roinujnosde.titansbattle.types.GameConfiguration;
import me.roinujnosde.titansbattle.utils.SoundUtils;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SpectateManagerTest {

    @Test
    public void spectatorStartsFlyingAndFlightIsClearedOnExit() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final ConfigManager configManager = mock(ConfigManager.class);
        final Server server = mock(Server.class);
        final Player player = mock(Player.class);
        final PlayerInventory inventory = mock(PlayerInventory.class);
        final Game game = mock(Game.class);
        final GameConfiguration gameConfig = mock(GameConfiguration.class);
        final Location watchroom = mock(Location.class);
        final Location exit = mock(Location.class);
        final UUID playerId = UUID.randomUUID();

        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getServer()).thenReturn(server);
        when(server.getOnlinePlayers()).thenReturn(List.of());
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getArmorContents()).thenReturn(new ItemStack[4]);
        when(inventory.getContents()).thenReturn(new ItemStack[36]);
        when(player.getUniqueId()).thenReturn(playerId);
        when(game.getConfig()).thenReturn(gameConfig);
        when(gameConfig.getWatchroom()).thenReturn(watchroom);
        when(player.teleport(watchroom)).thenReturn(true);
        when(configManager.getGeneralExit()).thenReturn(exit);
        when(player.teleport(exit, PlayerTeleportEvent.TeleportCause.PLUGIN)).thenReturn(true);

        final SpectateManager manager = new SpectateManager(plugin);

        try (MockedStatic<SoundUtils> ignored = mockStatic(SoundUtils.class)) {
            manager.addSpectator(player, game, null);

            verify(player).setAllowFlight(true);
            verify(player).setFlying(true);

            manager.removeSpectator(player);
        }

        final InOrder flightState = inOrder(player);
        flightState.verify(player).setAllowFlight(true);
        flightState.verify(player).setFlying(true);
        flightState.verify(player).setFlying(false);
        flightState.verify(player).setAllowFlight(false);
    }
}
