package me.jellysquid.mods.sodium.mixin.chunk.light;

import it.unimi.dsi.fastutil.longs.*;
import me.jellysquid.mods.phosphor.common.chunk.light.IReadonly;
import me.jellysquid.mods.phosphor.common.chunk.light.LevelPropagatorAccess;
import me.jellysquid.mods.phosphor.common.chunk.light.SharedLightStorageAccess;
import me.jellysquid.mods.phosphor.common.chunk.light.SkyLightStorageDataAccess;
import me.jellysquid.mods.phosphor.common.util.chunk.light.EmptyChunkNibbleArray;
import me.jellysquid.mods.phosphor.common.util.chunk.light.SkyLightChunkNibbleArray;
import me.jellysquid.mods.phosphor.common.util.math.ChunkSectionPosHelper;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.lighting.LightEngine;
import net.minecraft.world.lighting.SkyLightStorage;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.concurrent.locks.StampedLock;

@Mixin(SkyLightStorage.class)
public abstract class MixinSkyLightStorage extends MixinLightStorage<SkyLightStorage.StorageMap> {
    /**
     * An optimized implementation which avoids constantly unpacking and repacking integer coordinates.
     *
     * @reason Use faster implementation
     * @author JellySquid
     */
    @Overwrite
    public int getLightOrDefault(long pos) {
        int posX = BlockPos.unpackX(pos);
        int posYOrig = BlockPos.unpackY(pos);
        int posZ = BlockPos.unpackZ(pos);

        int chunkX = SectionPos.toChunk(posX);
        int chunkYOrig = SectionPos.toChunk(posYOrig);
        int chunkZ = SectionPos.toChunk(posZ);

        long chunkOrig = SectionPos.asLong(chunkX, chunkYOrig, chunkZ);

        StampedLock lock = ((SharedLightStorageAccess<SkyLightStorage.StorageMap>) this).getStorageLock();
        long stamp;

        NibbleArray array;

        optimisticRead:
        while (true) {
            stamp = lock.tryOptimisticRead();

            int posY = posYOrig;
            int chunkY = chunkYOrig;
            long chunk = chunkOrig;

            SkyLightStorage.StorageMap data = ((SharedLightStorageAccess<SkyLightStorage.StorageMap>) this).getStorage();
            SkyLightStorageDataAccess sdata = ((SkyLightStorageDataAccess) (Object) data);

            int height = sdata.getHeight(SectionPos.toSectionColumnPos(chunk));

            if (height == sdata.getDefaultHeight() || chunkY >= height) {
                if (lock.validate(stamp)) {
                    return 15;
                } else {
                    continue;
                }
            }

            array = data.getArray(chunk);

            while (array == null) {
                ++chunkY;

                if (chunkY >= height) {
                    if (lock.validate(stamp)) {
                        return 15;
                    } else {
                        continue optimisticRead;
                    }
                }

                chunk = ChunkSectionPosHelper.updateYLong(chunk, chunkY);
                array = data.getArray(chunk);

                posY = chunkY << 4;
            }

            if (lock.validate(stamp)) {
                return array.get(
                        SectionPos.mask(posX),
                        SectionPos.mask(posY),
                        SectionPos.mask(posZ)
                );
            }
        }
    }

    @Shadow
    protected abstract boolean isAboveWorld(long sectionPos);

    @Shadow
    protected abstract boolean isSectionEnabled(long sectionPos);

    @Override
    public int getLightWithoutLightmap(final long blockPos) {
        final long sectionPos = SectionPos.worldToSection(blockPos);
        final NibbleArray lightmap = this.getLightmapAbove(sectionPos);

        if (lightmap == null) {
            return this.isSectionEnabled(sectionPos) ? 15 : 0;
        }

        return lightmap.get(SectionPos.mask(BlockPos.unpackX(blockPos)), 0, SectionPos.mask(BlockPos.unpackZ(blockPos)));
    }

    @Redirect(
        method = "getOrCreateArray(J)Lnet/minecraft/world/chunk/NibbleArray;",
        at = @At(
            value = "NEW",
            target = "net/minecraft/world/chunk/NibbleArray"
        )
    )
    private NibbleArray initializeLightmap(final long pos) {
        final NibbleArray ret = new NibbleArray();

        if (this.isSectionEnabled(pos)) {
            Arrays.fill(ret.getData(), (byte) -1);
        }

        return ret;
    }

