package me.jellysquid.mods.sodium.mixin.features.gui.font;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.formats.glyph.GlyphVertexSink;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.gui.fonts.TexturedGlyph;
import net.minecraft.util.math.vector.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TexturedGlyph.class)
public class MixinGlyphRenderer {
    @Shadow
    @Final
    private float minX;

    @Shadow
    @Final
    private float maxX;

    @Shadow
    @Final
    private float minY;

    @Shadow
    @Final
    private float maxY;

    @Shadow
    @Final
    private float u0;

    @Shadow
    @Final
    private float v0;

    @Shadow
    @Final
    private float v1;

    @Shadow
    @Final
    private float u1;

    /**
     * @reason Use intrinsics
     * @author JellySquid
     */
    @Overwrite
    public void render(boolean italic, float x, float y, Matrix4f matrix, IVertexBuilder vertexConsumer, float red, float green, float blue, float alpha, int light) {
        float x1 = x + this.minX;
        float x2 = x + this.maxX;
        float y1 = this.minY - 3.0F;
        float y2 = this.maxY - 3.0F;
        float h1 = y + y1;
        float h2 = y + y2;
        float w1 = italic ? 1.0F - 0.25F * y1 : 0.0F;
        float w2 = italic ? 1.0F - 0.25F * y2 : 0.0F;

        int color = ColorABGR.pack(red, green, blue, alpha);

        GlyphVertexSink drain = VertexDrain.of(vertexConsumer)
                .createSink(VanillaVertexTypes.GLYPHS);
        drain.ensureCapacity(4);
        drain.writeGlyph(matrix, x1 + w1, h1, 0.0F, color, this.u0, this.v0, light);
        drain.writeGlyph(matrix, x1 + w2, h2, 0.0F, color, this.u0, this.v1, light);
        drain.writeGlyph(matrix, x2 + w2, h2, 0.0F, color, this.u1, this.v1, light);
        drain.writeGlyph(matrix, x2 + w1, h1, 0.0F, color, this.u1, this.v0, light);
        drain.flush();
    }
}
