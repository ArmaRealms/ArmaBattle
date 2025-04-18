package me.roinujnosde.titansbattle.listeners;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.events.GameFinishEvent;
import me.roinujnosde.titansbattle.events.GroupWinEvent;
import me.roinujnosde.titansbattle.events.PlayerWinEvent;
import me.roinujnosde.titansbattle.managers.ConfigManager;
import me.roinujnosde.titansbattle.managers.SpectateManager;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.text.MessageFormat;

public class SpectateListener extends TBListener {

    private final ConfigManager configManager;
    private final SpectateManager spectateManager;

    public SpectateListener(TitansBattle plugin) {
        super(plugin);
        this.configManager = plugin.getConfigManager();
        this.spectateManager = plugin.getSpectateManager();
    }

    @EventHandler
    public void onGameFinish(GameFinishEvent event) {
        spectateManager.removeAllSpectators();
    }

    @EventHandler
    public void onGroupWin(GroupWinEvent event) {
        spectateManager.removeAllSpectators();
    }

    @EventHandler
    public void onPlayerWin(PlayerWinEvent event) {
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
        if (!spectateManager.isSpectating(player)) {
            return;
        }
        for (String command : configManager.getAllowedCommandsInSpectator()) {
            if (event.getMessage().startsWith(command)) {
                return;
            }
        }
        if (!canBypassCommandRestrictions(player)) {
            player.sendMessage(MessageFormat.format(plugin.getLang("command-not-allowed-in-spectator"), event.getMessage()));
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        if (spectateManager.isSpectating(event.getPlayer())) {
            plugin.debug(String.format("Player %s tried to teleport while spectating. Teleport cancelled.", event.getPlayer().getName()));
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (spectateManager.isSpectating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerAttemptPickupItem(final PlayerAttemptPickupItemEvent event) {
        if (spectateManager.isSpectating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        final Player player;
        if (event.getDamager() instanceof Player damager) {
            player = damager;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            player = shooter;
        } else {
            return;
        }
        if (spectateManager.isSpectating(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && spectateManager.isSpectating(player)) {
            player.setFireTicks(0);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        if (event.getHitEntity() instanceof Player player && spectateManager.isSpectating(player)) {
            Location loc = proj.getLocation();
            Vector vel = proj.getVelocity();
            ProjectileSource shooter = proj.getShooter();
            EntityType type = proj.getType();

            proj.remove();

            Projectile copy = (Projectile) loc.getWorld().spawnEntity(loc, type);
            copy.setVelocity(vel);
            copy.setShooter(shooter);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && spectateManager.isSpectating(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityCombust(EntityCombustEvent event) {
        if (event.getEntity() instanceof Player player && spectateManager.isSpectating(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupArrow(PlayerPickupArrowEvent event) {
        if (spectateManager.isSpectating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    private boolean canBypassCommandRestrictions(Player player) {
        return player.hasPermission("titansbattle.command-bypass");
    }
}


