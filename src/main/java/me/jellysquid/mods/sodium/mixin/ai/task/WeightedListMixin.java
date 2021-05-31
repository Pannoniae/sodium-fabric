package me.jellysquid.mods.sodium.mixin.ai.task;

import me.jellysquid.mods.lithium.common.ai.WeightedListIterable;
import net.minecraft.util.WeightedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;

@Mixin(WeightedList.class)
public class WeightedListMixin<U> implements WeightedListIterable<U> {
    @Shadow
    @Final
    protected List<WeightedList.Entry<? extends U>> weightedEntries;

    @Override
    public Iterator<U> iterator() {
        return new WeightedListIterable.ListIterator<>(this.weightedEntries.iterator());
    }
}
