package me.roinujnosde.titansbattle.managers;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.games.Game;
import me.roinujnosde.titansbattle.types.CrossServerMessage;
import me.roinujnosde.titansbattle.types.TargetServerConfig;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages Redis connections and cross-server messaging
 */
public class RedisManager {
    
    private static final String CHANNEL_PREFIX = "titansbattle:";
    private static final String BROADCAST_CHANNEL = CHANNEL_PREFIX + "broadcast";
    
    private final TitansBattle plugin;
    private JedisPool jedisPool;
    private CrossServerMessageListener messageListener;
    private boolean enabled = false;
    
    public RedisManager(@NotNull TitansBattle plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initializes the Redis connection
     */
    public void initialize() {
        ConfigManager config = plugin.getConfigManager();
        
        if (!config.isRedisEnabled()) {
            plugin.getLogger().info("Redis is disabled in configuration");
            return;
        }
        
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(20);
            poolConfig.setMaxIdle(10);
            poolConfig.setMinIdle(1);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            
            String password = config.getRedisPassword();
            String username = config.getRedisUsername();
            
            if (password != null && !password.trim().isEmpty()) {
                if (username != null && !username.trim().isEmpty()) {
                    // Username and password authentication
                    jedisPool = new JedisPool(poolConfig, 
                        config.getRedisHost(), 
                        config.getRedisPort(), 
                        config.getRedisTimeout(),
                        username,
                        password,
                        config.getRedisDatabase());
                } else {
                    // Password-only authentication
                    jedisPool = new JedisPool(poolConfig, 
                        config.getRedisHost(), 
                        config.getRedisPort(), 
                        config.getRedisTimeout(),
                        password,
                        config.getRedisDatabase());
                }
            } else {
                // No authentication
                jedisPool = new JedisPool(poolConfig, 
                    config.getRedisHost(), 
                    config.getRedisPort(), 
                    config.getRedisTimeout());
            }
            
            // Test connection
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
                plugin.getLogger().info("Successfully connected to Redis server");
            }
            
            // Start message listener if not master
            if (!config.isRedisMaster()) {
                startMessageListener();
            }
            
            enabled = true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to Redis server", e);
            enabled = false;
        }
    }
    
    /**
     * Shuts down the Redis connection
     */
    public void shutdown() {
        enabled = false;
        
        if (messageListener != null) {
            messageListener.unsubscribe();
            messageListener = null;
        }
        
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
    
    /**
     * Checks if Redis is enabled and connected
     */
    public boolean isEnabled() {
        return enabled && jedisPool != null && !jedisPool.isClosed();
    }
    
    /**
     * Publishes a cross-server message to all target servers
     */
    public void publishMessage(@NotNull String messageType, @NotNull String message, @Nullable String gameId) {
        if (!isEnabled() || !plugin.getConfigManager().isRedisMaster()) {
            return;
        }
        
        // Check if this message type is enabled
        java.util.List<String> enabledTypes = plugin.getConfigManager().getEnabledMessageTypes();
        if (!enabledTypes.contains(messageType)) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                Map<String, TargetServerConfig> targetServers = plugin.getConfigManager().getRedisTargetServers();
                
                for (TargetServerConfig serverConfig : targetServers.values()) {
                    if (!serverConfig.isEnabled()) {
                        continue;
                    }
                    
                    CrossServerMessage crossMessage = new CrossServerMessage(
                        messageType,
                        plugin.getConfigManager().getRedisServerName(),
                        serverConfig.getServerName(),
                        serverConfig.getWorlds(),
                        message,
                        gameId
                    );
                    
                    String channel = CHANNEL_PREFIX + serverConfig.getServerName();
                    jedis.publish(channel, crossMessage.toJson());
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to publish Redis message", e);
            }
        });
    }
    
    /**
     * Starts the message listener for this server
     */
    private void startMessageListener() {
        String serverName = plugin.getConfigManager().getRedisServerName();
        String channel = CHANNEL_PREFIX + serverName;
        
        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                messageListener = new CrossServerMessageListener();
                plugin.getLogger().info("Starting Redis message listener on channel: " + channel);
                jedis.subscribe(messageListener, channel);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Redis message listener error", e);
            }
        });
    }
    
    /**
     * Redis message listener for cross-server messages
     */
    private class CrossServerMessageListener extends JedisPubSub {
        
        @Override
        public void onMessage(String channel, String message) {
            try {
                CrossServerMessage crossMessage = CrossServerMessage.fromJson(message);
                if (crossMessage == null) {
                    return;
                }
                
                // Process message on main thread
                Bukkit.getScheduler().runTask(plugin, () -> processMessage(crossMessage));
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error processing Redis message", e);
            }
        }
        
        @Override
        public void onSubscribe(String channel, int subscribedChannels) {
            plugin.getLogger().info("Subscribed to Redis channel: " + channel);
        }
        
        @Override
        public void onUnsubscribe(String channel, int subscribedChannels) {
            plugin.getLogger().info("Unsubscribed from Redis channel: " + channel);
        }
    }
    
    /**
     * Processes a received cross-server message
     */
    private void processMessage(@NotNull CrossServerMessage message) {
        // Check if message is for this server
        String serverName = plugin.getConfigManager().getRedisServerName();
        if (!serverName.equals(message.getTargetServer())) {
            return;
        }
        
        // Check if this message type is enabled on this slave server
        java.util.List<String> enabledTypes = plugin.getConfigManager().getEnabledMessageTypes();
        if (!enabledTypes.contains(message.getType())) {
            return;
        }
        
        DatabaseManager databaseManager = plugin.getDatabaseManager();
        GameManager gameManager = plugin.getGameManager();
        
        // Filter by world if specified
        if (message.getTargetWorlds() != null && !message.getTargetWorlds().isEmpty()) {
            // Only broadcast to players in specified worlds
            for (String worldName : message.getTargetWorlds()) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    for (Player player : world.getPlayers()) {
                        // Players in games always see messages, others only if enabled
                        boolean isInGame = gameManager.getCurrentGame()
                            .map(game -> game.getParticipants().stream()
                                .anyMatch(w -> w.getUniqueId().equals(player.getUniqueId())))
                            .orElse(false);
                        
                        if (isInGame || databaseManager.hasMessagesEnabled(player.getUniqueId())) {
                            player.sendMessage(message.getMessage());
                        }
                    }
                }
            }
        } else {
            // Broadcast to all online players who have messages enabled or are in a game
            for (Player player : Bukkit.getOnlinePlayers()) {
                boolean isInGame = gameManager.getCurrentGame()
                    .map(game -> game.getParticipants().stream()
                        .anyMatch(w -> w.getUniqueId().equals(player.getUniqueId())))
                    .orElse(false);
                
                if (isInGame || databaseManager.hasMessagesEnabled(player.getUniqueId())) {
                    player.sendMessage(message.getMessage());
                }
            }
        }
        
        plugin.getLogger().info(String.format("Processed cross-server message from %s: %s", 
            message.getSourceServer(), message.getType()));
    }
}