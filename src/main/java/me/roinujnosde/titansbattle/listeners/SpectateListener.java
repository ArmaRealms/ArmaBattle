package me.roinujnosde.titansbattle.listeners;

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.events.GameFinishEvent;
import me.roinujnosde.titansbattle.events.GroupWinEvent;
import me.roinujnosde.titansbattle.events.PlayerWinEvent;
import me.roinujnosde.titansbattle.managers.ConfigManager;
import me.roinujnosde.titansbattle.managers.SpectateManager;
import me.roinujnosde.titansbattle.utils.Helper;
import org.bukkit.Location;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Trident;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataContainer;

import java.text.MessageFormat;

public class SpectateListener extends TBListener {

    private final ConfigManager configManager;
    private final SpectateManager spectateManager;

    public SpectateListener(final TitansBattle plugin) {
        super(plugin);
        this.configManager = plugin.getConfigManager();
        this.spectateManager = plugin.getSpectateManager();
    }

    @EventHandler
    public void onGameFinish(final GameFinishEvent event) {
        spectateManager.removeAllSpectators();
    }

    @EventHandler
    public void onGroupWin(final GroupWinEvent event) {
        spectateManager.removeAllSpectators();
    }

    @EventHandler
    public void onPlayerWin(final PlayerWinEvent event) {
        spectateManager.removeAllSpectators();
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        if (spectateManager.isSpectating(player)) {
            plugin.debug(String.format("Player %s quit while spectating. Removing from spectator list.", player.getName()));
            spectateManager.removeSpectator(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        if (!spectateManager.isSpectating(player)) return;

        for (final String command : configManager.getAllowedCommandsInSpectator()) {
            if (event.getMessage().startsWith(command)) return;
        }

        if (!player.hasPermission("titansbattle.command-bypass")) {
            player.sendMessage(MessageFormat.format(plugin.getLang("command-not-allowed-in-spectator"), event.getMessage()));
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.COMMAND) {
            return;
        }
        final Player player = event.getPlayer();
        if (spectateManager.isSpectating(player) && !player.hasPermission("titansbattle.teleport-bypass")) {
            plugin.debug(String.format("Player %s tried to teleport while spectating. Teleport cancelled.", event.getPlayer().getName()));
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        cancelSpectatorAction(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerAttemptPickupItem(final PlayerAttemptPickupItemEvent event) {
        cancelSpectatorAction(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        final Player damager = Helper.getPlayerAttackerOrKiller(event.getDamager());
        if (damager != null) {
            cancelSpectatorAction(event, damager);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageEvent event) {
        if (event.getEntity() instanceof final Player player && spectateManager.isSpectating(player)) {
            player.setFireTicks(0);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(final FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof final Player player) {
            cancelSpectatorAction(event, player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityCombust(final EntityCombustEvent event) {
        if (event.getEntity() instanceof final Player player) {
            cancelSpectatorAction(event, player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupArrow(final PlayerPickupArrowEvent event) {
        cancelSpectatorAction(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupExperience(final PlayerPickupExperienceEvent event) {
        cancelSpectatorAction(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(final PotionSplashEvent event) {
        for (final LivingEntity ent : event.getAffectedEntities()) {
            if (ent instanceof final Player player && spectateManager.isSpectating(player)) {
                // Define a intensidade só para este jogador como zero
                event.setIntensity(ent, 0.0);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityPotionEffect(final EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof final Player player
                && event.getCause() != EntityPotionEffectEvent.Cause.PLUGIN
                && event.getAction() == EntityPotionEffectEvent.Action.ADDED) {
            cancelSpectatorAction(event, player);
        }
    }

    private void cancelSpectatorAction(final Cancellable event, final Player player) {
        if (spectateManager.isSpectating(player)) {
            event.setCancelled(true);
        }
    }
}


