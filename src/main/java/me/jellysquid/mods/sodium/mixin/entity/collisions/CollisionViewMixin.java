package me.jellysquid.mods.sodium.mixin.entity.collisions;

import me.jellysquid.mods.lithium.common.entity.LithiumEntityCollisions;
import me.jellysquid.mods.lithium.common.entity.movement.BlockCollisionPredicate;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.ICollisionReader;
import net.minecraft.world.IEntityReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Replaces collision testing methods with jumps to our own (faster) entity collision testing code.
 */
@Mixin(ICollisionReader.class)
public interface CollisionViewMixin {
    /**
     * @reason Use a faster implementation
     * @author JellySquid
     */
    @Overwrite
    default Stream<VoxelShape> getBlockCollisionShapes(final Entity entity, AxisAlignedBB box) {
        return LithiumEntityCollisions.getBlockCollisions((ICollisionReader) this, entity, box, BlockCollisionPredicate.ANY);
    }

    /**
     * @reason Avoid usage of streams
     * @author JellySquid
     */
    @Overwrite
    default boolean hasNoCollisions(Entity entity, AxisAlignedBB box, Predicate<Entity> predicate) {
        boolean ret = !LithiumEntityCollisions.doesBoxCollideWithBlocks((ICollisionReader) this, entity, box, BlockCollisionPredicate.ANY);

        // If no blocks were collided with, try to check for entity collisions if we can read entities
        if (ret && this instanceof IEntityReader) {
            ret = !LithiumEntityCollisions.doesBoxCollideWithEntities((IEntityReader) this, entity, box, predicate);
        }

        return ret;
    }
}