/*
 * The MIT License
 *
 * Copyright 2017 Edson Passos - edsonpassosjr@outlook.com.
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
package me.roinujnosde.titansbattle.managers;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.types.Event;
import me.roinujnosde.titansbattle.types.GameConfiguration;
import me.roinujnosde.titansbattle.types.Prizes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author RoinujNosde
 */
public class TaskManager {

    private final TitansBattle plugin = TitansBattle.getInstance();
    BukkitTask giveItemsTask;
    private Timer schedulerTimer;

    public void setupScheduler() {
        if (schedulerTimer != null) {
            schedulerTimer.cancel();
        }
        if (!plugin.getConfigManager().isScheduler()) {
            return;
        }
        schedulerTimer = new Timer("TitansBattle Scheduler", true);

        boolean loggedMonthly = false;
        for (Event event : plugin.getConfigManager().getEvents()) {
            TimerTask task = createTimerTask(event);
            if (event.frequency() == Event.Frequency.MONTHLY) {
                schedulerTimer.schedule(task, event.getDelay());
                if (!loggedMonthly) {
                    plugin.getLogger().info("Scheduled a monthly event. This event will be repeated only after a restart.");
                    loggedMonthly = true;
                }
                continue;
            }
            schedulerTimer.scheduleAtFixedRate(task, event.getDelay(), event.frequency().getPeriod());
        }
    }

    public void startGiveItemsTask(long interval) {
        interval = interval * 20;
        if (giveItemsTask != null) {
            giveItemsTask.cancel();
        }
        giveItemsTask = new GiveItemsTask().runTaskTimer(plugin, interval, interval);
    }

    @Contract(value = "_ -> new", pure = true)
    private @NotNull TimerTask createTimerTask(Event event) {
        return new TimerTask() {
            @Override
            public void run() {
                Optional<GameConfiguration> config = plugin.getConfigurationDao()
                        .getConfiguration(event.gameName(), GameConfiguration.class);
                if (config.isEmpty()) {
                    plugin.getLogger().warning(String.format("Game %s not found!", event.gameName()));
                    return;
                }
//                if (plugin.getGameManager().getCurrentGame().isPresent()) {
//                    plugin.getLogger().info("There is a game running. Skipping event.");
//                    return;
//                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getGameManager().getCurrentGame().ifPresent(game -> game.cancel(Bukkit.getConsoleSender()));
                    plugin.getGameManager().start(config.get());
                });
            }
        };
    }

    private class GiveItemsTask extends BukkitRunnable {

        @Override
        public void run() {
            if (!Prizes.getPlayersWithItemsToReceive().isEmpty()) {
                Iterator<Entry<Player, Collection<ItemStack>>> iterator = Prizes.getPlayersWithItemsToReceive()
                        .entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry<Player, Collection<ItemStack>> entry = iterator.next();
                    Player player = entry.getKey();
                    Collection<ItemStack> remainingItems = player.getInventory().addItem(entry.getValue()
                            .toArray(new ItemStack[0])).values();
                    if (remainingItems.isEmpty()) {
                        iterator.remove();
                    } else {
                        entry.setValue(remainingItems);
                        player.sendMessage(MessageFormat.format(plugin.getLang("items_to_receive"),
                                remainingItems.size()));
                    }
                }
            } else {
                giveItemsTask.cancel();
            }
        }
    }

}
