package fishontracker.client.tooltip;

import net.minecraft.client.renderer.state.gui.GuiTextRenderState;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Carries the current tooltip box width from the place the box is measured to the place the
 * frame glyphs are turned into renderables.
 *
 * <p>Text rendering is deferred: {@code GuiGraphicsExtractor.tooltip} only queues
 * {@link GuiTextRenderState}s, and the glyphs are prepared later in the same frame by
 * {@code GuiRenderer.prepareText}. A plain "current tooltip" field would therefore be gone by
 * the time the glyph work happens. Instead the width is stamped onto each queued text state
 * while the tooltip is being built, and read back when that state prepares its glyphs.</p>
 */
public final class TooltipFixContext {

    /** Width of the server's fancy frame glyphs, in pixels (all rarity textures are 200px). */
    public static final int FRAME_GLYPH_WIDTH = 200;

    /**
     * How far the frame reaches to the right of the content's start, in pixels. The server sizes
     * its frame textures for a content width of {@code FRAME_GLYPH_WIDTH - BOX_PADDING} (188
     * pixels), so a tooltip at that width keeps its frame untouched.
     */
    public static final int BOX_PADDING = 12;

    /**
     * First and last codepoint of the server's frame glyphs. They are contiguous across the five
     * rarities (top/middle/bottom), so a simple range check identifies every one of them.
     */
    public static final int FRAME_GLYPH_FIRST = 0xD001;
    public static final int FRAME_GLYPH_LAST = 0xD020;

    /** Box width of the tooltip currently being built, or -1 when no tooltip is being built. */
    private static int pendingBoxWidth = -1;

    /** Box width stamped onto each queued text state while a tooltip is being built. */
    private static final Map<GuiTextRenderState, Integer> STAMPED = new IdentityHashMap<>();

    /** Box width available while the glyphs of the current text state are prepared, or -1. */
    private static int activeBoxWidth = -1;

    /** Codepoint currently being turned into a renderable, or -1 outside of glyph preparation. */
    private static int currentCodepoint = -1;

    /**
     * Tail quad of the strikethrough run currently being prepared, or {@code null}.
     *
     * <p>Divider bars are runs of adjacent strikethrough quads. The quad that should grow is the one
     * that ends the run, but that is only known once the following character has been seen. Each new
     * strikethrough quad therefore takes over the tail flag from its predecessor, and any other
     * character closes the run.</p>
     */
    private static StretchedDividerRenderable dividerTail;

    private TooltipFixContext() {
    }

    /** Whether the given codepoint is one of the server's frame glyphs. */
    public static boolean isFrameGlyph(int codepoint) {
        return codepoint >= FRAME_GLYPH_FIRST && codepoint <= FRAME_GLYPH_LAST;
    }

    /**
     * Whether the given frame codepoint is one of the {@code top} pieces.
     *
     * <p>The server lays each rarity out as {@code top, +2, +1} and starts the next rarity one
     * codepoint later, so the tops are {@code D001, D005, D009, D013, D017}. A plain every-fourth
     * rule only agrees for the first three rarities, hence the explicit list.</p>
     */
    public static boolean isFrameTopGlyph(int codepoint) {
        return codepoint == 0xD001 || codepoint == 0xD005 || codepoint == 0xD009
            || codepoint == 0xD013 || codepoint == 0xD017;
    }

    /**
     * Source columns of the centre emblem carried by a {@code top} texture, as a {@code [from, to)}
     * pair, or {@code null} when that rarity has no emblem.
     *
     * <p>Every rarity draws a differently sized emblem at a different place: rare is a small diamond,
     * epic and legendary larger ones and mythical the largest; common has none at all. The values are
     * measured from the server's own 200 pixel {@code *_top.png} textures. Treating the emblem as one
     * fixed window smears the neighbouring rails into it, so the exact span matters.</p>
     */
    public static int[] emblemColumns(int codepoint) {
        return switch (codepoint) {
            case 0xD001 -> null;              // common: no emblem
            case 0xD005 -> new int[] {99, 103};   // rare
            case 0xD009 -> new int[] {94, 108};   // epic
            case 0xD013 -> new int[] {91, 111};   // legendary
            case 0xD017 -> new int[] {88, 114};   // mythical
            default -> null;
        };
    }

    /** Records the codepoint currently being prepared. */
    public static void setCurrentCodepoint(int codepoint) {
        currentCodepoint = codepoint;
    }

    /** Codepoint currently being prepared, or -1. */
    public static int currentCodepoint() {
        return currentCodepoint;
    }

    /**
     * Starts a tooltip run with the measured content width. Called when the tooltip background is
     * drawn, which happens right before the tooltip's text lines are queued.
     */
    public static void begin(int contentWidth) {
        pendingBoxWidth = contentWidth;
    }

    /** Ends the current tooltip run. */
    public static void end() {
        pendingBoxWidth = -1;
    }

    /**
     * Stamps the queued text state with the tooltip width, so the deferred glyph preparation can
     * still see it. Only states queued while a tooltip is being built are stamped.
     */
    public static void stamp(GuiTextRenderState state) {
        if (state != null && pendingBoxWidth >= 0) {
            STAMPED.put(state, pendingBoxWidth);
        }
    }

    /** Activates the stamped width for the duration of preparing one text state. */
    public static void activate(GuiTextRenderState state) {
        Integer width = STAMPED.remove(state);
        activeBoxWidth = width != null ? width : -1;
    }

    /** Releases the width activated by {@link #activate(GuiTextRenderState)}. */
    public static void deactivate() {
        activeBoxWidth = -1;
        dividerTail = null;
    }

    /**
     * Extra width the frame glyph should be drawn with, or 0 when no stretching should happen.
     *
     * <p>The server draws a frame texture whose own width is {@code FRAME_GLYPH_WIDTH} pixels but
     * which is authored for content up to {@code FRAME_GLYPH_WIDTH - BOX_PADDING} pixels wide. The
     * frame's left edge sits on the server's position, so the frame only has to grow by however much
     * the measured content width exceeds that authored width:</p>
     *
     * <pre>growth = contentWidth + BOX_PADDING - FRAME_GLYPH_WIDTH</pre>
     *
     * <p>At the authored width the growth is zero, which keeps the frame pixel identical to the
     * server's rendering. Only wider tooltips are grown, and the middle of the frame is stretched by
     * this amount while its decorated caps stay pixel perfect. Whether the option is enabled is
     * checked by the caller.</p>
     */
    public static int stretchGrowth() {
        if (activeBoxWidth < 0) return 0;

        int growth = activeBoxWidth + BOX_PADDING - FRAME_GLYPH_WIDTH;
        return Math.max(0, growth);
    }

    /**
     * Registers a freshly prepared strikethrough quad as the tail of the current run.
     *
     * <p>The previously registered quad, if any, is demoted because this one continues the run and
     * is therefore the new end of the bar.</p>
     */
    public static void pushDividerQuad(StretchedDividerRenderable quad) {
        if (dividerTail != null) {
            dividerTail.markNotTail();
        }
        dividerTail = quad;
    }

    /** Closes the current strikethrough run, e.g. because a non-strikethrough character followed. */
    public static void closeDividerRun() {
        dividerTail = null;
    }

    /** Drops any leftover state, e.g. when the resource reload swaps the font during a frame. */
    public static void reset() {
        pendingBoxWidth = -1;
        activeBoxWidth = -1;
        currentCodepoint = -1;
        dividerTail = null;
        STAMPED.clear();
    }
}
