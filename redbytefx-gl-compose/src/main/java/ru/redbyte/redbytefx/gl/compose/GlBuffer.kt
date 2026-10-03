package ru.redbyte.redbytefx.gl.compose

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

internal object GlBuffer {
    private val vec2UploadScratch: ThreadLocal<FloatBuffer> = ThreadLocal.withInitial {
        ByteBuffer.allocateDirect(6 * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }

    fun uploadVec2(vertices: FloatArray): Int {
        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, ids[0])
        val data = scratch(vertices.size)
        data.put(vertices).position(0)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            vertices.size * Float.SIZE_BYTES,
            data,
            GLES30.GL_STATIC_DRAW,
        )
        return ids[0]
    }

    fun replaceVec2(current: Int, vertices: FloatArray): Int {
        deleteBuffer(current)
        return uploadVec2(vertices)
    }

    fun uploadDynamic(buffer: Int, vertices: FloatArray, scratch: FloatBuffer): Int {
        if (buffer == 0) return uploadVec2(vertices)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffer)
        scratch.clear()
        if (scratch.capacity() < vertices.size) {
            val created = ByteBuffer.allocateDirect(vertices.size * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            created.put(vertices).position(0)
            GLES30.glBufferData(
                GLES30.GL_ARRAY_BUFFER,
                vertices.size * Float.SIZE_BYTES,
                created,
                GLES30.GL_DYNAMIC_DRAW,
            )
            return buffer
        }
        scratch.put(vertices).position(0)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            vertices.size * Float.SIZE_BYTES,
            scratch,
            GLES30.GL_DYNAMIC_DRAW,
        )
        return buffer
    }

    fun drawVec2(buffer: Int, attrib: Int, count: Int) {
        if (attrib < 0 || buffer == 0) return
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffer)
        GLES30.glEnableVertexAttribArray(attrib)
        GLES30.glVertexAttribPointer(attrib, 2, GLES30.GL_FLOAT, false, Float.SIZE_BYTES * 2, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, count)
    }

    fun attribLocation(name: String): Int {
        val programSlot = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_CURRENT_PROGRAM, programSlot, 0)
        return GLES30.glGetAttribLocation(programSlot[0], name)
    }

    fun deleteBuffer(id: Int) {
        if (id == 0) return
        GLES30.glDeleteBuffers(1, intArrayOf(id), 0)
    }

    private fun scratch(count: Int): FloatBuffer {
        val existing = vec2UploadScratch.get()
        if (existing != null && existing.capacity() >= count) {
            existing.clear()
            return existing
        }
        val created = ByteBuffer.allocateDirect(count * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vec2UploadScratch.set(created)
        return created
    }
}
