package me.jellysquid.mods.sodium.client.gl.shader;

import com.mojang.blaze3d.matrix.MatrixStack;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.jellysquid.mods.sodium.client.gl.GlObject;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttribute;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.function.IntConsumer;

/**
 * An OpenGL shader program.
 */
public abstract class GlProgram extends GlObject {
    private static final Logger LOGGER = LogManager.getLogger(GlProgram.class);

    private final ResourceLocation name;

    protected GlProgram(ResourceLocation name, int program) {
        this.name = name;
        this.setHandle(program);
    }

    public static Builder builder(ResourceLocation ResourceLocation) {
        return new Builder(ResourceLocation);
    }

    public void bind(MatrixStack matrixStack) {
        GL20.glUseProgram(this.handle());
    }

    public void unbind() {
        GL20.glUseProgram(0);
    }

    public ResourceLocation getName() {
        return this.name;
    }

    /**
     * Retrieves the index of the uniform with the given name.
     * @param name The name of the uniform to find the index of
     * @return The uniform's index
     * @throws NullPointerException If no uniform exists with the given name
     */
    public int getUniformLocation(String name) {
        int index = GL20.glGetUniformLocation(this.handle(), name);

        if (index < 0) {
            throw new NullPointerException("No uniform exists with name: " + name);
        }

        return index;
    }

    /**
     * Retrieves the index of the vertex attribute with the given name.
     * @param name The name of the attribute to find the index of
     * @return The attribute's index
     * @throws NullPointerException If no attribute exists with the given name
     */
    protected int getAttributeLocation(String name) {
        int index = GL20.glGetAttribLocation(this.handle(), name);

        if (index < 0) {
            throw new NullPointerException("No attribute exists with name: " + name);
        }

        return index;
    }

    public void delete() {
        unbind();
        GL20.glDeleteProgram(this.handle());

        this.invalidateHandle();
    }

    public static class Builder {
        private final ResourceLocation name;
        private final int program;
        private final IntArrayList shaderIds;

        public Builder(ResourceLocation name) {
            this.name = name;
            this.program = GL20.glCreateProgram();
            this.shaderIds = new IntArrayList();
        }

        public Builder attachShader(GlShader shader) {
            GL20.glAttachShader(this.program, shader.handle());
            shaderIds.add(shader.handle());
            return this;
        }

        /**
         * Links the attached shaders to this program and returns a user-defined container which wraps the shader
         * program. This container can, for example, provide methods for updating the specific uniforms of that shader
         * set.
         *
         * @param factory The factory which will create the shader program's container
         * @param <P> The type which should be instantiated with the new program's handle
         * @return An instantiated shader container as provided by the factory
         */
        public <P extends GlProgram> P build(ProgramFactory<P> factory) {
            GL20.glLinkProgram(this.program);

            String log = GL20.glGetProgramInfoLog(this.program);

            if (!log.isEmpty()) {
                LOGGER.warn("Program link log for " + this.name + ": " + log);
            }
            int result = GL20.glGetProgrami(this.program, GL20.GL_LINK_STATUS);
            GL20.glValidateProgram(this.program);
            result &= GL20.glGetProgrami(this.program, GL20.GL_VALIDATE_STATUS);

            if (result != GL11.GL_TRUE) {
                throw new RuntimeException("Shader program linking failed, see log for details");
            }

            shaderIds.forEach((IntConsumer) id -> GL20.glDetachShader(this.program, id));

            return factory.create(this.name, this.program);
        }

        public Builder bindAttribute(String name, GlVertexAttribute attribute) {
            GL20.glBindAttribLocation(this.program, attribute.getIndex(), name);

            return this;
        }
    }

    public interface ProgramFactory<P extends GlProgram> {
        P create(ResourceLocation name, int handle);
    }
}
