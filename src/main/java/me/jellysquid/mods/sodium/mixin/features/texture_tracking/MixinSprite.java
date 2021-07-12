package me.jellysquid.mods.sodium.mixin.features.texture_tracking;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.texture.SpriteExtended;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TextureAtlasSprite.class)
public abstract class MixinSprite implements SpriteExtended {
    private boolean forceNextUpdate;

    @Shadow
    private int tickCounter;

    @Shadow
    @Final
    private AnimationMetadataSection animationMetadata;

    @Shadow
    private int frameCounter;

    @Shadow
    public abstract int getFrameCount();

    @Shadow
    protected abstract void uploadFrames(int int_1);

    @Shadow
    @Final
    private TextureAtlasSprite.InterpolationData interpolationData;

    /**
     * @author JellySquid
     * @reason Allow conditional texture updating
     */
    @Overwrite
    public void updateAnimation() {
        this.tickCounter++;

        boolean onDemand = SodiumClientMod.options().advanced.animateOnlyVisibleTextures;

        // TODO: Update texture if this is for an item held in hand/inventory
        if (!onDemand || this.forceNextUpdate) {
            this.uploadTexture();
        }
    }

    private void uploadTexture() {
        if (this.tickCounter >= this.animationMetadata.getFrameTimeSingle(this.frameCounter)) {
            int prevFrameIndex = this.animationMetadata.getFrameIndex(this.frameCounter);
            int frameCount = this.animationMetadata.getFrameCount() == 0 ? this.getFrameCount() : this.animationMetadata.getFrameCount();

            this.frameCounter = (this.frameCounter + 1) % frameCount;
            this.tickCounter = 0;

            int frameIndex = this.animationMetadata.getFrameIndex(this.frameCounter);

            if (prevFrameIndex != frameIndex && frameIndex >= 0 && frameIndex < this.getFrameCount()) {
                this.uploadFrames(frameIndex);
            }
        } else if (this.interpolationData != null) {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(this::updateInterpolatedTexture);
            } else {
                this.updateInterpolatedTexture();
            }
        }

        this.forceNextUpdate = false;
    }

    @Override
    public void markActive() {
        this.forceNextUpdate = true;
    }

    private void updateInterpolatedTexture() {
        this.interpolationData.uploadInterpolated();
    }
}
