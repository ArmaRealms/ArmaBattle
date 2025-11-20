package me.roinujnosde.titansbattle.types;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Represents a message that can be sent between servers via Redis
 */
public class CrossServerMessage {
    
    private static final Gson GSON = new Gson();
    
    private final String type;
    private final String sourceServer;
    private final String targetServer;
    private final List<String> targetWorlds;
    private final String message;
    private final long timestamp;
    private final String gameId;
    
    public CrossServerMessage(@NotNull String type, 
                             @NotNull String sourceServer,
                             @NotNull String targetServer,
                             @Nullable List<String> targetWorlds,
                             @NotNull String message,
                             @Nullable String gameId) {
        this.type = type;
        this.sourceServer = sourceServer;
        this.targetServer = targetServer;
        this.targetWorlds = targetWorlds;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.gameId = gameId;
    }
    
    @NotNull
    public String getType() {
        return type;
    }
    
    @NotNull
    public String getSourceServer() {
        return sourceServer;
    }
    
    @NotNull
    public String getTargetServer() {
        return targetServer;
    }
    
    @Nullable
    public List<String> getTargetWorlds() {
        return targetWorlds;
    }
    
    @NotNull
    public String getMessage() {
        return message;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Nullable
    public String getGameId() {
        return gameId;
    }
    
    /**
     * Serializes the message to JSON
     */
    @NotNull
    public String toJson() {
        return GSON.toJson(this);
    }
    
    /**
     * Deserializes a message from JSON
     */
    @Nullable
    public static CrossServerMessage fromJson(@NotNull String json) {
        try {
            return GSON.fromJson(json, CrossServerMessage.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Message types for cross-server communication
     */
    public static class MessageType {
        public static final String GAME_STARTING = "GAME_STARTING";
        public static final String GAME_STARTED = "GAME_STARTED";
        public static final String GAME_ENDED = "GAME_ENDED";
        public static final String PLAYER_JOINED = "PLAYER_JOINED";
        public static final String PLAYER_DIED = "PLAYER_DIED";
        public static final String NEW_KILLER = "NEW_KILLER";
        public static final String ANNOUNCEMENT = "ANNOUNCEMENT";
    }
}