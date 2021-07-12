package me.jellysquid.mods.sodium.mixin.features.entity.smooth_lighting;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.model.light.EntityLighter;
import me.jellysquid.mods.sodium.client.render.entity.EntityLightSampler;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.entity.item.PaintingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PaintingRenderer.class)
public abstract class MixinPaintingEntityRenderer extends EntityRenderer<PaintingEntity> implements EntityLightSampler<PaintingEntity> {
    private PaintingEntity entity;
    private float tickDelta;

    protected MixinPaintingEntityRenderer(EntityRendererManager dispatcher) {
        super(dispatcher);
    }

    @Inject(method = "render", at = @At(value = "HEAD"))
    public void preRender(PaintingEntity entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, CallbackInfo ci) {
        this.entity = entityIn;
        this.tickDelta = partialTicks;
    }

    /**
     * @author FlashyReese
     * @reason Redirect Lightmap coord with Sodium's EntityLighter.
     */
    @Redirect(method = "func_229122_a_", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WorldRenderer;getCombinedLight(Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/util/math/BlockPos;)I"))
    public int redirectLightmapCoord(IBlockDisplayReader lightReaderIn, BlockPos blockPosIn) {
        if (SodiumClientMod.options().quality.smoothLighting == SodiumGameOptions.LightingQuality.HIGH) {
            return EntityLighter.getBlendedLight(this, this.entity, tickDelta);
        } else {
            return WorldRenderer.getCombinedLight(lightReaderIn, blockPosIn);
        }
    }

    @Override
    public int bridge$getBlockLight(PaintingEntity entity, BlockPos pos) {
        return this.getBlockLight(entity, pos);
    }

    @Override
    public int bridge$getSkyLight(PaintingEntity entity, BlockPos pos) {
        return this.getSkyLight(entity, pos);
    }
}