package me.roinujnosde.titansbattle.listeners;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.types.Kit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class ItemsProtectionListenerTest {

    private final ItemsProtectionListener listener = new ItemsProtectionListener(mock(TitansBattle.class));
    private final CraftingInventory inventory = mock(CraftingInventory.class);
    private final ItemStack ingredient = mock(ItemStack.class);

    @Test
    public void skipsMissingResultWithProtectedIngredient() {
        when(inventory.getMatrix()).thenReturn(new ItemStack[]{null, ingredient});

        try (MockedStatic<Kit> kit = mockStatic(Kit.class)) {
            kit.when(() -> Kit.isKitItem(ingredient)).thenReturn(true);
            kit.when(() -> Kit.applyNBTTag(null)).thenCallRealMethod();
            listener.on(craft(ClickType.LEFT, InventoryAction.PICKUP_ALL));
            listener.on(craft(ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY));
            kit.verifyNoInteractions();
        }
    }

    @Test
    public void marksResultForNormalAndShiftClicks() {
        final ItemStack result = mock(ItemStack.class);
        when(inventory.getResult()).thenReturn(result);
        when(inventory.getMatrix()).thenReturn(new ItemStack[]{null, ingredient});

        try (MockedStatic<Kit> kit = mockStatic(Kit.class)) {
            kit.when(() -> Kit.isKitItem(ingredient)).thenReturn(true);
            listener.on(craft(ClickType.LEFT, InventoryAction.PICKUP_ALL));
            listener.on(craft(ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY));
            kit.verify(() -> Kit.applyNBTTag(result), times(2));
        }
    }

    @Test
    public void leavesResultAloneWithoutProtectedIngredients() {
        final ItemStack result = mock(ItemStack.class);
        when(inventory.getResult()).thenReturn(result);
        when(inventory.getMatrix()).thenReturn(new ItemStack[]{null, ingredient});

        try (MockedStatic<Kit> kit = mockStatic(Kit.class)) {
            listener.on(craft(ClickType.LEFT, InventoryAction.PICKUP_ALL));
            kit.verify(() -> Kit.isKitItem(ingredient));
            kit.verifyNoMoreInteractions();
        }
    }

    private CraftItemEvent craft(ClickType click, InventoryAction action) {
        final InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(inventory);
        return new CraftItemEvent(mock(Recipe.class), view, SlotType.RESULT, 0, click, action);
    }

}
