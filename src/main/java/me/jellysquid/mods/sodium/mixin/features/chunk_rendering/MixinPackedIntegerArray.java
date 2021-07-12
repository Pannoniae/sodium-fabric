package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.client.world.cloned.PackedIntegerArrayExtended;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;
import net.minecraft.util.BitArray;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BitArray.class)
public class MixinPackedIntegerArray implements PackedIntegerArrayExtended {
    @Shadow
    @Final
    private long[] longArray;

    @Shadow
    @Final
    private int field_232982_f_;

    @Shadow
    @Final
    private long maxEntryValue;

    @Shadow
    @Final
    private int bitsPerEntry;

    @Shadow
    @Final
    private int arraySize;

    @Override
    public <T> void copyUsingPalette(T[] out, ClonedPalette<T> palette) {
        int idx = 0;

        for (long word : this.longArray) {
            long l = word;

            for (int j = 0; j < this.field_232982_f_; ++j) {
                out[idx] = palette.get((int) (l & this.maxEntryValue));
                l >>= this.bitsPerEntry;

                if (++idx >= this.arraySize) {
                    return;
                }
            }
        }
    }
}
