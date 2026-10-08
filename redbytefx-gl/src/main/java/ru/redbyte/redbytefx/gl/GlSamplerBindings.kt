package ru.redbyte.redbytefx.gl

import ru.redbyte.redbytefx.Shape
import ru.redbyte.redbytefx.Uniform
import java.util.IdentityHashMap

/**
 * Device. Sampler bindings of one program on a shared [GlTextureUnits].
 *
 * A texture upload binds a name on the active unit. [rebindDisturbed] puts each sampler's texture
 * back before a draw, so the upload does not change what that sampler samples.
 */
internal class GlSamplerBindings(
    private val device: GlDevice,
    private val textureUnits: GlTextureUnits,
    private val programId: () -> Int,
) {
    private val boundUnits = IdentityHashMap<Uniform<*>, Int>()
    private val textureIds = IdentityHashMap<Uniform<*>, Int>()
    private val samplerOrder = ArrayList<Uniform<*>>(4)

    fun samples(texture: Int): Boolean {
        for (bound in textureIds.values) {
            if (bound == texture) return true
        }
        return false
    }

    fun bind(uniform: Uniform<*>, texture: Int, cube: Boolean, location: Int): Boolean {
        val expected = if (cube) Shape.SamplerCube else Shape.Sampler2D
        require(uniform.shape == expected) { "GL sampler bind requires $expected" }
        if (location < 0) return false
        val unit = boundUnits[uniform] ?: assignUnit(uniform, location)
        val changed = !textureUnits.holds(unit, texture)
        if (changed) bindOnUnit(uniform, unit, texture)
        textureIds[uniform] = texture
        return changed
    }

    fun forget(texture: Int) {
        textureUnits.forget(texture)
        textureIds.values.removeAll { it == texture }
    }

    fun rebindDisturbed() {
        var index = 0
        while (index < samplerOrder.size) {
            val uniform = samplerOrder[index]
            val texture = textureIds[uniform]
            val unit = boundUnits[uniform]
            if (texture != null && unit != null && !textureUnits.holds(unit, texture)) {
                bindOnUnit(uniform, unit, texture)
            }
            index += 1
        }
    }

    fun clear() {
        var index = 0
        while (index < samplerOrder.size) {
            val unit = boundUnits[samplerOrder[index]]
            if (unit != null) textureUnits.release(unit)
            index += 1
        }
        boundUnits.clear()
        textureIds.clear()
        samplerOrder.clear()
    }

    private fun assignUnit(uniform: Uniform<*>, location: Int): Int {
        val limit = device.maxCombinedTextureImageUnits()
        val unit = textureUnits.take(limit)
        if (unit < 0) {
            reject(
                GlCode.TextureUnitLimit,
                "Texture unit is outside GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS $limit",
            )
        }
        try {
            device.useProgram(programId())
            device.uniform1i(location, unit)
        } catch (error: Throwable) {
            textureUnits.release(unit)
            throw error
        }
        boundUnits[uniform] = unit
        samplerOrder += uniform
        return unit
    }

    private fun bindOnUnit(uniform: Uniform<*>, unit: Int, texture: Int) {
        device.activeTexture(unit)
        if (uniform.shape == Shape.SamplerCube) {
            device.bindTextureCube(texture)
        } else {
            device.bindTexture2D(texture)
        }
        textureUnits.record(unit, texture)
    }
}
