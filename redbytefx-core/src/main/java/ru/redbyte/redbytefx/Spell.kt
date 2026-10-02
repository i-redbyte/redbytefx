package ru.redbyte.redbytefx

internal fun spell(shape: Shape, target: ShaderTarget): String {
    require(target == ShaderTarget.Agsl) { "GLSL spelling is not available yet" }
    require(shape != Shape.Sampler2D) { "AGSL has no sampler2D" }
    return when (shape) {
        is Shape.Scalar -> scalarSpell(shape.kind, shape.precision)
        is Shape.Vector -> scalarSpell(shape.kind, shape.precision) + shape.lanes
        is Shape.Matrix -> "float${shape.lanes}x${shape.lanes}"
        Shape.ChildShader -> "shader"
        Shape.Sampler2D -> error("sampler2D was rejected")
    }
}

private fun scalarSpell(kind: ScalarKind, precision: Precision?): String = when (kind) {
    ScalarKind.Float -> when (precision) {
        Precision.High -> "float"
        Precision.Med -> "half"
        null -> error("Float shape is missing precision")
    }
    ScalarKind.Int -> "int"
    ScalarKind.Bool -> "bool"
}
