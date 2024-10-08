package me.roinujnosde.titansbattle.utils;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.dao.ConfigurationDao;
import me.roinujnosde.titansbattle.games.Game;
import me.roinujnosde.titansbattle.types.GameConfiguration;
import me.roinujnosde.titansbattle.types.Warrior;
import me.roinujnosde.titansbattle.types.Winners;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class Helper {

    private static final TitansBattle plugin = TitansBattle.getInstance();

    private Helper() {
    }

    @Nullable
    public static GameConfiguration getGameConfigurationFromWinnerOrKiller(@Nullable Player player) {
        if (player == null) {
            return null;
        }
        final UUID uniqueId = player.getUniqueId();
        ConfigurationDao dao = plugin.getConfigurationDao();
        for (GameConfiguration game : dao.getConfigurations(GameConfiguration.class)) {
            String gameName = game.getName();
            Winners w = plugin.getDatabaseManager().getLatestWinners();
            if (w.getKiller(gameName) == null) {
                continue;
            }
            if (w.getKiller(gameName).equals(uniqueId)) {
                return game;
            }
            List<UUID> playerWinners = w.getPlayerWinners(gameName);
            if (playerWinners == null) {
                continue;
            }
            if (playerWinners.contains(uniqueId)) {
                return game;
            }
        }
        return null;
    }

    @Nullable
    public static FileConfiguration getConfigFromWinnerOrKiller(Player player) {
        if (player == null) {
            return null;
        }
        GameConfiguration gameConfig = getGameConfigurationFromWinnerOrKiller(player);
        if (gameConfig != null) {
            return gameConfig.getFileConfiguration();
        }
        return null;
    }

    /**
     * Returns whether he is a Winner or not
     *
     * @param player the player
     * @return true if he is a Winner
     */
    public static boolean isWinner(Player player) {
        ConfigurationDao dao = plugin.getConfigurationDao();
        for (GameConfiguration game : dao.getConfigurations(GameConfiguration.class)) {
            List<UUID> winners = plugin.getDatabaseManager().getLatestWinners().getPlayerWinners(game.getName());
            if (winners == null) {
                continue;
            }
            if (winners.contains(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether he is a Winner or not
     *
     * @param player the player
     * @return true if he is a Winner
     */
    public static boolean isKiller(Player player) {
        ConfigurationDao dao = plugin.getConfigurationDao();
        for (GameConfiguration game : dao.getConfigurations(GameConfiguration.class)) {
            Winners latestWinners = plugin.getDatabaseManager().getLatestWinners();
            UUID killer = latestWinners.getKiller(game.getName());
            if (killer == null) {
                continue;
            }
            if (killer.equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if Killer is set as priority for this Killer's game config
     *
     * @param player the killer
     * @return true if it is priority
     */
    public static boolean isKillerPriority(Player player) {
        if (isKiller(player)) {
            ConfigurationDao dao = plugin.getConfigurationDao();
            for (GameConfiguration game : dao.getConfigurations(GameConfiguration.class)) {
                Winners latestWinners = plugin.getDatabaseManager().getLatestWinners();
                UUID killer = latestWinners.getKiller(game.getName());
                if (killer == null) {
                    continue;
                }
                if (killer.equals(player.getUniqueId())) {
                    return game.isKillerPriority();
                }
            }
        }
        return false;
    }

    /**
     * Checks if Killer Join Message is enabled for this Killer's game config
     *
     * @param player the killer
     * @return if it is enabled
     */
    public static boolean isKillerJoinMessageEnabled(Player player) {
        if (isKiller(player)) {
            ConfigurationDao dao = plugin.getConfigurationDao();
            for (GameConfiguration game : dao.getConfigurations(GameConfiguration.class)) {
                Winners latestWinners = plugin.getDatabaseManager().getLatestWinners();
                UUID killer = latestWinners.getKiller(game.getName());
                if (killer == null) {
                    continue;
                }
                if (killer.equals(player.getUniqueId())) {
                    return game.isKillerJoinMessage();
                }
            }
        }
        return false;
    }

    /**
     * Checks if Killer Quit Message is enabled for this Killer's game config
     *
     * @param player the killer
     * @return if it is enabled
     */
    public static boolean isKillerQuitMessageEnabled(Player player) {
        if (isKiller(player)) {
            ConfigurationDao dao = plugin.getConfigurationDao();
            for (GameConfiguration game : dao.getConfigurations(GameConfiguration.class)) {
                Winners latestWinners = plugin.getDatabaseManager().getLatestWinners();
                UUID killer = latestWinners.getKiller(game.getName());
                if (killer == null) {
                    continue;
                }
                if (killer.equals(player.getUniqueId())) {
                    return game.isKillerQuitMessage();
                }
            }
        }
        return false;
    }

    @NotNull
    public static List<UUID> warriorListToUuidList(@NotNull List<Warrior> players) {
        return players.stream().map(Warrior::getUniqueId).toList();
    }

    /**
     * Converts a list of Strings to a list of UUIDs
     *
     * @param list the String list
     * @return the UUID list
     */
    public static List<UUID> stringListToUuidList(List<String> list) {
        List<UUID> uuidList = new ArrayList<>();
        for (String uuid : list) {
            uuidList.add(UUID.fromString(uuid));
        }
        return uuidList;
    }

    /**
     * Converts a list of UUIDs to a list of Strings using
     * {@link org.bukkit.Bukkit#getOfflinePlayer(java.util.UUID) getOfflinePlayer}
     *
     * @param list the UUID list
     * @return the String list
     */
    public static List<String> uuidListToPlayerNameList(List<UUID> list) {
        if (list == null) {
            return null;
        }
        List<String> playerNameList = new ArrayList<>();
        for (UUID uuid : list) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.getName() != null) {
                playerNameList.add(offlinePlayer.getName());
            }
        }
        return playerNameList;
    }

    /**
     * Converts a UUID list to a String list using {@link java.util.UUID#toString() toString}
     *
     * @param list the UUID list
     * @return the String list
     */
    public static List<String> uuidListToStringList(List<UUID> list) {
        List<String> stringList = new ArrayList<>();
        for (UUID uuid : list) {
            stringList.add(uuid.toString());
        }
        return stringList;
    }

    /**
     * Checks if Winner Join Message is enabled for this Winner's game config
     *
     * @param player the winner
     * @return if it is enabled
     */
    public static boolean isWinnerJoinMessageEnabled(Player player) {
        if (isKiller(player)) {
            ConfigurationDao dao = plugin.getConfigurationDao();
            for (GameConfiguration game : dao.getConfigurations(GameConfiguration.class)) {
                Winners latestWinners = plugin.getDatabaseManager().getLatestWinners();
                List<UUID> playerWinners = latestWinners.getPlayerWinners(game.getName());
                if (playerWinners == null) {
                    continue;
                }
                if (playerWinners.contains(player.getUniqueId())) {
                    return game.isWinnerJoinMessage();
                }
            }
        }
        return false;
    }

    /**
     * Checks if Winner Quit Message is enabled for this Winner's game config
     *
     * @param player the winner
     * @return if it is enabled
     */
    public static boolean isWinnerQuitMessageEnabled(Player player) {
        if (isWinner(player)) {
            ConfigurationDao dao = plugin.getConfigurationDao();
            for (GameConfiguration game : dao.getConfigurations(GameConfiguration.class)) {
                Winners latestWinners = plugin.getDatabaseManager().getLatestWinners();
                List<UUID> playerWinners = latestWinners.getPlayerWinners(game.getName());
                if (playerWinners == null) {
                    continue;
                }
                if (playerWinners.contains(player.getUniqueId())) {
                    return game.isWinnerQuitMessage();
                }
            }
        }
        return false;
    }

    /**
     * Gets a Player's attacker or Killer from an {@link org.bukkit.entity.Entity}
     *
     * @param entity the entity to investigate
     * @return the attacker/killer or null
     */
    public static @Nullable Player getPlayerAttackerOrKiller(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player playerShooter) {
                return playerShooter;
            }
        }
        return null;
    }

    /**
     * Gets a String representation of a String Collection
     * Example: "RoinujNosde, GhostTheWolf & Killer07"
     *
     * @param collection the String collection
     * @return the String representation
     */
    public static @NotNull String buildStringFrom(@NotNull Collection<String> collection) {
        StringBuilder sb = new StringBuilder();
        List<String> list = new ArrayList<>(collection);
        for (String s : list) {
            Game currentGame = plugin.getGameManager().getCurrentGame().orElse(null);
            String listColor = plugin.getLang("list-color", currentGame);
            sb.append(listColor);
            if (s.equalsIgnoreCase(list.get(0))) {
                sb.append(s);
            } else if (s.equals(list.get(list.size() - 1))) {
                sb.append(" & ");
                sb.append(s);
            } else {
                sb.append(", ");
                sb.append(s);
            }
        }
        return sb.toString();
    }

    /**
     * Gets a String filled with the specified quantity of spaces
     *
     * @param spaces the spaces
     * @return the spaces filled String
     */
    public static String getSpaces(int spaces) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < spaces; i++) {
            s.append(" ");
        }
        return s.toString();
    }

    /**
     * Gets the length of this number
     *
     * @param integer the number
     * @return the length
     */
    public static int getLength(int integer) {
        return String.valueOf(integer).length();
    }

    /**
     * Checks if the dates are equals, ignoring the time
     *
     * @param date1 date 1
     * @param date2 date 2
     * @return true if they are equals
     */
    public static boolean equalDates(Date date1, Date date2) {
        Calendar d1 = Calendar.getInstance();
        d1.setTime(date1);
        Calendar d2 = Calendar.getInstance();
        d2.setTime(date2);

        boolean sameYear = d1.get(Calendar.YEAR) == d2.get(Calendar.YEAR);
        boolean sameMonth = d1.get(Calendar.MONTH) == d2.get(Calendar.MONTH);
        boolean sameDay = d1.get(Calendar.DAY_OF_MONTH) == d2.get(Calendar.DAY_OF_MONTH);
        return sameDay && sameMonth && sameYear;
    }

    public static <V> @NotNull Map<String, V> caseInsensitiveMap() {
        return caseInsensitiveMap(null);
    }

    public static <V> @NotNull Map<String, V> caseInsensitiveMap(@Nullable Map<String, V> map) {
        TreeMap<String, V> treeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (map != null) {
            treeMap.putAll(map);
        }
        return treeMap;
    }
}
