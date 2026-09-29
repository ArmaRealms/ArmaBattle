package me.roinujnosde.titansbattle.listeners;

import me.roinujnosde.titansbattle.TitansBattle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Protects the vanilla npc proxies from damage, knockback and other unwanted interactions. Only
 * entities tagged as proxies are affected, so normal players and mobs keep their behaviour.
 *
 * @author RoinujNosde
 */
public class NpcProxyListener extends TBListener {

    public NpcProxyListener(@NotNull final TitansBattle plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(@NotNull final EntityDamageEvent event) {
        if (plugin.getNpcProxyManager().isProxy(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onKnockback(@NotNull final EntityKnockbackEvent event) {
        if (plugin.getNpcProxyManager().isProxy(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCombust(@NotNull final EntityCombustEvent event) {
        if (plugin.getNpcProxyManager().isProxy(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(@NotNull final PlayerInteractEntityEvent event) {
        if (plugin.getNpcProxyManager().isProxy(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }
}
