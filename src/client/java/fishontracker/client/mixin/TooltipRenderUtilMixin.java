package fishontracker.client.mixin;

import fishontracker.client.tooltip.TooltipFixContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records the width of every tooltip box that is drawn, so the frame glyph fix knows how far the
 * server's frame should be stretched.
 *
 * <p>{@code TooltipRenderUtil.extractTooltipBackground} is called by
 * {@code GuiGraphicsExtractor.tooltip} right after the box size has been computed and right before
 * the tooltip's text lines are queued, which makes it a stable place to read the width from.</p>
 */
@Mixin(TooltipRenderUtil.class)
public class TooltipRenderUtilMixin {

    @Inject(method = "extractTooltipBackground", at = @At("HEAD"))
    private static void fishontracker$captureTooltipWidth(GuiGraphicsExtractor graphics,
                                                          int x, int y, int width, int height,
                                                          Identifier style,
                                                          CallbackInfo ci) {
        TooltipFixContext.begin(width);
    }
}
