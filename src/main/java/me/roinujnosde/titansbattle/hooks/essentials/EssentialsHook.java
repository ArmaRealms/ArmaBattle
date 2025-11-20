package me.roinujnosde.titansbattle.hooks.essentials;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Hook for checking player playtime using Bukkit statistics
 */
public class EssentialsHook {

    private static final Logger LOGGER = Logger.getLogger("TitansBattle");

    /**
     * Get the playtime of a player in seconds
     * @param player the player to check
     * @return playtime in seconds
     */
    public long getPlaytime(@NotNull Player player) {
        try {
            // Get playtime in ticks (PLAY_ONE_TICK statistic for older versions, or TOTAL_WORLD_TIME)
            // 20 ticks = 1 second
            try {
                // Try newer statistic first (1.13+)
                long ticks = player.getStatistic(Statistic.valueOf("PLAY_ONE_MINUTE"));
                return ticks / 20;
            } catch (IllegalArgumentException e) {
                // Fallback for older versions
                try {
                    long ticks = player.getStatistic(Statistic.valueOf("PLAY_ONE_TICK"));
                    return ticks / 20;
                } catch (IllegalArgumentException ex) {
                    LOGGER.warning("Could not find appropriate playtime statistic");
                    return 0;
                }
            }
        } catch (Exception e) {
            LOGGER.warning(String.format("Error getting playtime for %s: %s", player.getName(), e.getMessage()));
            return 0;
        }
    }

    /**
     * Check if a player has the minimum required playtime
     * @param player the player to check
     * @param minimumSeconds minimum playtime required in seconds
     * @return true if player has enough playtime or if check is disabled
     */
    public boolean hasMinimumPlaytime(@NotNull Player player, int minimumSeconds) {
        if (minimumSeconds <= 0) {
            return true;
        }

        long playtime = getPlaytime(player);
        return playtime >= minimumSeconds;
    }
    
    /**
     * Format playtime in seconds to a readable string
     * @param seconds playtime in seconds
     * @return formatted string (e.g., "2d 5h 30m")
     */
    public String formatPlaytime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m");
        }
        
        return sb.toString().trim();
    }
}
