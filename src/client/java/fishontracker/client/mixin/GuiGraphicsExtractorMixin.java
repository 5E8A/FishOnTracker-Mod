package fishontracker.client.mixin;

import fishontracker.client.tooltip.TooltipFixContext;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Ends the tooltip run once {@code tooltip} has queued all of its text lines, so nothing drawn
 * outside a tooltip is affected by the frame stretching.
 */
@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {

    @Inject(
        method = "tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;)V",
        at = @At("RETURN"))
    private void fishontracker$endTooltipRun(Font font,
                                             List<ClientTooltipComponent> components,
                                             int x, int y,
                                             ClientTooltipPositioner positioner,
                                             Identifier style,
                                             CallbackInfo ci) {
        TooltipFixContext.end();
    }
}
