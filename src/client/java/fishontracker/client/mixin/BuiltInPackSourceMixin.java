package fishontracker.client.mixin;

import fishontracker.client.resource.ConfigurablePackSource;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Hooks the mod's own configurable built-in packs into the client pack repository, using the
 * same injection point the Fabric API uses for its built-in packs.
 */
@Mixin(BuiltInPackSource.class)
public class BuiltInPackSourceMixin {

    @Inject(method = "loadPacks", at = @At("RETURN"))
    private void fishontracker$addConfigurablePacks(Consumer<Pack> consumer, CallbackInfo ci) {
        // noinspection ConstantConditions
        if ((Object) this instanceof ClientPackSource) {
            ConfigurablePackSource.INSTANCE.loadPacks(consumer);
        }
    }
}
