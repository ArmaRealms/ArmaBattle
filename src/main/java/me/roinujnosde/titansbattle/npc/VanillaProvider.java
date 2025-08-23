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
package me.roinujnosde.titansbattle.npc;

import me.roinujnosde.titansbattle.TitansBattle;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Vanilla fallback NPC provider that doesn't create any NPCs
 *
 * @author RoinujNosde
 */
public class VanillaProvider implements NpcProvider {

    private final TitansBattle plugin;

    public VanillaProvider(@NotNull TitansBattle plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable() {
        // Always available but does nothing
        return false;
    }

    @Override
    @NotNull
    public NpcHandle spawnProxy(@NotNull Player player, @NotNull Location location, double health) {
        plugin.getLogger().warning("Attempted to spawn NPC proxy for " + player.getName() + " but no NPC provider is available");
        throw new UnsupportedOperationException("No NPC provider available");
    }

    @Override
    @NotNull
    public Optional<NpcHandle> getProxyByOwner(@NotNull UUID ownerPlayerId) {
        return Optional.empty();
    }

    @Override
    public boolean isProxyAlive(@NotNull UUID ownerPlayerId) {
        return false;
    }

    @Override
    public void despawnProxy(@NotNull UUID ownerPlayerId, @NotNull String reason) {
        // No-op
    }

    @Override
    public void onDisable() {
        // No-op
    }

    @Override
    @NotNull
    public String getName() {
        return "Vanilla";
    }
}