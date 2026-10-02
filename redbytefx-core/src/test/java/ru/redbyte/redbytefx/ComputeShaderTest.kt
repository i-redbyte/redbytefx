package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ComputeShaderTest {

    @Test
    fun std430DoesNotRoundAScalarBlockUpToVec4() {
        lateinit var cells: StorageBlock
        shader(ShaderTarget.Gles31) {
            cells = storageBlock("cells") { float("value") }
            compute(1) { }
        }
        assertEquals(0, cells.offsets[0])
        assertEquals(4, cells.byteSize)

        lateinit var color: StorageBlock
        shader(ShaderTarget.Gles31) {
            color = storageBlock("color") {
                vec3("rgb")
                float("gain")
            }
            compute(1) { }
        }
        assertEquals(0, color.offsets[0])
        assertEquals(12, color.offsets[1])
        assertEquals(16, color.byteSize)
        assertThrows(IllegalArgumentException::class.java) {
            packStd430(cells, floatArrayOf())
        }
    }

    @Test
    fun computeSpellsAStorageWriteAndLeavesGraphicsStagesOut() {
        lateinit var value: Expr<Vec4<Flt<High>>>
        val program = shader(ShaderTarget.Gles31) {
            storageBlock("cells") {
                value = vec4("value")
            }
            compute(64) { value.store(value) }
        }
        val source = program.computeSource()
        assertTrue(source.contains("#version 310 es"))
        assertTrue(source.contains("layout(local_size_x = 64) in;"))
        assertTrue(source.contains("layout(std430, binding = 0) buffer cells {"))
        assertTrue(source.contains("highp vec4 value;"))
        assertTrue(source.contains("b_cells.value = b_cells.value;"))
        assertEquals("cells", program.storageBlock?.name)
        assertThrows(IllegalStateException::class.java) { program.vertexSource() }
        assertThrows(IllegalStateException::class.java) { program.fragmentSource() }
    }

    @Test
    fun graphicsStagesAndASecondBlockAreRejected() {
        val vertex = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Gles31) {
                vertex { glPosition(attributeVec4("position")) }
            }
        }
        assertEquals(AuthoringCode.VertexOnGles31, vertex.code)

        val fragment = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Gles31) {
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(AuthoringCode.FragmentOnGles31, fragment.code)

        val agsl = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Agsl) {
                compute(8) { }
            }
        }
        assertEquals(AuthoringCode.ComputeOutsideGles31, agsl.code)

        val missing = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles31) { }
        }
        assertEquals(ProgramCode.MissingCompute, missing.code)

        assertThrows(IllegalArgumentException::class.java) {
            shader(ShaderTarget.Gles31) {
                storageBlock("cells") { float("value") }
                storageBlock("other") { float("gain") }
                compute(8) { }
            }
        }
    }
}
