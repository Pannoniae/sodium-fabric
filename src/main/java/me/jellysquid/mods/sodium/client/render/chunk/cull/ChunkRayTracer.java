package me.jellysquid.mods.sodium.client.render.chunk.cull;

import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.List;

public class ChunkRayTracer {

    private final Object2BooleanMap<BlockPos> blockStateCache;
    private final World world;

    public ChunkRayTracer(World world) {
        this.world = world;
        this.blockStateCache = new Object2BooleanArrayMap<>();
    }

    public void clear() {
        this.blockStateCache.clear();
    }

    public <T extends ChunkGraphicsState> boolean isInDirectView(ChunkRenderContainer<T> render, float camX, float camY, float camZ) {
        List<BlockPos> srcTranslucent = render.getData().getTranslucentBlocks();

        int minX = MathHelper.floor(camX);
        int minY = MathHelper.floor(camY);
        int minZ = MathHelper.floor(camZ);

        ChunkRenderBounds bounds = render.getBounds();
        float boundMinX = bounds.x1;
        float boundMinY = bounds.y1;
        float boundMinZ = bounds.z1;
        float boundMaxX = bounds.x2;
        float boundMaxY = bounds.y2;
        float boundMaxZ = bounds.z2;
        for (BlockPos pos : srcTranslucent) {
            if (checkIntersectingGrids(minX, minY, minZ, pos.getX(), pos.getY(), pos.getZ(),
                    boundMinX, boundMinY, boundMinZ, boundMaxX, boundMaxY, boundMaxZ)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Ray trace to find block positions from one coordinate to another.
     *
     * From http://playtechs.blogspot.com/2007/03/raytracing-on-grid.html.
     */
    private boolean checkIntersectingGrids(int x, int y, int z, int maxX, int maxY, int maxZ,
                                           float boundMinX, float boundMinY, float boundMinZ, float boundMaxX, float boundMaxY, float boundMaxZ) {
        float dx = Math.abs(maxX - x);
        float dy = Math.abs(maxY - y);
        float dz = Math.abs(maxZ - z);

        float xWeight = 1.0f/dx;
        float yWeight = 1.0f/dy;
        float zWeight = 1.0f/dz;

        int x_inc = Integer.compare(maxX, x);
        int y_inc = Integer.compare(maxY, y);
        int z_inc = Integer.compare(maxZ, z);

        float n = 1 + dx + dy + dz;
        float errorX = xWeight;
        float errorY = yWeight;
        float errorZ = zWeight;

        for (; n > 0; n--) {
            // We hit the source chunk we are testing for, so it's directly visible
            if ((x >= boundMinX || x <= boundMaxX) && (y >= boundMinY || x <= boundMaxY) && (z >= boundMinZ || x <= boundMaxZ)) {
                return true;
            }

            BlockPos curr = new BlockPos(x, y, z);
            if (blockStateCache.getOrDefault(curr, false)) {
                return false;
            } else {
                BlockState state = this.world.getBlockState(curr);
                // We found an opaque block from another chunk that's blocking the view to this translucent block
                if (!state.isAir() && state.isOpaqueCube(this.world, curr)) {
                    blockStateCache.put(curr, true);
                    return false;
                } else {
                    blockStateCache.put(curr, false);
                }
            }

            if (errorX < errorY && errorX < errorZ) {
                x += x_inc;
                errorX += xWeight;
            } else if (errorY < errorX && errorY < errorZ) {
                y += y_inc;
                errorY += yWeight;
            } else {
                z += z_inc;
                errorZ += zWeight;
            }
        }

        return true;
    }
}
