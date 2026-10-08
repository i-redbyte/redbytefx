package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ComputeShaderTest {

    @Test
    fun oversizedStd430ArrayIsRejectedBeforePacking() {
        assertThrows(IllegalArgumentException::class.java) {
            shader(ShaderTarget.Gles31) {
                storageBlock("cells") { vec4Array("values", Int.MAX_VALUE) }
                compute(1) { }
            }
        }
        val program = shader(ShaderTarget.Gles31) {
            storageBlock("cells") { floatArray("values") }
            compute(1) { }
        }
        assertThrows(IllegalArgumentException::class.java) {
            requireNotNull(program.storageBlock).byteSize(Int.MAX_VALUE)
        }
    }

    @Test
    fun publishedStorageLayoutAndProgramListCannotBeChanged() {
        val program = shader(ShaderTarget.Gles31) {
            storageBlock("cells") {
                vec3("rgb")
                float("gain")
            }
            compute(1) { }
        }
        val block = requireNotNull(program.storageBlock)
        block.offsets[1] = 0
        assertEquals(12, block.offsets[1])
        assertThrows(UnsupportedOperationException::class.java) {
            (program.storageBlocks as MutableList<StorageBlock>).clear()
        }
        assertEquals(1, program.storageBlocks.size)
    }

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
        val program = shader(ShaderTarget.Gles31) {
            storageBlock("cells") {
                val value = vec4("value")
                compute(64) { value.store(value) }
            }
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
    fun graphicsStagesAreRejectedOnAComputeProgram() {
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

    }

    @Test
    fun twoStorageBlocksBindInDeclarationOrder() {
        lateinit var cells: StorageBlock
        lateinit var gain: StorageBlock
        val program = shader(ShaderTarget.Gles31) {
            cells = storageBlock("cells") { float("value") }
            gain = storageBlock("gain") { float("amount") }
            compute(8) { }
        }
        val source = program.computeSource()
        assertEquals(0, cells.binding)
        assertEquals(1, gain.binding)
        assertTrue(source.contains("layout(std430, binding = 0) buffer cells {"))
        assertTrue(source.contains("layout(std430, binding = 1) buffer gain {"))
        assertEquals("cells", program.storageBlock?.name)
        assertEquals(2, program.storageBlocks.size)

        assertThrows(IllegalArgumentException::class.java) {
            shader(ShaderTarget.Gles31) {
                storageBlock("cells") { float("value") }
                storageBlock("cells") { float("gain") }
                compute(8) { }
            }
        }
    }

    @Test
    fun computeIndexesAStd430ArrayByInvocationId() {
        val program = shader(ShaderTarget.Gles31) {
            storageBlock("grid") {
                val values = vec4Array("values", 4)
                compute(8, 2, 1) {
                    values[globalId.x].store(vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit))
                }
            }
        }
        val source = program.computeSource()
        assertTrue(source.contains("ivec3(gl_GlobalInvocationID)"))
        assertTrue(source.contains("highp vec4 values[4];"))
        assertTrue(source.contains("layout(local_size_x = 8, local_size_y = 2, local_size_z = 1) in;"))
        assertTrue(source.contains("b_grid.values[ivec3(gl_GlobalInvocationID).x]"))

        lateinit var colors: StorageBlock
        shader(ShaderTarget.Gles31) {
            colors = storageBlock("colors") { vec3Array("values", 2) }
            compute(1) { }
        }
        assertEquals(0, colors.offsets[0])
        assertEquals(32, colors.byteSize)
        val packed = packStd430(colors, floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f))
        assertEquals(32, packed.size)
        val rejected = assertThrows(IllegalArgumentException::class.java) {
            packStd430(colors, floatArrayOf(1f, 2f, 3f))
        }
        assertTrue(rejected.message!!.contains("colors"))
        assertTrue(rejected.message!!.contains("6"))
    }

    @Test
    fun packStd430PadsAVec3AndDerivesAnUnsizedTail() {
        lateinit var color: StorageBlock
        shader(ShaderTarget.Gles31) {
            color = storageBlock("color") { vec3Array("rgb", 1) }
            compute(1) { }
        }
        val packed = packStd430(color, floatArrayOf(1f, 2f, 3f))
        assertEquals(16, packed.size)
        val view = java.nio.ByteBuffer.wrap(packed).order(java.nio.ByteOrder.nativeOrder())
        assertEquals(1f, view.float, 0f)
        assertEquals(2f, view.float, 0f)
        assertEquals(3f, view.float, 0f)
        assertEquals(0f, view.float, 0f)

        lateinit var tail: StorageBlock
        val unsized = shader(ShaderTarget.Gles31) {
            tail = storageBlock("tail") {
                float("head")
                floatArray("rest")
            }
            compute(1) { }
        }
        assertTrue(unsized.computeSource().contains("highp float rest[];"))
        val tailBytes = packStd430(tail, floatArrayOf(1f, 2f, 3f))
        assertEquals(12, tailBytes.size)
        assertEquals(12, tail.byteSize(3))
        val hidden = assertThrows(IllegalStateException::class.java) { tail.byteSize }
        assertTrue(hidden.message!!.contains("tail"))
        assertTrue(hidden.message!!.contains("byteSize"))
        val notLast = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles31) {
                storageBlock("cells") {
                    floatArray("rest")
                    float("head")
                }
                compute(1) { }
            }
        }
        assertEquals(ProgramCode.UnsizedStorageNotLast, notLast.code)
    }

    @Test
    fun unpackStd430DropsVec3PaddingAndKeepsAnUnsizedTail() {
        lateinit var color: StorageBlock
        shader(ShaderTarget.Gles31) {
            color = storageBlock("color") { vec3Array("rgb", 2) }
            compute(1) { }
        }
        val packed = packStd430(color, floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f))
        val view = java.nio.ByteBuffer.wrap(packed).order(java.nio.ByteOrder.nativeOrder())
        val rgb = FloatArray(6)
        assertEquals(6, unpackStd430(color, view, rgb.size, rgb))
        assertEquals(1f, rgb[0], 0f)
        assertEquals(3f, rgb[2], 0f)
        assertEquals(4f, rgb[3], 0f)
        assertEquals(6f, rgb[5], 0f)

        lateinit var tail: StorageBlock
        shader(ShaderTarget.Gles31) {
            tail = storageBlock("tail") {
                float("head")
                floatArray("rest")
            }
            compute(1) { }
        }
        val tailBytes = packStd430(tail, floatArrayOf(8f, 9f, 10f))
        val tailView = java.nio.ByteBuffer.wrap(tailBytes).order(java.nio.ByteOrder.nativeOrder())
        val logical = FloatArray(3)
        assertEquals(3, unpackStd430(tail, tailView, logical.size, logical))
        assertEquals(8f, logical[0], 0f)
        assertEquals(10f, logical[2], 0f)
    }

    @Test
    fun sharedMemoryStaysInsideComputeAndUniformBlocksStayOffAgsl() {
        val shared = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Gles30) {
                vertex { glPosition(attributeVec4("position")) }
                fragment {
                    sharedFloat("cells", 4)
                    vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
                }
            }
        }
        assertEquals(AuthoringCode.SharedOutsideCompute, shared.code)

        val agsl = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Agsl) {
                uniformBlock("frame") { float("gain") }
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(AuthoringCode.UniformBlockOnAgsl, agsl.code)

        lateinit var gain: HighFloat
        val compute = shader(ShaderTarget.Gles31) {
            uniformBlock("frame") { gain = float("gain") }
            storageBlock("cells") {
                val value = float("value")
                compute(4) { value.store(gain) }
            }
        }
        assertTrue(compute.computeSource().contains("layout(std140, binding = 0) uniform frame {"))
        assertTrue(compute.computeSource().contains("highp float gain;"))
    }
}
