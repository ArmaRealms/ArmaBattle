package me.roinujnosde.titansbattle;

import me.roinujnosde.titansbattle.events.GameStartEvent;
import me.roinujnosde.titansbattle.events.GroupDefeatedEvent;
import me.roinujnosde.titansbattle.events.LobbyStartEvent;
import me.roinujnosde.titansbattle.events.ParticipantDeathEvent;
import me.roinujnosde.titansbattle.events.PlayerExitGameEvent;
import me.roinujnosde.titansbattle.events.PlayerJoinGameEvent;
import me.roinujnosde.titansbattle.exceptions.CommandNotSupportedException;
import me.roinujnosde.titansbattle.hooks.papi.PlaceholderHook;
import me.roinujnosde.titansbattle.managers.CommandManager;
import me.roinujnosde.titansbattle.managers.GameManager;
import me.roinujnosde.titansbattle.managers.GroupManager;
import me.roinujnosde.titansbattle.types.Group;
import me.roinujnosde.titansbattle.types.Kit;
import me.roinujnosde.titansbattle.types.Warrior;
import me.roinujnosde.titansbattle.utils.MessageUtils;
import me.roinujnosde.titansbattle.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Stream;

import static me.roinujnosde.titansbattle.BaseGameConfiguration.Prize;
import static me.roinujnosde.titansbattle.utils.SoundUtils.Type.ALLY_DEATH;
import static me.roinujnosde.titansbattle.utils.SoundUtils.Type.BORDER;
import static me.roinujnosde.titansbattle.utils.SoundUtils.Type.ENEMY_DEATH;
import static me.roinujnosde.titansbattle.utils.SoundUtils.Type.JOIN_GAME;
import static me.roinujnosde.titansbattle.utils.SoundUtils.Type.LEAVE_GAME;
import static me.roinujnosde.titansbattle.utils.SoundUtils.Type.TELEPORT;
import static org.bukkit.ChatColor.GREEN;
import static org.bukkit.ChatColor.RED;
import static org.bukkit.ChatColor.YELLOW;

public abstract class BaseGame {

    protected final TitansBattle plugin;
    protected final GroupManager groupManager;
    protected final GameManager gameManager;

    protected BaseGameConfiguration config;
    protected boolean lobby;
    protected boolean preparation;
    protected boolean battle;
    protected final List<Warrior> participants = new ArrayList<>();
    protected final Map<Warrior, Group> groups = new HashMap<>();
    protected final HashMap<Warrior, Integer> killsCount = new HashMap<>();
    protected final Set<Warrior> casualties = new HashSet<>();
    protected final Set<Warrior> casualtiesWatching = new HashSet<>();

    private final List<BukkitTask> tasks = new ArrayList<>();
    private LobbyAnnouncementTask lobbyTask;

    protected BaseGame(@NotNull TitansBattle plugin, BaseGameConfiguration config) {
        this.plugin = plugin;
        this.groupManager = plugin.getGroupManager();
        this.gameManager = plugin.getGameManager();
        this.config = config;
        if (getConfig().isGroupMode() && groupManager == null) {
            throw new IllegalStateException("gameManager cannot be null in a group mode game");
        }
    }

    public void start() {
        if (getConfig().isGroupMode() && plugin.getGroupManager() == null) {
            throw new IllegalStateException("You cannot start a group based game without a supported Groups plugin!");
        }
        if (!getConfig().locationsSet()) {
            throw new IllegalStateException("You didn't set all locations!");
        }
        LobbyStartEvent event = new LobbyStartEvent(this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        lobby = true;
        Integer interval = getConfig().getAnnouncementStartingInterval();
        Integer startingTimes = getConfig().getAnnouncementStartingTimes();
        lobbyTask = new LobbyAnnouncementTask(startingTimes, interval);
        addTask(lobbyTask.runTaskTimer(plugin, 0, interval * 20L));
        addTask(new LobbyWantingAnnouncementTask((startingTimes + 1L) * interval).runTaskTimerAsynchronously(plugin, 0, 20L));
    }

    public void finish(boolean cancelled) {
        teleportAll(getConfig().getExit());
        killTasks();
        runCommandsAfterBattle(getParticipants());
        if (getConfig().isUseKits()) {
            getPlayerParticipantsStream().forEach(Kit::clearInventory);
        }
        if (getConfig().isWorldBorder()) {
            getConfig().getBorderCenter().getWorld().getWorldBorder().reset();
        }
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getDatabaseManager().saveAll());
        if (!cancelled) {
            processWinners();
        }
    }

