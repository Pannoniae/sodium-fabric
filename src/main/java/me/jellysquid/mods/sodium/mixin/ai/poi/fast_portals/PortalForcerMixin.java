package me.jellysquid.mods.sodium.mixin.ai.poi.fast_portals;

import me.jellysquid.mods.lithium.common.world.interests.PointOfInterestDataExtended;
import net.minecraft.block.BlockState;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.TeleportationRepositioner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.village.PointOfInterestType;
import net.minecraft.world.Teleporter;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(Teleporter.class)
public class PortalForcerMixin {
    @Shadow
    @Final
    protected ServerWorld world;

    /**
     * @author JellySquid
     * @reason Use optimized search for nearby points, avoid slow filtering, check for valid locations first
     */
    @Overwrite
    public Optional<TeleportationRepositioner.Result> getExistingPortal(BlockPos centerPos, boolean shrink) {
        int searchRadius = shrink ? 16 : 128;

        PointOfInterestManager poiStorage = this.world.getPointOfInterestManager();
        poiStorage.ensureLoadedAndValid(this.world, centerPos, searchRadius);
        Optional<BlockPos> ret = ((PointOfInterestDataExtended) poiStorage).findNearestInSquare(centerPos, searchRadius,
                PointOfInterestType.NETHER_PORTAL, PointOfInterestManager.Status.ANY,
                (poi) -> this.world.getBlockState(poi.getPos()).hasProperty(BlockStateProperties.HORIZONTAL_AXIS)
        );

        return ret.flatMap((pos) -> {
            BlockState state = this.world.getBlockState(pos);

            this.world.getChunkProvider().registerTicket(TicketType.PORTAL, new ChunkPos(pos), 3, pos);

            return Optional.of(TeleportationRepositioner.findLargestRectangle(pos, state.get(BlockStateProperties.HORIZONTAL_AXIS), 21, Direction.Axis.Y, 21, (searchPos) ->
                    this.world.getBlockState(searchPos) == state));
        });
    }
}
