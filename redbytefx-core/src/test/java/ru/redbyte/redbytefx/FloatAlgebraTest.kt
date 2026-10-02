package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FloatAlgebraTest {

    @Test
    fun floatLiteralsLiftInOnePlace() {
        assertEquals(Shape.Scalar(ScalarKind.Float, Precision.High), 1f.lit.shape)
        assertEquals(Shape.Scalar(ScalarKind.Float, Precision.Med), 1f.med.shape)
        assertEquals(2f.lit, 2.lit)

        val literal = 3f.lit.node as ExprNode.Literal
        assertEquals(3f, literal.value)
        assertEquals(Precision.High, literal.precision)
    }

    @Test
    fun nonFiniteLiteralsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { Float.NaN.lit }
        assertThrows(IllegalArgumentException::class.java) { Float.POSITIVE_INFINITY.lit }
        assertThrows(IllegalArgumentException::class.java) { Float.NEGATIVE_INFINITY.med }
    }

    @Test
    fun shapeVocabularyIsClosed() {
        assertEquals(Shape.Scalar(ScalarKind.Int, null), Shape.Scalar(ScalarKind.Int, null))
        assertEquals(Shape.Scalar(ScalarKind.Bool, null), Shape.Scalar(ScalarKind.Bool, null))
        assertEquals(Shape.Matrix(4), Shape.Matrix(4))
        assertEquals(Shape.Sampler2D, Shape.Sampler2D)
        assertEquals(Shape.ChildShader, Shape.ChildShader)

        assertThrows(IllegalArgumentException::class.java) {
            Shape.Scalar(ScalarKind.Float, null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Shape.Scalar(ScalarKind.Int, Precision.High)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Shape.Vector(ScalarKind.Float, Precision.High, 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Shape.Vector(ScalarKind.Bool, Precision.Med, 2)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Shape.Matrix(5)
        }
    }

    @Test
    fun additionAndSubtractionPreserveRankAndPrecision() {
        val left = 1f.lit
        val right = 2f.med
        val highSum = left + 4f.lit
        val medDifference = right - 5f.med

        assertEquals(arithShape(ArithOp.Add, left.shape, 4f.lit.shape), highSum.shape)
        assertEquals(arithShape(ArithOp.Sub, right.shape, 5f.med.shape), medDifference.shape)
        assertBinary(ArithOp.Add, left, 4f.lit, highSum)
        assertBinary(ArithOp.Sub, right, 5f.med, medDifference)

        val vector = vec3(1f.med, 0f.med, 1f.med)
        assertEquals(vector.shape, (vector + vector).shape)
        assertEquals(vector.shape, (vector - 2f.med).shape)
        assertEquals(vector.shape, (2f.med + vector).shape)
    }

    @Test
    fun multiplicationAndDivisionBroadcastScalarOverVector() {
        val scalar = 2f.lit
        val vector = vec4(1f.lit, 2f.lit, 3f.lit, 4f.lit)

        assertEquals(vector.shape, (vector * scalar).shape)
        assertEquals(vector.shape, (scalar * vector).shape)
        assertEquals(vector.shape, (vector / scalar).shape)
        assertEquals(vector.shape, (scalar / vector).shape)
        assertEquals(scalar.shape, (scalar * 3f.lit).shape)
        assertBinary(ArithOp.Mul, scalar, vector, scalar * vector)
    }

    @Test
    fun negationPreservesFloatShape() {
        val scalar = 1f.med
        val vector = vec2(1f.lit, 0f.lit)
        val negatedScalar = -scalar
        val negatedVector = -vector

        assertEquals(unaryShape(UnaryOp.Neg, scalar.shape), negatedScalar.shape)
        assertEquals(vector.shape, negatedVector.shape)
        val node = negatedVector.node as ExprNode.Unary
        assertEquals(UnaryOp.Neg, node.op)
        assertEquals(vector, node.arg)
    }

    @Test
    fun arithShapeRejectsFormsOutsideFloatArithmetic() {
        val high = 1f.lit.shape
        val med = 1f.med.shape
        val vec2 = vec2(0f.lit, 1f.lit).shape
        val vec3 = vec3(0f.lit, 1f.lit, 0f.lit).shape
        val int = Shape.Scalar(ScalarKind.Int, null)
        val matrix = Shape.Matrix(4)

        assertThrows(IllegalArgumentException::class.java) { arithShape(ArithOp.Add, high, med) }
        assertThrows(IllegalArgumentException::class.java) { arithShape(ArithOp.Add, vec2, vec3) }
        assertThrows(IllegalArgumentException::class.java) { arithShape(ArithOp.Add, int, int) }
        assertThrows(IllegalArgumentException::class.java) { arithShape(ArithOp.Mul, matrix, matrix) }
        assertThrows(IllegalArgumentException::class.java) { unaryShape(UnaryOp.Neg, int) }
    }

    @Test
    fun separatelyBuiltTreesAreEqual() {
        assertEquals(vec2(1f.lit, 2f.lit) * 3f.lit, vec2(1f.lit, 2f.lit) * 3f.lit)
    }

    private fun assertBinary(
        op: ArithOp,
        left: Expr<*>,
        right: Expr<*>,
        actual: Expr<*>,
    ) {
        val node = actual.node as ExprNode.Binary
        assertEquals(op, node.op)
        assertEquals(left, node.left)
        assertEquals(right, node.right)
    }
}
