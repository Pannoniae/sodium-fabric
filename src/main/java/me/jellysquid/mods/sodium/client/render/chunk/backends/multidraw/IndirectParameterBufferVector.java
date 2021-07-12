package me.jellysquid.mods.sodium.client.render.chunk.backends.multidraw;

import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

public class IndirectParameterBufferVector extends StructBuffer {

    private static final int INT_SIZE = 4;

    private IntBuffer intBuffer;

    protected IndirectParameterBufferVector(int bytes) {
        super(bytes, INT_SIZE);
        this.intBuffer = this.buffer.asIntBuffer();
    }

    public static IndirectParameterBufferVector create(int capacity) {
        return new IndirectParameterBufferVector(capacity);
    }

    public void begin() {
        this.intBuffer.clear();
    }

    public void end() {
        this.intBuffer.flip();
    }

    public void pushBatchCount(ChunkDrawCallBatcher batch) {
        int drawCount = batch.getCount();

        if (this.buffer.remaining() < INT_SIZE) {
            this.growBuffer(INT_SIZE);
        }

        this.intBuffer.put(drawCount);
    }

    protected void growBuffer(int n) {
        this.buffer = MemoryUtil.memRealloc(this.buffer, Math.max(this.buffer.capacity() * 2, this.buffer.capacity() + n));
        this.intBuffer = this.buffer.asIntBuffer();
    }
}
