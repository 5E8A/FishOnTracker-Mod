package fishontracker.client.mixin;

import fishontracker.client.tooltip.TooltipFixContext;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Activates the stamped tooltip width while a text state prepares its glyphs.
 *
 * <p>{@code ensurePrepared} is where {@code Font.prepareText} runs, which is exactly the point at
 * which the frame glyphs are turned into renderables. Activating the width here means the glyph
 * mixin can see it without any global "current tooltip" state leaking between states.</p>
 */
@Mixin(GuiTextRenderState.class)
public class GuiTextRenderStateMixin {

    @Inject(method = "ensurePrepared", at = @At("HEAD"))
    private void fishontracker$activateTooltipWidth(CallbackInfoReturnable<Font.PreparedText> cir) {
        TooltipFixContext.activate((GuiTextRenderState) (Object) this);
    }

    @Inject(method = "ensurePrepared", at = @At("RETURN"))
    private void fishontracker$deactivateTooltipWidth(CallbackInfoReturnable<Font.PreparedText> cir) {
        TooltipFixContext.deactivate();
    }
}
