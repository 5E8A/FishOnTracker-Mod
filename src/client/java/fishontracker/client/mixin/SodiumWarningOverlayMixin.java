package fishontracker.client.mixin;

import fishontracker.client.config.TrackerConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.console.ConsoleRenderer", remap = false)
public class SodiumWarningOverlayMixin {

    @Inject(
        method = "update",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void fishontracker$cancelConsoleUpdate(CallbackInfo ci) {
        if (TrackerConfig.INSTANCE.hideSodiumWarning) {
            ci.cancel();
        }
    }

    @Inject(
        method = "draw",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void fishontracker$cancelConsoleDraw(CallbackInfo ci) {
        if (TrackerConfig.INSTANCE.hideSodiumWarning) {
            ci.cancel();
        }
    }
}