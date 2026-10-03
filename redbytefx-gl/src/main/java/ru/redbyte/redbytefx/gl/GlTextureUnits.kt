package ru.redbyte.redbytefx.gl

private const val UNKNOWN: Int = -1

/**
 * Device. Texture-unit state of one EGL context: which units are taken, which unit is active,
 * and which texture each unit holds.
 *
 * Units are context state, not program state. Every [GlProgramRuntime] that draws on one context
 * must share one instance. Otherwise two programs both start at unit 0, and one skips a bind
 * because its own cache is stale. A recreated context needs a new instance.
 */
public class GlTextureUnits {
    private var next = 0
    private var active = 0
    private var held = IntArray(0)

    internal fun take(limit: Int): Int {
        if (next >= limit) return UNKNOWN
        val unit = next
        next += 1
        if (unit >= held.size) {
            val grown = IntArray(maxOf(4, held.size * 2)) { UNKNOWN }
            held.copyInto(grown)
            held = grown
        }
        return unit
    }

    internal fun holds(unit: Int, texture: Int): Boolean = held[unit] == texture

    internal fun record(unit: Int, texture: Int) {
        active = unit
        held[unit] = texture
    }

    /** An upload bound some texture on the active unit, so its cached binding is unknown. */
    internal fun disturbActive() {
        if (active < held.size) held[active] = UNKNOWN
    }

    /** `glDeleteTextures` unbinds the name from every unit. */
    internal fun forget(texture: Int) {
        for (unit in held.indices) {
            if (held[unit] == texture) held[unit] = UNKNOWN
        }
    }
}
