package ru.redbyte.redbytefx.scene

import kotlin.math.abs

/**
 * Transposes a column-major 4×4 [matrix]. [out] may be the same array as [matrix].
 * Pass [out] when updating a matrix each frame to avoid an allocation.
 */
public fun transpose(matrix: FloatArray, out: FloatArray = FloatArray(MATRIX_FLOATS)): FloatArray {
    require(matrix.size >= MATRIX_FLOATS && out.size >= MATRIX_FLOATS) {
        "Matrix transpose needs $MATRIX_FLOATS floats"
    }
    if (out === matrix) {
        for (column in 0..3) {
            for (row in column + 1..3) {
                val first = column * 4 + row
                val second = row * 4 + column
                val held = out[first]
                out[first] = out[second]
                out[second] = held
            }
        }
    } else {
        for (column in 0..3) {
            for (row in 0..3) out[row * 4 + column] = matrix[column * 4 + row]
        }
    }
    return out
}

/**
 * Inverts a column-major 4×4 [matrix]. [out] may be [matrix].
 * Singular, non-finite, or non-representable results throw before [out] is changed.
 * Pass [out] to avoid allocating on each frame.
 */
@Suppress("LongMethod")
public fun inverse(matrix: FloatArray, out: FloatArray = FloatArray(MATRIX_FLOATS)): FloatArray {
    require(matrix.size >= MATRIX_FLOATS && out.size >= MATRIX_FLOATS) {
        "Matrix inverse needs $MATRIX_FLOATS floats"
    }
    require((0 until MATRIX_FLOATS).all { matrix[it].isFinite() }) { "Matrix values must be finite" }
    val a00 = matrix[0].toDouble()
    val a01 = matrix[1].toDouble()
    val a02 = matrix[2].toDouble()
    val a03 = matrix[3].toDouble()
    val a10 = matrix[4].toDouble()
    val a11 = matrix[5].toDouble()
    val a12 = matrix[6].toDouble()
    val a13 = matrix[7].toDouble()
    val a20 = matrix[8].toDouble()
    val a21 = matrix[9].toDouble()
    val a22 = matrix[10].toDouble()
    val a23 = matrix[11].toDouble()
    val a30 = matrix[12].toDouble()
    val a31 = matrix[13].toDouble()
    val a32 = matrix[14].toDouble()
    val a33 = matrix[15].toDouble()
    val b00 = a00 * a11 - a01 * a10
    val b01 = a00 * a12 - a02 * a10
    val b02 = a00 * a13 - a03 * a10
    val b03 = a01 * a12 - a02 * a11
    val b04 = a01 * a13 - a03 * a11
    val b05 = a02 * a13 - a03 * a12
    val b06 = a20 * a31 - a21 * a30
    val b07 = a20 * a32 - a22 * a30
    val b08 = a20 * a33 - a23 * a30
    val b09 = a21 * a32 - a22 * a31
    val b10 = a21 * a33 - a23 * a31
    val b11 = a22 * a33 - a23 * a32
    val determinant = b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06
    require(determinant.isFinite() && determinant != 0.0) { "Matrix has no inverse" }
    val scale = 1.0 / determinant
    val r0 = (a11 * b11 - a12 * b10 + a13 * b09) * scale
    val r1 = (a02 * b10 - a01 * b11 - a03 * b09) * scale
    val r2 = (a31 * b05 - a32 * b04 + a33 * b03) * scale
    val r3 = (a22 * b04 - a21 * b05 - a23 * b03) * scale
    val r4 = (a12 * b08 - a10 * b11 - a13 * b07) * scale
    val r5 = (a00 * b11 - a02 * b08 + a03 * b07) * scale
    val r6 = (a32 * b02 - a30 * b05 - a33 * b01) * scale
    val r7 = (a20 * b05 - a22 * b02 + a23 * b01) * scale
    val r8 = (a10 * b10 - a11 * b08 + a13 * b06) * scale
    val r9 = (a01 * b08 - a00 * b10 - a03 * b06) * scale
    val r10 = (a30 * b04 - a31 * b02 + a33 * b00) * scale
    val r11 = (a21 * b02 - a20 * b04 - a23 * b00) * scale
    val r12 = (a11 * b07 - a10 * b09 - a12 * b06) * scale
    val r13 = (a00 * b09 - a01 * b07 + a02 * b06) * scale
    val r14 = (a31 * b01 - a30 * b03 - a32 * b00) * scale
    val r15 = (a20 * b03 - a21 * b01 + a22 * b00) * scale
    require(
        fitsFloat(r0) && fitsFloat(r1) && fitsFloat(r2) && fitsFloat(r3) &&
            fitsFloat(r4) && fitsFloat(r5) && fitsFloat(r6) && fitsFloat(r7) &&
            fitsFloat(r8) && fitsFloat(r9) && fitsFloat(r10) && fitsFloat(r11) &&
            fitsFloat(r12) && fitsFloat(r13) && fitsFloat(r14) && fitsFloat(r15),
    ) { "Matrix inverse cannot be represented as floats" }
    out[0] = r0.toFloat()
    out[1] = r1.toFloat()
    out[2] = r2.toFloat()
    out[3] = r3.toFloat()
    out[4] = r4.toFloat()
    out[5] = r5.toFloat()
    out[6] = r6.toFloat()
    out[7] = r7.toFloat()
    out[8] = r8.toFloat()
    out[9] = r9.toFloat()
    out[10] = r10.toFloat()
    out[11] = r11.toFloat()
    out[12] = r12.toFloat()
    out[13] = r13.toFloat()
    out[14] = r14.toFloat()
    out[15] = r15.toFloat()
    return out
}

