package me.roinujnosde.titansbattle;

import me.roinujnosde.titansbattle.managers.ConfigManager;
import org.junit.Test;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class TitansBattleDisconnectLoggingTest {

    @Test
    public void debugDisabledSuppressesNormalAndFailureLogs() {
        final Fixture fixture = new Fixture();

        fixture.plugin.debugDisconnect("Player disconnected");
        fixture.plugin.debugDisconnect(Level.WARNING, "Failed to restore proxy",
                new IllegalStateException("test"));

        verifyNoInteractions(fixture.logger);
    }

    @Test
    public void debugEnabledPrefixesNormalAndFailureLogs() {
        final Fixture fixture = new Fixture();
        final IllegalStateException failure = new IllegalStateException("test");
        when(fixture.configManager.isDebug()).thenReturn(true);

        fixture.plugin.debugDisconnect("Player disconnected");
        fixture.plugin.debugDisconnect(Level.WARNING, "Failed to restore proxy", failure);

        verify(fixture.logger).log(Level.INFO, "[DisconnectProtection] Player disconnected", (Throwable) null);
        verify(fixture.logger).log(Level.WARNING, "[DisconnectProtection] Failed to restore proxy", failure);
    }

    private static final class Fixture {
        private final TitansBattle plugin = mock(TitansBattle.class, CALLS_REAL_METHODS);
        private final ConfigManager configManager = mock(ConfigManager.class);
        private final Logger logger = mock(Logger.class);

        private Fixture() {
            doReturn(configManager).when(plugin).getConfigManager();
            doReturn(logger).when(plugin).getLogger();
        }
    }
}
