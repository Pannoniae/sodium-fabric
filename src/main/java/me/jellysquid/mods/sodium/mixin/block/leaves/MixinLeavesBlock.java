package me.jellysquid.mods.sodium.mixin.block.leaves;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.Direction;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LeavesBlock.class)
public class MixinLeavesBlock extends Block {

    public MixinLeavesBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isSideInvisible(BlockState state, BlockState adjacentBlockState, Direction side) {
        if (SodiumClientMod.options().advanced.useLeavesCulling) {
            return adjacentBlockState.getBlock() instanceof LeavesBlock;
        } else {
            return super.isSideInvisible(state, adjacentBlockState, side);
        }
    }
}
