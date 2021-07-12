package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.world.ClientWorldExtended;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.biome.IBiomeMagnifier;

import java.util.Arrays;

public class BiomeCache {
    private final IBiomeMagnifier type;
    private final long seed;

    private final Biome[] biomes;

    public BiomeCache(World world) {
        this.type = world.getDimensionType().getMagnifier();
        this.seed = ((ClientWorldExtended) world).getBiomeSeed();

        this.biomes = new Biome[16 * 16];
    }

    public Biome getBiome(BiomeManager.IBiomeReader storage, int x, int y, int z) {
        int idx = ((z & 15) << 4) | (x & 15);

        Biome biome = this.biomes[idx];

        if (biome == null) {
            this.biomes[idx] = biome = this.type.getBiome(this.seed, x, y, z, storage);
        }

        return biome;
    }

    public void reset() {
        Arrays.fill(this.biomes, null);
    }
}
