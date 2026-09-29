package me.roinujnosde.titansbattle.listeners;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.npc.NpcProxyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NpcProxyListenerTest {

    private NpcProxyManager proxyManager;
    private NpcProxyListener listener;

    @Before
    public void setUp() {
        final TitansBattle plugin = mock(TitansBattle.class);
        proxyManager = mock(NpcProxyManager.class);
        when(plugin.getNpcProxyManager()).thenReturn(proxyManager);
        listener = new NpcProxyListener(plugin);
    }

    @Test
    public void cancelsDamageOnProxy() {
        final Player proxy = mock(Player.class);
        final EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(proxy);
        when(proxyManager.isProxy(proxy)).thenReturn(true);

        listener.onDamage(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void ignoresDamageToNonProxy() {
        final Player player = mock(Player.class);
        final EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(proxyManager.isProxy(player)).thenReturn(false);

        listener.onDamage(event);

        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test
    public void cancelsKnockbackOnProxy() {
        final Player proxy = mock(Player.class);
        final EntityKnockbackEvent event = mock(EntityKnockbackEvent.class);
        when(event.getEntity()).thenReturn(proxy);
        when(proxyManager.isProxy(proxy)).thenReturn(true);

        listener.onKnockback(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void cancelsCombustOnProxy() {
        final Player proxy = mock(Player.class);
        final EntityCombustEvent event = mock(EntityCombustEvent.class);
        when(event.getEntity()).thenReturn(proxy);
        when(proxyManager.isProxy(proxy)).thenReturn(true);

        listener.onCombust(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void cancelsInteractionWithProxy() {
        final Player proxy = mock(Player.class);
        final PlayerInteractEntityEvent event = mock(PlayerInteractEntityEvent.class);
        when(event.getRightClicked()).thenReturn(proxy);
        when(proxyManager.isProxy(proxy)).thenReturn(true);

        listener.onInteract(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void ignoresInteractionWithNonProxy() {
        final Player player = mock(Player.class);
        final PlayerInteractEntityEvent event = mock(PlayerInteractEntityEvent.class);
        when(event.getRightClicked()).thenReturn(player);
        when(proxyManager.isProxy(player)).thenReturn(false);

        listener.onInteract(event);

        verify(event, never()).setCancelled(anyBoolean());
    }
}
