package me.jellysquid.mods.lithium.common.ai.pathing;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.fluid.FluidState;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;

public class PathNodeDefaults {
    public static PathNodeType getNeighborNodeType(BlockState state) {
        if (state.isAir()) {
            return PathNodeType.OPEN;
        }

        // [VanillaCopy] LandPathNodeMaker#getNodeTypeFromNeighbors
        // Determine what kind of obstacle type this neighbor is
        if (state.matchesBlock(Blocks.CACTUS)) {
            return PathNodeType.DANGER_CACTUS;
        } else if (state.matchesBlock(Blocks.SWEET_BERRY_BUSH)) {
            return PathNodeType.DANGER_OTHER;
        } else if (isFireDangerSource(state)) {
            return PathNodeType.DANGER_FIRE;
        } else if (state.getFluidState().isTagged(FluidTags.WATER)) {
            return PathNodeType.WATER_BORDER;
        } else {
            return PathNodeType.OPEN;
        }
    }

    public static PathNodeType getNodeType(BlockState state) {
        if (state.isAir()) {
            return PathNodeType.OPEN;
        }

        Block block = state.getBlock();
        Material material = state.getMaterial();

        if (state.isIn(BlockTags.TRAPDOORS) || state.matchesBlock(Blocks.LILY_PAD)) {
            return PathNodeType.TRAPDOOR;
        }

        if (state.matchesBlock(Blocks.CACTUS)) {
            return PathNodeType.DAMAGE_CACTUS;
        }

        if (state.matchesBlock(Blocks.SWEET_BERRY_BUSH)) {
            return PathNodeType.DAMAGE_OTHER;
        }

        if (state.matchesBlock(Blocks.HONEY_BLOCK)) {
            return PathNodeType.STICKY_HONEY;
        }

        if (state.matchesBlock(Blocks.COCOA)) {
            return PathNodeType.COCOA;
        }

        if (isFireDangerSource(state)) {
            return PathNodeType.DAMAGE_FIRE;
        }

        if (DoorBlock.isWooden(state) && !state.get(DoorBlock.OPEN)) {
            return PathNodeType.DOOR_WOOD_CLOSED;
        }

        if ((block instanceof DoorBlock) && (material == Material.IRON) && !state.get(DoorBlock.OPEN)) {
            return PathNodeType.DOOR_IRON_CLOSED;
        }

        if ((block instanceof DoorBlock) && state.get(DoorBlock.OPEN)) {
            return PathNodeType.DOOR_OPEN;
        }

        if (block instanceof AbstractRailBlock) {
            return PathNodeType.RAIL;
        }

        if (block instanceof LeavesBlock) {
            return PathNodeType.LEAVES;
        }

        if (block.isIn(BlockTags.FENCES) || block.isIn(BlockTags.WALLS) || ((block instanceof FenceGateBlock) && !state.get(FenceGateBlock.OPEN))) {
            return PathNodeType.FENCE;
        }

        // Retrieve the fluid state from the block state to avoid a second lookup
        FluidState fluid = state.getFluidState();

        if (fluid.isTagged(FluidTags.WATER)) {
            return PathNodeType.WATER;
        } else if (fluid.isTagged(FluidTags.LAVA)) {
            return PathNodeType.LAVA;
        }

        return PathNodeType.OPEN;
    }

    private static boolean isFireDangerSource(BlockState blockState) {
        return blockState.isIn(BlockTags.FIRE) || blockState.matchesBlock(Blocks.LAVA) || blockState.matchesBlock(Blocks.MAGMA_BLOCK) || CampfireBlock.isLit(blockState);
    }
}
