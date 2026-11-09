package me.roinujnosde.titansbattle.listeners;

import me.roinujnosde.titansbattle.BaseGame;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.combat.DisconnectTrackingManager;
import me.roinujnosde.titansbattle.managers.ConfigManager;
import me.roinujnosde.titansbattle.managers.DatabaseManager;
import me.roinujnosde.titansbattle.npc.NpcProxyManager;
import me.roinujnosde.titansbattle.types.Kit;
import me.roinujnosde.titansbattle.types.Warrior;
import me.roinujnosde.titansbattle.utils.Helper;
import me.roinujnosde.titansbattle.utils.SoundUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerJoinListenerTest {

    @Test
    public void legacyDuplicatesCleanOnlyTaggedSlotsBeforeTeleportAndAreConsumedOnce() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final ConfigManager configManager = mock(ConfigManager.class);
        final Player player = mock(Player.class);
        final PlayerInventory inventory = mock(PlayerInventory.class);
        final ItemStack personalItem = mock(ItemStack.class);
        final ItemStack kitItem = mock(ItemStack.class);
        final Location exit = mock(Location.class);
        final PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        final UUID playerId = UUID.randomUUID();
        final UUID otherPlayer = UUID.randomUUID();
        final List<UUID> pendingClear = new ArrayList<>(List.of(playerId, otherPlayer, playerId));
        final List<UUID> pendingExit = new ArrayList<>(List.of(playerId, playerId));
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getClearInventory()).thenReturn(pendingClear);
        when(configManager.getRespawn()).thenReturn(pendingExit);
        when(configManager.getGeneralExit()).thenReturn(exit);
        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Fighter");
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getSize()).thenReturn(41);
        when(inventory.getItem(0)).thenReturn(personalItem);
        when(inventory.getItem(1)).thenReturn(kitItem);
        when(inventory.getItem(39)).thenReturn(kitItem);
        when(inventory.getItem(40)).thenReturn(kitItem);
        when(personalItem.getType()).thenReturn(Material.DIAMOND);
        when(kitItem.getType()).thenReturn(Material.DIAMOND);
        when(kitItem.getAmount()).thenReturn(3);

        try (MockedStatic<Helper> ignored = mockStatic(Helper.class);
             MockedStatic<Kit> tags = mockStatic(Kit.class, invocation -> {
                 if (invocation.getMethod().getName().equals("isKitItem")) {
                     return invocation.getArgument(0) == kitItem;
                 }
                 return invocation.callRealMethod();
             });
             MockedStatic<SoundUtils> sound = mockStatic(SoundUtils.class)) {
            final PlayerJoinListener listener = new PlayerJoinListener(plugin);
            listener.onJoin(event);
            listener.onJoin(event);
        }

        final InOrder order = inOrder(inventory, player, configManager);
        order.verify(inventory).setItem(1, null);
        order.verify(inventory).setItem(39, null);
        order.verify(inventory).setItem(40, null);
        order.verify(player).teleport(exit);
        order.verify(configManager).save();
        verify(inventory, never()).setItem(0, null);
        verify(inventory, never()).clear();
        verify(player).teleport(exit);
        verify(configManager).save();
        assertEquals(List.of(otherPlayer), pendingClear);
        assertTrue(pendingExit.isEmpty());
    }

    @Test
    public void deadProxyRejectsReconnectAndSafelyConsumesNewCleanupInTheSameLogin() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final ConfigManager configManager = mock(ConfigManager.class);
        final DatabaseManager database = mock(DatabaseManager.class);
        final DisconnectTrackingManager tracking = mock(DisconnectTrackingManager.class);
        final NpcProxyManager npcProvider = mock(NpcProxyManager.class);
        final BaseGame game = mock(BaseGame.class);
        final Warrior warrior = mock(Warrior.class);
        final Player player = mock(Player.class);
        final PlayerInventory inventory = mock(PlayerInventory.class);
        final UUID playerId = UUID.randomUUID();
        final LivingEntity mob = mock(LivingEntity.class);
        final PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        final List<UUID> pendingClear = new ArrayList<>();
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getDatabaseManager()).thenReturn(database);
        when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
        when(plugin.getNpcProxyManager()).thenReturn(npcProvider);
        when(plugin.getBaseGameFrom(player)).thenReturn(game, null);
        when(database.getWarrior(player)).thenReturn(warrior);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getInventory()).thenReturn(inventory);
        when(event.getPlayer()).thenReturn(player);
        when(configManager.getRespawn()).thenReturn(new ArrayList<>());
        when(configManager.getClearInventory()).thenReturn(pendingClear);
        when(tracking.canPlayerReturn(playerId)).thenReturn(true);
        when(tracking.hasPendingTimeout(playerId)).thenReturn(true);
        when(npcProvider.isProxyAlive(playerId)).thenReturn(false);
        when(npcProvider.getProxyByOwner(playerId)).thenReturn(Optional.of(mob));
        doAnswer(invocation -> {
            pendingClear.add(playerId);
            return null;
        }).when(game).eliminateDisconnected(warrior, "missing-proxy-on-rejoin");

        try (MockedStatic<Helper> ignored = mockStatic(Helper.class)) {
            new PlayerJoinListener(plugin).onJoin(event);
        }

        verify(npcProvider).isProxyAlive(playerId);
        verify(game).eliminateDisconnected(warrior, "missing-proxy-on-rejoin");
        verify(game, never()).notifyPlayerReturned(org.mockito.ArgumentMatchers.any());
        verify(inventory).getSize();
        verify(inventory, never()).clear();
        verify(configManager).save();
        assertTrue(pendingClear.isEmpty());
    }

    @Test
    public void livingProxyRestoresPlayerAndDiscardsLegacyPendingActions() {
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
        final Location exit = mock(Location.class);
        final List<UUID> pendingClear = new ArrayList<>(List.of(playerId, playerId));
        final List<UUID> pendingExit = new ArrayList<>(List.of(playerId, playerId));
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
        when(plugin.getNpcProxyManager()).thenReturn(npcProvider);
        when(plugin.getBaseGameFrom(player)).thenReturn(game);
        when(player.getUniqueId()).thenReturn(playerId);
        when(event.getPlayer()).thenReturn(player);
        when(configManager.getRespawn()).thenReturn(pendingExit);
        when(configManager.getClearInventory()).thenReturn(pendingClear);
        when(configManager.getGeneralExit()).thenReturn(exit);
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
        verify(player, never()).getInventory();
        verify(player, never()).teleport(exit);
        verify(player).teleport(any(Location.class));
        verify(configManager).save();
        assertTrue(pendingClear.isEmpty());
        assertTrue(pendingExit.isEmpty());
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
