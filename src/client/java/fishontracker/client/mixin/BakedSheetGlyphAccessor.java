package fishontracker.client.mixin;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the baked geometry of a sheet glyph so the tooltip frame fix can re-emit its quad with
 * a different width. The values are normally private and only used by the glyph's own renderer.
 */
@Mixin(BakedSheetGlyph.class)
public interface BakedSheetGlyphAccessor {

    @Accessor("info")
    GlyphInfo fishontracker$getInfo();

    @Accessor("renderTypes")
    GlyphRenderTypes fishontracker$getRenderTypes();

    @Accessor("textureView")
    GpuTextureView fishontracker$getTextureView();

    @Accessor("u0")
    float fishontracker$getU0();

    @Accessor("u1")
    float fishontracker$getU1();

    @Accessor("v0")
    float fishontracker$getV0();

    @Accessor("v1")
    float fishontracker$getV1();

    @Accessor("left")
    float fishontracker$getLeft();

    @Accessor("right")
    float fishontracker$getRight();

    @Accessor("up")
    float fishontracker$getUp();

    @Accessor("down")
    float fishontracker$getDown();
}
