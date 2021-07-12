package me.jellysquid.mods.sodium.mixin.features.buffer_builder.intrinsics;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.transformers.SpriteTexturedVertexTransformer;
import me.jellysquid.mods.sodium.client.model.vertex.type.VertexType;
import net.minecraft.client.renderer.SpriteAwareVertexBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpriteAwareVertexBuilder.class)
public abstract class MixinSpriteTexturedVertexConsumer implements VertexDrain {
    @Shadow
    @Final
    private TextureAtlasSprite atlasSprite;

    @Shadow
    @Final
    private IVertexBuilder vertexBuilder;

    @SuppressWarnings("unchecked")
    @Override
    public <T extends VertexSink> T createSink(VertexType<T> type) {
        if (type == VanillaVertexTypes.QUADS) {
            return (T) new SpriteTexturedVertexTransformer.Quad(VertexDrain.of(this.vertexBuilder)
                    .createSink(VanillaVertexTypes.QUADS), this.atlasSprite);
        } else if (type == VanillaVertexTypes.PARTICLES) {
            return (T) new SpriteTexturedVertexTransformer.Particle(VertexDrain.of(this.vertexBuilder)
                    .createSink(VanillaVertexTypes.PARTICLES), this.atlasSprite);
        } else if (type == VanillaVertexTypes.GLYPHS) {
            return (T) new SpriteTexturedVertexTransformer.Glyph(VertexDrain.of(this.vertexBuilder)
                    .createSink(VanillaVertexTypes.GLYPHS), this.atlasSprite);
        }

        return type.createFallbackWriter((IVertexBuilder) this);
    }
}
