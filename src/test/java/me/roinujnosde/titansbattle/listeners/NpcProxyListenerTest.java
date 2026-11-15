package me.roinujnosde.titansbattle.listeners;

import io.papermc.paper.event.entity.EntityKnockbackEvent;
import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.npc.NpcProxyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.util.Vector;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
    public void cancelsAttackKnockbackOnProxy() {
        final Player proxy = mock(Player.class);
        final EntityKnockbackEvent event = new EntityPushedByEntityAttackEvent(
                proxy, EntityKnockbackEvent.Cause.ENTITY_ATTACK, mock(Player.class), new Vector(1, 0, 0));
        when(proxyManager.isProxy(proxy)).thenReturn(true);

        listener.onKnockback(event);

        assertTrue(event.isCancelled());
    }

    @Test
    public void cancelsNonAttackKnockbackOnProxy() {
        final Player proxy = mock(Player.class);
        final EntityKnockbackEvent event = new EntityKnockbackEvent(
                proxy, EntityKnockbackEvent.Cause.PUSH, new Vector(1, 0, 0));
        when(proxyManager.isProxy(proxy)).thenReturn(true);

        listener.onKnockback(event);

        assertTrue(event.isCancelled());
    }

    @Test
    public void leavesNonProxyKnockbackUnchanged() {
        final Player player = mock(Player.class);
        final Vector knockback = new Vector(1, 0, 0);
        final EntityKnockbackEvent event = new EntityKnockbackEvent(
                player, EntityKnockbackEvent.Cause.PUSH, knockback.clone());

        listener.onKnockback(event);

        assertFalse(event.isCancelled());
        assertEquals(knockback, event.getKnockback());
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
