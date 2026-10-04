package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LayoutRoundtripTest {
    @Test
    fun std140AndStd430RoundTripLogicalFloats() {
        roundTripUniform("lone", floatArrayOf(1f, 2f, 3f), 16, 12) {
            vec3("rgb")
        }
        roundTripUniform("colors", floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f), 32, 12) {
            vec3Array("rgb", 2)
        }
        roundTripUniform(
            "basis",
            floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
            48,
            12,
        ) {
            mat3("basis")
        }

        roundTripStorage("lone", floatArrayOf(1f, 2f, 3f), 16, 12) {
            vec3("rgb")
        }
        roundTripStorage("colors", floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f), 32, 12) {
            vec3Array("rgb", 2)
        }
        roundTripStorage("tail", floatArrayOf(8f, 9f, 10f), 16, 12) {
            vec3Array("rest")
        }
    }
}

private fun roundTripUniform(
    name: String,
    values: FloatArray,
    byteSize: Int,
    padAt: Int,
    members: UniformBlockBuilder.() -> Unit,
) {
    lateinit var block: UniformBlock
    shader(ShaderTarget.Gles30) {
        block = uniformBlock(name, members)
        vertex { glPosition(attributeVec4("position")) }
        fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
    }
    val packed = packStd140(block, values)
    assertEquals(byteSize, packed.size)
    val logical = FloatArray(values.size)
    assertEquals(values.size, unpackStd140(block, ByteBuffer.wrap(packed), logical))
    values.forEachIndexed { index, value -> assertEquals(value, logical[index], 0f) }
    val view = ByteBuffer.wrap(packed).order(ByteOrder.nativeOrder())
    assertEquals(0f, view.getFloat(padAt), 0f)
}

private fun roundTripStorage(
    name: String,
    values: FloatArray,
    byteSize: Int,
    padAt: Int,
    members: StorageBlockBuilder.() -> Unit,
) {
    lateinit var block: StorageBlock
    shader(ShaderTarget.Gles31) {
        block = storageBlock(name, members)
        compute(1) { }
    }
    val packed = packStd430(block, values)
    assertEquals(byteSize, packed.size)
    val logical = FloatArray(values.size)
    assertEquals(values.size, unpackStd430(block, ByteBuffer.wrap(packed), values.size, logical))
    values.forEachIndexed { index, value -> assertEquals(value, logical[index], 0f) }
    val view = ByteBuffer.wrap(packed).order(ByteOrder.nativeOrder())
    assertEquals(0f, view.getFloat(padAt), 0f)
}
