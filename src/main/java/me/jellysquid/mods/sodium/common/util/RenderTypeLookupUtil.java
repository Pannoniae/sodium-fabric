package me.jellysquid.mods.sodium.common.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.fluid.FluidState;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vanilla {@link net.minecraft.client.renderer.RenderTypeLookup} has a synchronized block around predicate function.
 * No need for that, we can instead cache the values and avoid computation every time.
 */
public class RenderTypeLookupUtil {

    private static final Map<BlockStateKey, Boolean> blockStateLookup = new ConcurrentHashMap<>();
    private static final Map<FluidStateKey, Boolean> fluidStateLookup = new ConcurrentHashMap<>();

    public synchronized static boolean canRenderInLayer(BlockState state, RenderType layer) {
        BlockStateKey key = new BlockStateKey(state, layer);
        if (blockStateLookup.containsKey(key) && !(state.getBlock() instanceof LeavesBlock)) {
            return blockStateLookup.get(key);
        } else {
            boolean canRender = RenderTypeLookup.canRenderInLayer(state, layer);
            blockStateLookup.put(key, canRender);
            return canRender;
        }
    }

    public synchronized static boolean canRenderInLayer(FluidState state, RenderType layer) {
        FluidStateKey key = new FluidStateKey(state, layer);
        if (fluidStateLookup.containsKey(key)) {
            return fluidStateLookup.get(key);
        } else {
            boolean canRender = RenderTypeLookup.canRenderInLayer(state, layer);
            fluidStateLookup.put(key, canRender);
            return canRender;
        }
    }

    private static class BlockStateKey {
        private final BlockState state;
        private final RenderType layer;

        public BlockStateKey(BlockState state, RenderType layer) {
            this.state = state;
            this.layer = layer;
        }

        // This returns true if the Block of the BlockState is equal
        @Override
        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof BlockStateKey))
                return false;
            final BlockStateKey other = (BlockStateKey) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$layer = this.layer;
            final Object other$layer = other.layer;
            if (this$layer == null ? other$layer != null : !this$layer.equals(other$layer)) return false;
            final BlockState this$state = this.state;
            final BlockState other$state = other.state;
            if (this$state == null && other$state != null || this$state != null && other$state == null) return false;
            if (this$state == other$state) return true;
            return this$state.getBlock().equals(other$state.getBlock());
        }

        protected boolean canEqual(final Object other) {
            return other instanceof BlockStateKey;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $state = this.state;
            result = result * PRIME + ($state == null ? 43 : $state.hashCode());
            final Object $layer = this.layer;
            result = result * PRIME + ($layer == null ? 43 : $layer.hashCode());
            return result;
        }
    }

    private static class FluidStateKey {
        private final FluidState state;
        private final RenderType layer;

        public FluidStateKey(FluidState state, RenderType layer) {
            this.state = state;
            this.layer = layer;
        }

        // This returns true if the Fluid of the FluidState is equal
        @Override
        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof FluidStateKey))
                return false;
            final FluidStateKey other = (FluidStateKey) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$layer = this.layer;
            final Object other$layer = other.layer;
            if (this$layer == null ? other$layer != null : !this$layer.equals(other$layer)) return false;
            final FluidState this$state = this.state;
            final FluidState other$state = other.state;
            if (this$state == null && other$state != null || this$state != null && other$state == null) return false;
            if (this$state == other$state) return true;
            return this$state.getFluid().equals(other$state.getFluid());
        }

        protected boolean canEqual(final Object other) {
            return other instanceof FluidStateKey;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $state = this.state;
            result = result * PRIME + ($state == null ? 43 : $state.hashCode());
            final Object $layer = this.layer;
            result = result * PRIME + ($layer == null ? 43 : $layer.hashCode());
            return result;
        }
    }
}
