package me.jellysquid.mods.sodium.mixin.features.entity.fast_render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.model.ModelCuboidAccessor;
import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.formats.quad.QuadVertexSink;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.math.Matrix3fExtended;
import me.jellysquid.mods.sodium.client.util.math.Matrix4fExtended;
import me.jellysquid.mods.sodium.client.util.math.MatrixUtil;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.math.vector.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ModelRenderer.class)
public class MixinModelPart {
    private static final float NORM = 1.0F / 16.0F;

    @Shadow
    @Final
    private ObjectList<ModelRenderer.ModelBox> cubeList;

    /**
     * @author JellySquid
     * @reason Use optimized vertex writer, avoid allocations, use quick matrix transformations
     */
    @Overwrite
    private void doRender(MatrixStack.Entry matrices, IVertexBuilder vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        Matrix3fExtended normalExt = MatrixUtil.getExtendedMatrix(matrices.getNormal());
        Matrix4fExtended modelExt = MatrixUtil.getExtendedMatrix(matrices.getMatrix());

        QuadVertexSink drain = VertexDrain.of(vertexConsumer).createSink(VanillaVertexTypes.QUADS);
        drain.ensureCapacity(this.cubeList.size() * 6 * 4);

        int color = ColorABGR.pack(red, green, blue, alpha);

        for (ModelRenderer.ModelBox cuboid : this.cubeList) {
            for (ModelRenderer.TexturedQuad quad : ((ModelCuboidAccessor) cuboid).getQuads()) {
                float normX = normalExt.transformVecX(quad.normal);
                float normY = normalExt.transformVecY(quad.normal);
                float normZ = normalExt.transformVecZ(quad.normal);

                int norm = Norm3b.pack(normX, normY, normZ);

                for (ModelRenderer.PositionTextureVertex vertex : quad.vertexPositions) {
                    Vector3f pos = vertex.position;

                    float x1 = pos.getX() * NORM;
                    float y1 = pos.getY() * NORM;
                    float z1 = pos.getZ() * NORM;

                    float x2 = modelExt.transformVecX(x1, y1, z1);
                    float y2 = modelExt.transformVecY(x1, y1, z1);
                    float z2 = modelExt.transformVecZ(x1, y1, z1);

                    drain.writeQuad(x2, y2, z2, color, vertex.textureU, vertex.textureV, light, overlay, norm);
                }
            }
        }

        drain.flush();
    }
}
