package ru.redbyte.redbytefx.scene

import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.math.tan

private const val DEGENERATE: Float = 1.0e-8f

/** Floats in one 4x4 matrix. */
public const val MATRIX_FLOATS: Int = 16

/**
 * Scene. Column-major identity. The array is shared: copy it with [identity] before writing.
 */
public val IDENTITY: FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f,
)

/**
 * Scene. Right-handed view matrix, Y up, column-major, the same order as
 * `glUniformMatrix4fv` with transpose false.
 * [eye] and [center] must differ. [up] must not be parallel to the view direction.
 * Either case throws [IllegalArgumentException] naming the arguments, before a NaN is written.
 * Allocates a new array; per-frame code should pass its own array to the other overload.
 */
public fun lookAt(
    eyeX: Float,
    eyeY: Float,
    eyeZ: Float,
    centerX: Float,
    centerY: Float,
    centerZ: Float,
    upX: Float,
    upY: Float,
    upZ: Float,
): FloatArray = lookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, FloatArray(MATRIX_FLOATS))

/**
 * Scene. Writes the [lookAt] view matrix into the first [MATRIX_FLOATS] floats of [out] and returns it.
 * Invalid arguments throw before [out] is touched.
 */
@Suppress("LongParameterList")
public fun lookAt(
    eyeX: Float,
    eyeY: Float,
    eyeZ: Float,
    centerX: Float,
    centerY: Float,
    centerZ: Float,
    upX: Float,
    upY: Float,
    upZ: Float,
    out: FloatArray,
): FloatArray {
    require(out.size >= MATRIX_FLOATS) { "Matrix needs $MATRIX_FLOATS floats, was ${out.size}" }
    require(
        eyeX.isFinite() && eyeY.isFinite() && eyeZ.isFinite() &&
            centerX.isFinite() && centerY.isFinite() && centerZ.isFinite() &&
            upX.isFinite() && upY.isFinite() && upZ.isFinite(),
    ) {
        "lookAt arguments must be finite"
    }
    var fx = centerX - eyeX
    var fy = centerY - eyeY
    var fz = centerZ - eyeZ
    val forwardLength = sqrt(fx * fx + fy * fy + fz * fz)
    require(forwardLength > DEGENERATE) { "lookAt eye and center must differ" }
    fx /= forwardLength
    fy /= forwardLength
    fz /= forwardLength
    var sx = fy * upZ - fz * upY
    var sy = fz * upX - fx * upZ
    var sz = fx * upY - fy * upX
    val sideLength = sqrt(sx * sx + sy * sy + sz * sz)
    require(sideLength > DEGENERATE) { "lookAt up must not be parallel to the view direction" }
    sx /= sideLength
    sy /= sideLength
    sz /= sideLength
    val ux = sy * fz - sz * fy
    val uy = sz * fx - sx * fz
    val uz = sx * fy - sy * fx
    out[0] = sx
    out[1] = ux
    out[2] = -fx
    out[3] = 0f
    out[4] = sy
    out[5] = uy
    out[6] = -fy
    out[7] = 0f
    out[8] = sz
    out[9] = uz
    out[10] = -fz
    out[11] = 0f
    out[12] = -(sx * eyeX + sy * eyeY + sz * eyeZ)
    out[13] = -(ux * eyeX + uy * eyeY + uz * eyeZ)
    out[14] = fx * eyeX + fy * eyeY + fz * eyeZ
    out[15] = 1f
    return out
}

/**
 * Scene. Perspective matrix, column-major, the same order as `glUniformMatrix4fv` with transpose false.
 * [fovy] is the vertical field of view in radians. Clip z is the OpenGL ES range −1..1.
 *
 * `f = 1 / tan(fovy / 2)`, `m00 = f / aspect`, `m11 = f`,
 * `m22 = (far + near) / (near - far)`, `m23 = -1`, `m32 = 2 * far * near / (near - far)`.
 * Allocates a new array; per-frame code should pass its own array to the other overload.
 */
public fun perspective(fovy: Float, aspect: Float, near: Float, far: Float): FloatArray =
    perspective(fovy, aspect, near, far, FloatArray(MATRIX_FLOATS))

/**
 * Scene. Writes the [perspective] matrix into the first [MATRIX_FLOATS] floats of [out] and returns it.
 * Invalid arguments throw before [out] is touched.
 */
public fun perspective(fovy: Float, aspect: Float, near: Float, far: Float, out: FloatArray): FloatArray {
    require(out.size >= MATRIX_FLOATS) { "Matrix needs $MATRIX_FLOATS floats, was ${out.size}" }
    require(fovy.isFinite() && aspect.isFinite() && near.isFinite() && far.isFinite()) {
        "perspective arguments must be finite"
    }
    require(fovy > 0f && fovy < PI.toFloat()) { "perspective fovy must be in (0, pi), was $fovy" }
    require(aspect > 0f) { "perspective aspect must be positive, was $aspect" }
    require(near > 0f) { "perspective near must be positive, was $near" }
    require(far > near) { "perspective far must be greater than near, was far=$far near=$near" }
    val focal = 1f / tan(fovy * 0.5f)
    val span = near - far
    out.fill(0f, 0, MATRIX_FLOATS)
    out[0] = focal / aspect
    out[5] = focal
    out[10] = (far + near) / span
    out[11] = -1f
    out[14] = (2f * far * near) / span
    return out
}

/**
 * Scene. Orthographic matrix, column-major, the same order as `glUniformMatrix4fv` with transpose false.
 * Clip z is the OpenGL ES range −1..1, the same convention as [perspective].
 *
 * `m00 = 2 / (right - left)`, `m11 = 2 / (top - bottom)`, `m22 = -2 / (far - near)`,
 * `m30 = -(right + left) / (right - left)`, `m31 = -(top + bottom) / (top - bottom)`,
 * `m32 = -(far + near) / (far - near)`.
 * Allocates a new array; per-frame code should pass its own array to the other overload.
 */
public fun ortho(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): FloatArray =
    ortho(left, right, bottom, top, near, far, FloatArray(MATRIX_FLOATS))

/**
 * Scene. Writes the [ortho] matrix into the first [MATRIX_FLOATS] floats of [out] and returns it.
 * Invalid arguments throw before [out] is touched.
 */
public fun ortho(
    left: Float,
    right: Float,
    bottom: Float,
    top: Float,
    near: Float,
    far: Float,
    out: FloatArray,
): FloatArray {
    require(out.size >= MATRIX_FLOATS) { "Matrix needs $MATRIX_FLOATS floats, was ${out.size}" }
    require(
        left.isFinite() && right.isFinite() && bottom.isFinite() && top.isFinite() &&
            near.isFinite() && far.isFinite(),
    ) {
        "ortho arguments must be finite"
    }
    require(left != right) { "ortho left and right must differ, was $left" }
    require(bottom != top) { "ortho bottom and top must differ, was $bottom" }
    require(far > near) { "ortho far must be greater than near, was far=$far near=$near" }
    val width = right - left
    val height = top - bottom
    val depth = far - near
    out.fill(0f, 0, MATRIX_FLOATS)
    out[0] = 2f / width
    out[5] = 2f / height
    out[10] = -2f / depth
    out[12] = -(right + left) / width
    out[13] = -(top + bottom) / height
    out[14] = -(far + near) / depth
    out[15] = 1f
    return out
}
