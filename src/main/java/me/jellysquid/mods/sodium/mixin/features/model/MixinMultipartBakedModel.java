package me.jellysquid.mods.sodium.mixin.features.model;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.MultipartBakedModel;
import net.minecraft.util.Direction;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;
import java.util.function.Predicate;

@Mixin(MultipartBakedModel.class)
public class MixinMultipartBakedModel {
    private final Map<BlockState, List<IBakedModel>> stateCacheFast = new Reference2ReferenceOpenHashMap<>();

    @Shadow
    @Final
    private List<Pair<Predicate<BlockState>, IBakedModel>> selectors;

    /**
     * @author JellySquid
     * @reason Avoid expensive allocations and replace bitfield indirection
     */
    @Overwrite(remap = false)
    public List<BakedQuad> getQuads(BlockState state, Direction face, Random random, net.minecraftforge.client.model.data.IModelData modelData) {
        if (state == null) {
            return Collections.emptyList();
        }

        List<IBakedModel> models;

        // FIXME: Synchronization-hack because getQuads must be thread-safe
        // Vanilla is actually affected by the exact same issue safety issue, but crashes seem rare in practice
        synchronized (this.stateCacheFast) {
            models = this.stateCacheFast.get(state);

            if (models == null) {
                models = new ArrayList<>(this.selectors.size());

                for (Pair<Predicate<BlockState>, IBakedModel> pair : this.selectors) {
                    if ((pair.getLeft()).test(state)) {
                        models.add(pair.getRight());
                    }
                }

                this.stateCacheFast.put(state, models);
            }
        }

        List<BakedQuad> list = new ArrayList<>();

        long seed = random.nextLong();

        for (IBakedModel model : models) {
            random.setSeed(seed);

            list.addAll(model.getQuads(state, face, random, modelData));
        }

        return list;
    }

}
