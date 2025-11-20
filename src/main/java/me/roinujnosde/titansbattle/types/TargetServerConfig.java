package me.roinujnosde.titansbattle.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Configuration for a target server in the Redis network
 */
public class TargetServerConfig {
    
    private final String serverName;
    private final boolean enabled;
    private final List<String> worlds;
    
    public TargetServerConfig(@NotNull String serverName, boolean enabled, @Nullable List<String> worlds) {
        this.serverName = serverName;
        this.enabled = enabled;
        this.worlds = worlds;
    }
    
    @NotNull
    public String getServerName() {
        return serverName;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Gets the target worlds for this server
     * @return List of world names, or null/empty for all worlds
     */
    @Nullable
    public List<String> getWorlds() {
        return worlds;
    }
    
    /**
     * Checks if this server should receive messages for the specified world
     */
    public boolean shouldReceiveForWorld(@Nullable String worldName) {
        if (!enabled) {
            return false;
        }
        
        // If no worlds specified, accept all
        if (worlds == null || worlds.isEmpty()) {
            return true;
        }
        
        // Check if world is in the list
        return worldName != null && worlds.contains(worldName);
    }
    
    /**
     * Creates a TargetServerConfig from a configuration map
     */
    @NotNull
    public static TargetServerConfig fromMap(@NotNull String serverName, @NotNull Map<String, Object> config) {
        boolean enabled = (Boolean) config.getOrDefault("enabled", false);
        @SuppressWarnings("unchecked")
        List<String> worlds = (List<String>) config.get("worlds");
        
        return new TargetServerConfig(serverName, enabled, worlds);
    }
}