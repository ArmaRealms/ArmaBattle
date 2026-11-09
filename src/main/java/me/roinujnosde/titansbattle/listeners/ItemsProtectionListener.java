package me.roinujnosde.titansbattle.listeners;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.types.Kit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemsProtectionListener extends TBListener {

    public ItemsProtectionListener(@NotNull TitansBattle plugin) {
        super(plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void on(InventoryCloseEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> clearItems((Player) event.getPlayer(), "inventory-close"), 1L);
    }

    @EventHandler
    public void on(PlayerRespawnEvent event) {
        clearItems(event.getPlayer(), "respawn");
    }

    @EventHandler
    public void on(PlayerJoinEvent event) {
        clearItems(event.getPlayer(), "login");
    }

    private void clearItems(@NotNull Player player, @NotNull String reason) {
        // Player is in a game, it's possible (and normal) that they have a kit
        if (plugin.getBaseGameFrom(player) != null) {
            return;
        }

        Kit.removeKitItems(plugin, player, reason);
    }

    @EventHandler(ignoreCancelled = true)
    public void on(CraftItemEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack result = inventory.getResult();

        for (ItemStack item : inventory.getMatrix()) {
            if (item != null && Kit.isKitItem(item)) {
                Kit.applyNBTTag(result);
                return;
            }
        }
    }

}
