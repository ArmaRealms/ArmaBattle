package me.roinujnosde.titansbattle.combat;

import me.roinujnosde.titansbattle.TitansBattle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test for CombatLogService
 */
public class CombatLogServiceTest {

    @Mock
    private TitansBattle plugin;

    private CombatLogService combatLogService;
    private UUID ownerId;
    private UUID attackerId;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getConfig()).thenReturn(org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.StringReader("battle:\n  npcProxy:\n    combatTimeoutMs: 15000")
        ));

        combatLogService = new CombatLogService(plugin);
        ownerId = UUID.randomUUID();
        attackerId = UUID.randomUUID();
    }

    @Test
    public void testRecordDamageAndGetLastAttacker() {
        // Record damage
        combatLogService.recordDamageToProxy(ownerId, attackerId, 10.0);

        // Verify last attacker is recorded
        final Optional<UUID> lastAttacker = combatLogService.getLastAttacker(ownerId);
        assertTrue(lastAttacker.isPresent());
        assertEquals(attackerId, lastAttacker.get());

        // Verify total damage
        assertEquals(10.0, combatLogService.getTotalDamage(ownerId), 0.01);
    }

    @Test
    public void testCombatTimeout() throws InterruptedException {
        // Record damage
        combatLogService.recordDamageToProxy(ownerId, attackerId, 5.0);

        // Simulate timeout by creating service with 0ms timeout
        when(plugin.getConfig()).thenReturn(org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.StringReader("battle:\n  npcProxy:\n    combatTimeoutMs: 0")
        ));

        final CombatLogService shortTimeoutService = new CombatLogService(plugin);
        shortTimeoutService.recordDamageToProxy(ownerId, attackerId, 5.0);

        // Wait a bit and check - should have no last attacker due to timeout
        Thread.sleep(10);
        final Optional<UUID> lastAttacker = shortTimeoutService.getLastAttacker(ownerId);
        assertFalse(lastAttacker.isPresent());
    }

    @Test
    public void testClearRecords() {
        // Record damage
        combatLogService.recordDamageToProxy(ownerId, attackerId, 15.0);

        // Clear records
        combatLogService.clear(ownerId);

        // Verify records are cleared
        final Optional<UUID> lastAttacker = combatLogService.getLastAttacker(ownerId);
        assertFalse(lastAttacker.isPresent());
        assertEquals(0.0, combatLogService.getTotalDamage(ownerId), 0.01);
    }

    @Test
    public void testMultipleAttackers() {
        final UUID attacker2 = UUID.randomUUID();

        // Record damage from multiple attackers
        combatLogService.recordDamageToProxy(ownerId, attackerId, 8.0);
        combatLogService.recordDamageToProxy(ownerId, attacker2, 12.0);

        // Last attacker should be the most recent
        final Optional<UUID> lastAttacker = combatLogService.getLastAttacker(ownerId);
        assertTrue(lastAttacker.isPresent());
        assertEquals(attacker2, lastAttacker.get());

        // Total damage should be sum of all damage
        assertEquals(20.0, combatLogService.getTotalDamage(ownerId), 0.01);
    }
}