package fishontracker.client.tooltip;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4fc;

/**
 * Extends one of the server's tooltip divider bars so it ends flush with the stretched frame.
 *
 * <p>The server draws a divider as a run of adjacent strikethrough quads, one per space character,
 * all sharing the same top and bottom line. Only the <em>last</em> quad of a run is wrapped by this
 * class. Whether a quad is the last of its run is only known once the following character is seen,
 * so the flag is flipped by the caller ({@link #markNotTail()}) as soon as the run continues. A quad
 * that is never followed by another quad of the same run keeps the tail flag and is therefore the
 * one that grows.</p>
 *
 * <p>The underlying quad is built lazily through the server's own {@link EffectGlyph} factory, once
 * with the original right edge and once with the extended one. Sampling the real factory keeps the
 * colour, shadow, depth and shear handling exactly as vanilla wrote them.</p>
 */
public final class StretchedDividerRenderable implements TextRenderable {

    private final EffectGlyph factory;
    private final float x0;
    private final float y0;
    private final float x1;
    private final float y1;
    private final float depth;
    private final int color;
    private final int shadowColor;
    private final float shadowOffset;
    private final float extension;

    /** Whether this quad is currently the last one of its run and should therefore grow. */
    private boolean tail = true;

    private TextRenderable original;
    private TextRenderable stretched;

    public StretchedDividerRenderable(EffectGlyph factory,
                                      float x0, float y0, float x1, float y1, float depth,
                                      int color, int shadowColor, float shadowOffset,
                                      float extension) {
        this.factory = factory;
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.depth = depth;
        this.color = color;
        this.shadowColor = shadowColor;
        this.shadowOffset = shadowOffset;
        this.extension = extension;
    }

    /**
     * Marks this quad as no longer being the tail of its run, because another quad of the same run
     * followed it. Called by the mixin when the next strikethrough character is prepared.
     */
    public void markNotTail() {
        tail = false;
    }

    private TextRenderable current() {
        if (tail && extension > 0.0F) {
            if (stretched == null) {
                stretched = factory.createEffect(x0, y0, x1 + extension, y1, depth, color, shadowColor, shadowOffset);
            }
            return stretched;
        }

        if (original == null) {
            original = factory.createEffect(x0, y0, x1, y1, depth, color, shadowColor, shadowOffset);
        }
        return original;
    }

    @Override
    public void render(Matrix4fc pose, VertexConsumer consumer, int light, boolean notShadow) {
        current().render(pose, consumer, light, notShadow);
    }

    @Override
    public RenderType renderType(Font.DisplayMode displayMode) {
        return current().renderType(displayMode);
    }

    @Override
    public GpuTextureView textureView() {
        return current().textureView();
    }

    @Override
    public RenderPipeline guiPipeline() {
        return current().guiPipeline();
    }

    @Override
    public float left() {
        return x0;
    }

    @Override
    public float top() {
        return y0;
    }

    @Override
    public float right() {
        return tail ? x1 + extension : x1;
    }

    @Override
    public float bottom() {
        return y1;
    }
}
