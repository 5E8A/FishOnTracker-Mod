package fishontracker.client.tooltip;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Style;
import org.joml.Matrix4fc;

/**
 * Re-emits one of the server's frame glyphs with a wider body, so the frame spans the whole
 * tooltip box instead of stopping at its fixed 200 pixel width.
 *
 * <p>The frame textures are stretchable decorations: both ends carry a chevron pattern and the
 * span between them is made of plain horizontal rails. The ends therefore stay pixel perfect and
 * only the rails in between get longer. The {@code top} textures additionally carry a small emblem
 * in their centre; every rarity draws a differently sized emblem at a different place, so the
 * emblem's own source columns are drawn one to one and centred, and are excluded from the rails so
 * they are not smeared.</p>
 *
 * <p>Source columns are mapped directly onto the screen: the frame is cut into a left cap, the
 * rails and the right cap, and each piece samples the matching slice of the original 200 pixel
 * texture.</p>
 */
public final class StretchedFrameRenderable implements TextRenderable.Styled {

    /** Width of the fixed decoration at each end of the frame, in pixels. */
    private static final float CAP = 15.0F;

    private final GlyphRenderTypes renderTypes;
    private final GpuTextureView textureView;
    private final float u0;
    private final float u1;
    private final float v0;
    private final float v1;
    private final float bearing;
    private final float up;
    private final float down;

    private final float x;
    private final float y;
    private final int color;
    private final int shadowColor;
    private final float shadowOffset;
    private final float boldOffset;
    private final Style style;

    /** Width the frame should span, in pixels. */
    private final float width;

    /**
     * Source columns of the centre emblem as {@code [from, to)}, or {@code null} for a frame piece
     * without an emblem (the {@code middle}/{@code bottom} rows and the common rarity's top).
     */
    private final int[] emblem;

    public StretchedFrameRenderable(GlyphRenderTypes renderTypes, GpuTextureView textureView,
                                    float u0, float u1, float v0, float v1,
                                    float bearing, float up, float down,
                                    float x, float y,
                                    int color, int shadowColor,
                                    Style style, float boldOffset, float shadowOffset,
                                    float width,
                                    int[] emblem) {
        this.renderTypes = renderTypes;
        this.textureView = textureView;
        this.u0 = u0;
        this.u1 = u1;
        this.v0 = v0;
        this.v1 = v1;
        this.bearing = bearing;
        this.up = up;
        this.down = down;
        this.x = x;
        this.y = y;
        this.color = color;
        this.shadowColor = shadowColor;
        this.style = style;
        this.boldOffset = boldOffset;
        this.shadowOffset = shadowOffset;
        this.width = width;
        this.emblem = emblem;
    }

    @Override
    public void render(Matrix4fc pose, VertexConsumer consumer, int light, boolean notShadow) {
        if (shadowColor != 0) {
            emit(pose, consumer, light, x + shadowOffset, y + shadowOffset, shadowColor);
            if (style.isBold()) {
                emit(pose, consumer, light, x + boldOffset + shadowOffset, y + shadowOffset, shadowColor);
            }
        }
        emit(pose, consumer, light, x, y, color);
        if (style.isBold()) {
            emit(pose, consumer, light, x + boldOffset, y, color);
        }
    }

