package ru.redbyte.redbytefx.scene

import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.math.tan

private const val DEGENERATE: Float = 1.0e-8f

/** Floats in one 4x4 matrix. */
public const val MATRIX_FLOATS: Int = 16

/**
 * Scene. Column-major identity. Each read returns a new array that callers may modify.
 */
public val IDENTITY: FloatArray get() = identityValues.copyOf()

private val identityValues: FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f,
)

internal fun copyIdentityInto(out: FloatArray) {
    identityValues.copyInto(out, endIndex = MATRIX_FLOATS)
}

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
    require(forwardLength.isFinite() && forwardLength > DEGENERATE) {
        "lookAt eye and center must have a finite separation"
    }
    fx /= forwardLength
    fy /= forwardLength
    fz /= forwardLength
    var sx = fy * upZ - fz * upY
    var sy = fz * upX - fx * upZ
    var sz = fx * upY - fy * upX
    val sideLength = sqrt(sx * sx + sy * sy + sz * sz)
    require(sideLength.isFinite() && sideLength > DEGENERATE) {
        "lookAt up must have a finite, non-parallel direction"
    }
    sx /= sideLength
    sy /= sideLength
    sz /= sideLength
    val ux = sy * fz - sz * fy
    val uy = sz * fx - sx * fz
    val uz = sx * fy - sy * fx
    val tx = -(sx * eyeX + sy * eyeY + sz * eyeZ)
    val ty = -(ux * eyeX + uy * eyeY + uz * eyeZ)
    val tz = fx * eyeX + fy * eyeY + fz * eyeZ
    require(tx.isFinite() && ty.isFinite() && tz.isFinite()) {
        "lookAt translation must be finite"
    }
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
    out[12] = tx
    out[13] = ty
    out[14] = tz
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
    val focal = 1.0 / tan(fovy.toDouble() * 0.5)
    val span = near.toDouble() - far
    val xScale = focal / aspect
    val depthScale = (far.toDouble() + near) / span
    val depthOffset = (2.0 * far * near) / span
    require(fitsFloat(xScale) && fitsFloat(focal) && fitsFloat(depthScale) && fitsFloat(depthOffset)) {
        "perspective matrix coefficients must be finite"
    }
    out.fill(0f, 0, MATRIX_FLOATS)
    out[0] = xScale.toFloat()
    out[5] = focal.toFloat()
    out[10] = depthScale.toFloat()
    out[11] = -1f
    out[14] = depthOffset.toFloat()
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
    val width = right.toDouble() - left
    val height = top.toDouble() - bottom
    val depth = far.toDouble() - near
    val xScale = 2.0 / width
    val yScale = 2.0 / height
    val zScale = -2.0 / depth
    val xOffset = -(right.toDouble() + left) / width
    val yOffset = -(top.toDouble() + bottom) / height
    val zOffset = -(far.toDouble() + near) / depth
    require(
        fitsFloat(xScale) && fitsFloat(yScale) && fitsFloat(zScale) &&
            fitsFloat(xOffset) && fitsFloat(yOffset) && fitsFloat(zOffset),
    ) { "ortho matrix coefficients must be finite" }
    out.fill(0f, 0, MATRIX_FLOATS)
    out[0] = xScale.toFloat()
    out[5] = yScale.toFloat()
    out[10] = zScale.toFloat()
    out[12] = xOffset.toFloat()
    out[13] = yOffset.toFloat()
    out[14] = zOffset.toFloat()
    out[15] = 1f
    return out
}