    @Inject(
        method = "scheduleSurfaceUpdate(J)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void disable_enqueueRemoveSection(final CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(
        method = "scheduleFullUpdate(J)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void disable_enqueueAddSection(final CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * Forceload a lightmap above the world for initial skylight
     */
    @Unique
    private final LongSet preInitSkylightChunks = new LongOpenHashSet();

    @Override
    public void beforeChunkEnabled(final long chunkPos) {
        if (!this.isSectionEnabled(chunkPos)) {
            this.preInitSkylightChunks.add(chunkPos);
            this.scheduleUpdate(Long.MAX_VALUE, SectionPos.asLong(SectionPos.extractX(chunkPos), 16, SectionPos.extractZ(chunkPos)), 1, true);
        }
    }

    @Override
    public void afterChunkDisabled(final long chunkPos) {
        if (this.preInitSkylightChunks.remove(chunkPos)) {
            this.scheduleUpdate(Long.MAX_VALUE, SectionPos.asLong(SectionPos.extractX(chunkPos), 16, SectionPos.extractZ(chunkPos)), 2, false);
        }
    }

    @Override
    protected int getSourceLevel(final long id) {
        final int ret = super.getSourceLevel(id);

        if (ret >= 2 && SectionPos.extractY(id) == 16 && this.preInitSkylightChunks.contains(SectionPos.toSectionColumnPos(id))) {
            return 1;
        }

        return ret;
    }

    @Unique
    private final LongSet initSkylightChunks = new LongOpenHashSet();

    @Shadow
    @Final
    private LongSet enabledColumns;

    /**
     * @author PhiPro
     * @reason Re-implement completely.
     * This method now schedules initial lighting when enabling source light for a chunk that already has light updates enabled.
     */
    @Override
    @Overwrite
    public void setColumnEnabled(final long chunkPos, final boolean enabled) {
        if (enabled) {
            if (this.preInitSkylightChunks.contains(chunkPos)) {
                this.initSkylightChunks.add(chunkPos);
                this.updateHasPendingUpdates();
            } else {
                this.enabledColumns.add(chunkPos);
            }
        } else {
            this.enabledColumns.remove(chunkPos);
            this.initSkylightChunks.remove(chunkPos);
            this.updateHasPendingUpdates();
        }
    }

    @Unique
    private static void spreadSourceSkylight(final LevelPropagatorAccess lightProvider, final long src, final Direction dir) {
        lightProvider.invokePropagateLevel(src, BlockPos.offset(src, dir), 0, true);
    }

    /**
     * @author PhiPro
     * @reason Re-implement completely
     */
    @Overwrite
    public void updateSections(LightEngine<SkyLightStorage.StorageMap, ?> lightProvider, boolean doSkylight, boolean skipEdgeLightPropagation) {
        super.updateSections(lightProvider, doSkylight, skipEdgeLightPropagation);

        if (!doSkylight || !this.hasPendingUpdates) {
            return;
        }

        for (final LongIterator it = this.initSkylightChunks.iterator(); it.hasNext(); ) {
            final long chunkPos = it.nextLong();

            final LevelPropagatorAccess levelPropagator = (LevelPropagatorAccess) lightProvider;
            final int minY = this.fillSkylightColumn(lightProvider, chunkPos);

            this.enabledColumns.add(chunkPos);
            this.preInitSkylightChunks.remove(chunkPos);
            this.scheduleUpdate(Long.MAX_VALUE, SectionPos.asLong(SectionPos.extractX(chunkPos), 16, SectionPos.extractZ(chunkPos)), 2, false);

            if (this.hasSection(SectionPos.asLong(SectionPos.extractX(chunkPos), minY, SectionPos.extractZ(chunkPos)))) {
                final long blockPos = BlockPos.pack(SectionPos.toWorld(SectionPos.extractX(chunkPos)), SectionPos.toWorld(minY), SectionPos.toWorld(SectionPos.extractZ(chunkPos)));

                for (int x = 0; x < 16; ++x) {
                    for (int z = 0; z < 16; ++z) {
                        spreadSourceSkylight(levelPropagator, BlockPos.offset(blockPos, x, 16, z), Direction.DOWN);
                    }
                }
            }

            for (final Direction dir : Direction.Plane.HORIZONTAL) {
                // Skip propagations into sections directly exposed to skylight that are initialized in this update cycle
                boolean spread = !this.initSkylightChunks.contains(SectionPos.withOffset(chunkPos, dir));

                for (int y = 16; y > minY; --y) {
                    final long sectionPos = SectionPos.asLong(SectionPos.extractX(chunkPos), y, SectionPos.extractZ(chunkPos));
                    final long neighborSectionPos = SectionPos.withOffset(sectionPos, dir);

                    if (!this.hasSection(neighborSectionPos)) {
                        continue;
                    }

                    if (!spread) {
                        if (this.activeLightSections.contains(neighborSectionPos)) {
                            spread = true;
                        } else {
                            continue;
                        }
                    }

                    final long blockPos = BlockPos.pack(SectionPos.toWorld(SectionPos.extractX(sectionPos)), SectionPos.toWorld(y), SectionPos.toWorld(SectionPos.extractZ(sectionPos)));

                    final int ox = 15 * Math.max(dir.getXOffset(), 0);
                    final int oz = 15 * Math.max(dir.getZOffset(), 0);

                    final int dx = Math.abs(dir.getZOffset());
                    final int dz = Math.abs(dir.getXOffset());

                    for (int t = 0; t < 16; ++t) {
                        for (int dy = 0; dy < 16; ++dy) {
                            spreadSourceSkylight(levelPropagator, BlockPos.offset(blockPos, ox + t * dx, dy, oz + t * dz), dir);
                        }
                    }
                }
            }
        }

        this.initSkylightChunks.clear();

        if (!this.removedLightmaps.isEmpty()) {
            final LongSet removedLightmaps = new LongOpenHashSet(this.removedLightmaps);

            for (final LongIterator it = removedLightmaps.iterator(); it.hasNext(); ) {
                final long sectionPos = it.nextLong();

                if (!this.enabledChunks.contains(SectionPos.toSectionColumnPos(sectionPos))) {
                    continue;
                }

                if (!this.removedLightmaps.contains(sectionPos)) {
                    continue;
                }

                final long sectionPosAbove = this.getSectionAbove(sectionPos);

                if (sectionPosAbove == Long.MAX_VALUE) {
                    this.updateVanillaLightmapsBelow(sectionPos, this.isSectionEnabled(sectionPos) ? DIRECT_SKYLIGHT_MAP : null, true);
                } else {
                    long removedLightmapPosAbove = sectionPos;

                    for (long pos = sectionPos; pos != sectionPosAbove; pos = SectionPos.withOffset(pos, Direction.UP)) {
                        if (this.removedLightmaps.remove(pos)) {
                            removedLightmapPosAbove = pos;
                        }
                    }

                    this.updateVanillaLightmapsBelow(removedLightmapPosAbove, this.vanillaLightmapComplexities.get(sectionPosAbove) == 0 ? null : this.getArray(sectionPosAbove, true), false);
                }
            }

            this.removedLightmaps.clear();
        }

        this.hasPendingUpdates = false;
    }

    /**
     * Fill all sections above the topmost block with source skylight.
     * @return The section containing the topmost block or the section corresponding to {@link SkyLightStorage.StorageMap#minSectionY} if none exists.
     */
    private int fillSkylightColumn(final LightEngine<SkyLightStorage.StorageMap, ?> lightProvider, final long chunkPos) {
        int minY = 16;
        NibbleArray lightmapAbove = null;

        // First need to remove all pending light updates before changing any light value

        for (; this.isAboveBottom(minY); --minY) {
            final long sectionPos = SectionPos.asLong(SectionPos.extractX(chunkPos), minY, SectionPos.extractZ(chunkPos));

            if (this.activeLightSections.contains(sectionPos)) {
                break;
            }

            if (this.hasSection(sectionPos)) {
                this.cancelSectionUpdates(lightProvider, sectionPos);
            }

            final NibbleArray lightmap = this.getLightmap(sectionPos);

            if (lightmap != null) {
                lightmapAbove = lightmap;
            }
        }

        // Set up a lightmap and adjust the complexity for the section below

        final long sectionPosBelow = SectionPos.asLong(SectionPos.extractX(chunkPos), minY, SectionPos.extractZ(chunkPos));

        if (this.hasSection(sectionPosBelow)) {
            final NibbleArray lightmapBelow = this.getLightmap(sectionPosBelow);

            if (lightmapBelow == null) {
                int complexity = 15 * 16 * 16;

                if (lightmapAbove != null) {
                    for (int z = 0; z < 16; ++z) {
                        for (int x = 0; x < 16; ++x) {
                            complexity -= lightmapAbove.get(x, 0, z);
                        }
                    }
                }

                this.getOrAddLightmap(sectionPosBelow);
                this.setLightmapComplexity(sectionPosBelow, complexity);
            } else {
                int amount = 0;

                for (int z = 0; z < 16; ++z) {
                    for (int x = 0; x < 16; ++x) {
                        amount += getComplexityChange(lightmapBelow.get(x, 15, z), lightmapAbove == null ? 0 : lightmapAbove.get(x, 0, z), 15);
                    }
                }

                this.changeLightmapComplexity(sectionPosBelow, amount);
            }
        }

        // Now light values can be changed
        // Delete lightmaps so the sections inherit direct skylight

        int sections = 0;

        for (int y = 16; y > minY; --y) {
            final long sectionPos = SectionPos.asLong(SectionPos.extractX(chunkPos), y, SectionPos.extractZ(chunkPos));

            if (this.removeLightmap(sectionPos)) {
                sections |= 1 << (y + 1);
            }
        }

        // Calling onUnloadSection() after removing all the lightmaps is slightly more efficient

        this.cachedLightData.invalidateCaches();

        for (int y = 16; y > minY; --y) {
            if ((sections & (1 << (y + 1))) != 0) {
                this.removeSection(SectionPos.asLong(SectionPos.extractX(chunkPos), y, SectionPos.extractZ(chunkPos)));
            }
        }

        // Add trivial lightmaps for vanilla compatibility

        for (int y = 16; y > minY; --y) {
            final long sectionPos = SectionPos.asLong(SectionPos.extractX(chunkPos), y, SectionPos.extractZ(chunkPos));

            if (this.nonOptimizableSections.contains(sectionPos)) {
                this.cachedLightData.setArray(sectionPos, this.createTrivialVanillaLightmap(DIRECT_SKYLIGHT_MAP));
                this.dirtyCachedSections.add(sectionPos);
            }
        }

        this.cachedLightData.invalidateCaches();

        return minY;
    }

    @Shadow
    private volatile boolean hasPendingUpdates;

    /**
     * @author PhiPro
     * @reason Re-implement completely
     */
    @Overwrite
    private void updateHasPendingUpdates() {
        this.hasPendingUpdates = !this.initSkylightChunks.isEmpty();
    }

    @Unique
    private static final NibbleArray DIRECT_SKYLIGHT_MAP = createDirectSkyLightMap();

    @Unique
    private final Long2IntMap vanillaLightmapComplexities = new Long2IntOpenHashMap();
    @Unique
    private final LongSet removedLightmaps = new LongOpenHashSet();

    @Unique
    private static NibbleArray createDirectSkyLightMap() {
        final NibbleArray lightmap = new NibbleArray();
        Arrays.fill(lightmap.getData(), (byte) -1);

        return lightmap;
    }

    @Override
    public boolean hasSection(final long sectionPos) {
        return super.hasSection(sectionPos) && this.getArray(sectionPos, true) != null;
    }

    // Queued lightmaps are only added to the world via updateLightmaps()
    @Redirect(
        method = "getOrCreateArray(J)Lnet/minecraft/world/chunk/NibbleArray;",
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lnet/minecraft/world/lighting/SkyLightStorage;newArrays:Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;",
                opcode = Opcodes.GETFIELD
            )
        ),
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;",
            ordinal = 0,
            remap = false
        )
    )
    private Object cancelLightmapLookupFromQueue(final Long2ObjectMap<NibbleArray> lightmapArray, final long pos) {
        return null;
    }

    @Unique
    private static int getComplexityChange(final int val, final int oldNeighborVal, final int newNeighborVal) {
        return Math.abs(newNeighborVal - val) - Math.abs(oldNeighborVal - val);
    }

    @Override
    protected void beforeLightChange(final long blockPos, final int oldVal, final int newVal, final NibbleArray lightmap) {
        final long sectionPos = SectionPos.worldToSection(blockPos);

        if (SectionPos.mask(BlockPos.unpackY(blockPos)) == 0) {
            this.vanillaLightmapComplexities.put(sectionPos, this.vanillaLightmapComplexities.get(sectionPos) + newVal - oldVal);

            final long sectionPosBelow = this.getSectionBelow(sectionPos);

            if (sectionPosBelow != Long.MAX_VALUE) {
                final NibbleArray lightmapBelow = this.getOrAddLightmap(sectionPosBelow);

                final int x = SectionPos.mask(BlockPos.unpackX(blockPos));
                final int z = SectionPos.mask(BlockPos.unpackZ(blockPos));

                this.changeLightmapComplexity(sectionPosBelow, getComplexityChange(lightmapBelow.get(x, 15, z), oldVal, newVal));
            }
        }

        // Vanilla lightmaps need to be re-parented as they otherwise leak a reference to the old lightmap

        if (this.dirtyCachedSections.add(sectionPos)) {
            this.cachedLightData.copyArray(sectionPos);
            this.updateVanillaLightmapsBelow(sectionPos, this.getArray(sectionPos, true), false);
        }
    }

    @Shadow
    protected abstract boolean isAboveBottom(final int sectionY);

    /**
     * Returns the first section below the provided <code>sectionPos</code> that {@link #hasSection(long) supports light propagations} or {@link Long#MAX_VALUE} if no such section exists.
     */
    @Unique
    private long getSectionBelow(long sectionPos) {
        for (int y = SectionPos.extractY(sectionPos); this.isAboveBottom(y); --y) {
            if (this.hasSection(sectionPos = SectionPos.withOffset(sectionPos, Direction.DOWN))) {
                return sectionPos;
            }
        }

        return Long.MAX_VALUE;
    }

    @Override
    protected int getLightmapComplexityChange(final long blockPos, final int oldVal, final int newVal, final NibbleArray lightmap) {
        final long sectionPos = SectionPos.worldToSection(blockPos);
        final int x = SectionPos.mask(BlockPos.unpackX(blockPos));
        final int y = SectionPos.mask(BlockPos.unpackY(blockPos));
        final int z = SectionPos.mask(BlockPos.unpackZ(blockPos));

        final int valAbove;

        if (y < 15) {
            valAbove = lightmap.get(x, y + 1, z);
        } else {
            final NibbleArray lightmapAbove = this.getLightmapAbove(sectionPos);
            valAbove = lightmapAbove == null ? this.getDirectSkylight(sectionPos) : lightmapAbove.get(x, 0, z);
        }

        int amount = getComplexityChange(valAbove, oldVal, newVal);

        if (y > 0) {
            amount += getComplexityChange(lightmap.get(x, y - 1, z), oldVal, newVal);
        }

        return amount;
    }

    /**
     * Returns the first lightmap above the provided <code>sectionPos</code> or <code>null</code> if none exists.
     */
    @Unique
    private NibbleArray getLightmapAbove(long sectionPos) {
        final long sectionPosAbove = this.getSectionAbove(sectionPos);

        return sectionPosAbove == Long.MAX_VALUE ? null : this.getArray(sectionPosAbove, true);
    }

    /**
     * Returns the first section above the provided <code>sectionPos</code> that {@link #hasLightmap(long)}  has a lightmap} or {@link Long#MAX_VALUE} if none exists.
     */
    @Unique
    private long getSectionAbove(long sectionPos) {
        sectionPos = SectionPos.withOffset(sectionPos, Direction.UP);

        if (this.isAboveWorld(sectionPos)) {
            return Long.MAX_VALUE;
        }

        while (!this.hasLightmap(sectionPos)) {
            sectionPos = SectionPos.withOffset(sectionPos, Direction.UP);
        }

        return sectionPos;
    }

    @Unique
    private int getDirectSkylight(final long sectionPos) {
        return this.isSectionEnabled(sectionPos) ? 15 : 0;
    }

    @Override
    protected void beforeLightmapChange(final long sectionPos, final NibbleArray oldLightmap, final NibbleArray newLightmap) {
        final long sectionPosBelow = this.getSectionBelow(sectionPos);

        if (sectionPosBelow != Long.MAX_VALUE) {
            final NibbleArray lightmapBelow = this.getArray(sectionPosBelow, true);
            final NibbleArray lightmapAbove = oldLightmap == null ? this.getLightmapAbove(sectionPos) : oldLightmap;

            final int skyLight = this.getDirectSkylight(sectionPos);

            if (lightmapBelow == null) {
                int complexity = 0;

                for (int z = 0; z < 16; ++z) {
                    for (int x = 0; x < 16; ++x) {
                        complexity += Math.abs(newLightmap.get(x, 0, z) - (lightmapAbove == null ? skyLight : lightmapAbove.get(x, 0, z)));
                    }
                }

                if (complexity != 0) {
                    this.getOrAddLightmap(sectionPosBelow);
                    this.setLightmapComplexity(sectionPosBelow, complexity);
                }
            } else {
                int amount = 0;

                for (int z = 0; z < 16; ++z) {
                    for (int x = 0; x < 16; ++x) {
                        amount += getComplexityChange(lightmapBelow.get(x, 15, z), lightmapAbove == null ? skyLight : lightmapAbove.get(x, 0, z), newLightmap.get(x, 0, z));
                    }
                }

                this.changeLightmapComplexity(sectionPosBelow, amount);
            }
        }

        // Vanilla lightmaps need to be re-parented as they otherwise leak a reference to the old lightmap

        this.updateVanillaLightmapsOnLightmapCreation(sectionPos, newLightmap);
    }

    @Override
    protected int getInitialLightmapComplexity(final long sectionPos, final NibbleArray lightmap) {
        int complexity = 0;

        for (int y = 0; y < 15; ++y) {
            for (int z = 0; z < 16; ++z) {
                for (int x = 0; x < 16; ++x) {
                    complexity += Math.abs(lightmap.get(x, y + 1, z) - lightmap.get(x, y, z));
                }
            }
        }

        final NibbleArray lightmapAbove = this.getLightmapAbove(sectionPos);
        final int skyLight = this.getDirectSkylight(sectionPos);

        for (int z = 0; z < 16; ++z) {
            for (int x = 0; x < 16; ++x) {
                complexity += Math.abs((lightmapAbove == null ? skyLight : lightmapAbove.get(x, 0, z)) - lightmap.get(x, 15, z));
            }
        }

        return complexity;
    }

    @Redirect(
        method = "removeSection(J)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/lighting/SkyLightStorage;hasSection(J)Z"
        )
    )
    private boolean hasActualLightmap(final SkyLightStorage lightStorage, long sectionPos) {
        return this.hasLightmap(sectionPos);
    }

    @Override
    public void setLevel(final long id, final int level) {
        final int oldLevel = this.getLevel(id);

        if (oldLevel >= 2 && level < 2) {
            ((SkyLightStorageDataAccess) (Object) this.cachedLightData).updateMinHeight(SectionPos.extractY(id));
        }

        super.setLevel(id, level);
    }

    @Override
    protected NibbleArray createInitialVanillaLightmap(final long sectionPos) {
        // Attempt to restore data stripped from vanilla saves. See MC-198987

        if (!this.activeLightSections.contains(sectionPos) && !this.activeLightSections.contains(SectionPos.withOffset(sectionPos, Direction.UP))) {
            return this.createTrivialVanillaLightmap(sectionPos);
        }

        // A lightmap should have been present in this case unless it was stripped from the vanilla save or the chunk is loaded for the first time.
        // In both cases the lightmap should be initialized with zero.

        final long sectionPosAbove = this.getSectionAbove(sectionPos);
        final int complexity;

        if (sectionPosAbove == Long.MAX_VALUE) {
            complexity = this.isSectionEnabled(sectionPos) ? 15 * 16 * 16 : 0;
        } else {
            complexity = this.vanillaLightmapComplexities.get(sectionPosAbove);
        }

        if (complexity == 0) {
            return this.createTrivialVanillaLightmap(null);
        }

        // Need to create an actual lightmap in this case as it is non-trivial

        final NibbleArray lightmap = new NibbleArray(new byte[2048]);
        this.cachedLightData.setArray(sectionPos, lightmap);
        this.cachedLightData.invalidateCaches();

        this.addSection(sectionPos);
        this.setLightmapComplexity(sectionPos, complexity);

        return lightmap;
    }

    @Override
    protected NibbleArray createTrivialVanillaLightmap(final long sectionPos) {
        final long sectionPosAbove = this.getSectionAbove(sectionPos);

        if (sectionPosAbove == Long.MAX_VALUE) {
            return this.createTrivialVanillaLightmap(this.isSectionEnabled(sectionPos) ? DIRECT_SKYLIGHT_MAP : null);
        }

        return this.createTrivialVanillaLightmap(this.vanillaLightmapComplexities.get(sectionPosAbove) == 0 ? null : this.getArray(sectionPosAbove, true));
    }

    @Unique
    private NibbleArray createTrivialVanillaLightmap(final NibbleArray lightmapAbove) {
        return lightmapAbove == null ? new EmptyChunkNibbleArray() : new SkyLightChunkNibbleArray(lightmapAbove);
    }

    @Inject(
        method = "addSection(J)V",
        at = @At("HEAD")
    )
    private void updateVanillaLightmapsOnLightmapCreation(final long sectionPos, final CallbackInfo ci) {
        this.updateVanillaLightmapsOnLightmapCreation(sectionPos, this.getArray(sectionPos, true));
    }

    @Unique
    private void updateVanillaLightmapsOnLightmapCreation(final long sectionPos, final NibbleArray lightmap) {
        int complexity = 0;

        for (int z = 0; z < 16; ++z) {
            for (int x = 0; x < 16; ++x) {
                complexity += lightmap.get(x, 0, z);
            }
        }

        this.vanillaLightmapComplexities.put(sectionPos, complexity);
        this.removedLightmaps.remove(sectionPos);

        // Enabling the chunk already creates all relevant vanilla lightmaps

        if (!this.enabledChunks.contains(SectionPos.toSectionColumnPos(sectionPos))) {
            return;
        }

        // Vanilla lightmaps need to be re-parented immediately as the old parent can now be modified without informing them

        this.updateVanillaLightmapsBelow(sectionPos, complexity == 0 ? null : lightmap, false);
    }

    @Inject(
        method = "removeSection(J)V",
        at = @At("HEAD")
    )
    private void updateVanillaLightmapsOnLightmapRemoval(final long sectionPos, final CallbackInfo ci) {
        this.vanillaLightmapComplexities.remove(sectionPos);

        if (!this.enabledChunks.contains(SectionPos.toSectionColumnPos(sectionPos))) {
            return;
        }

        // Re-parenting can be deferred as the removed parent is now unmodifiable

        this.removedLightmaps.add(sectionPos);
    }

    @Unique
    private void updateVanillaLightmapsBelow(final long sectionPos, final NibbleArray lightmapAbove, final boolean stopOnRemovedLightmap) {
        for (int y = SectionPos.extractY(sectionPos) - 1; this.isAboveBottom(y); --y) {
            final long sectionPosBelow = SectionPos.asLong(SectionPos.extractX(sectionPos), y, SectionPos.extractZ(sectionPos));

            if (stopOnRemovedLightmap) {
                if (this.removedLightmaps.contains(sectionPosBelow)) {
                    break;
                }
            } else {
                this.removedLightmaps.remove(sectionPosBelow);
            }

            final NibbleArray lightmapBelow = this.getArray(sectionPosBelow, true);

            if (lightmapBelow == null) {
                continue;
            }

            if (!((IReadonly) lightmapBelow).isReadonly()) {
                break;
            }

            this.cachedLightData.setArray(sectionPosBelow, this.createTrivialVanillaLightmap(lightmapAbove));
            this.dirtyCachedSections.add(sectionPosBelow);
        }

        this.cachedLightData.invalidateCaches();
    }
}
