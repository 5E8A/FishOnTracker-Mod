package fishontracker.client.mixin;

import fishontracker.client.tooltip.TooltipFixContext;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stamps the tooltip width onto each text state queued while a tooltip is being built.
 *
 * <p>{@code addText} is called synchronously from the text drawing path, so the tooltip run is
 * still open at this point. The stamp is what lets the deferred glyph preparation know the width
 * later in the frame.</p>
 */
@Mixin(GuiRenderState.class)
public class GuiRenderStateMixin {

    @Inject(method = "addText", at = @At("HEAD"))
    private void fishontracker$stampTooltipText(GuiTextRenderState state, CallbackInfo ci) {
        TooltipFixContext.stamp(state);
    }
}