    public abstract void setWinner(@NotNull Warrior warrior) throws CommandNotSupportedException;

    public void cancel(@NotNull CommandSender sender) {
        broadcastKey("cancelled", sender.getName());
        finish(true);
    }

    public void onJoin(@NotNull Warrior warrior) {
        if (!canJoin(warrior)) {
            plugin.debug(String.format("Warrior %s can't join", warrior.getName()));
            return;
        }
        Player player = warrior.toOnlinePlayer();
        if (player == null) {
            plugin.debug(String.format("onJoin() -> player %s %s == null", warrior.getName(), warrior.getUniqueId()));
            return;
        }
        if (!teleport(warrior, getConfig().getLobby())) {
            plugin.debug(String.format("Player %s is dead: %s", player, player.isDead()), false);
            player.sendMessage(getLang("teleport.error"));
            return;
        }

        healPlayer(player);
        SoundUtils.playSound(JOIN_GAME, plugin.getConfig(), player);
        participants.add(warrior);
        groups.put(warrior, warrior.getGroup());
        setKit(warrior);
        broadcastKey("player_joined", warrior.getName());
        player.sendMessage(getLang("objective"));
        if (participants.size() == getConfig().getMaximumPlayers() && lobbyTask != null) {
            lobbyTask.processEnd();
        }
    }

    public void onChallengeJoin(@NotNull Warrior warrior) {
        if (!canJoin(warrior)) {
            plugin.debug(String.format("Warrior %s can't join", warrior.getName()));
            return;
        }

        Player player = warrior.toOnlinePlayer();
        if (player == null) {
            plugin.debug(String.format("onChallengeJoin() -> player %s %s == null", warrior.getName(), warrior.getUniqueId()));
            return;
        }

        if (!teleport(warrior, getConfig().getLobby())) {
            plugin.debug(String.format("Player %s is dead: %s", player, player.isDead()), false);
            player.sendMessage(getLang("teleport.error"));
            return;
        }

        healPlayer(player);
        SoundUtils.playSound(JOIN_GAME, plugin.getConfig(), player);
        participants.add(warrior);
        groups.put(warrior, warrior.getGroup());
        setKit(warrior);
        broadcastKey("challenge.player_joined", warrior.getName());
        player.sendMessage(getLang("objective"));
        if (participants.size() == getConfig().getMaximumPlayers() && lobbyTask != null) {
            lobbyTask.processEnd();
        }
    }

