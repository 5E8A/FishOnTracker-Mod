package fishontracker.client.mixin;

import fishontracker.client.resource.ConfigurablePackSource;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Keeps the mod's own resource packs out of the vanilla resource pack screen.
 *
 * <p>The packs are always selected in the repository (their content is gated by the mod config
 * instead), so listing them here as well would just be a second, conflicting switch.</p>
 */
@Mixin(PackSelectionModel.class)
public class PackSelectionModelMixin {

    @Shadow
    @Final
    private List<Pack> selected;

    @Shadow
    @Final
    private List<Pack> unselected;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fishontracker$hideOwnPacks(CallbackInfo ci) {
        hideOwnPacks();
    }

    @Inject(method = "findNewPacks", at = @At("TAIL"))
    private void fishontracker$hideOwnPacksOnRefresh(CallbackInfo ci) {
        hideOwnPacks();
    }

    private void hideOwnPacks() {
        selected.removeIf(ConfigurablePackSource::isOwnPack);
        unselected.removeIf(ConfigurablePackSource::isOwnPack);
    }
}
