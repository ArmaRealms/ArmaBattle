package me.roinujnosde.titansbattle;

import me.roinujnosde.titansbattle.combat.DisconnectTrackingManager;
import me.roinujnosde.titansbattle.events.GroupDefeatedEvent;
import me.roinujnosde.titansbattle.hooks.papi.PlaceholderHook;
import me.roinujnosde.titansbattle.managers.ConfigManager;
import me.roinujnosde.titansbattle.managers.DatabaseManager;
import me.roinujnosde.titansbattle.managers.GroupManager;
import me.roinujnosde.titansbattle.npc.VanillaProvider;
import me.roinujnosde.titansbattle.types.Group;
import me.roinujnosde.titansbattle.types.GroupData;
import me.roinujnosde.titansbattle.types.Warrior;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

public class BaseGameDisconnectTest {

    @Test
    public void playerIndependentCommandRunsForOfflineParticipant() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final BaseGameConfiguration config = mock(BaseGameConfiguration.class);
        final PlaceholderHook hook = mock(PlaceholderHook.class);
        final ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        final Warrior offline = mock(Warrior.class);
        when(plugin.getPlaceholderHook()).thenReturn(hook);
        when(hook.parse((OfflinePlayer) null, "say done")).thenReturn("say done");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getConsoleSender).thenReturn(console);
            new TestGame(plugin, config).runAfterBattle(List.of(offline), List.of("say done"));
            bukkit.verify(() -> Bukkit.dispatchCommand(console, "say done"));
        }
    }

    @Test
    public void finishingWithOfflineWinnerQueuesKitCleanup() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final BaseGameConfiguration config = mock(BaseGameConfiguration.class);
        final ConfigManager configManager = mock(ConfigManager.class);
        final DatabaseManager database = mock(DatabaseManager.class);
        final VanillaProvider npcProvider = mock(VanillaProvider.class);
        final DisconnectTrackingManager tracking = mock(DisconnectTrackingManager.class);
        final Warrior winner = mock(Warrior.class);
        final UUID winnerId = UUID.randomUUID();
        final List<UUID> toClear = new ArrayList<>();
        final List<UUID> toRespawn = new ArrayList<>();
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getDatabaseManager()).thenReturn(database);
        when(plugin.getNpcProvider()).thenReturn(npcProvider);
        when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
        when(configManager.getClearInventory()).thenReturn(toClear);
        when(configManager.getRespawn()).thenReturn(toRespawn);
        when(config.isUseKits()).thenReturn(true);
        when(winner.getUniqueId()).thenReturn(winnerId);
        when(npcProvider.getProxyByOwner(winnerId)).thenReturn(Optional.empty());
        final TestGame game = new TestGame(plugin, config);
        game.addParticipant(winner);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            game.finish(false);
        }

        assertEquals(List.of(winnerId), toClear);
        assertEquals(List.of(winnerId), toRespawn);
        verify(configManager).save();
    }

    @Test
    public void eliminatingDisconnectedPlayerQueuesKitAndRespawnCleanup() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final BaseGameConfiguration config = mock(BaseGameConfiguration.class);
        final ConfigManager configManager = mock(ConfigManager.class);
        final VanillaProvider npcProvider = mock(VanillaProvider.class);
        final DisconnectTrackingManager tracking = mock(DisconnectTrackingManager.class);
        final Warrior warrior = mock(Warrior.class);
        final UUID playerId = UUID.randomUUID();
        final List<UUID> toClear = new ArrayList<>();
        final List<UUID> toRespawn = new ArrayList<>();
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getNpcProvider()).thenReturn(npcProvider);
        when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
        when(configManager.getClearInventory()).thenReturn(toClear);
        when(configManager.getRespawn()).thenReturn(toRespawn);
        when(config.isUseKits()).thenReturn(true);
        when(warrior.getUniqueId()).thenReturn(playerId);
        when(npcProvider.getProxyByOwner(playerId)).thenReturn(Optional.empty());
        final TestGame game = new TestGame(plugin, config);
        game.addParticipant(warrior);

        game.eliminateDisconnected(warrior, "missing-proxy-on-rejoin");

        assertEquals(List.of(playerId), toClear);
        assertEquals(List.of(playerId), toRespawn);
        verify(configManager).save();
        assertTrue(game.isDisconnectElimination(warrior));
    }

    /**
     * A fighter that actually dies in the tournament is a real casualty and must stay eligible for
     * the third-place fight, unlike one removed by the disconnect/reconnect handling.
     */
    @Test
    public void tournamentDeathIsNotTreatedAsDisconnectElimination() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final BaseGameConfiguration config = mock(BaseGameConfiguration.class);
        final ConfigManager configManager = mock(ConfigManager.class);
        final VanillaProvider npcProvider = mock(VanillaProvider.class);
        final DisconnectTrackingManager tracking = mock(DisconnectTrackingManager.class);
        final Warrior warrior = mock(Warrior.class);
        final Player player = mock(Player.class);
        final UUID playerId = UUID.randomUUID();
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getNpcProvider()).thenReturn(npcProvider);
        when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
        when(configManager.getClearInventory()).thenReturn(new ArrayList<>());
        when(configManager.getRespawn()).thenReturn(new ArrayList<>());
        when(config.isUseKits()).thenReturn(true);
        when(warrior.getUniqueId()).thenReturn(playerId);
        when(warrior.toOnlinePlayer()).thenReturn(player);
        when(player.isOnline()).thenReturn(false);
        when(npcProvider.getProxyByOwner(playerId)).thenReturn(Optional.empty());
        final TestGame game = new TestGame(plugin, config);
        game.addParticipant(warrior);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            game.eliminate(warrior, "killed");
        }

        assertTrue(game.casualties.contains(warrior));
        assertTrue(!game.isDisconnectElimination(warrior));
    }

    @Test
    public void offlineLastGroupMemberStillFiresDefeatEvent() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final BaseGameConfiguration config = mock(BaseGameConfiguration.class);
        final Group group = mock(Group.class);
        final GroupData groupData = mock(GroupData.class);
        final Warrior warrior = mock(Warrior.class);
        final OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        final VanillaProvider npcProvider = mock(VanillaProvider.class);
        final PluginManager pluginManager = mock(PluginManager.class);
        final UUID playerId = UUID.randomUUID();
        when(plugin.getGroupManager()).thenReturn(mock(GroupManager.class));
        when(plugin.getDisconnectTrackingManager()).thenReturn(mock(DisconnectTrackingManager.class));
        when(plugin.getNpcProvider()).thenReturn(npcProvider);
        when(config.isGroupMode()).thenReturn(true);
        when(group.getData()).thenReturn(groupData);
        when(warrior.getUniqueId()).thenReturn(playerId);
        when(warrior.toPlayer()).thenReturn(offlinePlayer);
        when(npcProvider.getProxyByOwner(playerId)).thenReturn(Optional.empty());
        final TestGame game = new TestGame(plugin, config);
        game.addParticipant(warrior);
        game.groups.put(warrior, group);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            game.eliminate(warrior, "timeout");
        }

        final ArgumentCaptor<Event> event = ArgumentCaptor.forClass(Event.class);
        verify(pluginManager).callEvent(event.capture());
        final GroupDefeatedEvent defeated = (GroupDefeatedEvent) event.getValue();
        assertEquals(offlinePlayer, defeated.getLastParticipantOffline());
        assertNull(defeated.getLastParticipant());
        verify(groupData).increaseDefeats(config.getName());
    }

    /**
     * Warriors loaded from storage wrap an {@link OfflinePlayer} even while their owner is online.
     * The live {@link Player} must still be handed to listeners of the ordinary online defeat path.
     */
    @Test
    public void onlineDefeatExposesTheLivePlayerThroughTheOfflineWrapper() {
        final Group group = mock(Group.class);
        final OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        final Player livePlayer = mock(Player.class);
        when(offlinePlayer.getPlayer()).thenReturn(livePlayer);

        // BaseGame builds the event from warrior.toPlayer(), which is the OfflinePlayer wrapper.
        final GroupDefeatedEvent defeated = new GroupDefeatedEvent(group, offlinePlayer);

        assertEquals(offlinePlayer, defeated.getLastParticipantOffline());
        assertEquals(livePlayer, defeated.getLastParticipant());
    }

    @Test
    public void groupDefeatOfAnOnlinePlayerStillExposesThatPlayer() {
        final Group group = mock(Group.class);
        final Player livePlayer = mock(Player.class);

        final GroupDefeatedEvent defeated = new GroupDefeatedEvent(group, livePlayer);

        assertEquals(livePlayer, defeated.getLastParticipantOffline());
        assertEquals(livePlayer, defeated.getLastParticipant());
    }

    @Test
    public void missingDisconnectRecordIsRecoveredAfterProxySpawn() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final BaseGameConfiguration config = mock(BaseGameConfiguration.class);
        final FileConfiguration pluginConfig = mock(FileConfiguration.class);
        final ConfigManager configManager = mock(ConfigManager.class);
        final VanillaProvider npcProvider = mock(VanillaProvider.class);
        final DisconnectTrackingManager tracking = mock(DisconnectTrackingManager.class);
        final Warrior warrior = mock(Warrior.class);
        final Player player = mock(Player.class);
        final Location location = mock(Location.class);
        final UUID playerId = UUID.randomUUID();
        when(plugin.getConfig()).thenReturn(pluginConfig);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getTimeFormat()).thenReturn("{mm}:{ss}");
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        when(plugin.getNpcProvider()).thenReturn(npcProvider);
        when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
        when(pluginConfig.getStringList("battle.npcProxy.bypass-reasons")).thenReturn(List.of());
        when(pluginConfig.getBoolean("battle.npcProxy.enabled", true)).thenReturn(true);
        when(warrior.toOnlinePlayer()).thenReturn(player);
        when(warrior.getUniqueId()).thenReturn(playerId);
        when(player.getLocation()).thenReturn(location);
        when(npcProvider.isAvailable()).thenReturn(true);
        final TestGame game = new TestGame(plugin, config);
        game.addParticipant(warrior);
        when(tracking.trackDisconnection(playerId, game)).thenReturn(true);
        when(tracking.startOfflineTimeout(playerId)).thenReturn(false, true);

        game.onDisconnect(warrior, null);

        verify(npcProvider).spawnProxy(player, location);
        verify(tracking, times(2)).startOfflineTimeout(playerId);
        verify(tracking, times(2)).trackDisconnection(playerId, game);
        assertTrue(game.isParticipant(warrior));
    }

    @Test
    public void protectedDisconnectNotifiesParticipantsWithEffectiveValues() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final BaseGameConfiguration config = mock(BaseGameConfiguration.class);
        final FileConfiguration pluginConfig = mock(FileConfiguration.class);
        final ConfigManager configManager = mock(ConfigManager.class);
        final VanillaProvider npcProvider = mock(VanillaProvider.class);
        final DisconnectTrackingManager tracking = mock(DisconnectTrackingManager.class);
        final Warrior warrior = mock(Warrior.class);
        final Warrior other = mock(Warrior.class);
        final Player player = mock(Player.class);
        final Location location = mock(Location.class);
        final UUID playerId = UUID.randomUUID();
        when(plugin.getConfig()).thenReturn(pluginConfig);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getNpcProvider()).thenReturn(npcProvider);
        when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
        stubNoticeLanguage(plugin);
        when(pluginConfig.getStringList("battle.npcProxy.bypass-reasons")).thenReturn(List.of());
        when(pluginConfig.getBoolean("battle.npcProxy.enabled", true)).thenReturn(true);
        when(configManager.getTimeFormat()).thenReturn("{mm}:{ss}");
        when(warrior.toOnlinePlayer()).thenReturn(player);
        when(warrior.getName()).thenReturn("Alice");
        when(warrior.getUniqueId()).thenReturn(playerId);
        when(player.getLocation()).thenReturn(location);
        when(npcProvider.isAvailable()).thenReturn(true);
        when(tracking.trackDisconnection(eq(playerId), any())).thenReturn(true);
        when(tracking.startOfflineTimeout(playerId)).thenReturn(true);
        when(tracking.getDisconnectionCount(playerId)).thenReturn(2);
        when(tracking.getMaxDisconnections()).thenReturn(3);
        when(tracking.getMaxOfflineTimeSeconds()).thenReturn(300L);
        final TestGame game = new TestGame(plugin, config);
        game.addParticipant(warrior);
        game.addParticipant(other);

        game.onDisconnect(warrior, null);

        verify(warrior).sendMessage("Alice:2/3:05:00");
        verify(other).sendMessage("Alice:2/3:05:00");
    }

    @Test
    public void playerLeftNoticeIsNotSentWhenTheDisconnectIsNotProtected() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final BaseGameConfiguration config = mock(BaseGameConfiguration.class);
        final FileConfiguration pluginConfig = mock(FileConfiguration.class);
        final ConfigManager configManager = mock(ConfigManager.class);
        final VanillaProvider npcProvider = mock(VanillaProvider.class);
        final DisconnectTrackingManager tracking = mock(DisconnectTrackingManager.class);
        final Warrior warrior = mock(Warrior.class);
        final Player player = mock(Player.class);
        final UUID playerId = UUID.randomUUID();
        when(plugin.getConfig()).thenReturn(pluginConfig);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getNpcProvider()).thenReturn(npcProvider);
        when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
        stubNoticeLanguage(plugin);
        when(pluginConfig.getStringList("battle.npcProxy.bypass-reasons")).thenReturn(List.of());
        when(pluginConfig.getBoolean("battle.npcProxy.enabled", true)).thenReturn(false);
        when(configManager.getRespawn()).thenReturn(new ArrayList<>());
        when(configManager.getClearInventory()).thenReturn(new ArrayList<>());
        when(warrior.toOnlinePlayer()).thenReturn(player);
        when(warrior.getUniqueId()).thenReturn(playerId);
        when(player.isOnline()).thenReturn(false);
        when(npcProvider.getProxyByOwner(playerId)).thenReturn(Optional.empty());
        final TestGame game = new TestGame(plugin, config);
        game.addParticipant(warrior);

        game.onDisconnect(warrior, null);

        verify(warrior, never()).sendMessage(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    public void timeoutEliminationIsAnnouncedExactlyOnce() {
        final Fixture fixture = new Fixture();

        fixture.game.eliminateDisconnected(fixture.warrior, BaseGame.ELIMINATION_REASON_TIMEOUT);

        verify(fixture.other, times(1)).sendMessage("timeout:duelist");
        verify(fixture.other, never()).sendMessage("limit:duelist");
        verify(fixture.other, never()).sendMessage("returned:duelist");
    }

    @Test
    public void disconnectLimitEliminationIsAnnouncedWithoutAProtectedReturnTime() {
        final Fixture fixture = new Fixture();

        fixture.game.eliminateDisconnected(fixture.warrior, BaseGame.ELIMINATION_REASON_DISCONNECT_LIMIT_EXCEEDED);

        verify(fixture.other).sendMessage("limit:duelist");
        verify(fixture.other, never()).sendMessage("timeout:duelist");
    }

    /**
     * A stale timeout task must not announce an elimination after the player already returned or was
     * removed from the game, and nobody outside the game may be notified.
     */
    @Test
    public void eliminationIsNotAnnouncedForNonParticipants() {
        final Fixture fixture = new Fixture();
        final Warrior outsider = mock(Warrior.class);

        fixture.game.eliminateDisconnected(outsider, BaseGame.ELIMINATION_REASON_TIMEOUT);

        verify(fixture.warrior, never()).sendMessage(org.mockito.ArgumentMatchers.anyString());
        verify(fixture.other, never()).sendMessage(org.mockito.ArgumentMatchers.anyString());
        verify(outsider, never()).sendMessage(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    public void returnAnnouncesToTheGameAndPrivatelyInformsOnlyTheReturningPlayer() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final BaseGameConfiguration config = mock(BaseGameConfiguration.class);
        final DisconnectTrackingManager tracking = mock(DisconnectTrackingManager.class);
        final Warrior warrior = mock(Warrior.class);
        final Warrior other = mock(Warrior.class);
        final Player player = mock(Player.class);
        final UUID playerId = UUID.randomUUID();
        when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
        stubNoticeLanguage(plugin);
        when(tracking.getMaxDisconnections()).thenReturn(3);
        when(tracking.getDisconnectionCount(playerId)).thenReturn(2);
        when(player.getName()).thenReturn("Alice");
        when(player.getUniqueId()).thenReturn(playerId);
        final TestGame game = new TestGame(plugin, config);
        game.addParticipant(warrior);
        game.addParticipant(other);

        game.notifyPlayerReturned(player);

        verify(warrior).sendMessage("returned:Alice");
        verify(other).sendMessage("returned:Alice");
        verify(player).sendMessage("remaining:1");
        verify(warrior, never()).sendMessage("remaining:1");
        verify(other, never()).sendMessage("remaining:1");
    }

    @Test
    public void remainingProtectionsAreNeverNegative() {
        final TitansBattle plugin = mock(TitansBattle.class);
        final BaseGameConfiguration config = mock(BaseGameConfiguration.class);
        final DisconnectTrackingManager tracking = mock(DisconnectTrackingManager.class);
        final Player player = mock(Player.class);
        final UUID playerId = UUID.randomUUID();
        when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
        stubNoticeLanguage(plugin);
        when(tracking.getMaxDisconnections()).thenReturn(3);
        when(tracking.getDisconnectionCount(playerId)).thenReturn(5);
        when(player.getUniqueId()).thenReturn(playerId);
        final TestGame game = new TestGame(plugin, config);

        game.notifyPlayerReturned(player);

        verify(player).sendMessage("remaining:0");
    }

    /**
     * Makes the mocked plugin resolve the disconnect notices with the placeholders of the real
     * language files, so the tests can assert the values that reach the players.
     */
    private static void stubNoticeLanguage(final TitansBattle plugin) {
        when(plugin.getLang(anyString(), any(BaseGame.class), any(Object[].class))).thenAnswer(invocation -> {
            final Object[] arguments = invocation.getArguments();
            final String path = (String) arguments[0];
            final Object[] values;
            if (arguments.length > 2 && arguments[2] instanceof Object[]) {
                values = (Object[]) arguments[2];
            } else {
                values = Arrays.copyOfRange(arguments, Math.min(2, arguments.length), arguments.length);
            }
            final String template;
            switch (path) {
                case "disconnect-player-left":
                    template = "{0}:{1}/{2}:{3}";
                    break;
                case "disconnect-player-returned":
                    template = "returned:{0}";
                    break;
                case "disconnect-remaining-protections":
                    template = "remaining:{0}";
                    break;
                case "disconnect-timeout-eliminated":
                    template = "timeout:{0}";
                    break;
                case "disconnect-limit-eliminated":
                    template = "limit:{0}";
                    break;
                default:
                    template = path;
            }
            return MessageFormat.format(template, values);
        });
    }

    /**
     * Shared stubs for the elimination notices: a game with the disconnecting fighter and one other
     * participant, and a mocked language that formats the values like the real one does.
     */
    private static final class Fixture {

        private final TitansBattle plugin = mock(TitansBattle.class);
        private final BaseGameConfiguration config = mock(BaseGameConfiguration.class);
        private final ConfigManager configManager = mock(ConfigManager.class);
        private final VanillaProvider npcProvider = mock(VanillaProvider.class);
        private final DisconnectTrackingManager tracking = mock(DisconnectTrackingManager.class);
        private final Warrior warrior = mock(Warrior.class);
        private final Warrior other = mock(Warrior.class);
        private final UUID playerId = UUID.randomUUID();
        private final TestGame game;

        private Fixture() {
            when(plugin.getConfigManager()).thenReturn(configManager);
            when(plugin.getNpcProvider()).thenReturn(npcProvider);
            when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
            stubNoticeLanguage(plugin);
            when(configManager.getRespawn()).thenReturn(new ArrayList<>());
            when(configManager.getClearInventory()).thenReturn(new ArrayList<>());
            when(warrior.getName()).thenReturn("duelist");
            when(warrior.getUniqueId()).thenReturn(playerId);
            when(npcProvider.getProxyByOwner(playerId)).thenReturn(Optional.empty());
            this.game = new TestGame(plugin, config);
            this.game.addParticipant(warrior);
            this.game.addParticipant(other);
        }
    }

    private static final class TestGame extends BaseGame {
        private TestGame(final TitansBattle plugin, final BaseGameConfiguration config) {
            super(plugin, config);
        }

        private void addParticipant(final Warrior warrior) {
            participants.add(warrior);
        }

        private void runAfterBattle(final Collection<Warrior> warriors, final Collection<String> commands) {
            runCommands(warriors, commands);
        }

        @Override
        public void setWinner(final Warrior warrior) {
        }

        @Override
        public boolean isInBattle(final Warrior warrior) {
            return false;
        }

        @Override
        public boolean shouldClearDropsOnDeath(final Warrior warrior) {
            return false;
        }

        @Override
        public boolean shouldKeepInventoryOnDeath(final Warrior warrior) {
            return false;
        }

        @Override
        public Collection<Warrior> getCurrentFighters() {
            return participants;
        }

        @Override
        protected void onLobbyEnd() {
        }

        @Override
        protected void processWinners() {
        }

        @Override
        protected void processRemainingPlayers(final Warrior warrior) {
        }
    }
}
