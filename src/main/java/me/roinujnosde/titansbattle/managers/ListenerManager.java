package me.roinujnosde.titansbattle.managers;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.listeners.EntityDamageListener;
import me.roinujnosde.titansbattle.listeners.ItemsProtectionListener;
import me.roinujnosde.titansbattle.listeners.JoinGameListener;
import me.roinujnosde.titansbattle.listeners.PlayerCommandPreprocessListener;
import me.roinujnosde.titansbattle.listeners.PlayerDeathListener;
import me.roinujnosde.titansbattle.listeners.PlayerJoinListener;
import me.roinujnosde.titansbattle.listeners.PlayerMoveListener;
import me.roinujnosde.titansbattle.listeners.PlayerQuitListener;
import me.roinujnosde.titansbattle.listeners.PlayerRespawnListener;
import me.roinujnosde.titansbattle.listeners.PlayerTeleportListener;
import me.roinujnosde.titansbattle.listeners.ProjectileLaunchListener;
import me.roinujnosde.titansbattle.listeners.SimpleClansListener;
import me.roinujnosde.titansbattle.listeners.TBListener;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

import java.util.HashSet;
import java.util.Set;

public class ListenerManager {

    private final TitansBattle plugin;
    private final Set<TBListener> registered = new HashSet<>();

    public ListenerManager(TitansBattle plugin) {
        this.plugin = plugin;
    }

    public void registerGeneralListeners() {
        registerListener(new PlayerQuitListener(plugin), true);
        registerListener(new PlayerJoinListener(plugin), true);
        registerListener(new ItemsProtectionListener(plugin), true);
    }

    public void registerBattleListeners() {
        registerListener(new PlayerRespawnListener(plugin));
        registerListener(new PlayerCommandPreprocessListener(plugin));
        registerListener(new PlayerDeathListener(plugin));
        registerListener(new EntityDamageListener(plugin));
        registerListener(new PlayerTeleportListener(plugin));
        registerListener(new JoinGameListener(plugin));
        registerListener(new PlayerMoveListener(plugin));
        registerListener(new ProjectileLaunchListener(plugin));
        if (Bukkit.getPluginManager().isPluginEnabled("SimpleClans")) {
            registerListener(new SimpleClansListener(plugin));
        }
        plugin.getLogger().info("Registering battle listeners...");
    }

    public void unregisterBattleListeners() {
        if (plugin.getGameManager().getCurrentGame().isPresent()) {
            return;
        }
        if (!plugin.getChallengeManager().getChallenges().isEmpty()) {
            return;
        }
        for (TBListener listener : registered) {
            HandlerList.unregisterAll(listener);
        }
        registered.clear();
        plugin.getLogger().info("Unregistering battle listeners...");
    }

    private void registerListener(TBListener listener, boolean permanent) {
        if (permanent || add(listener)) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
            plugin.debug(String.format("Registering %s", listener.getClass().getName()));
        }
    }

    private void registerListener(TBListener listener) {
        registerListener(listener, false);
    }

    private boolean add(TBListener listener) {
        for (TBListener rl : registered) {
            if (rl.getClass().equals(listener.getClass())) {
                return false;
            }
        }
        registered.add(listener);
        return true;
    }
}
