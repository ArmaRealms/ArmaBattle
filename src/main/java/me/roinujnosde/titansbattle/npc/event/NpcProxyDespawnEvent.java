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
package me.roinujnosde.titansbattle.npc.event;

import me.roinujnosde.titansbattle.npc.NpcHandle;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Event fired when an NPC proxy is despawned (player rejoined)
 *
 * @author RoinujNosde
 */
public class NpcProxyDespawnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final UUID ownerId;
    private final NpcHandle npcHandle;
    private final String reason;

    public NpcProxyDespawnEvent(@NotNull UUID ownerId, @NotNull NpcHandle npcHandle, @NotNull String reason) {
        this.ownerId = ownerId;
        this.npcHandle = npcHandle;
        this.reason = reason;
    }

    /**
     * Get the UUID of the player this proxy represented
     *
     * @return the owner's UUID
     */
    @NotNull
    public UUID getOwnerId() {
        return ownerId;
    }

    /**
     * Get the NPC handle
     *
     * @return the NPC handle
     */
    @NotNull
    public NpcHandle getNpcHandle() {
        return npcHandle;
    }

    /**
     * Get the reason for despawning
     *
     * @return the reason
     */
    @NotNull
    public String getReason() {
        return reason;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}