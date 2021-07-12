package me.jellysquid.mods.sodium.client.model.vertex.type;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;

/**
 * Provides factories which create a {@link VertexSink} for the given vertex format.
 *
 * @param <T> The {@link VertexSink} type this factory produces
 */
public interface VertexType<T extends VertexSink> {
    /**
     * Creates a {@link VertexSink} which can write into any {@link IVertexBuilder}. This is generally used when
     * a special implementation of {@link IVertexBuilder} is used that cannot be optimized for, or when
     * complex/unsupported transformations need to be performed using vanilla code paths.
     * @param consumer The {@link IVertexBuilder} to write into
     */
    T createFallbackWriter(IVertexBuilder consumer);

    /**
     * If this vertex type supports {@link BufferVertexType}, then this method returns this vertex type as a
     * blittable type, performing a safe cast.
     */
    default BlittableVertexType<T> asBlittable() {
        return null;
    }
}