private fun fitsFloat(value: Double): Boolean = value.isFinite() && abs(value) <= Float.MAX_VALUE

/**
 * Transforms a point by a column-major 4×4 [matrix] and divides by homogeneous `w`.
 * For a model or view matrix, `w` is 1; for a projection it performs the perspective divide.
 * [out] receives x, y, z and may be reused each frame.
 */
public fun transformPoint(
    matrix: FloatArray,
    x: Float,
    y: Float,
    z: Float,
    out: FloatArray = FloatArray(3),
): FloatArray {
    require(matrix.size >= MATRIX_FLOATS && out.size >= 3) { "Point transform needs a 4x4 matrix and 3 output floats" }
    require(x.isFinite() && y.isFinite() && z.isFinite()) { "Point coordinates must be finite" }
    val tx = matrix[0] * x + matrix[4] * y + matrix[8] * z + matrix[12]
    val ty = matrix[1] * x + matrix[5] * y + matrix[9] * z + matrix[13]
    val tz = matrix[2] * x + matrix[6] * y + matrix[10] * z + matrix[14]
    val w = matrix[3] * x + matrix[7] * y + matrix[11] * z + matrix[15]
    require(w.isFinite() && w != 0f) { "Point has no finite perspective divide" }
    val px = tx / w
    val py = ty / w
    val pz = tz / w
    require(px.isFinite() && py.isFinite() && pz.isFinite()) { "Transformed point must be finite" }
    out[0] = px
    out[1] = py
    out[2] = pz
    return out
}

/** Transforms a direction with homogeneous `w = 0`, so translation has no effect. */
public fun transformDirection(
    matrix: FloatArray,
    x: Float,
    y: Float,
    z: Float,
    out: FloatArray = FloatArray(3),
): FloatArray {
    require(matrix.size >= MATRIX_FLOATS && out.size >= 3) { "Direction transform needs a 4x4 matrix and 3 output floats" }
    require(x.isFinite() && y.isFinite() && z.isFinite()) { "Direction coordinates must be finite" }
    val dx = matrix[0] * x + matrix[4] * y + matrix[8] * z
    val dy = matrix[1] * x + matrix[5] * y + matrix[9] * z
    val dz = matrix[2] * x + matrix[6] * y + matrix[10] * z
    require(dx.isFinite() && dy.isFinite() && dz.isFinite()) { "Transformed direction must be finite" }
    out[0] = dx
    out[1] = dy
    out[2] = dz
    return out
}
