package me.jellysquid.mods.sodium.client.world.cloned;

import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.math.SectionPos;

public class ChunkRenderContext {
    private final SectionPos origin;
    private final ClonedChunkSection[] sections;
    private final MutableBoundingBox volume;

    public ChunkRenderContext(SectionPos origin, ClonedChunkSection[] sections, MutableBoundingBox volume) {
        this.origin = origin;
        this.sections = sections;
        this.volume = volume;
    }

    public ClonedChunkSection[] getSections() {
        return this.sections;
    }

    public SectionPos getOrigin() {
        return this.origin;
    }

    public MutableBoundingBox getVolume() {
        return this.volume;
    }

    public void releaseResources() {
        for (ClonedChunkSection section : sections) {
            if (section != null) {
                section.getBackingCache()
                        .release(section);
            }
        }
    }
}
