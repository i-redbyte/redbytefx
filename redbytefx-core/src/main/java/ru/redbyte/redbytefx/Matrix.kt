package ru.redbyte.redbytefx

import kotlin.jvm.JvmName

/**
 * Column-major `mat2` / `mat3` / `mat4` constructors and matrix–vector multiply.
 *
 * Each [mat2] argument is one **column** (`vec2` … `vec4`). Use `matrix * vector` via [times].
 */

/** Column-major 2×2 matrix from two column vectors. */
public fun mat2(
    c0: Expr<Vec2<Flt<High>>>,
    c1: Expr<Vec2<Flt<High>>>,
): Expr<Mat2> = matrix(c0, c1)

public fun mat3(
    c0: Expr<Vec3<Flt<High>>>,
    c1: Expr<Vec3<Flt<High>>>,
    c2: Expr<Vec3<Flt<High>>>,
): Expr<Mat3> = matrix(c0, c1, c2)

public fun mat4(
    c0: Expr<Vec4<Flt<High>>>,
    c1: Expr<Vec4<Flt<High>>>,
    c2: Expr<Vec4<Flt<High>>>,
    c3: Expr<Vec4<Flt<High>>>,
): Expr<Mat4> = matrix(c0, c1, c2, c3)

@JvmName("timesMat2Vec2")
public operator fun Expr<Mat2>.times(vector: Expr<Vec2<Flt<High>>>): Expr<Vec2<Flt<High>>> =
    matVec(this, vector)

@JvmName("timesMat3Vec3")
public operator fun Expr<Mat3>.times(vector: Expr<Vec3<Flt<High>>>): Expr<Vec3<Flt<High>>> =
    matVec(this, vector)

@JvmName("timesMat4Vec4")
public operator fun Expr<Mat4>.times(vector: Expr<Vec4<Flt<High>>>): Expr<Vec4<Flt<High>>> =
    matVec(this, vector)

internal fun matVecShape(matrix: Shape, vector: Shape): Shape {
    require(matrix is Shape.Matrix) {
        "Left side of a matrix-vector product must be a matrix, was $matrix"
    }
    require(
        vector is Shape.Vector &&
            vector.kind == ScalarKind.Float &&
            vector.precision == Precision.High &&
            vector.lanes == matrix.lanes,
    ) {
        "Matrix of ${matrix.lanes} columns requires a high float vector of ${matrix.lanes} lanes, was $vector"
    }
    return vector
}

private fun <T : ShType> matrix(vararg columns: Expr<*>): Expr<T> {
    require(columns.size in 2..4) { "Matrix constructor needs 2, 3, or 4 columns" }
    val columnShape = Shape.Vector(ScalarKind.Float, Precision.High, columns.size)
    for (column in columns) {
        require(column.shape == columnShape) {
            "Matrix column must be $columnShape"
        }
    }
    return Expr(Shape.Matrix(columns.size), ExprNode.Construct(columns.asList()))
}

private fun <T : ShType> matVec(matrix: Expr<*>, vector: Expr<*>): Expr<T> =
    Expr(matVecShape(matrix.shape, vector.shape), ExprNode.Binary(ArithOp.Mul, matrix, vector))
