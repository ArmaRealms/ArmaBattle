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
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Event fired when an NPC proxy is about to be spawned for a disconnected player
 *
 * @author RoinujNosde
 */
public class NpcProxySpawnEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final UUID ownerId;
    private final NpcHandle npcHandle;
    private final double initialHealth;
    private boolean cancelled = false;

    public NpcProxySpawnEvent(@NotNull final UUID ownerId, @Nullable final NpcHandle npcHandle, final double initialHealth) {
        this.ownerId = ownerId;
        this.npcHandle = npcHandle;
        this.initialHealth = initialHealth;
    }

    /**
     * Get the UUID of the player this proxy represents
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
     * Note: This will be null when the event is fired before spawning (for cancellation checks)
     *
     * @return the NPC handle, or null if not yet spawned
     */
    @Nullable
    public NpcHandle getNpcHandle() {
        return npcHandle;
    }

    /**
     * Get the initial health the NPC will be spawned with
     *
     * @return the initial health
     */
    public double getInitialHealth() {
        return initialHealth;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
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