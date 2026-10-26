package fishontracker.client.mixin;

import fishontracker.client.AmuletStarTextures;
import fishontracker.client.config.TrackerConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the single yellow "pet item equipped" star drawn by FishOnMC Extras with a star
 * coloured after the amulet equipped on the pet.
 *
 * <p>The mixin is declared against the FishOnMC Extras target by name so the mod still
 * compiles and runs when FishOnMC Extras is not present. The injection cancels the original
 * draw only when a matching texture was found; every other case (skins, trails, unknown
 * amulets, option disabled) keeps the original FishOnMC Extras behaviour, and
 * {@code require = 0} tolerates a changed renderer.</p>
 */
@Pseudo
@Mixin(
    targets = "dannypx.foe.handler.renderer.ItemRendererHandler",
    remap = false
)
public class AmuletStarMixin {

    @Inject(
        method = "drawPetItemEquipped",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void fishontracker$drawColoredAmuletStar(GuiGraphicsExtractor graphics,
                                                     ItemStack stack,
                                                     int x, int y,
                                                     CallbackInfo ci) {
        if (!TrackerConfig.INSTANCE.coloredAmuletStars) return;

        Identifier texture = AmuletStarTextures.getStarTexture(stack);
        if (texture == null) return;

        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
        ci.cancel();
    }
}
