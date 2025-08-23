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
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Resolver for selecting the best available NPC provider
 *
 * @author RoinujNosde
 */
public final class NpcProviderResolver {

    private NpcProviderResolver() {
        // Utility class
    }

    /**
     * Resolve the best available NPC provider
     *
     * @param plugin the plugin instance
     * @return the best available NPC provider
     */
    @NotNull
    public static NpcProvider resolve(@NotNull TitansBattle plugin) {
        // Try FancyNpcs first
        if (Bukkit.getPluginManager().getPlugin("FancyNpcs") != null) {
            NpcProvider provider = new FancyNpcsProvider(plugin);
            if (provider.isAvailable()) {
                plugin.getLogger().info("Using FancyNpcs for NPC proxy system");
                return provider;
            }
        }

        // Try Citizens as fallback
        if (Bukkit.getPluginManager().getPlugin("Citizens") != null) {
            NpcProvider provider = new CitizensProvider(plugin);
            if (provider.isAvailable()) {
                plugin.getLogger().info("Using Citizens for NPC proxy system");
                return provider;
            }
        }

        // Use vanilla fallback (always available but does nothing)
        plugin.getLogger().info("No NPC plugin available, using vanilla fallback (NPC proxy system disabled)");
        return new VanillaProvider(plugin);
    }
}