package me.roinujnosde.titansbattle.npc;

import com.destroystokyo.paper.profile.PlayerProfile;
import me.roinujnosde.titansbattle.TitansBattle;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NpcProxyManagerTest {

    private TitansBattle plugin;
    private FileConfiguration config;
    private World world;
    private NpcProxyManager manager;

    @Before
    public void setUp() {
        plugin = mock(TitansBattle.class);
        config = mock(FileConfiguration.class);
        world = mock(World.class);
        when(plugin.namespace()).thenReturn("titansbattle");
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(config.getString("disconnect-protection.mobType", "MANNEQUIN")).thenReturn("ZOMBIE");
        manager = new NpcProxyManager(plugin);
    }

    private Player player() {
        final Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getPlayerProfile()).thenReturn(mock(PlayerProfile.class));
        return player;
    }

    private LivingEntity livingMob() {
        final LivingEntity mob = mock(LivingEntity.class);
        when(mob.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        return mob;
    }

    private Location location() {
        final Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        return location;
    }

    @Test
    public void spawnsConfiguredMobTypeAndProtectsIt() {
        final Player player = player();
        final Location location = location();
        final LivingEntity mob = livingMob();
        final PersistentDataContainer container = mob.getPersistentDataContainer();
        when(world.spawnEntity(location, EntityType.ZOMBIE)).thenReturn(mob);

        final LivingEntity spawned = manager.spawnProxy(player, location);

        assertSame(mob, spawned);
        verify(mob).setInvulnerable(true);
        verify(mob).setAI(false);
        verify(mob).setCollidable(false);
        verify(mob).setPersistent(true);
        verify(mob).setRemoveWhenFarAway(false);
        verify(container).set(any(NamespacedKey.class), any(), any());
        assertTrue(manager.getProxyByOwner(player.getUniqueId()).isPresent());
    }

    @Test
    public void invalidMobTypeFallsBackToMannequin() {
        when(config.getString("disconnect-protection.mobType", "MANNEQUIN")).thenReturn("NOT_A_MOB");
        final Player player = player();
        final Location location = location();
        final LivingEntity mob = livingMob();
        when(world.spawnEntity(location, EntityType.MANNEQUIN)).thenReturn(mob);

        manager.spawnProxy(player, location);

        verify(world).spawnEntity(location, EntityType.MANNEQUIN);
    }

    @Test
    public void nonLivingMobTypeFallsBackToMannequin() {
        when(config.getString("disconnect-protection.mobType", "MANNEQUIN")).thenReturn("ARROW");
        final Player player = player();
        final Location location = location();
        final LivingEntity mob = livingMob();
        when(world.spawnEntity(location, EntityType.MANNEQUIN)).thenReturn(mob);

        manager.spawnProxy(player, location);

        verify(world).spawnEntity(location, EntityType.MANNEQUIN);
    }

    @Test
    public void despawnRemovesAndForgetsProxy() {
        final Player player = player();
        final Location location = location();
        final LivingEntity mob = livingMob();
        when(world.spawnEntity(location, EntityType.ZOMBIE)).thenReturn(mob);
        manager.spawnProxy(player, location);

        manager.despawnProxy(player.getUniqueId(), "test");

        verify(mob).remove();
        assertFalse(manager.getProxyByOwner(player.getUniqueId()).isPresent());
    }

    @Test
    public void isProxyReadsPersistentData() {
        final LivingEntity proxy = livingMob();
        when(proxy.getPersistentDataContainer().has(any(NamespacedKey.class),
                eq(PersistentDataType.STRING))).thenReturn(true);

        final LivingEntity plain = livingMob();

        assertTrue(manager.isProxy(proxy));
        assertFalse(manager.isProxy(plain));
    }
}
