package me.jellysquid.mods.sodium.client.render.chunk.data;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * The render data for a chunk render container containing all the information about which meshes are attached, the
 * block entities contained by it, and any data used for occlusion testing.
 */
public class ChunkRenderData {
    public static final ChunkRenderData ABSENT = new ChunkRenderData.Builder()
            .build();
    public static final ChunkRenderData EMPTY = createEmptyData();

    private List<TileEntity> globalBlockEntities;
    private List<TileEntity> blockEntities;
    @Getter
    private List<BlockPos> translucentBlocks;

    private EnumMap<BlockRenderPass, ChunkMeshData> meshes;

    private SetVisibility occlusionData;
    private ChunkRenderBounds bounds;

    private List<TextureAtlasSprite> animatedSprites;

    private boolean isEmpty;
    private int meshByteSize;
    private int facesWithData;

    /**
     * @return True if the chunk has no renderables, otherwise false
     */
    public boolean isEmpty() {
        return this.isEmpty;
    }

    public ChunkRenderBounds getBounds() {
        return this.bounds;
    }

    public SetVisibility getOcclusionData() {
        return this.occlusionData;
    }

    public List<TextureAtlasSprite> getAnimatedSprites() {
        return this.animatedSprites;
    }

    /**
     * The collection of block entities contained by this rendered chunk.
     */
    public Collection<TileEntity> getBlockEntities() {
        return this.blockEntities;
    }

    /**
     * The collection of block entities contained by this rendered chunk section which are not part of its culling
     * volume. These entities should always be rendered regardless of the render being visible in the frustum.
     */
    public Collection<TileEntity> getGlobalBlockEntities() {
        return this.globalBlockEntities;
    }

    /**
     * The collection of chunk meshes belonging to this render.
     */
    public ChunkMeshData getMesh(BlockRenderPass pass) {
        return this.meshes.get(pass);
    }

    public int getMeshSize() {
        return this.meshByteSize;
    }

    public int getFacesWithData() {
        return this.facesWithData;
    }

    public static class Builder {
        private final List<TileEntity> globalBlockEntities = new ArrayList<>();
        private final List<TileEntity> blockEntities = new ArrayList<>();
        private final List<BlockPos> translucentBlocks = new ArrayList<>();
        private final Set<TextureAtlasSprite> animatedSprites = new ObjectOpenHashSet<>();

        private final EnumMap<BlockRenderPass, ChunkMeshData> meshes = new EnumMap<>(BlockRenderPass.class);

        private SetVisibility occlusionData;
        private ChunkRenderBounds bounds = ChunkRenderBounds.ALWAYS_FALSE;

        public Builder() {
            for (BlockRenderPass pass : BlockRenderPass.VALUES) {
                this.setMesh(pass, ChunkMeshData.EMPTY);
            }
        }

        public void setBounds(ChunkRenderBounds bounds) {
            this.bounds = bounds;
        }

        public void setOcclusionData(SetVisibility data) {
            this.occlusionData = data;
        }

        /**
         * Adds a TextureAtlasSprite to this data container for tracking. If the TextureAtlasSprite is tickable, it will be ticked every frame
         * before rendering as necessary.
         * @param TextureAtlasSprite The TextureAtlasSprite
         */
        public void addSprite(TextureAtlasSprite TextureAtlasSprite) {
            if (TextureAtlasSprite.hasAnimationMetadata()) {
                this.animatedSprites.add(TextureAtlasSprite);
            }
        }

        public void setMesh(BlockRenderPass pass, ChunkMeshData data) {
            this.meshes.put(pass, data);
        }

        /**
         * Adds a block entity to the data container.
         * @param entity The block entity itself
         * @param cull True if the block entity can be culled to this chunk render's volume, otherwise false
         */
        public void addBlockEntity(TileEntity entity, boolean cull) {
            (cull ? this.blockEntities : this.globalBlockEntities).add(entity);
        }

        public void addTranslucentBlock(BlockPos pos) {
            this.translucentBlocks.add(pos);
        }

        public ChunkRenderData build() {
            ChunkRenderData data = new ChunkRenderData();
            data.globalBlockEntities = this.globalBlockEntities;
            data.blockEntities = this.blockEntities;
            data.translucentBlocks = this.translucentBlocks;
            data.occlusionData = this.occlusionData;
            data.meshes = this.meshes;
            data.bounds = this.bounds;
            data.animatedSprites = new ObjectArrayList<>(this.animatedSprites);

            int facesWithData = 0;
            int size = 0;

            for (ChunkMeshData meshData : this.meshes.values()) {
                size += meshData.getVertexDataSize();

                for (Map.Entry<ModelQuadFacing, BufferSlice> entry : meshData.getSlices()) {
                    facesWithData |= 1 << entry.getKey().ordinal();
                }
            }

            data.isEmpty = this.globalBlockEntities.isEmpty() && this.blockEntities.isEmpty() && facesWithData == 0;
            data.meshByteSize = size;
            data.facesWithData = facesWithData;

            return data;
        }
    }

    private static ChunkRenderData createEmptyData() {
        SetVisibility occlusionData = new SetVisibility();
        occlusionData.setManyVisible(EnumSet.allOf(Direction.class));

        ChunkRenderData.Builder meshInfo = new ChunkRenderData.Builder();
        meshInfo.setOcclusionData(occlusionData);

        return meshInfo.build();
    }
}