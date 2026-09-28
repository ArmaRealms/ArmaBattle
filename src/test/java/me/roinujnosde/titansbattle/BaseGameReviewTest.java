package me.roinujnosde.titansbattle;

import me.roinujnosde.titansbattle.combat.DisconnectTrackingManager;
import me.roinujnosde.titansbattle.hooks.papi.PlaceholderHook;
import me.roinujnosde.titansbattle.managers.ConfigManager;
import me.roinujnosde.titansbattle.managers.DatabaseManager;
import me.roinujnosde.titansbattle.npc.VanillaProvider;
import me.roinujnosde.titansbattle.types.Warrior;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.PluginManager;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BaseGameReviewTest {

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
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getDatabaseManager()).thenReturn(database);
        when(plugin.getNpcProvider()).thenReturn(npcProvider);
        when(plugin.getDisconnectTrackingManager()).thenReturn(tracking);
        when(configManager.getClearInventory()).thenReturn(toClear);
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
            return List.of();
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
