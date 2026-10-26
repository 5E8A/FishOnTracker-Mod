package fishontracker.client.mixin;

import fishontracker.client.config.TrackerConfig;
import fishontracker.client.tooltip.StretchedDividerRenderable;
import fishontracker.client.tooltip.StretchedFrameRenderable;
import fishontracker.client.tooltip.TooltipFixContext;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Swaps the server's frame glyphs for stretched versions while a tooltip that overflows the frame
 * is being prepared.
 *
 * <p>{@code PreparedTextBuilder.accept} is the single place where a codepoint and its glyph are
 * turned into a renderable, which makes it the natural spot to both recognise the frame glyphs
 * (they are the contiguous codepoints {@code U+D001..U+D020}) and to hand back a wider renderable
 * in their place. Everything else, including the glyph advance and therefore the tooltip width,
 * stays untouched.</p>
 */
@Mixin(targets = "net.minecraft.client.gui.Font$PreparedTextBuilder")
public class PreparedTextBuilderMixin {

    /**
     * Remembers which codepoint is being prepared.
     *
     * <p>The codepoint has to be captured from the {@code (int, Style, int)} overload, because that
     * is the one that carries the actual character. The glyph overload that runs right after it
     * only receives the position in the text sequence, and then delegates here.</p>
     */
    @Inject(
        method = "accept(ILnet/minecraft/network/chat/Style;I)Z",
        at = @At("HEAD"))
    private void fishontracker$rememberCodepoint(int index, Style style, int codepoint,
                                                 CallbackInfoReturnable<Boolean> cir) {
        TooltipFixContext.setCurrentCodepoint(codepoint);
    }

    @Redirect(
        method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;createGlyph(FFIILnet/minecraft/network/chat/Style;FF)Lnet/minecraft/client/gui/font/TextRenderable$Styled;"))
    private TextRenderable.Styled fishontracker$stretchFrameGlyph(BakedGlyph glyph,
                                                                  float x, float y,
                                                                  int color, int shadowColor,
                                                                  Style style,
                                                                  float boldOffset, float shadowOffset) {
        Style frameStyle = style;
        int growth = TrackerConfig.INSTANCE.tooltipFix ? TooltipFixContext.stretchGrowth() : 0;
        int codepoint = TooltipFixContext.currentCodepoint();

        if (growth <= 0
            || !TooltipFixContext.isFrameGlyph(codepoint)
            || !(glyph instanceof BakedSheetGlyph sheet)) {
            return glyph.createGlyph(x, y, color, shadowColor, frameStyle, boldOffset, shadowOffset);
        }

        BakedSheetGlyphAccessor accessor = (BakedSheetGlyphAccessor) (Object) sheet;
        float originalWidth = accessor.fishontracker$getRight() - accessor.fishontracker$getLeft();
        if (originalWidth <= 0.0F) {
            return glyph.createGlyph(x, y, color, shadowColor, frameStyle, boldOffset, shadowOffset);
        }

        return new StretchedFrameRenderable(
            accessor.fishontracker$getRenderTypes(),
            accessor.fishontracker$getTextureView(),
            accessor.fishontracker$getU0(),
            accessor.fishontracker$getU1(),
            accessor.fishontracker$getV0(),
            accessor.fishontracker$getV1(),
            accessor.fishontracker$getLeft(),
            accessor.fishontracker$getUp(),
            accessor.fishontracker$getDown(),
            x, y,
            color, shadowColor,
            frameStyle, boldOffset, shadowOffset,
            originalWidth + growth,
            TooltipFixContext.emblemColumns(codepoint));
    }

    /**
     * Closes the current divider run whenever a character that does not continue it is prepared.
     *
     * <p>Runs of strikethrough quads are only adjacent within a single struck segment, so any other
     * character (including a plain space or the frame glyphs) ends the run and lets the run's tail
     * quad keep its growth.</p>
     */
    @Inject(
        method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z",
        at = @At("HEAD"))
    private void fishontracker$trackDividerRuns(int index, Style style, BakedGlyph glyph,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (!style.isStrikethrough()) {
            TooltipFixContext.closeDividerRun();
        }
    }

    /**
     * Swaps the strikethrough quads of the server's divider bars for versions whose tail quad reaches
     * further right, so the bars end flush with the stretched frame.
     *
     * <p>Only the tooltip text states stamped by the tooltip fix are touched, and only while the
     * tooltip fix option is on and the tooltip actually overflows the frame. The underline call site
     * is left alone because it does not carry a strikethrough style.</p>
     */
    @Redirect(
        method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/font/glyphs/EffectGlyph;createEffect(FFFFFIIF)Lnet/minecraft/client/gui/font/TextRenderable;",
            ordinal = 0))
    private TextRenderable fishontracker$stretchDivider(EffectGlyph effect,
                                                        float x0, float y0, float x1, float y1, float depth,
                                                        int color, int shadowColor, float shadowOffset,
                                                        int index, Style style, BakedGlyph glyph) {
        int growth = TrackerConfig.INSTANCE.tooltipFix ? TooltipFixContext.stretchGrowth() : 0;
        if (growth <= 0 || !style.isStrikethrough()) {
            return effect.createEffect(x0, y0, x1, y1, depth, color, shadowColor, shadowOffset);
        }

        StretchedDividerRenderable quad = new StretchedDividerRenderable(
            effect, x0, y0, x1, y1, depth, color, shadowColor, shadowOffset, growth);
        TooltipFixContext.pushDividerQuad(quad);
        return quad;
    }
}
