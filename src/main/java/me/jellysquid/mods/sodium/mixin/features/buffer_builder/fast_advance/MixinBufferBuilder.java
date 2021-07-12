package me.jellysquid.mods.sodium.mixin.features.buffer_builder.fast_advance;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.DefaultColorVertexBuilder;
import com.mojang.blaze3d.vertex.IVertexConsumer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder extends DefaultColorVertexBuilder implements IVertexConsumer {
    @Shadow
    private VertexFormat vertexFormat;

    @Shadow
    private VertexFormatElement vertexFormatElement;

    @Shadow
    private int nextElementBytes;

    @Shadow
    private int vertexFormatIndex;

    /**
     * @author JellySquid
     * @reason Remove modulo operations and recursion
     */
    @Override
    @Overwrite
    public void nextVertexFormatIndex() {
        ImmutableList<VertexFormatElement> elements = this.vertexFormat.getElements();

        do {
            this.nextElementBytes += this.vertexFormatElement.getSize();

            // Wrap around the element pointer without using modulo
            if (++this.vertexFormatIndex >= elements.size()) {
                this.vertexFormatIndex -= elements.size();
            }

            this.vertexFormatElement = elements.get(this.vertexFormatIndex);
        } while (this.vertexFormatElement.getUsage() == VertexFormatElement.Usage.PADDING);

        if (this.defaultColor && this.vertexFormatElement.getUsage() == VertexFormatElement.Usage.COLOR) {
            IVertexConsumer.super.color(this.defaultRed, this.defaultGreen, this.defaultBlue, this.defaultAlpha);
        }
    }
}
