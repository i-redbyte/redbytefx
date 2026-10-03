package ru.redbyte.redbytefx

/** Runtime witness of a [ShType]. Phantom type arguments are erased; this value is not. */
public enum class Precision {
    High,
    Med,
}

/** Scalar family stored in a [Shape]. */
public enum class ScalarKind {
    Float,
    Int,
    Bool,
}

/**
 * Closed runtime algebra of shader shapes.
 *
 * Float forms carry [Precision]. Integer and boolean forms do not. Vector and matrix lane
 * counts are only 2, 3, and 4.
 */
public sealed interface Shape {
    public data class Scalar(
        val kind: ScalarKind,
        val precision: Precision?,
    ) : Shape {
        init {
            when (kind) {
                ScalarKind.Float -> require(precision != null) { "Float scalar requires precision" }
                ScalarKind.Int, ScalarKind.Bool -> require(precision == null) {
                    "Non-float scalar cannot carry precision"
                }
            }
        }
    }

    public data class Vector(
        val kind: ScalarKind,
        val precision: Precision?,
        val lanes: Int,
    ) : Shape {
        init {
            require(lanes in VALID_LANES) { "Vector lanes must be 2, 3, or 4" }
            when (kind) {
                ScalarKind.Float -> require(precision != null) { "Float vector requires precision" }
                ScalarKind.Int, ScalarKind.Bool -> require(precision == null) {
                    "Non-float vector cannot carry precision"
                }
            }
        }
    }

    public data class Matrix(val lanes: Int) : Shape {
        init {
            require(lanes in VALID_LANES) { "Matrix lanes must be 2, 3, or 4" }
        }
    }

    public data object Sampler2D : Shape

    public data object SamplerCube : Shape

    public data object ChildShader : Shape

    private companion object {
        val VALID_LANES = 2..4
    }
}
