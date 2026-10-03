package ru.redbyte.redbytefx

internal fun spell(shape: Shape, target: ShaderTarget): String = when (target) {
    ShaderTarget.Agsl -> spellAgsl(shape)
    ShaderTarget.Gles30, ShaderTarget.Gles31, ShaderTarget.Gles32 -> spellGlsl(shape)
}

internal fun glslDeclaration(shape: Shape): String {
    val type = spell(shape, ShaderTarget.Gles30)
    val precision = glslPrecision(shape) ?: return type
    return "$precision $type"
}

private fun spellAgsl(shape: Shape): String {
    require(shape != Shape.Sampler2D && shape != Shape.SamplerCube) { "AGSL has no sampler" }
    return when (shape) {
        is Shape.Scalar -> agslScalar(shape.kind, shape.precision)
        is Shape.Vector -> agslScalar(shape.kind, shape.precision) + shape.lanes
        is Shape.Matrix -> "float${shape.lanes}x${shape.lanes}"
        Shape.ChildShader -> "shader"
        Shape.Sampler2D, Shape.SamplerCube -> error("sampler was rejected")
    }
}

private fun spellGlsl(shape: Shape): String {
    require(shape != Shape.ChildShader) { "GLSL has no child shader" }
    return when (shape) {
        is Shape.Scalar -> glslScalar(shape.kind)
        is Shape.Vector -> glslVector(shape.kind, shape.lanes)
        is Shape.Matrix -> "mat${shape.lanes}"
        Shape.Sampler2D -> "sampler2D"
        Shape.SamplerCube -> "samplerCube"
        Shape.ChildShader -> error("child shader was rejected")
    }
}

private fun agslScalar(kind: ScalarKind, precision: Precision?): String = when (kind) {
    ScalarKind.Float -> when (precision) {
        Precision.High -> "float"
        Precision.Med -> "half"
        null -> error("Float shape is missing precision")
    }
    ScalarKind.Int -> "int"
    ScalarKind.Bool -> "bool"
}

private fun glslScalar(kind: ScalarKind): String = when (kind) {
    ScalarKind.Float -> "float"
    ScalarKind.Int -> "int"
    ScalarKind.Bool -> "bool"
}

private fun glslVector(kind: ScalarKind, lanes: Int): String = when (kind) {
    ScalarKind.Float -> "vec$lanes"
    ScalarKind.Int -> "ivec$lanes"
    ScalarKind.Bool -> "bvec$lanes"
}

private fun glslPrecision(shape: Shape): String? = when (shape) {
    is Shape.Scalar -> floatPrecision(shape.kind, shape.precision)
    is Shape.Vector -> floatPrecision(shape.kind, shape.precision)
    is Shape.Matrix, Shape.Sampler2D, Shape.SamplerCube -> "highp"
    Shape.ChildShader -> null
}

private fun floatPrecision(kind: ScalarKind, precision: Precision?): String? = when (kind) {
    ScalarKind.Float -> when (precision) {
        Precision.High -> "highp"
        Precision.Med -> "mediump"
        null -> error("Float shape is missing precision")
    }
    ScalarKind.Int, ScalarKind.Bool -> null
}
