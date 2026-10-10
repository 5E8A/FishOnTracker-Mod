package fishontracker.client.mixin;

import fishontracker.client.ScannerManager;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
    @Shadow
    public abstract AbstractContainerMenu getMenu();

    @Inject(method = "removed", at = @At("HEAD"))
    private void onScreenClose(CallbackInfo ci) {
        if (!ScannerManager.isScanning) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        String title = screen.getTitle().getString().trim();


        if (title.equalsIgnoreCase("Crafting")) {
            return;
        }

        AbstractContainerMenu menu = this.getMenu();
        if (menu == null || menu.slots == null) return;

        List<ItemStack> items = new ArrayList<>();


        int totalSlots = menu.slots.size();
        int containerSlots = totalSlots > 36 ? totalSlots - 36 : totalSlots;

        for (int i = 0; i < containerSlots; i++) {
            Slot slot = menu.slots.get(i);
            if (slot.hasItem()) {
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty()) {
                    items.add(stack.copy());
                }
            }
        }


        String finalTitle = title.isEmpty() ? "Chest" : title;
        ScannerManager.recordContainer(finalTitle, items);
    }
}