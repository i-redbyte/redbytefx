package ru.redbyte.redbytefx.gl

import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.StorageBlock
import ru.redbyte.redbytefx.UniformBlock
import java.util.IdentityHashMap

/**
 * Uniform and storage buffers owned by one [GlProgramRuntime].
 *
 * Binding points follow declaration order. A block from another shader is rejected.
 */
internal class GlOwnedBlocks(
    private val device: GlDevice,
    private val program: ShaderProgram,
) {
    private val uniformBuffers = IdentityHashMap<UniformBlock, GlBlockBuffer>()
    private val storageBuffers = IdentityHashMap<StorageBlock, GlBlockBuffer>()

    fun rejectExcess() {
        if (program.uniformBlocks.isNotEmpty()) {
            val uniformLimit = device.maxUniformBufferBindings()
            for (block in program.uniformBlocks) {
                if (block.binding >= uniformLimit) {
                    reject(
                        GlCode.BlockBindingLimit,
                        "Uniform block \"${block.name}\" needs binding ${block.binding}, the context allows $uniformLimit",
                    )
                }
            }
        }
        if (program.storageBlocks.isNotEmpty()) {
            val storageLimit = device.maxShaderStorageBufferBindings()
            for (block in program.storageBlocks) {
                if (block.binding >= storageLimit) {
                    reject(
                        GlCode.BlockBindingLimit,
                        "Storage block \"${block.name}\" needs binding ${block.binding}, the context allows $storageLimit",
                    )
                }
            }
        }
    }

    fun uniform(block: UniformBlock): GlBlockBuffer {
        require(program.uniformBlocks.any { it === block }) { "Uniform block does not belong to this shader" }
        return uniformBuffers.getOrPut(block) {
            GlBlockBuffer(device, storage = false, binding = block.binding)
        }
    }

    fun storage(block: StorageBlock): GlBlockBuffer {
        require(program.storageBlocks.any { it === block }) { "Storage block does not belong to this shader" }
        return storageBuffers.getOrPut(block) {
            GlBlockBuffer(device, storage = true, binding = block.binding)
        }
    }

    fun bind() {
        uniformBuffers.values.forEach { it.bind() }
        storageBuffers.values.forEach { it.bind() }
    }

    fun delete() {
        uniformBuffers.values.forEach { it.delete() }
        uniformBuffers.clear()
        storageBuffers.values.forEach { it.delete() }
        storageBuffers.clear()
    }
}
