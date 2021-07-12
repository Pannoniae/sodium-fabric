package me.jellysquid.mods.sodium.client.render.chunk.compile;

import com.google.common.primitives.Floats;
import it.unimi.dsi.fastutil.ints.IntArrays;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.BitSet;

public class ChunkBufferSorter {

    public static void sortStandardFormat(ChunkVertexType vertexType, ByteBuffer buffer, int bufferLen, float x, float y, float z) {
        x *= vertexType.getModelScale();
        y *= vertexType.getModelScale();
        z *= vertexType.getModelScale();
        FloatBuffer floatBuffer = buffer.asFloatBuffer();

        // Quad stride by Float size
        int quadStride = vertexType.getBufferVertexFormat().getStride();

        int quadStart = buffer.position();
        int quadCount = bufferLen/quadStride/4;
        int vertexSizeInteger = quadStride / 4;

        float[] distanceArray = new float[quadCount];
        int[] indicesArray = new int[quadCount];

        for (int quadIdx = 0; quadIdx < quadCount; ++quadIdx) {
            distanceArray[quadIdx] = getDistanceSq(floatBuffer, x, y, z, vertexSizeInteger, quadStart + (quadIdx * quadStride));
            indicesArray[quadIdx] = quadIdx;
        }

        IntArrays.mergeSort(indicesArray, (a, b) -> Floats.compare(distanceArray[b], distanceArray[a]));

        BitSet bits = new BitSet();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer tmp = stack.mallocFloat(quadStride);

            for (int l = bits.nextClearBit(0); l < indicesArray.length; l = bits.nextClearBit(l + 1)) {
                int m = indicesArray[l];

                if (m != l) {
                    sliceQuad(floatBuffer, m, quadStride, quadStart);
                    tmp.clear();
                    tmp.put(floatBuffer);

                    int n = m;

                    for (int o = indicesArray[m]; n != l; o = indicesArray[o]) {
                        sliceQuad(floatBuffer, o, quadStride, quadStart);
                        FloatBuffer floatBuffer3 = floatBuffer.slice();

                        sliceQuad(floatBuffer, n, quadStride, quadStart);
                        floatBuffer.put(floatBuffer3);

                        bits.set(n);
                        n = o;
                    }

                    sliceQuad(floatBuffer, l, quadStride, quadStart);
                    tmp.flip();

                    floatBuffer.put(tmp);
                }

                bits.set(l);
            }
        }
    }

    private static void sliceQuad(FloatBuffer floatBuffer, int quadIdx, int quadStride, int quadStart) {
        int base = quadStart + (quadIdx * quadStride);

        floatBuffer.limit(base + quadStride);
        floatBuffer.position(base);
    }

    private static float getDistanceSq(FloatBuffer buffer, float xCenter, float yCenter, float zCenter, int stride, int start) {
        int vertexBase = start;
        float x1 = buffer.get(vertexBase);
        float y1 = buffer.get(vertexBase + 1);
        float z1 = buffer.get(vertexBase + 2);

        //System.out.println("camera: " + xCenter + "," + yCenter + "," + zCenter);
        //System.out.println("buffer1: " + x1 + "," + y1 + "," + z1);

        vertexBase += stride;
        float x2 = buffer.get(vertexBase);
        float y2 = buffer.get(vertexBase + 1);
        float z2 = buffer.get(vertexBase + 2);
        //System.out.println("buffer2: " + x2 + "," + y2 + "," + z2);

        vertexBase += stride;
        float x3 = buffer.get(vertexBase);
        float y3 = buffer.get(vertexBase + 1);
        float z3 = buffer.get(vertexBase + 2);
        //System.out.println("buffer3: " + x3 + "," + y3 + "," + z3);

        vertexBase += stride;
        float x4 = buffer.get(vertexBase);
        float y4 = buffer.get(vertexBase + 1);
        float z4 = buffer.get(vertexBase + 2);
        //System.out.println("buffer4: " + x4 + "," + y4 + "," + z4);

        float xDist = ((x1 + x2 + x3 + x4) * 0.25F) - xCenter;
        float yDist = ((y1 + y2 + y3 + y4) * 0.25F) - yCenter;
        float zDist = ((z1 + z2 + z3 + z4) * 0.25F) - zCenter;

        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }
}