    public void onDeath(@NotNull Warrior victim, @Nullable Warrior killer) {
        if (!isParticipant(victim)) {
            return;
        }
        if (!isLobby()) {
            ParticipantDeathEvent event = new ParticipantDeathEvent(victim, killer);
            Bukkit.getPluginManager().callEvent(event);
            String gameName = getConfig().getName();
            casualties.add(victim);
            if (getConfig().isGroupMode()) {
                victim.sendMessage(getLang("watch_to_the_end"));
            }
            if (killer != null) {
                killer.increaseKills(gameName);
                increaseKills(killer);
            }
            victim.increaseDeaths(gameName);
            playDeathSound(victim);
        }
        broadcastDeathMessage(victim, killer);
        processPlayerExit(victim);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isLobby() {
        return lobby;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public abstract boolean isInBattle(@NotNull Warrior warrior);

    public @NotNull BaseGameConfiguration getConfig() {
        return config;
    }

    public boolean isParticipant(@NotNull Warrior warrior) {
        return participants.contains(warrior);
    }

    public void onDisconnect(@NotNull Warrior warrior) {
        if (!isParticipant(warrior)) {
            return;
        }
        if (getConfig().isUseKits()) {
            plugin.getConfigManager().getClearInventory().add(warrior.getUniqueId());
        }
        if (!isLobby() && getCurrentFighters().contains(warrior)) {
            Player player = warrior.toOnlinePlayer();
            if (player != null) {
                player.setHealth(0);
            }
            return;
        }
        casualties.add(warrior);
        casualtiesWatching.add(warrior); //adding to this Collection, so they are not teleported on respawn
        plugin.getConfigManager().getRespawn().add(warrior.getUniqueId());
        plugin.getConfigManager().save();
        processPlayerExit(warrior);
    }

    public void onLeave(@NotNull Warrior warrior) {
        if (!isParticipant(warrior)) {
            return;
        }
        if (getConfig().isUseKits()) {
            Kit.clearInventory(warrior.toOnlinePlayer());
        }
        Player player = Objects.requireNonNull(warrior.toOnlinePlayer());
        if (!isLobby() && getCurrentFighters().contains(warrior)) {
            player.setHealth(0);
            return;
        }
        player.sendMessage(getLang("you-have-left"));
        SoundUtils.playSound(LEAVE_GAME, plugin.getConfig(), player);
        processPlayerExit(warrior);
    }

    public void onRespawn(@NotNull Warrior warrior) {
        if (casualties.contains(warrior) && !casualtiesWatching.contains(warrior)) {
            teleport(warrior, getConfig().getWatchroom());
            casualtiesWatching.add(warrior);
        }
    }

    public abstract boolean shouldClearDropsOnDeath(@NotNull Warrior warrior);

    public abstract boolean shouldKeepInventoryOnDeath(@NotNull Warrior warrior);

    public @NotNull List<Warrior> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    @NotNull
    protected Stream<Player> getPlayerParticipantsStream() {
        return getParticipants().stream().map(Warrior::toOnlinePlayer).filter(Objects::nonNull);
    }

    public Map<Group, Integer> getGroupParticipants() {
        if (!getConfig().isGroupMode()) {
            return Collections.emptyMap();
        }
        Map<Group, Integer> groupIntegerMap = new HashMap<>();
        for (Warrior w : participants) {
            groupIntegerMap.compute(getGroup(w), (g, i) -> i == null ? 1 : i + 1);
        }
        return groupIntegerMap;
    }

    protected @Nullable Group getGroup(@NotNull Warrior warrior) {
        return groups.get(warrior);
    }

    public Collection<Warrior> getCasualties() {
        return casualties;
    }

    public abstract @NotNull Collection<Warrior> getCurrentFighters();

    public Map<Warrior, Integer> getKillsCount() {
        return killsCount;
    }

    public void broadcastKey(@NotNull String key, Object... args) {
        broadcast(getLang(key), args);
    }

    public void discordAnnounce(@NotNull String key, Object... args) {
        plugin.sendDiscordMessage(getLang(key, args));
    }

    public void broadcast(@Nullable String message, Object... args) {
        if (message == null || message.isEmpty()) {
            return;
        }
        message = MessageFormat.format(message, args);
        if (message.startsWith("!!broadcast")) {
            Bukkit.broadcast(message.replace("!!broadcast", ""), "titansbattle.broadcast");
        } else {
            for (Warrior warrior : getParticipants()) {
                warrior.sendMessage(message);
            }
        }
    }

    @Override
    public int hashCode() {
        return getConfig().getName().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BaseGame otherGame)) {
            return false;
        }
        return otherGame.getConfig().getName().equals(getConfig().getName());
    }

    public @NotNull String getLang(@NotNull String key, Object... args) {
        return plugin.getLang(key, this, args);
    }

    protected boolean teleport(@Nullable Warrior warrior, @NotNull Location destination) {
        plugin.debug(String.format("teleport() -> destination %s", destination));
        Player player = warrior != null ? warrior.toOnlinePlayer() : null;
        if (player == null) {
            plugin.debug(String.format("teleport() -> warrior %s", warrior));
            return false;
        }
        SoundUtils.playSound(TELEPORT, plugin.getConfig(), player);
        return player.teleport(destination);
    }

    protected void addTask(@NotNull BukkitTask task) {
        tasks.add(task);
    }

