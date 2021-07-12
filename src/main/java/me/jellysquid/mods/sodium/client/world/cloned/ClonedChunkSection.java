package me.jellysquid.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPaletteFallback;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalleteArray;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BitArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.palette.IPalette;
import net.minecraft.util.palette.IdentityPalette;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.NibbleArray;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClonedChunkSection {
    private static final LightType[] LIGHT_TYPES = LightType.values();
    private static final ChunkSection EMPTY_SECTION = new ChunkSection(0);

    private final AtomicInteger referenceCount = new AtomicInteger(0);
    private final ClonedChunkSectionCache backingCache;

    private final Long2ObjectOpenHashMap<TileEntity> blockEntities;
    private final NibbleArray[] lightDataArrays;
    private final World world;

    private SectionPos pos;

    private BitArray blockStateData;
    private ClonedPalette<BlockState> blockStatePalette;

    private BiomeContainer biomeData;

    ClonedChunkSection(ClonedChunkSectionCache backingCache, World world) {
        this.backingCache = backingCache;
        this.world = world;
        this.blockEntities = new Long2ObjectOpenHashMap<>(8);
        this.lightDataArrays = new NibbleArray[LIGHT_TYPES.length];
    }

    public void init(SectionPos pos) {
        Chunk chunk = world.getChunk(pos.getX(), pos.getZ());

        if (chunk == null) {
            throw new RuntimeException("Couldn't retrieve chunk at " + pos.asChunkPos());
        }

        ChunkSection section = getChunkSection(chunk, pos);

        if (ChunkSection.isEmpty(section)) {
            section = EMPTY_SECTION;
        }

        this.pos = pos;

        PalettedContainerExtended<BlockState> container = PalettedContainerExtended.cast(section.getData());;

        this.blockStateData = copyBlockData(container);
        this.blockStatePalette = copyPalette(container);

        for (LightType type : LIGHT_TYPES) {
            this.lightDataArrays[type.ordinal()] = world.getLightManager()
                    .getLightEngine(type)
                    .getData(pos);
        }

        this.biomeData = chunk.getBiomes();

        MutableBoundingBox box = new MutableBoundingBox(pos.getWorldStartX(), pos.getWorldStartY(), pos.getWorldStartZ(),
                pos.getWorldEndX(), pos.getWorldEndY(), pos.getWorldEndZ());

        this.blockEntities.clear();

        for (Map.Entry<BlockPos, TileEntity> entry : chunk.getTileEntityMap().entrySet()) {
            BlockPos entityPos = entry.getKey();

            if (box.isVecInside(entityPos)) {
                this.blockEntities.put(BlockPos.pack(entityPos.getX() & 15, entityPos.getY() & 15, entityPos.getZ() & 15), entry.getValue());
            }
        }
    }

    public BlockState getBlockState(int x, int y, int z) {
        return this.blockStatePalette.get(this.blockStateData.getAt(y << 8 | z << 4 | x));
    }

    public int getLightLevel(LightType type, int x, int y, int z) {
        NibbleArray array = this.lightDataArrays[type.ordinal()];

        if (array != null) {
            return array.get(x, y, z);
        }

        return 0;
    }

    public Biome getBiomeForNoiseGen(int x, int y, int z) {
        return this.biomeData.getNoiseBiome(x, y, z);
    }

    public TileEntity getBlockEntity(int x, int y, int z) {
        return this.blockEntities.get(BlockPos.pack(x, y, z));
    }

    public BitArray getBlockData() {
        return this.blockStateData;
    }

    public ClonedPalette<BlockState> getBlockPalette() {
        return this.blockStatePalette;
    }

    public SectionPos getPosition() {
        return this.pos;
    }

    private static ClonedPalette<BlockState> copyPalette(PalettedContainerExtended<BlockState> container) {
        IPalette<BlockState> palette = container.getPalette();

        if (palette instanceof IdentityPalette) {
            // TODO: Use GameRegistry
            return new ClonedPaletteFallback<>(Block.BLOCK_STATE_IDS);
        }

        BlockState[] array = new BlockState[1 << container.getPaletteSize()];

        for (int i = 0; i < array.length; i++) {
            array[i] = palette.get(i);

            if (array[i] == null) {
                break;
            }
        }

        return new ClonedPalleteArray<>(array, container.getDefaultValue());
    }

    private static BitArray copyBlockData(PalettedContainerExtended<BlockState> container) {
        BitArray array = container.getDataArray();
        long[] storage = array.getBackingLongArray();

        return new BitArray(container.getPaletteSize(), array.size(), storage.clone());
    }

    private static ChunkSection getChunkSection(Chunk chunk, SectionPos pos) {
        ChunkSection section = null;

        if (!World.isYOutOfBounds(SectionPos.toWorld(pos.getY()))) {
            section = chunk.getSections()[pos.getY()];
        }

        return section;
    }

    public void acquireReference() {
        this.referenceCount.incrementAndGet();
    }

    public boolean releaseReference() {
        return this.referenceCount.decrementAndGet() <= 0;
    }

    public ClonedChunkSectionCache getBackingCache() {
        return this.backingCache;
    }
}
