package ru.redbyte.redbytefx.scene

import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.Mat4
import ru.redbyte.redbytefx.Sampler2D
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.lambert
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

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
)

/**
 * Scene. A GLES program with attributes `position`, `normal`, and `uv`, plus `view` and
 * `projection` matrices. The fragment samples [LitMesh.albedo] and multiplies it by [ru.redbyte.redbytefx.lambert].
 * AGSL cannot compile it.
 */
public fun litTexturedMesh(): LitMesh {
    lateinit var view: Uniform<Mat4>
    lateinit var projection: Uniform<Mat4>
    lateinit var light: Uniform<Vec3<Flt<High>>>
    lateinit var albedo: Uniform<Sampler2D>
    val program = shader(ShaderTarget.Gles30) {
        view = uniformMat4("view")
        projection = uniformMat4("projection")
        light = uniformVec3("light", 0.3f, 0.8f, 0.4f)
        albedo = sampler2D("albedo")
        val uv = varyingVec2("uv")
        val facing = varyingVec3("normal")
        vertex {
            val position = attributeVec3("position")
            val normal = attributeVec3("normal")
            val texcoord = attributeVec2("uv")
            uv.set(texcoord)
            facing.set(normal)
            val clip = projection.expr * (view.expr * vec4(position.x, position.y, position.z, 1f.lit))
            glPosition(clip)
        }
        fragment {
            val shade = lambert(facing.expr, light.expr)
            val texel = texture(albedo, uv.expr)
            vec4(texel.x * shade, texel.y * shade, texel.z * shade, 1f.lit)
        }
    }
    return LitMesh(program, view, projection, light, albedo)
}
