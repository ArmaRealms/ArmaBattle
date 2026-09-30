package me.roinujnosde.titansbattle.managers;

import org.bukkit.entity.Player;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

public class SpectateManagerTest {

    @Test
    public void spectatorFlightLifecycleUsesSafeStateOrder() {
        final Player player = mock(Player.class);

        SpectateManager.enableSpectatorFlight(player);
        SpectateManager.disableSpectatorFlight(player);

        final InOrder flightState = inOrder(player);
        flightState.verify(player).setAllowFlight(true);
        flightState.verify(player).setFlying(true);
        flightState.verify(player).setFlying(false);
        flightState.verify(player).setAllowFlight(false);
    }
}
