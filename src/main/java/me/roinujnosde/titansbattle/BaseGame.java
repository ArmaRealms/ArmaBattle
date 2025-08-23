package me.roinujnosde.titansbattle;

import me.roinujnosde.titansbattle.BaseGameConfiguration.Prize;
import me.roinujnosde.titansbattle.events.GameFinishEvent;
import me.roinujnosde.titansbattle.events.GameStartEvent;
import me.roinujnosde.titansbattle.events.GroupDefeatedEvent;
import me.roinujnosde.titansbattle.events.LobbyStartEvent;
import me.roinujnosde.titansbattle.events.ParticipantDeathEvent;
import me.roinujnosde.titansbattle.events.PlayerExitGameEvent;
import me.roinujnosde.titansbattle.events.PlayerJoinGameEvent;
import me.roinujnosde.titansbattle.exceptions.CommandNotSupportedException;
import me.roinujnosde.titansbattle.hooks.papi.PlaceholderHook;
import me.roinujnosde.titansbattle.hooks.viaversion.ViaVersionHook;
import me.roinujnosde.titansbattle.managers.CommandManager;
import me.roinujnosde.titansbattle.managers.GameManager;
import me.roinujnosde.titansbattle.managers.GroupManager;
import me.roinujnosde.titansbattle.npc.NpcHandle;
import me.roinujnosde.titansbattle.npc.NpcProvider;
import me.roinujnosde.titansbattle.npc.event.NpcProxySpawnEvent;
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
import org.bukkit.Statistic;
import org.bukkit.WorldBorder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Stream;

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

    private static final double DEFAULT_MAX_HEALTH = 20.0D;
    private static final float DEFAULT_EXHAUSTION = 0.0F;
    private static final float DEFAULT_SATURATION = 5.0F;
    private static final int DEFAULT_MAX_FOOD_LEVEL = 20;
    protected final TitansBattle plugin;
    protected final GroupManager groupManager;
    protected final GameManager gameManager;
    protected final List<Warrior> participants = new ArrayList<>();
    protected final Map<Warrior, Group> groups = new HashMap<>();
    protected final HashMap<Warrior, Integer> killsCount = new HashMap<>();
    protected final Set<Warrior> casualties = new HashSet<>();
    protected final Set<Warrior> casualtiesWatching = new HashSet<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    protected BaseGameConfiguration config;
    protected boolean lobby;
    protected boolean preparation;
    protected boolean battle;
    private LobbyAnnouncementTask lobbyTask;

    protected BaseGame(@NotNull final TitansBattle plugin, final BaseGameConfiguration config) {
        this.plugin = plugin;
        this.groupManager = plugin.getGroupManager();
        this.gameManager = plugin.getGameManager();
        this.config = config;
        if (getConfig().isGroupMode() && groupManager == null) {
            throw new IllegalStateException("groupManager cannot be null in a group mode game");
        }
    }

    public void start() {
        if (getConfig().isGroupMode() && plugin.getGroupManager() == null) {
            throw new IllegalStateException("You cannot start a group based game without a supported Groups plugin!");
        }
        if (!getConfig().locationsSet()) {
            throw new IllegalStateException("You didn't set all locations!");
        }
        final LobbyStartEvent event = new LobbyStartEvent(this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        lobby = true;
        final Integer interval = getConfig().getAnnouncementStartingInterval();
        final Integer startingTimes = getConfig().getAnnouncementStartingTimes();
        lobbyTask = new LobbyAnnouncementTask(startingTimes, interval);
        addTask(lobbyTask.runTaskTimer(plugin, 0, interval * 20L));
        addTask(new LobbyWantingAnnouncementTask((startingTimes + 1L) * interval).runTaskTimerAsynchronously(plugin, 0, 20L));
    }

    public void finish(final boolean cancelled) {
        new GameFinishEvent(this).callEvent();

        // Clean up any remaining NPC proxies
        cleanupNpcProxies("game-end");

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

    /**
     * Clean up NPC proxies for all participants
     */
    private void cleanupNpcProxies(@NotNull final String reason) {
        if (!plugin.getNpcProvider().isAvailable()) {
            return;
        }

        for (final Warrior participant : participants) {
            final UUID playerId = participant.getUniqueId();
            if (plugin.getNpcProvider().isProxyAlive(playerId)) {
                plugin.debug("Cleaning up NPC proxy for " + participant.getName() + " (reason: " + reason + ")");
                plugin.getNpcProvider().despawnProxy(playerId, reason);
                plugin.getCombatLogService().clear(playerId);
            }
            // Clean up disconnect tracking for all participants when game ends
            plugin.getDisconnectTrackingService().clearPlayer(playerId);
        }
    }

    public abstract void setWinner(@NotNull Warrior warrior) throws CommandNotSupportedException;

    public void cancel(@NotNull final CommandSender sender) {
        broadcastKey("cancelled", sender.getName());
        finish(true);
    }

    public void onJoin(@NotNull final Warrior warrior) {
        if (!canJoin(warrior)) {
            plugin.debug(String.format("Warrior %s can't join", warrior.getName()));
            return;
        }

        final Player player = warrior.toOnlinePlayer();
        if (player == null) {
            plugin.debug(String.format("onJoin() -> player %s %s == null", warrior.getName(), warrior.getUniqueId()));
            return;
        }

        final int playtimeInSeconds = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
        final int minimumPlaytimeInSeconds = getConfig().getMinimumPlaytimeInSeconds();
        if (playtimeInSeconds < minimumPlaytimeInSeconds && !player.hasPermission("titansbattle.playtime-bypass")) {
            plugin.debug(String.format("Player %s has not enough playtime: %d < %d", player.getName(), playtimeInSeconds, minimumPlaytimeInSeconds));
            final StringBuilder formattedTime = getFormattedTime(minimumPlaytimeInSeconds, playtimeInSeconds);
            player.sendMessage(getLang("not.enough.playtime", formattedTime.toString()));
            return;
        }

        final ViaVersionHook viaVersionHook = plugin.getViaVersionHook();
        if (viaVersionHook != null) {
            if (viaVersionHook.isPlayerVersionBlocked(player)) {
                player.sendMessage(getLang("blocked.version"));
                return;
            }
            if (viaVersionHook.isPlayerVersionBlocked(player, getConfig())) {
                player.sendMessage(getLang("blocked.version"));
                return;
            }
        }

        if (!teleport(warrior, getConfig().getLobby())) {
            plugin.debug(String.format("Player %s is dead: %s", player, player.isDead()), false);
            player.sendMessage(getLang("teleport.error"));
            return;
        }

        SoundUtils.playSound(JOIN_GAME, plugin.getConfig(), player);
        participants.add(warrior);
        groups.put(warrior, warrior.getGroup());
        setKit(warrior);
        healAndClearEffects(warrior);
        broadcastKey("player_joined", warrior.getName());
        player.sendMessage(getLang("objective"));

        if (participants.size() == getConfig().getMaximumPlayers() && lobbyTask != null) {
            lobbyTask.processEnd();
        }
    }

    public void onChallengeJoin(@NotNull final Warrior warrior) {
        if (!canJoin(warrior)) {
            plugin.debug(String.format("Warrior %s can't join", warrior.getName()));
            return;
        }

        final Player player = warrior.toOnlinePlayer();
        if (player == null) {
            plugin.debug(String.format("onChallengeJoin() -> player %s %s == null", warrior.getName(), warrior.getUniqueId()));
            return;
        }

        if (!teleport(warrior, getConfig().getLobby())) {
            plugin.debug(String.format("Player %s is dead: %s", player, player.isDead()), false);
            player.sendMessage(getLang("teleport.error"));
            return;
        }

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

    public void onDeath(@NotNull final Warrior victim, @Nullable final Warrior killer) {
        plugin.debug(String.format("onDeath() -> victim %s, killer %s", victim.getName(), killer));
        if (!isParticipant(victim)) {
            return;
        }
        if (!isLobby()) {
            final ParticipantDeathEvent event = new ParticipantDeathEvent(victim, killer);
            Bukkit.getPluginManager().callEvent(event);
            casualties.add(victim);
            if (getConfig().isGroupMode()) {
                victim.sendMessage(getLang("watch_to_the_end"));
            }
            final String gameName = getConfig().getName();
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

    public boolean isParticipant(@NotNull final Warrior warrior) {
        return participants.contains(warrior);
    }

    public void onDisconnect(@NotNull final Warrior warrior) {
        if (!isParticipant(warrior)) {
            return;
        }
        if (getConfig().isUseKits()) {
            plugin.getConfigManager().getClearInventory().add(warrior.getUniqueId());
        }

        // Check if player is in combat and should have an NPC proxy created
        if (!isLobby() && getCurrentFighters().contains(warrior)) {
            final Player player = warrior.toOnlinePlayer();
            if (player != null && shouldCreateNpcProxy()) {
                try {
                    // Check if player hasn't exceeded disconnect limits
                    if (!plugin.getDisconnectTrackingService().trackDisconnection(warrior.getUniqueId())) {
                        plugin.debug("Player " + player.getName() + " exceeded disconnect limit, eliminating");
                        broadcastKey("player_eliminated_disconnect_limit", player.getName());
                        if (player != null) {
                            plugin.debug(String.format("onDisconnect() -> kill player %s (disconnect limit exceeded)", player.getName()));
                            player.setHealth(0);
                        }
                        return;
                    }

                    // Create NPC proxy instead of killing the player
                    final NpcProvider npcProvider = plugin.getNpcProvider();
                    if (npcProvider.isAvailable()) {
                        final double currentHealth = player.getHealth();
                        final Location location = player.getLocation();

                        final NpcHandle npcHandle = npcProvider.spawnProxy(player, location, currentHealth);

                        final NpcProxySpawnEvent event = new NpcProxySpawnEvent(warrior.getUniqueId(), npcHandle, currentHealth);
                        Bukkit.getPluginManager().callEvent(event);

                        plugin.debug(String.format("onDisconnect() -> spawned NPC proxy for %s with %.2f health (disconnect #%d)",
                                player.getName(), currentHealth, plugin.getDisconnectTrackingService().getDisconnectionCount(warrior.getUniqueId())));

                        broadcastKey("npc_proxy_spawned", player.getName());
                        return; // Don't continue with normal disconnect processing
                    } else {
                        plugin.debug("NPC provider not available, falling back to normal disconnect behavior");
                    }
                } catch (final Exception e) {
                    plugin.getLogger().warning("Failed to create NPC proxy for " + player.getName() + ": " + e.getMessage());
                    plugin.debug("onDisconnect() -> kill player " + player.getName() + " (NPC proxy failed)");
                }
            }

            // Fallback behavior: kill the player if NPC proxy creation failed or is disabled
            if (player != null) {
                plugin.debug(String.format("onDisconnect() -> kill player %s", player.getName()));
                player.setHealth(0);
            }
            return;
        }

        // Normal disconnect processing for non-combat situations
        casualties.add(warrior);
        casualtiesWatching.add(warrior); //adding to this Collection, so they are not teleported on respawn
        plugin.getConfigManager().getRespawn().add(warrior.getUniqueId());
        plugin.getConfigManager().save();
        processPlayerExit(warrior);
    }

    /**
     * Check if NPC proxy should be created based on configuration
     */
    private boolean shouldCreateNpcProxy() {
        return plugin.getConfig().getBoolean("battle.npcProxy.enabled", true);
    }

    public void onLeave(@NotNull final Warrior warrior) {
        if (!isParticipant(warrior)) {
            return;
        }
        if (getConfig().isUseKits()) {
            Kit.clearInventory(warrior.toOnlinePlayer());
        }
        final Player player = Objects.requireNonNull(warrior.toOnlinePlayer());
        if (!isLobby() && getCurrentFighters().contains(warrior)) {
            plugin.debug(String.format("onLeave() -> kill player %s", player.getName()));
            player.setHealth(0);
            return;
        }
        player.sendMessage(getLang("you-have-left"));
        SoundUtils.playSound(LEAVE_GAME, plugin.getConfig(), player);
        processPlayerExit(warrior);
    }

    public void onRespawn(final PlayerRespawnEvent event, @NotNull final Warrior warrior) {
        plugin.debug(String.format("onRespawn() -> warrior %s", warrior.getName()));
        if (casualties.contains(warrior) && !casualtiesWatching.contains(warrior)) {
            casualtiesWatching.add(warrior);
            event.setRespawnLocation(getConfig().getExit());
        }
    }

    public abstract boolean shouldClearDropsOnDeath(@NotNull Warrior warrior);

    public abstract boolean shouldKeepInventoryOnDeath(@NotNull Warrior warrior);

    public @Unmodifiable @NotNull List<Warrior> getParticipants() {
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
        final Map<Group, Integer> groupIntegerMap = new HashMap<>();
        for (final Warrior w : participants) {
            groupIntegerMap.compute(getGroup(w), (g, i) -> i == null ? 1 : i + 1);
        }
        return groupIntegerMap;
    }

    protected @Nullable Group getGroup(@NotNull final Warrior warrior) {
        return groups.get(warrior);
    }

    public Collection<Warrior> getCasualties() {
        return casualties;
    }

    public abstract @NotNull Collection<Warrior> getCurrentFighters();

    public Map<Warrior, Integer> getKillsCount() {
        return killsCount;
    }

    public void broadcastKey(@NotNull final String key, final Object... args) {
        broadcast(getLang(key), args);
    }

    public void discordAnnounce(@NotNull final String key, final Object... args) {
        plugin.sendDiscordMessage(getLang(key, args));
    }

    public void broadcast(@Nullable String message, final Object... args) {
        if (message == null || message.isEmpty()) {
            return;
        }
        message = MessageFormat.format(message, args);
        if (message.startsWith("!!broadcast")) {
            Bukkit.broadcast(message.replace("!!broadcast", ""), "titansbattle.broadcast");
        } else {
            for (final Warrior warrior : getParticipants()) {
                warrior.sendMessage(message);
            }
        }
    }

    protected void healAndClearEffects(@NotNull final Collection<Warrior> warriors) {
        warriors.forEach(this::healAndClearEffects);
    }

    protected void healAndClearEffects(@NotNull final Warrior warrior) {
        final Player player = warrior.toOnlinePlayer();
        if (player == null) return;

        player.setHealth(DEFAULT_MAX_HEALTH);
        player.setExhaustion(DEFAULT_EXHAUSTION);
        player.setSaturation(DEFAULT_SATURATION);
        player.setFoodLevel(DEFAULT_MAX_FOOD_LEVEL);
        player.setFireTicks(0);

        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    }

    @Override
    public int hashCode() {
        return getConfig().getName().hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof final BaseGame otherGame)) {
            return false;
        }
        return otherGame.getConfig().getName().equals(getConfig().getName());
    }

    public @NotNull String getLang(@NotNull final String key, final Object... args) {
        return plugin.getLang(key, this, args);
    }

    protected boolean teleport(@Nullable final Warrior warrior, @NotNull final Location destination) {
        final Player player = warrior != null ? warrior.toOnlinePlayer() : null;
        if (player == null) {
            return false;
        }
        SoundUtils.playSound(TELEPORT, plugin.getConfig(), player);
        return player.teleport(destination);
    }

    protected void addTask(@NotNull final BukkitTask task) {
        tasks.add(task);
    }

    protected void killTasks() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
    }

    protected void increaseKills(final Warrior warrior) {
        killsCount.compute(warrior, (p, i) -> i == null ? 1 : i + 1);
    }

    protected abstract void onLobbyEnd();

    protected abstract void processWinners();

    protected void givePrizes(final Prize prize, @Nullable final Group group, @Nullable final List<Warrior> warriors) {
        final List<Player> leaders = new ArrayList<>();
        final List<Player> members;
        if (warriors == null) return;
        final List<Player> players = new ArrayList<>(warriors.stream()
                .map(Warrior::toOnlinePlayer)
                .filter(Objects::nonNull)
                .toList());

        if (group != null) {
            members = new ArrayList<>();
            for (final Player p : players) {
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
        final GameStartEvent event = new GameStartEvent(this);
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
    protected boolean canJoin(@NotNull final Warrior warrior) {
        final Player player = warrior.toOnlinePlayer();
        if (player == null) {
            plugin.getLogger().log(Level.WARNING, "Joining player {0} ({1}) is null",
                    new Object[]{warrior.getName(), warrior.getUniqueId()});
            return false;
        }

        final PlayerJoinGameEvent event = new PlayerJoinGameEvent(warrior, player, this);
        Bukkit.getPluginManager().callEvent(event);
        plugin.debug("cancel: " + event.isCancelled());

        return !event.isCancelled();
    }

    protected void processPlayerExit(@NotNull final Warrior warrior) {
        plugin.debug(String.format("processPlayerExit() -> warrior %s", warrior.getName()));
        if (!isParticipant(warrior)) {
            return;
        }
        final Player player = warrior.toOnlinePlayer();
        if (player != null) {
            teleport(warrior, getConfig().getExit());
            final PlayerExitGameEvent event = new PlayerExitGameEvent(player, this);
            Bukkit.getPluginManager().callEvent(event);
        }
        participants.remove(warrior);
        final Group group = getGroup(warrior);
        if (!isLobby()) {
            runCommandsAfterBattle(List.of(warrior));
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

    /**
     * Eliminate a player from the game (add to casualties and process exit)
     *
     * @param warrior the warrior to eliminate
     * @param reason  the reason for elimination (for logging)
     */
    public void eliminate(@NotNull final Warrior warrior, @NotNull final String reason) {
        plugin.debug(String.format("eliminate() -> warrior %s, reason: %s", warrior.getName(), reason));

        if (!isParticipant(warrior)) {
            plugin.debug("Warrior " + warrior.getName() + " is not a participant, cannot eliminate");
            return;
        }

        // Add to casualties
        casualties.add(warrior);
        casualtiesWatching.add(warrior);

        // Process the player exit
        processPlayerExit(warrior);
    }

    protected abstract void processRemainingPlayers(@NotNull Warrior warrior);

    protected void setKit(@NotNull final Warrior warrior) {
        final Player player = warrior.toOnlinePlayer();
        final Kit kit = getConfig().getKit();
        if (getConfig().isUseKits() && kit != null && player != null) {
            Kit.clearInventory(player);
            kit.set(player);
        }
    }

    protected void playDeathSound(@NotNull final Warrior victim) {
        final Stream<Player> players = getPlayerParticipantsStream();
        if (!getConfig().isGroupMode()) {
            players.forEach(p -> SoundUtils.playSound(ENEMY_DEATH, plugin.getConfig(), p));
            return;
        }

        if (groupManager == null) {
            return;
        }

        final Group victimGroup = groupManager.getGroup(victim.getUniqueId());
        players.forEach(participant -> {
            final Group group = groupManager.getGroup(participant.getUniqueId());
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
            final int remainingPlayers = getRemainingOpponents();
            final int remainingGroups = getRemainingOpponentGroups(p);
            if (Math.min(remainingPlayers, remainingGroups) <= 0) {
                return;
            }
            MessageUtils.sendActionBar(p, MessageFormat.format(getLang("action-bar-remaining-opponents"), remainingPlayers, remainingGroups));
        });
    }

    protected int getRemainingOpponentGroups(@NotNull final Player player) {
        int opponents = 0;
        final Warrior warrior = plugin.getDatabaseManager().getWarrior(player);
        for (final Map.Entry<Group, Integer> entry : getGroupParticipants().entrySet()) {
            final Group group = entry.getKey();
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

    protected void runCommandsBeforeBattle(@NotNull final Collection<Warrior> warriors) {
        runCommands(warriors, getConfig().getCommandsBeforeBattle());
    }

    protected void runCommandsAfterBattle(@NotNull final Collection<Warrior> warriors) {
        runCommands(warriors, getConfig().getCommandsAfterBattle());
    }

    protected void runCommands(@NotNull final Collection<Warrior> warriors, @Nullable final Collection<String> commands) {
        if (commands == null) return;
        final PlaceholderHook hook = plugin.getPlaceholderHook();

        for (final String command : commands) {
            for (final Warrior warrior : warriors) {
                final Player player = warrior.toOnlinePlayer();
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

    protected void teleport(@NotNull final Collection<Warrior> warriors, @NotNull final Location destination) {
        warriors.forEach(warrior -> teleport(warrior, destination));
    }

    protected void teleportAll(final Location destination) {
        getParticipants().forEach(player -> teleport(player, destination));
    }

    protected void teleportToArena(final List<Warrior> warriors) {
        final List<Location> arenaEntrances = new ArrayList<>(getConfig().getArenaEntrances().values());
        if (arenaEntrances.size() == 1) {
            teleport(warriors, arenaEntrances.getFirst());
            return;
        }

        if (config.isGroupMode()) {
            final List<Group> groupList = warriors.stream().map(this::getGroup).distinct().toList();

            for (int i = 0; i < groupList.size(); i++) {
                final Set<Warrior> groupWarriors = Objects.requireNonNull(plugin.getGroupManager()).getWarriors(groupList.get(i));
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
    protected void broadcastDeathMessage(@NotNull final Warrior victim, @Nullable final Warrior killer) {
        if (killer == null) {
            broadcastKey("died_by_himself", victim.getName());
        } else {
            final ItemStack itemInHand = Objects.requireNonNull(killer.toOnlinePlayer()).getItemInHand();
            String weaponName = getLang("fist");
            if (itemInHand != null && itemInHand.getType() != Material.AIR) {
                final ItemMeta itemMeta = itemInHand.getItemMeta();
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
        plugin.debug("startPreparation()");
        preparation = true;
        addTask(new PreparationTimeTask().runTaskLater(plugin, getConfig().getPreparationTime() * 20L));
        addTask(new CountdownTitleTask(getCurrentFighters(), getConfig().getPreparationTime()).runTaskTimer(plugin, 0L, 20L));
    }

    public boolean isPreparation() {
        return preparation;
    }

    private ChatColor getColor(final long timer) {
        ChatColor color = GREEN;
        if (timer <= 3) {
            color = RED;
        } else if (timer <= 7) {
            color = YELLOW;
        }
        return color;
    }

    private @NotNull StringBuilder getFormattedTime(final int minimumPlaytimeInSeconds, final int playerPlaytimeInSeconds) {
        final int remainingSeconds = minimumPlaytimeInSeconds - playerPlaytimeInSeconds;
        final Duration duration = Duration.ofSeconds(remainingSeconds);
        final StringBuilder formattedTime = new StringBuilder();

        final long hours = duration.toHours();
        final long minutes = duration.toMinutesPart();
        final long seconds = duration.toSecondsPart();

        if (hours > 0) {
            formattedTime.append(String.format("%02dh ", hours));
        }
        if (minutes > 0 || hours > 0) {
            formattedTime.append(String.format("%02dm ", minutes));
        }
        formattedTime.append(String.format("%02ds", seconds));
        return formattedTime;
    }

    public class LobbyAnnouncementTask extends BukkitRunnable {
        private final long interval;
        private int times;

        public LobbyAnnouncementTask(final int times, final long interval) {
            this.times = times + 1;
            this.interval = interval;
        }

        @Override
        public void run() {
            final long seconds = times * interval;
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

        public LobbyWantingAnnouncementTask(final long seconds) {
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

        public BorderTask(final WorldBorder worldBorder) {
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
                shrinkSize = currentSize - getConfig().getBorderFinalSize();
                if (shrinkSize <= 0) {
                    return;
                }
                newSize = getConfig().getBorderFinalSize();
            }

            getPlayerParticipantsStream().forEach(player -> {
                player.sendTitle(getLang("border.title"), getLang("border.subtitle"));
                SoundUtils.playSound(BORDER, getConfig().getFileConfiguration(), player);
            });

            worldBorder.setSize(newSize, shrinkSize);
            currentSize = newSize;
        }

    }

    public class PreparationTimeTask extends BukkitRunnable {

        @Override
        public void run() {
            broadcastKey("preparation_over");
            runCommandsBeforeBattle(getCurrentFighters());
            preparation = false;
            battle = true;

            if (getConfig().isWorldBorder()) {
                final long borderInterval = getConfig().getBorderInterval() * 20L;
                final WorldBorder worldBorder = getConfig().getBorderCenter().getWorld().getWorldBorder();
                addTask(new BorderTask(worldBorder).runTaskTimer(plugin, borderInterval, borderInterval));
            }
        }
    }

    public class CountdownTitleTask extends BukkitRunnable {

        private final Collection<Warrior> warriors;
        private int timer;

        public CountdownTitleTask(final Collection<Warrior> warriors, int timer) {
            this.warriors = warriors;
            if (timer < 0) {
                timer = 0;
            }
            this.timer = timer;
        }

        @SuppressWarnings("deprecation")
        @Override
        public void run() {
            final List<Player> players = warriors.stream().map(Warrior::toOnlinePlayer).filter(Objects::nonNull).toList();
            final String title;
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
