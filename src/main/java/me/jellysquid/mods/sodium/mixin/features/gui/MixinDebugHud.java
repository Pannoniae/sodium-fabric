package me.jellysquid.mods.sodium.mixin.features.gui;

import com.google.common.base.Strings;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.overlay.DebugOverlayGui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DebugOverlayGui.class)
public abstract class MixinDebugHud {
    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    @Final
    private FontRenderer fontRenderer;

    private List<String> capturedList = null;

    @Redirect(method = { "renderDebugInfoLeft", "renderDebugInfoRight" }, at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
    private int preRenderText(List<String> list) {
        // Capture the list to be rendered later
        this.capturedList = list;

        return 0; // Prevent the rendering of any text
    }

    @Inject(method = "renderDebugInfoLeft", at = @At("RETURN"))
    public void renderLeftText(MatrixStack matrixStack, CallbackInfo ci) {
        this.renderCapturedText(matrixStack, false);
    }

    @Inject(method = "renderDebugInfoRight", at = @At("RETURN"))
    public void renderRightText(MatrixStack matrixStack, CallbackInfo ci) {
        this.renderCapturedText(matrixStack, true);
    }

    private void renderCapturedText(MatrixStack matrixStack, boolean right) {
        Validate.notNull(this.capturedList, "Failed to capture string list");

        this.renderBackdrop(matrixStack, this.capturedList, right);
        this.renderStrings(matrixStack, this.capturedList, right);

        this.capturedList = null;
    }

    private void renderStrings(MatrixStack matrixStack, List<String> list, boolean right) {
        IRenderTypeBuffer.Impl immediate = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());

        Matrix4f modelMatrix = matrixStack.getLast().getMatrix();

        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);

            if (!Strings.isNullOrEmpty(string)) {
                int height = 9;
                int width = this.fontRenderer.getStringWidth(string);

                float x1 = right ? this.mc.getMainWindow().getScaledWidth() - 2 - width : 2;
                float y1 = 2 + (height * i);

                this.fontRenderer.drawBidiString(string, x1, y1, 0xe0e0e0, false, modelMatrix, immediate,
                        false, 0, 15728880, this.fontRenderer.getBidiFlag());
            }
        }

        immediate.finish();
    }

    private void renderBackdrop(MatrixStack matrixStack, List<String> list, boolean right) {
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        int color = 0x90505050;

        float f = (float) (color >> 24 & 255) / 255.0F;
        float g = (float) (color >> 16 & 255) / 255.0F;
        float h = (float) (color >> 8 & 255) / 255.0F;
        float k = (float) (color & 255) / 255.0F;

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        Matrix4f matrix = matrixStack.getLast()
                .getMatrix();

        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);

            if (Strings.isNullOrEmpty(string)) {
                continue;
            }

            int height = 9;
            int width = this.fontRenderer.getStringWidth(string);

            int x = right ? this.mc.getMainWindow().getScaledWidth() - 2 - width : 2;
            int y = 2 + height * i;

            float x1 = x - 1;
            float y1 = y - 1;
            float x2 = x + width + 1;
            float y2 = y + height - 1;

            bufferBuilder.pos(matrix, x1, y2, 0.0F).color(g, h, k, f).endVertex();
            bufferBuilder.pos(matrix, x2, y2, 0.0F).color(g, h, k, f).endVertex();
            bufferBuilder.pos(matrix, x2, y1, 0.0F).color(g, h, k, f).endVertex();
            bufferBuilder.pos(matrix, x1, y1, 0.0F).color(g, h, k, f).endVertex();
        }

        bufferBuilder.finishDrawing();

        WorldVertexBufferUploader.draw(bufferBuilder);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
}
