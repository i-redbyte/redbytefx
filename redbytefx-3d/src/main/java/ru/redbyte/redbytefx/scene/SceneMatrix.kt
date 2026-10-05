package ru.redbyte.redbytefx.scene

import kotlin.math.cos
import kotlin.math.sin

private const val DEGENERATE: Float = 1.0e-8f

/** Scene. Writes a column-major identity into [out] and returns it. */
public fun identity(out: FloatArray = FloatArray(MATRIX_FLOATS)): FloatArray {
    require(out.size >= MATRIX_FLOATS) { "Matrix needs $MATRIX_FLOATS floats, was ${out.size}" }
    copyIdentityInto(out)
    return out
}

/**
 * Scene. Column-major `out = a * b`, the same product as GLSL `a * b`.
 * [out] may be [a] or [b].
 */
public fun multiply(a: FloatArray, b: FloatArray, out: FloatArray = FloatArray(MATRIX_FLOATS)): FloatArray {
    require(a.size >= MATRIX_FLOATS && b.size >= MATRIX_FLOATS && out.size >= MATRIX_FLOATS) {
        "Matrix product needs $MATRIX_FLOATS floats"
    }
    val c0 = a[0] * b[0] + a[4] * b[1] + a[8] * b[2] + a[12] * b[3]
    val c1 = a[1] * b[0] + a[5] * b[1] + a[9] * b[2] + a[13] * b[3]
    val c2 = a[2] * b[0] + a[6] * b[1] + a[10] * b[2] + a[14] * b[3]
    val c3 = a[3] * b[0] + a[7] * b[1] + a[11] * b[2] + a[15] * b[3]
    val c4 = a[0] * b[4] + a[4] * b[5] + a[8] * b[6] + a[12] * b[7]
    val c5 = a[1] * b[4] + a[5] * b[5] + a[9] * b[6] + a[13] * b[7]
    val c6 = a[2] * b[4] + a[6] * b[5] + a[10] * b[6] + a[14] * b[7]
    val c7 = a[3] * b[4] + a[7] * b[5] + a[11] * b[6] + a[15] * b[7]
    val c8 = a[0] * b[8] + a[4] * b[9] + a[8] * b[10] + a[12] * b[11]
    val c9 = a[1] * b[8] + a[5] * b[9] + a[9] * b[10] + a[13] * b[11]
    val c10 = a[2] * b[8] + a[6] * b[9] + a[10] * b[10] + a[14] * b[11]
    val c11 = a[3] * b[8] + a[7] * b[9] + a[11] * b[10] + a[15] * b[11]
    val c12 = a[0] * b[12] + a[4] * b[13] + a[8] * b[14] + a[12] * b[15]
    val c13 = a[1] * b[12] + a[5] * b[13] + a[9] * b[14] + a[13] * b[15]
    val c14 = a[2] * b[12] + a[6] * b[13] + a[10] * b[14] + a[14] * b[15]
    val c15 = a[3] * b[12] + a[7] * b[13] + a[11] * b[14] + a[15] * b[15]
    out[0] = c0
    out[1] = c1
    out[2] = c2
    out[3] = c3
    out[4] = c4
    out[5] = c5
    out[6] = c6
    out[7] = c7
    out[8] = c8
    out[9] = c9
    out[10] = c10
    out[11] = c11
    out[12] = c12
    out[13] = c13
    out[14] = c14
    out[15] = c15
    return out
}

/** Scene. Translation in the last column. */
public fun translation(x: Float, y: Float, z: Float, out: FloatArray = FloatArray(MATRIX_FLOATS)): FloatArray {
    require(x.isFinite() && y.isFinite() && z.isFinite()) { "translation components must be finite" }
    identity(out)
    out[12] = x
    out[13] = y
    out[14] = z
    return out
}

/** Scene. Non-uniform scale on the diagonal. */
public fun scale(x: Float, y: Float, z: Float, out: FloatArray = FloatArray(MATRIX_FLOATS)): FloatArray {
    require(
        x.isFinite() && y.isFinite() && z.isFinite() &&
            kotlin.math.abs(x) > DEGENERATE && kotlin.math.abs(y) > DEGENERATE && kotlin.math.abs(z) > DEGENERATE,
    ) {
        "scale factors must be finite and non-zero, was ($x, $y, $z)"
    }
    identity(out)
    out[0] = x
    out[5] = y
    out[10] = z
    return out
}

/** Scene. Rotation around +X, column-major. */
public fun rotationX(radians: Float, out: FloatArray = FloatArray(MATRIX_FLOATS)): FloatArray {
    require(radians.isFinite()) { "rotation angle must be finite" }
    identity(out)
    val cosine = cos(radians)
    val sine = sin(radians)
    out[5] = cosine
    out[6] = sine
    out[9] = -sine
    out[10] = cosine
    return out
}

/** Scene. Rotation around +Y, column-major. */
public fun rotationY(radians: Float, out: FloatArray = FloatArray(MATRIX_FLOATS)): FloatArray {
    require(radians.isFinite()) { "rotation angle must be finite" }
    identity(out)
    val cosine = cos(radians)
    val sine = sin(radians)
    out[0] = cosine
    out[2] = -sine
    out[8] = sine
    out[10] = cosine
    return out
}

/** Scene. Rotation around +Z, column-major. */
public fun rotationZ(radians: Float, out: FloatArray = FloatArray(MATRIX_FLOATS)): FloatArray {
    require(radians.isFinite()) { "rotation angle must be finite" }
    identity(out)
    val cosine = cos(radians)
    val sine = sin(radians)
    out[0] = cosine
    out[1] = sine
    out[4] = -sine
    out[5] = cosine
    return out
}
