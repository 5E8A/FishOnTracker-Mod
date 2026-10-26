package fishontracker.client.tooltip;

import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooltipFixContextTest {

    @AfterEach
    void reset() {
        TooltipFixContext.reset();
    }

    @Test
    void frameGlyphsAreIdentifiedByTheirCodepointRange() {
        assertFalse(TooltipFixContext.isFrameGlyph(0xD000));
        assertTrue(TooltipFixContext.isFrameGlyph(0xD001));
        assertTrue(TooltipFixContext.isFrameGlyph(0xD020));
        assertFalse(TooltipFixContext.isFrameGlyph(0xD021));
        assertFalse(TooltipFixContext.isFrameGlyph('A'));
    }

    @Test
    void onlyTheServerTopFramePiecesAreTops() {
        assertTrue(TooltipFixContext.isFrameTopGlyph(0xD001));
        assertTrue(TooltipFixContext.isFrameTopGlyph(0xD005));
        assertTrue(TooltipFixContext.isFrameTopGlyph(0xD009));
        assertTrue(TooltipFixContext.isFrameTopGlyph(0xD013));
        assertTrue(TooltipFixContext.isFrameTopGlyph(0xD017));

        // The "+4 every time" pattern breaks after epic: these are not tops.
        assertFalse(TooltipFixContext.isFrameTopGlyph(0xD00D));
        assertFalse(TooltipFixContext.isFrameTopGlyph(0xD011));
        assertFalse(TooltipFixContext.isFrameTopGlyph(0xD002));
        assertFalse(TooltipFixContext.isFrameTopGlyph(0xD003));
        assertFalse(TooltipFixContext.isFrameTopGlyph(0xD004));
        assertFalse(TooltipFixContext.isFrameTopGlyph(0xD015));
        assertFalse(TooltipFixContext.isFrameTopGlyph(0xD019));
    }

    @Test
    void frameGrowsByTheAmountTheContentOverflowsTheAuthoredWidth() {
        // The server authors the frame for 188px of content, so a wider tooltip grows by the rest.
        withActiveContentWidth(288);
        assertEquals(288 + 12 - 200, TooltipFixContext.stretchGrowth());
    }

    @Test
    void shortTooltipsAreNotStretched() {
        // Content narrower than the frame: nothing to grow.
        withActiveContentWidth(120);
        assertEquals(0, TooltipFixContext.stretchGrowth());
    }

    @Test
    void authoredWidthTooltipsAreNotStretched() {
        // The server's own design width must stay pixel identical.
        withActiveContentWidth(188);
        assertEquals(0, TooltipFixContext.stretchGrowth());
    }

    @Test
    void onePixelOfOverflowGrowsTheFrameByOnePixel() {
        withActiveContentWidth(189);
        assertEquals(1, TooltipFixContext.stretchGrowth());
    }

    @Test
    void exactlyFullTooltipsAreNotStretched() {
        withActiveContentWidth(176);
        assertEquals(0, TooltipFixContext.stretchGrowth());
    }

    @Test
    void noTooltipMeansNoGrowth() {
        assertEquals(0, TooltipFixContext.stretchGrowth());
    }

    @Test
    void onlyTheLastDividerQuadOfARunGrows() {
        withActiveContentWidth(288);
        int growth = TooltipFixContext.stretchGrowth();

        StretchedDividerRenderable first = dividerQuad(growth);
        TooltipFixContext.pushDividerQuad(first);
        assertEquals(104.0F + growth, first.right(), 0.001F);

        // A second quad of the same run takes over the tail flag, so the first stops growing.
        StretchedDividerRenderable second = dividerQuad(growth);
        TooltipFixContext.pushDividerQuad(second);

        assertEquals(104.0F, first.right(), 0.001F);
        assertEquals(104.0F + growth, second.right(), 0.001F);
    }

    @Test
    void closingTheRunKeepsTheCurrentTailAsTheGrownQuad() {
        withActiveContentWidth(288);
        int growth = TooltipFixContext.stretchGrowth();

        StretchedDividerRenderable tail = dividerQuad(growth);
        TooltipFixContext.pushDividerQuad(tail);
        TooltipFixContext.closeDividerRun();

        // A later run starts fresh and must not demote the previous run's tail.
        StretchedDividerRenderable otherTail = dividerQuad(growth);
        TooltipFixContext.pushDividerQuad(otherTail);

        assertEquals(104.0F + growth, tail.right(), 0.001F);
        assertEquals(104.0F + growth, otherTail.right(), 0.001F);
    }

    @Test
    void deactivatingTheTextStateDropsThePendingDividerRun() {
        withActiveContentWidth(288);
        int growth = TooltipFixContext.stretchGrowth();

        StretchedDividerRenderable tail = dividerQuad(growth);
        TooltipFixContext.pushDividerQuad(tail);

        TooltipFixContext.deactivate();

        StretchedDividerRenderable otherTail = dividerQuad(10);
        TooltipFixContext.pushDividerQuad(otherTail);

        // The dropped run's tail must not be demoted by a quad from the next text state.
        assertEquals(104.0F + growth, tail.right(), 0.001F);
    }

    @Test
    void emblemColumnsMatchTheServerTexturesPerRarity() {
        // Measured from the server's 200px *_top.png textures.
        assertNull(TooltipFixContext.emblemColumns(0xD001), "common has no emblem");
        assertArrayEquals(new int[] {99, 103}, TooltipFixContext.emblemColumns(0xD005), "rare");
        assertArrayEquals(new int[] {94, 108}, TooltipFixContext.emblemColumns(0xD009), "epic");
        assertArrayEquals(new int[] {91, 111}, TooltipFixContext.emblemColumns(0xD013), "legendary");
        assertArrayEquals(new int[] {88, 114}, TooltipFixContext.emblemColumns(0xD017), "mythical");
    }

    @Test
    void onlyTopFramePiecesCarryAnEmblem() {
        assertNull(TooltipFixContext.emblemColumns(0xD003), "middle");
        assertNull(TooltipFixContext.emblemColumns(0xD004), "bottom");
        assertNull(TooltipFixContext.emblemColumns('A'), "not a frame glyph");
    }

    private static StretchedDividerRenderable dividerQuad(int extension) {
        return new StretchedDividerRenderable(
            (x0, y0, x1, y1, depth, color, shadowColor, shadowOffset) -> null,
            0.0F, 0.0F, 104.0F, 1.0F, 0.01F,
            0xFFFFFFFF, 0, 0.0F, extension);
    }

    private static void withActiveContentWidth(int contentWidth) {
        GuiTextRenderState state = new GuiTextRenderState(
            null, FormattedCharSequence.EMPTY, new Matrix3x2f(),
            0, 0, 0, 0, false, false, null);

        TooltipFixContext.begin(contentWidth);
        TooltipFixContext.stamp(state);
        TooltipFixContext.activate(state);
    }
}
