package fishontracker.client.mixin;
import fishontracker.client.scan.ScannerManager;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {

    /**
     * Fallback container size when a menu does not expose any player-inventory slots
     * (e.g. server-side virtual menus that put everything into one container):
     * 9 hotbar + 27 main inventory slots.
     */
    private static final int PLAYER_INVENTORY_SLOTS = 36;

    @Shadow
    public abstract AbstractContainerMenu getMenu();

    @Inject(method = "removed", at = @At("HEAD"))
    private void onScreenClose(CallbackInfo ci) {
        if (!ScannerManager.isScanning()) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        String title = screen.getTitle().getString().trim();

        AbstractContainerMenu menu = this.getMenu();
        if (menu == null || menu.slots == null) return;

        // The vanilla crafting screen is skipped by type; the title check additionally
        // catches server-side menus that are also called "Crafting".
        if (menu instanceof CraftingMenu || title.equalsIgnoreCase("Crafting")) {
            return;
        }

        List<ItemStack> items = collectContainerItems(menu);
        if (items.isEmpty()) return;

        String finalTitle = title.isEmpty() ? "Chest" : title;
        ScannerManager.recordContainer(finalTitle, items);
    }

    /**
     * Collects the container's own items, excluding the player's inventory.
     *
     * <p>Slot ownership is detected via {@code slot.container} (an {@link Inventory} means
     * the slot belongs to the player), so hoppers, droppers and other non-standard menu
     * sizes are read correctly. Menus without any player-inventory slots — typical for
     * server-side virtual containers — fall back to ignoring the last
     * {@value #PLAYER_INVENTORY_SLOTS} slots.</p>
     */
    private static List<ItemStack> collectContainerItems(AbstractContainerMenu menu) {
        List<Slot> slots = menu.slots;
        List<ItemStack> items = new ArrayList<>();

        boolean hasPlayerInventory = false;
        for (Slot slot : slots) {
            if (slot.container instanceof Inventory) {
                hasPlayerInventory = true;
                break;
            }
        }

        if (hasPlayerInventory) {
            for (Slot slot : slots) {
                if (slot.container instanceof Inventory) continue;
                addIfPresent(items, slot);
            }
        } else {
            int containerSlots = Math.max(0, slots.size() - PLAYER_INVENTORY_SLOTS);
            for (int i = 0; i < containerSlots; i++) {
                addIfPresent(items, slots.get(i));
            }
        }

        return items;
    }

    private static void addIfPresent(List<ItemStack> items, Slot slot) {
        if (!slot.hasItem()) return;
        ItemStack stack = slot.getItem();
        if (!stack.isEmpty()) {
            items.add(stack.copy());
        }
    }
}
