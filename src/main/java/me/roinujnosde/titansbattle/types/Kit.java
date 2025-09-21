package me.roinujnosde.titansbattle.types;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteItemNBT;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

@SerializableAs("kit")
public class Kit implements ConfigurationSerializable {

    public static final String NBT_TAG = "TitansBattle.Kit";
    private static final String HELMET_KEY = "armor.helmet";
    private static final String CHESTPLATE_KEY = "armor.chestplate";
    private static final String LEGGINGS_KEY = "armor.leggings";
    private static final String BOOTS_KEY = "armor.boots";
    private final ItemStack[] contents;
    private final ItemStack helmet;
    private final ItemStack chestplate;
    private final ItemStack leggings;
    private final ItemStack boots;

    public Kit(@NotNull final PlayerInventory inventory) {
        final ItemStack[] invContents = inventory.getContents();
        this.contents = new ItemStack[invContents.length];
        this.helmet = clone(inventory.getHelmet());
        this.chestplate = clone(inventory.getChestplate());
        this.leggings = clone(inventory.getLeggings());
        this.boots = clone(inventory.getBoots());

        clone(invContents, contents);
    }

    public Kit(@NotNull final Map<String, Object> data) {
        final int size = data.keySet().stream().mapToInt(s -> {
            try {
                return Integer.parseInt(s);
            } catch (final NumberFormatException ex) {
                return 0;
            }
        }).max().orElse(0) + 1;
        contents = new ItemStack[size];
        for (final Map.Entry<String, Object> entry : data.entrySet()) {
            try {
                final int index = Integer.parseInt(entry.getKey());
                contents[index] = ((ItemStack) entry.getValue());
            } catch (final NumberFormatException ignore) {
            }
        }
        this.helmet = getItem(data.get(HELMET_KEY));
        this.chestplate = getItem(data.get(CHESTPLATE_KEY));
        this.leggings = getItem(data.get(LEGGINGS_KEY));
        this.boots = getItem(data.get(BOOTS_KEY));

        setNBTTag(contents);
    }

    public static boolean inventoryHasItems(@NotNull final Player player) {
        final PlayerInventory inventory = player.getInventory();
        return hasItems(inventory.getArmorContents()) || hasItems(inventory.getContents());
    }

    private static boolean hasItems(final ItemStack @NotNull [] items) {
        for (final ItemStack item : items) {
            if (item == null) {
                continue;
            }
            if (item.getType() != Material.AIR) {
                return true;
            }
        }
        return false;
    }

    public static void clearInventory(@NotNull final Warrior warrior) {
        final Player player = warrior.toOnlinePlayer();
        if (player != null) {
            clearInventory(player);
        }
    }

    public static void clearInventory(@Nullable final Player player) {
        if (player == null) return;

        player.getInventory().clear();
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);
    }

    public static void applyNBTTag(@NotNull final ItemStack item) {
        if (item.getType() == Material.AIR) {
            return;
        }
        NBT.modify(item, nbtItem -> {
            nbtItem.setBoolean(NBT_TAG, true);
        });
    }

    public static boolean isKitItem(@NotNull final ItemStack item) {
        if (item.getType() == Material.AIR) {
            return false;
        }
        return NBT.get(item, n -> (boolean) n.getBoolean(Kit.NBT_TAG));
    }

    private void clone(final ItemStack @NotNull [] source, final ItemStack[] destination) {
        for (int i = 0; i < source.length; i++) {
            final ItemStack itemStack = source[i];
            destination[i] = itemStack != null ? itemStack.clone() : null;
        }
        setNBTTag(destination);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        final TreeMap<String, Object> data = new TreeMap<>();
        for (int i = 0; i < contents.length; i++) {
            final ItemStack item = contents[i];
            if (item != null) {
                data.put(String.valueOf(i), item);
            }
        }
        data.put(HELMET_KEY, helmet);
        data.put(CHESTPLATE_KEY, chestplate);
        data.put(LEGGINGS_KEY, leggings);
        data.put(BOOTS_KEY, boots);
        return data;
    }

    public void set(@NotNull final Player player) {
        final PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(helmet);
        inventory.setChestplate(chestplate);
        inventory.setLeggings(leggings);
        inventory.setBoots(boots);
        inventory.setContents(contents);
    }

    private ItemStack getItem(final Object object) {
        return clone((ItemStack) object);
    }

    private void removeNBTTag(final ItemStack item) {
        NBT.modify(item, (Consumer<ReadWriteItemNBT>) nbtItem -> nbtItem.removeKey(NBT_TAG));
    }

    private ItemStack clone(ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            item = item.clone();
            applyNBTTag(item);
        }
        return item;
    }

    private void setNBTTag(final ItemStack @NotNull [] items) {
        for (final ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                applyNBTTag(item);
            }
        }
    }

    public ItemStack[] getContentsWithoutNBT() {
        final ItemStack[] items = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            final ItemStack item = contents[i];
            if (item != null) {
                items[i] = item.clone();
                removeNBTTag(items[i]);
            }
        }
        return items;
    }
}