    /** Draws the frame as a fixed left cap, a stretched middle, an optional emblem and a fixed right cap. */
    private void emit(Matrix4fc pose, VertexConsumer consumer, int light,
                      float penX, float penY, int argb) {
        float left = penX + bearing;
        float right = left + width;
        float top = penY + up;
        float bottom = penY + down;

        float leftCapEnd = Math.min(left + CAP, right);
        float rightCapStart = Math.max(right - CAP, leftCapEnd);

        // Fixed left cap: screen [left, leftCapEnd] <- source [0, CAP].
        quad(pose, consumer, light, left, leftCapEnd, top, bottom,
            uAt(0.0F), uAt(CAP), argb);

        // Fixed right cap: screen [rightCapStart, right] <- source [200 - CAP, 200].
        float rightCapSourceStart = TooltipFixContext.FRAME_GLYPH_WIDTH - CAP;
        quad(pose, consumer, light, rightCapStart, right, top, bottom,
            uAt(rightCapSourceStart), uAt(TooltipFixContext.FRAME_GLYPH_WIDTH), argb);

        // Stretchable middle between the caps. The middle is what carries the rails; the top
        // textures also carry a fixed emblem in the centre, whose source columns must be excluded
        // from the rail slices or the emblem gets smeared across them.
        if (emblem != null) {
            float emblemSourceWidth = emblem[1] - emblem[0];
            float emblemCentre = left + width / 2.0F;
            float emblemScreenStart = emblemCentre - emblemSourceWidth / 2.0F;
            float emblemScreenStop = emblemCentre + emblemSourceWidth / 2.0F;

            if (emblemScreenStart <= leftCapEnd || emblemScreenStop >= rightCapStart) {
                // Not enough room for the emblem, fall back to a plain stretched middle.
                rail(pose, consumer, light, leftCapEnd, rightCapStart, top, bottom, CAP,
                    TooltipFixContext.FRAME_GLYPH_WIDTH - CAP, argb);
            } else {
                // Left rail: source [CAP, emblemFrom] spread over the space left of the emblem.
                rail(pose, consumer, light, leftCapEnd, emblemScreenStart, top, bottom,
                    CAP, emblem[0], argb);
                // Right rail: source [emblemTo, 200 - CAP] spread over the space right of the emblem.
                rail(pose, consumer, light, emblemScreenStop, rightCapStart, top, bottom,
                    emblem[1], TooltipFixContext.FRAME_GLYPH_WIDTH - CAP, argb);
                // Fixed emblem: drawn one to one, centred on the frame.
                quad(pose, consumer, light, emblemScreenStart, emblemScreenStop, top, bottom,
                    uAt(emblem[0]), uAt(emblem[1]), argb);
            }
        } else {
            rail(pose, consumer, light, leftCapEnd, rightCapStart, top, bottom, CAP,
                TooltipFixContext.FRAME_GLYPH_WIDTH - CAP, argb);
        }
    }

    /** Stretches a source column span across the given screen span. */
    private void rail(Matrix4fc pose, VertexConsumer consumer, int light,
                      float from, float to, float top, float bottom,
                      float sourceStart, float sourceEnd, int argb) {
        if (to <= from || sourceEnd <= sourceStart) return;
        quad(pose, consumer, light, from, to, top, bottom, uAt(sourceStart), uAt(sourceEnd), argb);
    }

    private void quad(Matrix4fc pose, VertexConsumer consumer, int light,
                      float x0, float x1, float y0, float y1,
                      float texU0, float texU1, int argb) {
        if (x1 <= x0 || y1 <= y0) return;
        if (texU1 < texU0) {
            float swap = texU0;
            texU0 = texU1;
            texU1 = swap;
        }

        consumer.addVertex(pose, x0, y0, 0.0F).setColor(argb).setUv(texU0, v0).setLight(light);
        consumer.addVertex(pose, x0, y1, 0.0F).setColor(argb).setUv(texU0, v1).setLight(light);
        consumer.addVertex(pose, x1, y1, 0.0F).setColor(argb).setUv(texU1, v1).setLight(light);
        consumer.addVertex(pose, x1, y0, 0.0F).setColor(argb).setUv(texU1, v0).setLight(light);
    }

    /** Texture U coordinate for a source column, derived from the glyph's own UV span. */
    private float uAt(float sourceColumn) {
        float fraction = sourceColumn / TooltipFixContext.FRAME_GLYPH_WIDTH;
        return u0 + (u1 - u0) * fraction;
    }

    @Override
    public RenderType renderType(Font.DisplayMode displayMode) {
        return renderTypes.select(displayMode);
    }

    @Override
    public GpuTextureView textureView() {
        return textureView;
    }

    @Override
    public RenderPipeline guiPipeline() {
        return renderTypes.guiPipeline();
    }

    @Override
    public float left() {
        return x + bearing;
    }

    @Override
    public float top() {
        return y + up;
    }

    @Override
    public float right() {
        return left() + width;
    }

    @Override
    public float bottom() {
        return y + down;
    }

    @Override
    public Style style() {
        return style;
    }
}
