package me.jellysquid.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.function.Function;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkProgram extends GlProgram {
    // Uniform variable binding indexes
    private final int uModelViewProjectionMatrix;
    private final int uModelScale;
    private final int uTextureScale;
    private final int uBlockTex;
    private final int uLightTex;

    // The fog shader component used by this program in order to setup the appropriate GL state
    private final ChunkShaderFogComponent fogShader;

    protected ChunkProgram(RenderDevice owner, ResourceLocation name, int handle, Function<ChunkProgram, ChunkShaderFogComponent> fogShaderFunction) {
        super(owner, name, handle);

        this.uModelViewProjectionMatrix = this.getUniformLocation("u_ModelViewProjectionMatrix");
        this.uBlockTex = this.getUniformLocation("u_BlockTex");
        this.uLightTex = this.getUniformLocation("u_LightTex");
        this.uModelScale = this.getUniformLocation("u_ModelScale");
        this.uTextureScale = this.getUniformLocation("u_TextureScale");

        this.fogShader = fogShaderFunction.apply(this);
    }

    public void setup(MatrixStack matrixStack, float modelScale, float textureScale, Matrix4f projection) {
        GL20C.glUniform1i(this.uBlockTex, 0);
        GL20C.glUniform1i(this.uLightTex, 2);

        GL20C.glUniform3f(this.uModelScale, modelScale, modelScale, modelScale);
        GL20C.glUniform2f(this.uTextureScale, textureScale, textureScale);

        this.fogShader.setup();

        MatrixStack.Entry matrices = matrixStack.getLast();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer bufModelViewProjection = stack.mallocFloat(16);

            Matrix4f modelView = matrices.getMatrix().copy();
            modelView.multiplyBackward(projection);
            modelView.write(bufModelViewProjection);
            GL20.glUniformMatrix4fv(this.uModelViewProjectionMatrix, false, bufModelViewProjection);
            // If for some reason vanilla minecraft doesn't expose the projection matrix anymore, we can fetch it
            // if it was pushed onto the GL stack with below code
            /*

                try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                    GL20C.glUniformMatrix4fv(this.uModelViewProjectionMatrix, false,
                            GameRendererContext.getModelViewProjectionMatrix(matrixStack.peek(), memoryStack));
                }
             */
            }
    }
}
