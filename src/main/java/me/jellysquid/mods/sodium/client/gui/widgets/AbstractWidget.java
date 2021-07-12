package me.jellysquid.mods.sodium.client.gui.widgets;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.SoundEvents;
import org.lwjgl.opengl.GL20C;

import java.util.function.Consumer;

public abstract class AbstractWidget implements IRenderable, IGuiEventListener {
    protected final FontRenderer font;

    protected AbstractWidget() {
        this.font = Minecraft.getInstance().fontRenderer;
    }

    protected void drawString(MatrixStack matrixStack, String str, int x, int y, int color) {
        this.font.drawString(matrixStack, str, x, y, color);
    }

    protected void drawRect(double x1, double y1, double x2, double y2, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        this.drawQuads(vertices -> addQuad(vertices, x1, y1, x2, y2, a, r, g, b));
    }

    protected void drawQuads(Consumer<IVertexBuilder> consumer) {
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(GL20C.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        consumer.accept(bufferBuilder);

        bufferBuilder.finishDrawing();

        WorldVertexBufferUploader.draw(bufferBuilder);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    protected static void addQuad(IVertexBuilder consumer, double x1, double y1, double x2, double y2, float a, float r, float g, float b) {
        consumer.pos(x2, y1, 0.0D).color(r, g, b, a).endVertex();
        consumer.pos(x1, y1, 0.0D).color(r, g, b, a).endVertex();
        consumer.pos(x1, y2, 0.0D).color(r, g, b, a).endVertex();
        consumer.pos(x2, y2, 0.0D).color(r, g, b, a).endVertex();
    }

    protected void playClickSound() {
        Minecraft.getInstance().getSoundHandler()
                .play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    protected int getStringWidth(String text) {
        return this.font.getStringWidth(text);
    }
}