    protected void killTasks() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
    }

    protected void increaseKills(Warrior warrior) {
        killsCount.compute(warrior, (p, i) -> i == null ? 1 : i + 1);
    }

    protected abstract void onLobbyEnd();

    protected abstract void processWinners();

    protected void givePrizes(Prize prize, @Nullable Group group, @Nullable List<Warrior> warriors) {
        List<Player> leaders = new ArrayList<>();
        List<Player> members;
        if (warriors == null) return;
        List<Player> players = new ArrayList<>(warriors.stream()
                .map(Warrior::toOnlinePlayer)
                .filter(Objects::nonNull)
                .toList());

        if (group != null) {
            members = new ArrayList<>();
            for (Player p : players) {
                if (group.isLeaderOrOfficer(p.getUniqueId())) {
                    leaders.add(p);
                } else {
                    members.add(p);
                }
            }
        } else {
            members = new ArrayList<>(players);
        }
        getConfig().getPrizes(prize).give(plugin, leaders, members);
    }

    protected boolean canStartBattle() {
        GameStartEvent event = new GameStartEvent(this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            broadcastKey("cancelled", "Server");
            return false;
        }
        if (getParticipants().size() < getConfig().getMinimumPlayers()) {
            broadcastKey("not_enough_participants");
            return false;
        }
        if (getConfig().isGroupMode() && getGroupParticipants().size() < getConfig().getMinimumGroups()) {
            broadcastKey("not_enough_participants");
            return false;
        }
        return true;
    }

    @SuppressWarnings({"BooleanMethodIsAlwaysInverted"})
    protected boolean canJoin(@NotNull Warrior warrior) {
        Player player = warrior.toOnlinePlayer();
        if (player == null) {
            plugin.getLogger().log(Level.WARNING, "Joining player {0} ({1}) is null",
                    new Object[]{warrior.getName(), warrior.getUniqueId()});
            return false;
        }

        PlayerJoinGameEvent event = new PlayerJoinGameEvent(warrior, player, this);
        Bukkit.getPluginManager().callEvent(event);
        plugin.debug("cancel: " + event.isCancelled());

        return !event.isCancelled();
    }

    protected void processPlayerExit(@NotNull Warrior warrior) {
        if (!isParticipant(warrior)) {
            return;
        }
        Player player = warrior.toOnlinePlayer();
        if (player != null) {
            teleport(warrior, getConfig().getExit());
            PlayerExitGameEvent event = new PlayerExitGameEvent(player, this);
            Bukkit.getPluginManager().callEvent(event);
        }
        participants.remove(warrior);
        Group group = getGroup(warrior);
        if (!isLobby()) {
            runCommandsAfterBattle(Collections.singletonList(warrior));
            processRemainingPlayers(warrior);
            //last participant
            if (getConfig().isGroupMode() && group != null && !getGroupParticipants().containsKey(group)) {
                broadcastKey("group_defeated", group.getName());
                Bukkit.getPluginManager().callEvent(new GroupDefeatedEvent(group, warrior.toOnlinePlayer()));
                group.getData().increaseDefeats(getConfig().getName());
            }
            sendRemainingOpponentsCount();
        }
    }

    protected abstract void processRemainingPlayers(@NotNull Warrior warrior);

    protected void setKit(@NotNull Warrior warrior) {
        Player player = warrior.toOnlinePlayer();
        Kit kit = getConfig().getKit();
        if (getConfig().isUseKits() && kit != null && player != null) {
            Kit.clearInventory(player);
            kit.set(player);
        }
    }

    protected void playDeathSound(@NotNull Warrior victim) {
        Stream<Player> players = getPlayerParticipantsStream();
        if (!getConfig().isGroupMode()) {
            players.forEach(p -> SoundUtils.playSound(ENEMY_DEATH, plugin.getConfig(), p));
            return;
        }

        if (groupManager == null) {
            return;
        }

        Group victimGroup = groupManager.getGroup(victim.getUniqueId());
        players.forEach(participant -> {
            Group group = groupManager.getGroup(participant.getUniqueId());
            if (group == null) {
                return;
            }
            if (group.equals(victimGroup)) {
                SoundUtils.playSound(ALLY_DEATH, plugin.getConfig(), participant);
            } else {
                SoundUtils.playSound(ENEMY_DEATH, plugin.getConfig(), participant);
            }
        });
    }

    protected void sendRemainingOpponentsCount() {
        getPlayerParticipantsStream().forEach(p -> {
            int remainingPlayers = getRemainingOpponents();
            int remainingGroups = getRemainingOpponentGroups(p);
            if (Math.min(remainingPlayers, remainingGroups) <= 0) {
                return;
            }
            MessageUtils.sendActionBar(p, MessageFormat.format(getLang("action-bar-remaining-opponents"), remainingPlayers, remainingGroups));
        });
    }

    protected int getRemainingOpponentGroups(@NotNull Player player) {
        int opponents = 0;
        Warrior warrior = plugin.getDatabaseManager().getWarrior(player);
        for (Map.Entry<Group, Integer> entry : getGroupParticipants().entrySet()) {
            Group group = entry.getKey();
            if (group.equals(getGroup(warrior))) {
                continue;
            }
            opponents += entry.getValue();
        }
        return opponents;
    }

    protected int getRemainingOpponents() {
        return getParticipants().size() - 1;
    }

    protected void runCommandsBeforeBattle(@NotNull Collection<Warrior> warriors) {
        runCommands(warriors, getConfig().getCommandsBeforeBattle());
    }

    protected void runCommandsAfterBattle(@NotNull Collection<Warrior> warriors) {
        runCommands(warriors, getConfig().getCommandsAfterBattle());
    }

    protected void runCommands(@NotNull Collection<Warrior> warriors, @Nullable Collection<String> commands) {
        if (commands == null) return;
        PlaceholderHook hook = plugin.getPlaceholderHook();

        for (String command : commands) {
            for (Warrior warrior : warriors) {
                Player player = warrior.toOnlinePlayer();
                if (player == null) {
                    continue;
                }
                if (!command.contains("%player%")) { // Runs the command once when %player% is not used
                    CommandManager.dispatchCommand(Bukkit.getConsoleSender(), hook.parse((OfflinePlayer) null, command));
                    break;
                }
                CommandManager.dispatchCommand(Bukkit.getConsoleSender(), hook.parse(warrior, command,
                        "%player%", warrior.getName()));
            }
        }
    }

    protected void teleport(@NotNull Collection<Warrior> warriors, @NotNull Location destination) {
        warriors.forEach(warrior -> teleport(warrior, destination));
    }

    protected void teleportAll(Location destination) {
        getParticipants().forEach(player -> teleport(player, destination));
    }

    protected void teleportToArena(List<Warrior> warriors) {
        List<Location> arenaEntrances = new ArrayList<>(getConfig().getArenaEntrances().values());
        if (arenaEntrances.size() == 1) {
            teleport(warriors, arenaEntrances.get(0));
            return;
        }

        if (config.isGroupMode()) {
            List<Group> groupList = warriors.stream().map(this::getGroup).distinct().toList();

            for (int i = 0; i < groupList.size(); i++) {
                Set<Warrior> groupWarriors = Objects.requireNonNull(plugin.getGroupManager()).getWarriors(groupList.get(i));
                groupWarriors.retainAll(warriors);

                teleport(groupWarriors, arenaEntrances.get(i % arenaEntrances.size()));
            }
        } else {
            for (int i = 0; i < warriors.size(); i++) {
                teleport(warriors.get(i), arenaEntrances.get(i % arenaEntrances.size()));
            }
        }
    }

    @SuppressWarnings("deprecation")
    protected void broadcastDeathMessage(@NotNull Warrior victim, @Nullable Warrior killer) {
        if (killer == null) {
            broadcastKey("died_by_himself", victim.getName());
        } else {
            ItemStack itemInHand = Objects.requireNonNull(killer.toOnlinePlayer()).getItemInHand();
            String weaponName = getLang("fist");
            if (itemInHand != null && itemInHand.getType() != Material.AIR) {
                ItemMeta itemMeta = itemInHand.getItemMeta();
                if (itemMeta != null && itemMeta.hasDisplayName()) {
                    weaponName = itemMeta.getDisplayName();
                } else {
                    weaponName = itemInHand.getType().name().replace("_", " ").toLowerCase();
                }
            }
            broadcastKey("killed_by", victim.getName(), killsCount.getOrDefault(victim, 0), killer.getName(), killsCount.get(killer), weaponName);
        }
    }

    protected void startPreparation() {
        preparation = true;
        addTask(new PreparationTimeTask().runTaskLater(plugin, getConfig().getPreparationTime() * 20L));
        addTask(new CountdownTitleTask(getCurrentFighters(), getConfig().getPreparationTime()).runTaskTimer(plugin, 0L, 20L));
        if (getConfig().isWorldBorder()) {
            long borderInterval = getConfig().getBorderInterval() * 20L;
            WorldBorder worldBorder = getConfig().getBorderCenter().getWorld().getWorldBorder();
            addTask(new BorderTask(worldBorder).runTaskTimer(plugin, borderInterval, borderInterval));
        }
    }

    public boolean isPreparation() {
        return preparation;
    }

    private ChatColor getColor(long timer) {
        ChatColor color = GREEN;
        if (timer <= 3) {
            color = RED;
        } else if (timer <= 7) {
            color = YELLOW;
        }
        return color;
    }

    public void healPlayer(@NotNull Player player) {
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.setFoodLevel(20);
        player.setFireTicks(0);
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute == null) return;
        player.setHealth(attribute.getDefaultValue());
    }

    public class LobbyAnnouncementTask extends BukkitRunnable {
        private int times;
        private final long interval;

        public LobbyAnnouncementTask(int times, long interval) {
            this.times = times + 1;
            this.interval = interval;
        }

        @Override
        public void run() {
            long seconds = times * interval;
            if (times > 0) {
                broadcastKey("starting_game", seconds, getConfig().getMinimumGroups(), getConfig().getMinimumPlayers(), getGroupParticipants().size(), getParticipants().size());
                times--;
            } else {
                processEnd();
            }
        }

        public void processEnd() {
            if (canStartBattle()) {
                lobby = false;
                onLobbyEnd();
                addTask(new GameExpirationTask().runTaskLater(plugin, getConfig().getExpirationTime() * 20L));
            } else {
                broadcastKey("cancelled", "Server");
                finish(true);
            }
            this.cancel();
            lobbyTask = null;
        }
    }

    public class LobbyWantingAnnouncementTask extends BukkitRunnable {
        private long seconds;

        public LobbyWantingAnnouncementTask(long seconds) {
            this.seconds = seconds;
        }

        @Override
        public void run() {
            if (seconds > 0) {
                seconds--;
                participants.stream()
                        .map(Warrior::toOnlinePlayer)
                        .filter(Objects::nonNull)
                        .forEach(p -> p.sendTitle(getColor(seconds) + "" + seconds, ""));
            } else {
                this.cancel();
            }
        }
    }

    public class BorderTask extends BukkitRunnable {

        private final WorldBorder worldBorder;
        private int currentSize;

        public BorderTask(WorldBorder worldBorder) {
            this.worldBorder = worldBorder;
            currentSize = getConfig().getBorderInitialSize();
            worldBorder.setCenter(getConfig().getBorderCenter());
            worldBorder.setSize(currentSize);
            worldBorder.setDamageAmount(getConfig().getBorderDamage());
            worldBorder.setDamageBuffer(0);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void run() {
            int shrinkSize = getConfig().getBorderShrinkSize();
            int newSize = currentSize - shrinkSize;

            if (getConfig().getBorderFinalSize() > newSize) {
                this.cancel();
                return;
            }
            worldBorder.setSize(newSize, shrinkSize);
            getPlayerParticipantsStream().forEach(player -> {
                player.sendTitle(getLang("border.title"), getLang("border.subtitle"));
                SoundUtils.playSound(BORDER, getConfig().getFileConfiguration(), player);
            });
            currentSize = newSize;
        }

    }

    public class PreparationTimeTask extends BukkitRunnable {

        @Override
        public void run() {
            broadcastKey("preparation_over");
            runCommandsBeforeBattle(getCurrentFighters());
            battle = true;
            preparation = false;
        }
    }

    public class CountdownTitleTask extends BukkitRunnable {

        private final Collection<Warrior> warriors;
        private int timer;

        public CountdownTitleTask(Collection<Warrior> warriors, int timer) {
            this.warriors = warriors;
            if (timer < 0) {
                timer = 0;
            }
            this.timer = timer;
        }

        @SuppressWarnings("deprecation")
        @Override
        public void run() {
            List<Player> players = warriors.stream().map(Warrior::toOnlinePlayer).filter(Objects::nonNull).toList();
            String title;
            if (timer > 0) {
                title = getColor(timer) + "" + timer;
            } else {
                title = RED + getLang("title.fight");
                this.cancel();
                Bukkit.getScheduler().runTaskLater(plugin, () -> players.forEach(Player::resetTitle), 20L);
            }
            players.forEach(player -> player.sendTitle(title, ""));
            timer--;
        }
    }

    public class GameExpirationTask extends BukkitRunnable {

        @Override
        public void run() {
            gameManager.getCurrentGame().ifPresent(game -> {
                game.finish(true);
                broadcastKey("game_expired");
            });
        }
    }

}
