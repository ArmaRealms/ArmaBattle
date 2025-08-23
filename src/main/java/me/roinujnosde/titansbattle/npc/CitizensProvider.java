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
 * Citizens implementation of NPC provider (placeholder - not yet implemented)
 *
 * @author RoinujNosde
 */
public class CitizensProvider implements NpcProvider {

    private final TitansBattle plugin;

    public CitizensProvider(@NotNull final TitansBattle plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable() {
        // TODO: Implement Citizens support
        return false;
    }

    @Override
    @NotNull
    public NpcHandle spawnProxy(@NotNull final Player player, @NotNull final Location location) {
        throw new UnsupportedOperationException("Citizens provider not yet implemented");
    }

    @Override
    @NotNull
    public Optional<NpcHandle> getProxyByOwner(@NotNull final UUID ownerPlayerId) {
        return Optional.empty();
    }

    @Override
    public boolean isProxyAlive(@NotNull final UUID ownerPlayerId) {
        return false;
    }

    @Override
    public void despawnProxy(@NotNull final UUID ownerPlayerId, @NotNull final String reason) {
        // No-op
    }

    @Override
    public void onDisable() {
        // No-op
    }

    @Override
    @NotNull
    public String getName() {
        return "Citizens";
    }
}