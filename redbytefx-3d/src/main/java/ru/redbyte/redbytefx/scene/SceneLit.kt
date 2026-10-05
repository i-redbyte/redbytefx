package ru.redbyte.redbytefx.scene

import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.Mat3
import ru.redbyte.redbytefx.Mat4
import ru.redbyte.redbytefx.Sampler2D
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.a
import ru.redbyte.redbytefx.lambert
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z
import kotlin.math.abs

/**
 * One textured triangle mesh lit by [ru.redbyte.redbytefx.lambert].
 * This module does not depend on stdlib. `agslSource()` has no text:
 * [program] is [ShaderTarget.Gles30].
 */
public class LitMesh(
    public val program: ShaderProgram,
    public val view: Uniform<Mat4>,
    public val projection: Uniform<Mat4>,
    public val light: Uniform<Vec3<Flt<High>>>,
    public val albedo: Uniform<Sampler2D>,
    public val model: Uniform<Mat4>,
    public val normalMatrix: Uniform<Mat3>,
)

/**
 * Scene. A GLES program with attributes `position`, `normal`, and `uv`, plus `view`,
 * `projection`, and [model] matrices. Vertices are multiplied by [model]. Normals are multiplied
 * by [normalMatrix], the upper 3×3 inverse-transpose of [model] computed on the CPU.
 * When [model] changes at runtime, upload both [model] and a fresh normal matrix from
 * [normalMatrix], or call [ru.redbyte.redbytefx.gl.compose.setLitModel] on GLES.
 * The default [model] is the identity, which matches a mesh drawn with no model.
 * The fragment samples [LitMesh.albedo] and multiplies it by [ru.redbyte.redbytefx.lambert].
 * AGSL cannot compile it.
 */
public fun litTexturedMesh(model: FloatArray = MODEL_IDENTITY): LitMesh {
    val normals = normalMatrix(model)
    lateinit var view: Uniform<Mat4>
    lateinit var projection: Uniform<Mat4>
    lateinit var modelUniform: Uniform<Mat4>
    lateinit var normalUniform: Uniform<Mat3>
    lateinit var light: Uniform<Vec3<Flt<High>>>
    lateinit var albedo: Uniform<Sampler2D>
    val program = shader(ShaderTarget.Gles30) {
        view = uniformMat4("view")
        projection = uniformMat4("projection")
        modelUniform = uniformMat4("model", model)
        normalUniform = uniformMat3("normalMatrix", normals)
        light = uniformVec3("light", 0.3f, 0.8f, 0.4f)
        albedo = sampler2D("albedo")
        val uv = varyingVec2("uv")
        val facing = varyingVec3("normal")
        vertex {
            val position = attributeVec3("position")
            val normal = attributeVec3("normal")
            val texcoord = attributeVec2("uv")
            uv.set(texcoord)
            val world = modelUniform.expr * vec4(position.x, position.y, position.z, 1f.lit)
            facing.set(normalUniform.expr * normal)
            val clip = projection.expr * (view.expr * world)
            glPosition(clip)
        }
        fragment {
            val shade = lambert(facing.expr, light.expr)
            val texel = texture(albedo, uv.expr)
            vec4(texel.x * shade, texel.y * shade, texel.z * shade, texel.a)
        }
    }
    return LitMesh(program, view, projection, light, albedo, modelUniform, normalUniform)
}

/**
 * Scene. Upper 3×3 inverse-transpose of a column-major [model], for transforming normals.
 * A non-uniform scale is inverted. The shader does not compute this inverse.
 * Invalid or singular input throws before [out] is touched.
 */
public fun normalMatrix(model: FloatArray, out: FloatArray = FloatArray(NORMAL_MATRIX_FLOATS)): FloatArray {
    require(model.size >= MATRIX_FLOATS) { "Model needs $MATRIX_FLOATS floats, was ${model.size}" }
    require(out.size >= NORMAL_MATRIX_FLOATS) { "Normal matrix needs $NORMAL_MATRIX_FLOATS floats, was ${out.size}" }
    require((0 until MATRIX_FLOATS).all { model[it].isFinite() }) { "Model values must be finite" }
    val a00 = model[0]
    val a01 = model[4]
    val a02 = model[8]
    val a10 = model[1]
    val a11 = model[5]
    val a12 = model[9]
    val a20 = model[2]
    val a21 = model[6]
    val a22 = model[10]
    val c00 = a11 * a22 - a12 * a21
    val c01 = -(a10 * a22 - a12 * a20)
    val c02 = a10 * a21 - a11 * a20
    val c10 = -(a01 * a22 - a02 * a21)
    val c11 = a00 * a22 - a02 * a20
    val c12 = -(a00 * a21 - a01 * a20)
    val c20 = a01 * a12 - a02 * a11
    val c21 = -(a00 * a12 - a02 * a10)
    val c22 = a00 * a11 - a01 * a10
    val det = a00 * c00 + a01 * c01 + a02 * c02
    require(det.isFinite() && abs(det) > NORMAL_DEGENERATE) { "Model has no inverse for normals" }
    val inv = 1f / det
    out[0] = c00 * inv
    out[1] = c10 * inv
    out[2] = c20 * inv
    out[3] = c01 * inv
    out[4] = c11 * inv
    out[5] = c21 * inv
    out[6] = c02 * inv
    out[7] = c12 * inv
    out[8] = c22 * inv
    return out
}

private const val NORMAL_MATRIX_FLOATS: Int = 9
private const val NORMAL_DEGENERATE: Float = 1.0e-8f

private val MODEL_IDENTITY = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f,
)
