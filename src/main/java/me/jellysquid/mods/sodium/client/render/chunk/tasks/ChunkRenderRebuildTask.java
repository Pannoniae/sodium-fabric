package me.jellysquid.mods.sodium.client.render.chunk.tasks;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.common.util.RenderTypeLookupUtil;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.fluid.FluidState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.ForgeHooksClient;

/**
 * Rebuilds all the meshes of a chunk for each given render pass with non-occluded blocks. The result is then uploaded
 * to graphics memory on the main thread.
 *
 * This task takes a slice of the world from the thread it is created on. Since these slices require rather large
 * array allocations, they are pooled to ensure that the garbage collector doesn't become overloaded.
 */
public class ChunkRenderRebuildTask<T extends ChunkGraphicsState> extends ChunkRenderBuildTask<T> {

    // 16x16x16
    private static final int CHUNK_BUILD_SIZE = 16;

    private final ChunkRenderContainer<T> render;
    private final Vector3d camera;
    private final BlockPos offset;

    private final boolean translucencySorting;
    private final ChunkRenderContext context;

    public ChunkRenderRebuildTask(ChunkRenderContainer<T> render, ChunkRenderContext context, BlockPos offset, Vector3d camera) {
        this.render = render;
        this.offset = offset;
        this.camera = camera;
        this.translucencySorting = SodiumClientMod.options().advanced.translucencySorting;
        this.context = context;
    }

    @Override
    public ChunkBuildResult<T> performBuild(ChunkRenderCacheLocal cache, ChunkBuildBuffers buffers, CancellationSource cancellationSource) {
        ChunkRenderData.Builder renderData = new ChunkRenderData.Builder();
        VisGraph occluder = new VisGraph();
        ChunkRenderBounds.Builder bounds = new ChunkRenderBounds.Builder();

        buffers.init(renderData);

        cache.init(this.context);

        WorldSlice slice = cache.getWorldSlice();

        int baseX = this.render.getOriginX();
        int baseY = this.render.getOriginY();
        int baseZ = this.render.getOriginZ();

        BlockPos.Mutable pos = new BlockPos.Mutable();
        BlockPos renderOffset = this.offset;

        boolean shouldSortBackwards = false;

        for (int relY = 0; relY < CHUNK_BUILD_SIZE; relY++) {
            if (cancellationSource.isCancelled()) {
                return null;
            }
            for (int relZ = 0; relZ < CHUNK_BUILD_SIZE; relZ++) {
                for (int relX = 0; relX < CHUNK_BUILD_SIZE; relX++) {
                    BlockState state = slice.getBlockState(baseX + relX, baseY + relY, baseZ + relZ);

                    if (this.translucencySorting && !shouldSortBackwards) {
                        for (BlockRenderPass pass : BlockRenderPass.TRANSLUCENTS) {
                            if (RenderTypeLookupUtil.canRenderInLayer(state, pass.getLayer())) {
                                shouldSortBackwards = true;
                                break;
                            }
                        }
                    }

                    setupBlockRender(cache, buffers, renderData, slice, occluder, bounds, pos, renderOffset, state, relX, relY, relZ,
                            relX+baseX, relY+baseY, relZ+baseZ);
                }
            }
        }

        render.setRebuildableForTranslucents(shouldSortBackwards);

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            ChunkMeshData mesh = buffers.createMesh(pass, (float) camera.x - offset.getX(),
                    (float) camera.y - offset.getY(),
                    (float) camera.z - offset.getZ(), shouldSortBackwards);

            if (mesh != null) {
                renderData.setMesh(pass, mesh);
            }
        }

        renderData.setOcclusionData(occluder.computeVisibility());
        renderData.setBounds(bounds.build(this.render.getChunkPos()));

        return new ChunkBuildResult<>(this.render, renderData.build());
    }



    private void setupBlockRender(ChunkRenderCacheLocal cache, ChunkBuildBuffers buffers, ChunkRenderData.Builder renderData, WorldSlice slice,
                                  VisGraph occluder, ChunkRenderBounds.Builder bounds, BlockPos.Mutable pos, BlockPos offset, BlockState blockState,
                                  int relX, int relY, int relZ, int x, int y, int z) {
        if (blockState.isAir()) {
            return;
        }

        pos.setPos(x, y, z);
        buffers.setRenderOffset(x - offset.getX(), y - offset.getY(), z - offset.getZ());

        // TODO: don't create a new BlockPos, just use coordinates
        if (blockState.isOpaqueCube(slice, pos)) {
            occluder.setOpaqueCube(pos);
        }

        if (blockState.hasTileEntity()) {
            TileEntity entity = slice.getTileEntity(pos);

            if (entity != null) {
                TileEntityRenderer<TileEntity> renderer = TileEntityRendererDispatcher.instance.getRenderer(entity);

                if (renderer != null) {
                    renderData.addBlockEntity(entity, !renderer.isGlobalRenderer(entity));

                    bounds.addBlock(relX, relY, relZ);
                }
            }
        }

        for (RenderType layer : RenderType.getBlockRenderTypes()) {
            ForgeHooksClient.setRenderLayer(layer);
            // Fluids
            FluidState fluidState = blockState.getFluidState();

            if (!fluidState.isEmpty() && RenderTypeLookupUtil.canRenderInLayer(fluidState, layer)) {
                if (layer == RenderType.getTranslucent() || layer == RenderType.getTripwire()) {
                    renderData.addTranslucentBlock(pos.toImmutable());
                }

                if (cache.getFluidRenderer().render(slice, fluidState, pos, buffers.get(layer))) {
                    bounds.addBlock(relX, relY, relZ);
                }
            }

            if (blockState.getRenderType() != BlockRenderType.MODEL || !RenderTypeLookupUtil.canRenderInLayer(blockState, layer)) {
                continue;
            }

            if (layer == RenderType.getTranslucent() || layer == RenderType.getTripwire()) {
                renderData.addTranslucentBlock(pos.toImmutable());
            }

            IBakedModel model = cache.getBlockModels()
                    .getModel(blockState);

            long seed = blockState.getPositionRandom(pos);

            // Solid blocks
            if (cache.getBlockRenderer().renderModel(slice, blockState, pos, model, buffers.get(layer), true, seed)) {
                bounds.addBlock(relX, relY, relZ);
            }
        }
        ForgeHooksClient.setRenderLayer(null);
    }

    @Override
    public void releaseResources() {
        this.context.releaseResources();
    }
}
