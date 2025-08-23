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

import java.util.Optional;

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
     * Resolve the best available NPC provider based on configuration
     *
     * @param plugin the plugin instance
     * @return the selected NPC provider
     */
    @NotNull
    public static NpcProvider resolve(@NotNull final TitansBattle plugin) {
        final String providerConfig = plugin.getConfig().getString("battle.npcProxy.provider", "auto").toLowerCase();
        
        // If specific provider is requested, try to use it
        return switch (providerConfig) {
            case "fancynpcs" ->
                    tryFancyNpcs(plugin).orElseGet(() -> fallbackToVanilla(plugin, "FancyNpcs requested but not available"));
            case "citizens" ->
                    tryCitizens(plugin).orElseGet(() -> fallbackToVanilla(plugin, "Citizens requested but not available"));
            case "vanilla" -> {
                plugin.getLogger().info("Using vanilla mob proxies for NPC proxy system (configured)");
                yield new VanillaProvider(plugin);
            }
            default ->
                // Auto mode: try providers in order of preference
                    tryFancyNpcs(plugin)
                            .or(() -> tryCitizens(plugin))
                            .orElseGet(() -> {
                                plugin.getLogger().info("Using vanilla mob proxies as fallback for NPC proxy system");
                                return new VanillaProvider(plugin);
                            });
        };
    }
    
    private static Optional<NpcProvider> tryFancyNpcs(@NotNull final TitansBattle plugin) {
        if (Bukkit.getPluginManager().isPluginEnabled("FancyNpcs")) {
            final NpcProvider provider = new FancyNpcsProvider(plugin);
            if (provider.isAvailable()) {
                plugin.getLogger().info("Using FancyNpcs for NPC proxy system");
                return Optional.of(provider);
            }
        }
        return Optional.empty();
    }
    
    private static Optional<NpcProvider> tryCitizens(@NotNull final TitansBattle plugin) {
        if (Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            final NpcProvider provider = new CitizensProvider(plugin);
            if (provider.isAvailable()) {
                plugin.getLogger().info("Using Citizens for NPC proxy system");
                return Optional.of(provider);
            }
        }
        return Optional.empty();
    }
    
    private static NpcProvider fallbackToVanilla(@NotNull final TitansBattle plugin, @NotNull final String reason) {
        plugin.getLogger().warning(reason + ", falling back to vanilla mob proxies");
        return new VanillaProvider(plugin);
    }
}