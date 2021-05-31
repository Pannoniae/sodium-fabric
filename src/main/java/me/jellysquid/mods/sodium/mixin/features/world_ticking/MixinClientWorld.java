package me.jellysquid.mods.sodium.mixin.features.world_ticking;

import me.jellysquid.mods.sodium.client.util.rand.XoRoShiRoRandom;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.ParticleEffectAmbience;
import net.minecraft.world.storage.ISpawnWorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;
import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld extends World {
    @Shadow
    protected abstract void spawnFluidParticle(BlockPos pos, BlockState state, IParticleData parameters, boolean bl);

    protected MixinClientWorld(ISpawnWorldInfo mutableWorldProperties, RegistryKey<World> registryKey,
                               DimensionType dimensionType,
                               Supplier<IProfiler> profiler, boolean bl, boolean bl2, long l) {
        super(mutableWorldProperties, registryKey, dimensionType, profiler, bl, bl2, l);
    }

    @Redirect(method = "animateTick(III)V", at = @At(value = "NEW", target = "java/util/Random"))
    private Random redirectRandomTickRandom() {
        return new XoRoShiRoRandom();
    }

    /**
     * @reason Avoid allocations, branch code out, early-skip some code
     * @author JellySquid
     */
    @Overwrite
    public void animateTick(int xCenter, int yCenter, int zCenter, int radius, Random random, boolean spawnBarrierParticles, BlockPos.Mutable pos) {
        int x = xCenter + (random.nextInt(radius) - random.nextInt(radius));
        int y = yCenter + (random.nextInt(radius) - random.nextInt(radius));
        int z = zCenter + (random.nextInt(radius) - random.nextInt(radius));

        pos.setPos(x, y, z);

        BlockState blockState = this.getBlockState(pos);

        if (!blockState.isAir()) {
            this.performBlockDisplayTick(blockState, pos, random, spawnBarrierParticles);
        }

        if (!blockState.isOpaqueCube(this, pos)) {
            this.performBiomeParticleDisplayTick(pos, random);
        }

        FluidState fluidState = blockState.getFluidState();

        if (!fluidState.isEmpty()) {
            this.performFluidDisplayTick(blockState, fluidState, pos, random);
        }
    }

    private void performBlockDisplayTick(BlockState blockState, BlockPos pos, Random random, boolean spawnBarrierParticles) {
        blockState.getBlock().animateTick(blockState, this, pos, random);

        if (spawnBarrierParticles && blockState.matchesBlock(Blocks.BARRIER)) {
            this.performBarrierDisplayTick(pos);
        }
    }

    private void performBarrierDisplayTick(BlockPos pos) {
        this.addParticle(ParticleTypes.BARRIER, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                0.0D, 0.0D, 0.0D);
    }

    private void performBiomeParticleDisplayTick(BlockPos pos, Random random) {
        ParticleEffectAmbience config = this.getBiome(pos)
                .getAmbientParticle()
                .orElse(null);

        if (config != null && config.shouldParticleSpawn(random)) {
            this.addParticle(config.getParticleOptions(),
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    0.0D, 0.0D, 0.0D);
        }
    }

    private void performFluidDisplayTick(BlockState blockState, FluidState fluidState, BlockPos pos, Random random) {
        fluidState.animateTick(this, pos, random);

        IParticleData particleEffect = fluidState.getDripParticleData();

        if (particleEffect != null && random.nextInt(10) == 0) {
            boolean solid = blockState.isSolidSide(this, pos, Direction.DOWN);

            // FIXME: don't allocate here
            BlockPos blockPos = pos.down();
            this.spawnFluidParticle(blockPos, this.getBlockState(blockPos), particleEffect, solid);
        }
    }
}
