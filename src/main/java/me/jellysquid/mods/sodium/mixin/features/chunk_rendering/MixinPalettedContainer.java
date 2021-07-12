package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.client.world.cloned.PalettedContainerExtended;
import net.minecraft.util.BitArray;
import net.minecraft.util.palette.IPalette;
import net.minecraft.util.palette.PalettedContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PalettedContainer.class)
public class MixinPalettedContainer<T> implements PalettedContainerExtended<T> {
    @Shadow
    private int bits;

    @Shadow
    protected BitArray storage;

    @Shadow
    private IPalette<T> palette;

    @Shadow
    @Final
    private T defaultState;

    @Override
    public BitArray getDataArray() {
        return this.storage;
    }

    @Override
    public IPalette<T> getPalette() {
        return this.palette;
    }

    @Override
    public T getDefaultValue() {
        return this.defaultState;
    }

    @Override
    public int getPaletteSize() {
        return this.bits;
    }
}
