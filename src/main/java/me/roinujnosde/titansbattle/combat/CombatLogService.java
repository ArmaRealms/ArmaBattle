/*
 * The MIT License
 *
 * Copyright 2024 Edson Passos - edsonpassosjr@outlook.com.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.roinujnosde.titansbattle.combat;

import me.roinujnosde.titansbattle.TitansBattle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking combat damage to NPC proxies
 *
 * @author RoinujNosde
 */
public class CombatLogService {

    private final TitansBattle plugin;
    private final Map<UUID, CombatRecord> combatRecords = new ConcurrentHashMap<>();
    private final long combatTimeoutMs;

    public CombatLogService(@NotNull TitansBattle plugin) {
        this.plugin = plugin;
        // Get timeout from config, default to 15 seconds
        this.combatTimeoutMs = plugin.getConfig().getLong("battle.npcProxy.combatTimeoutMs", 15000L);
    }

    /**
     * Record damage dealt to a proxy NPC
     *
     * @param ownerPlayerId the UUID of the player who owns the proxy
     * @param attackerId    the UUID of the attacking player
     * @param damage        the damage amount
     */
    public void recordDamageToProxy(@NotNull UUID ownerPlayerId, @NotNull UUID attackerId, double damage) {
        long currentTime = System.currentTimeMillis();
        CombatRecord record = combatRecords.computeIfAbsent(ownerPlayerId, 
                k -> new CombatRecord(ownerPlayerId));
        record.recordDamage(attackerId, damage, currentTime);
        
        plugin.debug(String.format("Recorded damage: %s -> %s (%.2f damage)", 
                attackerId, ownerPlayerId, damage));
    }

    /**
     * Get the last attacker of a proxy owner within the combat timeout
     *
     * @param ownerPlayerId the UUID of the player who owns the proxy
     * @return the UUID of the last valid attacker, if any
     */
    @NotNull
    public Optional<UUID> getLastAttacker(@NotNull UUID ownerPlayerId) {
        CombatRecord record = combatRecords.get(ownerPlayerId);
        if (record == null) {
            return Optional.empty();
        }

        long currentTime = System.currentTimeMillis();
        return record.getLastAttacker(currentTime, combatTimeoutMs);
    }

    /**
     * Clear combat records for a player
     *
     * @param ownerPlayerId the UUID of the player to clear records for
     */
    public void clear(@NotNull UUID ownerPlayerId) {
        combatRecords.remove(ownerPlayerId);
        plugin.debug("Cleared combat records for " + ownerPlayerId);
    }

    /**
     * Clear all combat records (used on plugin disable)
     */
    public void clearAll() {
        combatRecords.clear();
        plugin.debug("Cleared all combat records");
    }

    /**
     * Get total damage dealt to a proxy owner
     *
     * @param ownerPlayerId the UUID of the player who owns the proxy
     * @return the total damage dealt
     */
    public double getTotalDamage(@NotNull UUID ownerPlayerId) {
        CombatRecord record = combatRecords.get(ownerPlayerId);
        return record != null ? record.getTotalDamage() : 0.0;
    }

    /**
     * Record of combat damage for a single proxy owner
     */
    private static class CombatRecord {
        private final UUID ownerId;
        private final Map<UUID, DamageEntry> damageByAttacker = new ConcurrentHashMap<>();
        private volatile UUID lastAttackerId;
        private volatile long lastAttackTime;

        public CombatRecord(@NotNull UUID ownerId) {
            this.ownerId = ownerId;
        }

        public void recordDamage(@NotNull UUID attackerId, double damage, long timestamp) {
            damageByAttacker.compute(attackerId, (k, existing) -> {
                if (existing == null) {
                    return new DamageEntry(damage, timestamp);
                } else {
                    existing.addDamage(damage, timestamp);
                    return existing;
                }
            });
            
            this.lastAttackerId = attackerId;
            this.lastAttackTime = timestamp;
        }

        @NotNull
        public Optional<UUID> getLastAttacker(long currentTime, long timeoutMs) {
            if (lastAttackerId == null || (currentTime - lastAttackTime) > timeoutMs) {
                return Optional.empty();
            }
            return Optional.of(lastAttackerId);
        }

        public double getTotalDamage() {
            return damageByAttacker.values().stream()
                    .mapToDouble(DamageEntry::getTotalDamage)
                    .sum();
        }
    }

    /**
     * Entry for damage from a specific attacker
     */
    private static class DamageEntry {
        private double totalDamage;
        private long lastDamageTime;

        public DamageEntry(double damage, long timestamp) {
            this.totalDamage = damage;
            this.lastDamageTime = timestamp;
        }

        public void addDamage(double damage, long timestamp) {
            this.totalDamage += damage;
            this.lastDamageTime = timestamp;
        }

        public double getTotalDamage() {
            return totalDamage;
        }

        public long getLastDamageTime() {
            return lastDamageTime;
        }
    }
}