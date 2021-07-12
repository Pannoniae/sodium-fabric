package me.jellysquid.mods.sodium.client.model.vertex.formats.particle;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.formats.particle.writer.ParticleVertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.model.vertex.formats.particle.writer.ParticleVertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.model.vertex.formats.particle.writer.ParticleVertexWriterFallback;
import me.jellysquid.mods.sodium.client.model.vertex.type.BlittableVertexType;
import me.jellysquid.mods.sodium.client.model.vertex.type.VanillaVertexType;
import net.minecraft.client.renderer.vertex.VertexFormat;

public class ParticleVertexType implements VanillaVertexType<ParticleVertexSink>, BlittableVertexType<ParticleVertexSink> {
    @Override
    public ParticleVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
        return direct ? new ParticleVertexBufferWriterUnsafe(buffer) : new ParticleVertexBufferWriterNio(buffer);
    }

    @Override
    public ParticleVertexSink createFallbackWriter(IVertexBuilder consumer) {
        return new ParticleVertexWriterFallback(consumer);
    }

    @Override
    public BlittableVertexType<ParticleVertexSink> asBlittable() {
        return this;
    }

    @Override
    public VertexFormat getVertexFormat() {
        return ParticleVertexSink.VERTEX_FORMAT;
    }
}
