package me.jellysquid.mods.sodium.client.gl.func;

import org.lwjgl.opengl.ARBMultiDrawIndirect;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.opengl.GLCapabilities;


public enum GlIndirectMultiDrawFunctions {
    CORE_46 {
        @Override
        public void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride) {
            GL43C.glMultiDrawArraysIndirect(mode, indirect, primcount, stride);
        }

        @Override
        public void glMultiDrawArraysIndirectCount(int mode, long indirect, long drawCount, int maxDrawCount, int stride) {
            GL46C.glMultiDrawArraysIndirectCount(mode, indirect, drawCount, maxDrawCount, stride);
        }
    },
    CORE {
        @Override
        public void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride) {
            GL43C.glMultiDrawArraysIndirect(mode, indirect, primcount, stride);
        }
    },
    ARB {
        @Override
        public void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride) {
            ARBMultiDrawIndirect.glMultiDrawArraysIndirect(mode, indirect, primcount, stride);
        }
    },
    UNSUPPORTED {
        @Override
        public void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride) {
            throw new UnsupportedOperationException();
        }
    };

    public static GlIndirectMultiDrawFunctions load(GLCapabilities capabilities) {
        if (capabilities.OpenGL46) {
            return CORE_46;
        } else if (capabilities.OpenGL43) {
            return CORE;
        } else if (capabilities.GL_ARB_multi_draw_indirect && capabilities.GL_ARB_draw_indirect) {
            return ARB;
        } else {
            return UNSUPPORTED;
        }
    }

    public abstract void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride);

    public void glMultiDrawArraysIndirectCount(int mode, long indirect, long drawCount, int maxDrawCount, int stride) {
        throw new UnsupportedOperationException("This is only supported by OpenGL 4.6");
    }
}
