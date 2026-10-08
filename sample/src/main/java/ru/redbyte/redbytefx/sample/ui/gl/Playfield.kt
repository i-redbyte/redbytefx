package ru.redbyte.redbytefx.sample.ui.gl

import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.gl.compose.GlAttrib
import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal class Playfield(
    val program: ShaderProgram,
    val mesh: GlMesh,
    val aspect: Uniform<Flt<High>>,
    val flash: Uniform<Flt<High>>,
)

internal fun playfield(clearR: Float, clearG: Float, clearB: Float): Playfield {
    lateinit var aspect: Uniform<Flt<High>>
    lateinit var flash: Uniform<Flt<High>>
    val program = shader(ShaderTarget.Gles30) {
        aspect = uniform("aspect", 1f)
        flash = uniform("flash", 0f)
        val paint = varyingVec3("paint")
        vertex {
            val position = attributeVec4("position")
            val tint = attributeVec4("tint")
            paint.set(vec3(tint.x, tint.y, tint.z))
            glPosition(vec4(position.x / aspect.expr, position.y, position.z * 0.18f.lit - 0.12f.lit, position.z))
        }
        fragment {
            val wash = vec3(flash.expr, flash.expr * 0.92f.lit, flash.expr * 0.75f.lit)
            val rgb = paint.expr + wash
            vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
        }
    }
    return Playfield(
        program,
        GlMesh(
            vertices = FloatArray(24),
            stride = 8,
            attribs = listOf(GlAttrib("a_position", 4, 0), GlAttrib("a_tint", 4, 4)),
            depth = true,
            clearR = clearR,
            clearG = clearG,
            clearB = clearB,
        ),
        aspect,
        flash,
    )
}

internal class WorldPen(
    camX: Float = 0f,
    camY: Float = 0f,
    camZ: Float = 0f,
    pitch: Float = 0f,
) {
    private var camX = camX
    private var camY = camY
    private var camZ = camZ
    private var pitchCos = cos(pitch)
    private var pitchSin = sin(pitch)
    private var data = FloatArray(4096)
    private var written = 0
    private val tri = FloatArray(12)
    private var count = 0
    private var red = 1f
    private var green = 1f
    private var blue = 1f

    fun reset(camX: Float, camY: Float, camZ: Float, pitch: Float = 0f) {
        this.camX = camX
        this.camY = camY
        this.camZ = camZ
        pitchCos = cos(pitch)
        pitchSin = sin(pitch)
        written = 0
        count = 0
    }

    fun box(
        cx: Float,
        cy: Float,
        cz: Float,
        hx: Float,
        hy: Float,
        hz: Float,
        r: Float,
        g: Float,
        b: Float,
    ) {
        red = r
        green = g
        blue = b
        writeBox(cx, cy, cz, hx, hy, hz) { x, y, z, shade ->
            vertex(x, y, z, shade)
        }
    }

    fun sphere(cx: Float, cy: Float, cz: Float, radius: Float, r: Float, g: Float, b: Float) {
        red = r
        green = g
        blue = b
        val bands = 5
        val steps = 8
        for (band in 0 until bands) {
            val v0 = PI.toFloat() * band / bands
            val v1 = PI.toFloat() * (band + 1) / bands
            for (step in 0 until steps) {
                val u0 = PI.toFloat() * 2f * step / steps
                val u1 = PI.toFloat() * 2f * (step + 1) / steps
                triPoint(cx, cy, cz, radius, v0, u0)
                triPoint(cx, cy, cz, radius, v1, u0)
                triPoint(cx, cy, cz, radius, v1, u1)
                triPoint(cx, cy, cz, radius, v0, u0)
                triPoint(cx, cy, cz, radius, v1, u1)
                triPoint(cx, cy, cz, radius, v0, u1)
            }
        }
    }

    fun toArray(): FloatArray = data.copyOf(written)

    private fun triPoint(cx: Float, cy: Float, cz: Float, radius: Float, v: Float, u: Float) {
        val ring = sin(v)
        val shade = (0.45f + 0.55f * cos(v).coerceIn(0f, 1f))
        vertex(cx + radius * ring * cos(u), cy + radius * cos(v), cz + radius * ring * sin(u), shade)
    }

    private fun vertex(x: Float, y: Float, z: Float, shade: Float) {
        val slot = count * 4
        val dy = y - camY
        val dz = z - camZ
        tri[slot] = x - camX
        tri[slot + 1] = dy * pitchCos + dz * pitchSin
        tri[slot + 2] = dz * pitchCos - dy * pitchSin
        tri[slot + 3] = shade
        count += 1
        if (count < 3) return
        count = 0
        if (tri[2] < NEAR || tri[6] < NEAR || tri[10] < NEAR) return
        val required = written + 3 * 8
        if (required > data.size) data = data.copyOf(maxOf(required, data.size * 2))
        var index = 0
        while (index < 3) {
            val offset = index * 4
            val lit = tri[offset + 3]
            data[written++] = tri[offset]
            data[written++] = tri[offset + 1]
            data[written++] = tri[offset + 2]
            data[written++] = 1f
            data[written++] = red * lit
            data[written++] = green * lit
            data[written++] = blue * lit
            data[written++] = 1f
            index += 1
        }
    }

    private companion object {
        const val NEAR = 0.28f
    }
}

internal const val PLAYFIELD_DSL = """
shader(ShaderTarget.Gles30) {
    aspect = uniform("aspect", 1f)
    flash = uniform("flash", 0f)
    val paint = varyingVec3("paint")
    vertex {
        val position = attributeVec4("position")
        val tint = attributeVec4("tint")
        paint.set(vec3(tint.x, tint.y, tint.z))
        glPosition(vec4(position.x / aspect.expr, position.y, position.z * 0.18f.lit - 0.12f.lit, position.z))
    }
    fragment {
        val rgb = paint.expr + vec3(flash.expr, flash.expr * 0.92f.lit, flash.expr * 0.75f.lit)
        vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
    }
}
"""
