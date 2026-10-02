package ru.redbyte.redbytefx

public enum class GeometryInput {
    Points,
    Lines,
    Triangles,
    LinesAdjacency,
    TrianglesAdjacency,
}

public enum class GeometryOutput {
    Points,
    LineStrip,
    TriangleStrip,
}

public enum class TessPrimitive {
    Triangles,
    Quads,
    Isolines,
}

public enum class TessSpacing {
    Equal,
    FractionalEven,
    FractionalOdd,
}

public enum class TessVertexOrder {
    Ccw,
    Cw,
}

internal val GeometryInput.vertices: Int
    get() = when (this) {
        GeometryInput.Points -> 1
        GeometryInput.Lines -> 2
        GeometryInput.Triangles -> 3
        GeometryInput.LinesAdjacency -> 4
        GeometryInput.TrianglesAdjacency -> 6
    }

internal val GeometryInput.glslName: String
    get() = when (this) {
        GeometryInput.Points -> "points"
        GeometryInput.Lines -> "lines"
        GeometryInput.Triangles -> "triangles"
        GeometryInput.LinesAdjacency -> "lines_adjacency"
        GeometryInput.TrianglesAdjacency -> "triangles_adjacency"
    }

internal val GeometryOutput.glslName: String
    get() = when (this) {
        GeometryOutput.Points -> "points"
        GeometryOutput.LineStrip -> "line_strip"
        GeometryOutput.TriangleStrip -> "triangle_strip"
    }

internal sealed interface PrimitiveCommand {
    data class Position(val value: Expr<*>) : PrimitiveCommand

    data object EmitVertex : PrimitiveCommand

    data object EndPrimitive : PrimitiveCommand

    data class OuterLevel(val index: Int, val value: Expr<*>) : PrimitiveCommand

    data class InnerLevel(val index: Int, val value: Expr<*>) : PrimitiveCommand

    data object PassPosition : PrimitiveCommand
}

internal class GeometryStage(
    val input: GeometryInput,
    val output: GeometryOutput,
    val maxVertices: Int,
    val commands: List<PrimitiveCommand>,
)

internal class TessControlStage(
    val vertices: Int,
    val commands: List<PrimitiveCommand>,
)

internal class TessEvalStage(
    val primitive: TessPrimitive,
    val spacing: TessSpacing,
    val order: TessVertexOrder,
    val commands: List<PrimitiveCommand>,
)

public class GeometryDsl internal constructor(
    private val input: GeometryInput,
    private val advance: (AuthoringAction) -> Unit,
    private val record: (PrimitiveCommand) -> Unit,
) {
    public fun glIn(index: Int): HighVec4 {
        require(index in 0 until input.vertices) {
            "Geometry input ${input.name} has ${input.vertices} vertices, index was $index"
        }
        return glInExpr(index)
    }

    public fun glPosition(value: HighVec4) {
        advance(AuthoringAction.GlPosition)
        require(isFloatVec4(value.shape)) { "gl_Position expects a float vec4, was ${value.shape}" }
        record(PrimitiveCommand.Position(value))
    }

    public fun emitVertex() {
        advance(AuthoringAction.EmitVertex)
        record(PrimitiveCommand.EmitVertex)
    }

    public fun endPrimitive() {
        advance(AuthoringAction.EndPrimitive)
        record(PrimitiveCommand.EndPrimitive)
    }
}

public class TessControlDsl internal constructor(
    private val vertices: Int,
    private val advance: (AuthoringAction) -> Unit,
    private val record: (PrimitiveCommand) -> Unit,
) {
    public fun glIn(index: Int): HighVec4 {
        require(index in 0 until vertices) {
            "Tessellation control has $vertices vertices, index was $index"
        }
        return glInExpr(index)
    }

    public fun tessLevelOuter(index: Int, value: HighFloat) {
        advance(AuthoringAction.TessLevel)
        require(index in 0..3) { "gl_TessLevelOuter index must be 0..3, was $index" }
        record(PrimitiveCommand.OuterLevel(index, value))
    }

    public fun tessLevelInner(index: Int, value: HighFloat) {
        advance(AuthoringAction.TessLevel)
        require(index in 0..1) { "gl_TessLevelInner index must be 0..1, was $index" }
        record(PrimitiveCommand.InnerLevel(index, value))
    }

    public fun passPosition() {
        advance(AuthoringAction.TessLevel)
        record(PrimitiveCommand.PassPosition)
    }
}

public class TessEvalDsl internal constructor(
    private val advance: (AuthoringAction) -> Unit,
    private val record: (PrimitiveCommand) -> Unit,
) {
    public val tessCoord: HighVec3 = Expr(
        Shape.Vector(ScalarKind.Float, Precision.High, 3),
        ExprNode.TessCoord,
    )

    public fun glIn(index: Int): HighVec4 {
        require(index >= 0) { "gl_in index must be non-negative, was $index" }
        return glInExpr(index)
    }

    public fun glPosition(value: HighVec4) {
        advance(AuthoringAction.GlPosition)
        require(isFloatVec4(value.shape)) { "gl_Position expects a float vec4, was ${value.shape}" }
        record(PrimitiveCommand.Position(value))
    }
}

internal const val GLSL_300 = 300
internal const val GLSL_320 = 320
internal const val MAX_GEOMETRY_VERTICES = 256
internal const val MAX_PATCH_VERTICES = 32

private fun glInExpr(index: Int): HighVec4 = Expr(
    Shape.Vector(ScalarKind.Float, Precision.High, 4),
    ExprNode.GlIn(index),
)
