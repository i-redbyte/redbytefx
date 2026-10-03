package ru.redbyte.redbytefx

/** GLES geometry shader input primitive ([ShaderTarget.Gles32]). */
public enum class GeometryInput {
    Points,
    Lines,
    Triangles,
    LinesAdjacency,
    TrianglesAdjacency,
}

/** GLES geometry shader output primitive layout. */
public enum class GeometryOutput {
    Points,
    LineStrip,
    TriangleStrip,
}

/** Tessellation primitive mode for patch shaders. */
public enum class TessPrimitive {
    Triangles,
    Quads,
    Isolines,
}

/** Tessellation spacing mode (`equal_spacing`, … in GLSL). */
public enum class TessSpacing {
    Equal,
    FractionalEven,
    FractionalOdd,
}

/** Winding order for tessellation patches. */
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

    data class VaryingSet(val varying: Varying<*>, val value: Expr<*>) : PrimitiveCommand

    data class Store(val target: Expr<*>, val value: Expr<*>) : PrimitiveCommand

    data object Discard : PrimitiveCommand

    data class DiscardIf(val condition: Expr<*>) : PrimitiveCommand

    data object Barrier : PrimitiveCommand

    data class Repeat(
        val count: Int,
        val indexName: String,
        val body: List<PrimitiveCommand>,
    ) : PrimitiveCommand

    data class When(val condition: Expr<*>, val body: List<PrimitiveCommand>) : PrimitiveCommand

    data class LocalSet(val slot: LocalSlot, val value: Expr<*>) : PrimitiveCommand
}

internal const val MAX_REPEAT_COUNT = 64

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
    private val sink: StatementSink,
    private val functions: StageFunctions,
    private val owns: (Varying<*>) -> Boolean,
) {
    public fun glIn(index: Int): HighVec4 = glInChecked(index, input.vertices, "Geometry input ${input.name}")

    public fun glIn(index: Expr<IntS>): HighVec4 = glInDynamic(index)

    public fun <T : ShType> Varying<T>.at(index: Int): Expr<T> =
        varyingAt(this, index, input.vertices, "Geometry input ${input.name}")

    public fun <T : ShType> Varying<T>.at(index: Expr<IntS>): Expr<T> = varyingAtDynamic(this, index)

    public fun <T : ShType> local(initializer: Expr<T>, name: String? = null): LocalVar<T> =
        sink.declareLocal(initializer, name)

    public fun whenTrue(condition: Expr<BoolS>, body: () -> Unit) {
        sink.whenTrue(condition, body)
    }

    public fun <T : ShType> Varying<T>.set(value: Expr<T>) {
        writeVarying(this, value)
    }

    public fun glPosition(value: HighVec4) {
        advance(AuthoringAction.GlPosition)
        require(isFloatVec4(value.shape)) { "gl_Position expects a float vec4, was ${value.shape}" }
        sink.add(PrimitiveCommand.Position(value))
    }

    public fun emitVertex() {
        advance(AuthoringAction.EmitVertex)
        sink.add(PrimitiveCommand.EmitVertex)
    }

    public fun endPrimitive() {
        advance(AuthoringAction.EndPrimitive)
        sink.add(PrimitiveCommand.EndPrimitive)
    }

    public fun repeat(count: Int, body: (Expr<IntS>) -> Unit) {
        advance(AuthoringAction.Repeat)
        sink.repeat(count, body)
    }

    public fun <R : ShType> fn(name: String? = null, block: GeometryDsl.() -> Expr<R>): Fn0<R> =
        functions.fn0(name) { block() }

    public fun <A : ShType, R : ShType> fn(
        witness: Expr<A>,
        name: String? = null,
        block: GeometryDsl.(Expr<A>) -> Expr<R>,
    ): Fn1<A, R> = functions.fn1(name, witness) { block(it) }

    public fun <A : ShType, B : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        name: String? = null,
        block: GeometryDsl.(Expr<A>, Expr<B>) -> Expr<R>,
    ): Fn2<A, B, R> = functions.fn2(name, first, second) { left, right -> block(left, right) }

    public fun <A : ShType, B : ShType, C : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        name: String? = null,
        block: GeometryDsl.(Expr<A>, Expr<B>, Expr<C>) -> Expr<R>,
    ): Fn3<A, B, C, R> = functions.fn3(name, first, second, third) { left, mid, right ->
        block(left, mid, right)
    }

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        name: String? = null,
        block: GeometryDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>) -> Expr<R>,
    ): Fn4<A, B, C, D, R> = functions.fn4(name, first, second, third, fourth) { a, b, c, d ->
        block(a, b, c, d)
    }

    public fun <T : ShType> recur(arg: Expr<T>): Expr<T> = functions.recur(arg)

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        name: String? = null,
        block: GeometryDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>) -> Expr<R>,
    ): Fn5<A, B, C, D, E, R> = functions.fn5(name, first, second, third, fourth, fifth) { a, b, c, d, e ->
        block(a, b, c, d, e)
    }

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, F : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        name: String? = null,
        block: GeometryDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>) -> Expr<R>,
    ): Fn6<A, B, C, D, E, F, R> =
        functions.fn6(name, first, second, third, fourth, fifth, sixth) { a, b, c, d, e, f ->
            block(a, b, c, d, e, f)
        }

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, F : ShType, G : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        seventh: Expr<G>,
        name: String? = null,
        block: GeometryDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>, Expr<G>) -> Expr<R>,
    ): Fn7<A, B, C, D, E, F, G, R> =
        functions.fn7(name, first, second, third, fourth, fifth, sixth, seventh) { a, b, c, d, e, f, g ->
            block(a, b, c, d, e, f, g)
        }

    public fun <
        A : ShType,
        B : ShType,
        C : ShType,
        D : ShType,
        E : ShType,
        F : ShType,
        G : ShType,
        H : ShType,
        R : ShType,
        > fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        seventh: Expr<G>,
        eighth: Expr<H>,
        name: String? = null,
        block: GeometryDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>, Expr<G>, Expr<H>) -> Expr<R>,
    ): Fn8<A, B, C, D, E, F, G, H, R> =
        functions.fn8(name, first, second, third, fourth, fifth, sixth, seventh, eighth) { a, b, c, d, e, f, g, h ->
            block(a, b, c, d, e, f, g, h)
        }

    private fun <T : ShType> writeVarying(varying: Varying<T>, value: Expr<T>) {
        require(owns(varying)) { "Varying \"${varying.name}\" does not belong to this shader" }
        require(value.shape == varying.shape) {
            "Varying \"${varying.name}\" expects ${varying.shape}, was ${value.shape}"
        }
        sink.add(PrimitiveCommand.VaryingSet(varying, value))
    }

    private fun <T : ShType> varyingAt(varying: Varying<T>, index: Int, limit: Int, label: String): Expr<T> {
        require(owns(varying)) { "Varying \"${varying.name}\" does not belong to this shader" }
        require(index in 0 until limit) { "$label has $limit vertices, index was $index" }
        return varyingAtDynamic(varying, index.intLit)
    }

    private fun <T : ShType> varyingAtDynamic(varying: Varying<T>, index: Expr<IntS>): Expr<T> {
        require(owns(varying)) { "Varying \"${varying.name}\" does not belong to this shader" }
        requireInt(index)
        return Expr(varying.shape, ExprNode.VaryingAt(varying, index))
    }
}

public class TessControlDsl internal constructor(
    private val vertices: Int,
    private val advance: (AuthoringAction) -> Unit,
    private val sink: StatementSink,
    private val functions: StageFunctions,
    private val owns: (Varying<*>) -> Boolean,
) {
    public fun glIn(index: Int): HighVec4 = glInChecked(index, vertices, "Tessellation control")

    public fun glIn(index: Expr<IntS>): HighVec4 = glInDynamic(index)

    public fun <T : ShType> Varying<T>.at(index: Int): Expr<T> {
        require(owns(this)) { "Varying \"${this.name}\" does not belong to this shader" }
        require(index in 0 until vertices) {
            "Tessellation control has $vertices vertices, index was $index"
        }
        return Expr(shape, ExprNode.VaryingAt(this, index.intLit))
    }

    public fun <T : ShType> Varying<T>.at(index: Expr<IntS>): Expr<T> {
        require(owns(this)) { "Varying \"${this.name}\" does not belong to this shader" }
        requireInt(index)
        return Expr(shape, ExprNode.VaryingAt(this, index))
    }

    public fun <T : ShType> local(initializer: Expr<T>, name: String? = null): LocalVar<T> =
        sink.declareLocal(initializer, name)

    public fun whenTrue(condition: Expr<BoolS>, body: () -> Unit) {
        sink.whenTrue(condition, body)
    }

    public fun <T : ShType> Varying<T>.set(value: Expr<T>) {
        require(owns(this)) { "Varying \"${this.name}\" does not belong to this shader" }
        require(value.shape == shape) { "Varying \"${this.name}\" expects $shape, was ${value.shape}" }
        sink.add(PrimitiveCommand.VaryingSet(this, value))
    }

    public fun tessLevelOuter(index: Int, value: HighFloat) {
        advance(AuthoringAction.TessLevel)
        require(index in 0..3) { "gl_TessLevelOuter index must be 0..3, was $index" }
        sink.add(PrimitiveCommand.OuterLevel(index, value))
    }

    public fun tessLevelInner(index: Int, value: HighFloat) {
        advance(AuthoringAction.TessLevel)
        require(index in 0..1) { "gl_TessLevelInner index must be 0..1, was $index" }
        sink.add(PrimitiveCommand.InnerLevel(index, value))
    }

    public fun passPosition() {
        advance(AuthoringAction.TessLevel)
        sink.add(PrimitiveCommand.PassPosition)
    }

    public fun repeat(count: Int, body: (Expr<IntS>) -> Unit) {
        advance(AuthoringAction.Repeat)
        sink.repeat(count, body)
    }

    public fun <R : ShType> fn(name: String? = null, block: TessControlDsl.() -> Expr<R>): Fn0<R> =
        functions.fn0(name) { block() }

    public fun <A : ShType, R : ShType> fn(
        witness: Expr<A>,
        name: String? = null,
        block: TessControlDsl.(Expr<A>) -> Expr<R>,
    ): Fn1<A, R> = functions.fn1(name, witness) { block(it) }

    public fun <A : ShType, B : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        name: String? = null,
        block: TessControlDsl.(Expr<A>, Expr<B>) -> Expr<R>,
    ): Fn2<A, B, R> = functions.fn2(name, first, second) { left, right -> block(left, right) }

    public fun <A : ShType, B : ShType, C : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        name: String? = null,
        block: TessControlDsl.(Expr<A>, Expr<B>, Expr<C>) -> Expr<R>,
    ): Fn3<A, B, C, R> = functions.fn3(name, first, second, third) { left, mid, right ->
        block(left, mid, right)
    }

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        name: String? = null,
        block: TessControlDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>) -> Expr<R>,
    ): Fn4<A, B, C, D, R> = functions.fn4(name, first, second, third, fourth) { a, b, c, d ->
        block(a, b, c, d)
    }

    public fun <T : ShType> recur(arg: Expr<T>): Expr<T> = functions.recur(arg)

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        name: String? = null,
        block: TessControlDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>) -> Expr<R>,
    ): Fn5<A, B, C, D, E, R> = functions.fn5(name, first, second, third, fourth, fifth) { a, b, c, d, e ->
        block(a, b, c, d, e)
    }

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, F : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        name: String? = null,
        block: TessControlDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>) -> Expr<R>,
    ): Fn6<A, B, C, D, E, F, R> =
        functions.fn6(name, first, second, third, fourth, fifth, sixth) { a, b, c, d, e, f ->
            block(a, b, c, d, e, f)
        }

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, F : ShType, G : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        seventh: Expr<G>,
        name: String? = null,
        block: TessControlDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>, Expr<G>) -> Expr<R>,
    ): Fn7<A, B, C, D, E, F, G, R> =
        functions.fn7(name, first, second, third, fourth, fifth, sixth, seventh) { a, b, c, d, e, f, g ->
            block(a, b, c, d, e, f, g)
        }

    public fun <
        A : ShType,
        B : ShType,
        C : ShType,
        D : ShType,
        E : ShType,
        F : ShType,
        G : ShType,
        H : ShType,
        R : ShType,
        > fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        seventh: Expr<G>,
        eighth: Expr<H>,
        name: String? = null,
        block: TessControlDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>, Expr<G>, Expr<H>) -> Expr<R>,
    ): Fn8<A, B, C, D, E, F, G, H, R> =
        functions.fn8(name, first, second, third, fourth, fifth, sixth, seventh, eighth) { a, b, c, d, e, f, g, h ->
            block(a, b, c, d, e, f, g, h)
        }
}

public class TessEvalDsl internal constructor(
    private val advance: (AuthoringAction) -> Unit,
    private val sink: StatementSink,
    private val functions: StageFunctions,
    private val owns: (Varying<*>) -> Boolean,
) {
    public val tessCoord: HighVec3 = Expr(
        Shape.Vector(ScalarKind.Float, Precision.High, 3),
        ExprNode.TessCoord,
    )

    public fun glIn(index: Int): HighVec4 {
        require(index >= 0) { "gl_in index must be non-negative, was $index" }
        return glInExpr(index.intLit)
    }

    public fun glIn(index: Expr<IntS>): HighVec4 = glInDynamic(index)

    public fun <T : ShType> Varying<T>.at(index: Int): Expr<T> {
        require(owns(this)) { "Varying \"${this.name}\" does not belong to this shader" }
        require(index >= 0) { "gl_in index must be non-negative, was $index" }
        return Expr(shape, ExprNode.VaryingAt(this, index.intLit))
    }

    public fun <T : ShType> Varying<T>.at(index: Expr<IntS>): Expr<T> {
        require(owns(this)) { "Varying \"${this.name}\" does not belong to this shader" }
        requireInt(index)
        return Expr(shape, ExprNode.VaryingAt(this, index))
    }

    public fun <T : ShType> local(initializer: Expr<T>, name: String? = null): LocalVar<T> =
        sink.declareLocal(initializer, name)

    public fun whenTrue(condition: Expr<BoolS>, body: () -> Unit) {
        sink.whenTrue(condition, body)
    }

    public fun <T : ShType> Varying<T>.set(value: Expr<T>) {
        require(owns(this)) { "Varying \"${this.name}\" does not belong to this shader" }
        require(value.shape == shape) { "Varying \"${this.name}\" expects $shape, was ${value.shape}" }
        sink.add(PrimitiveCommand.VaryingSet(this, value))
    }

    public fun glPosition(value: HighVec4) {
        advance(AuthoringAction.GlPosition)
        require(isFloatVec4(value.shape)) { "gl_Position expects a float vec4, was ${value.shape}" }
        sink.add(PrimitiveCommand.Position(value))
    }

    public fun repeat(count: Int, body: (Expr<IntS>) -> Unit) {
        advance(AuthoringAction.Repeat)
        sink.repeat(count, body)
    }

    public fun <R : ShType> fn(name: String? = null, block: TessEvalDsl.() -> Expr<R>): Fn0<R> =
        functions.fn0(name) { block() }

    public fun <A : ShType, R : ShType> fn(
        witness: Expr<A>,
        name: String? = null,
        block: TessEvalDsl.(Expr<A>) -> Expr<R>,
    ): Fn1<A, R> = functions.fn1(name, witness) { block(it) }

    public fun <A : ShType, B : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        name: String? = null,
        block: TessEvalDsl.(Expr<A>, Expr<B>) -> Expr<R>,
    ): Fn2<A, B, R> = functions.fn2(name, first, second) { left, right -> block(left, right) }

    public fun <A : ShType, B : ShType, C : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        name: String? = null,
        block: TessEvalDsl.(Expr<A>, Expr<B>, Expr<C>) -> Expr<R>,
    ): Fn3<A, B, C, R> = functions.fn3(name, first, second, third) { left, mid, right ->
        block(left, mid, right)
    }

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        name: String? = null,
        block: TessEvalDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>) -> Expr<R>,
    ): Fn4<A, B, C, D, R> = functions.fn4(name, first, second, third, fourth) { a, b, c, d ->
        block(a, b, c, d)
    }

    public fun <T : ShType> recur(arg: Expr<T>): Expr<T> = functions.recur(arg)

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        name: String? = null,
        block: TessEvalDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>) -> Expr<R>,
    ): Fn5<A, B, C, D, E, R> = functions.fn5(name, first, second, third, fourth, fifth) { a, b, c, d, e ->
        block(a, b, c, d, e)
    }

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, F : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        name: String? = null,
        block: TessEvalDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>) -> Expr<R>,
    ): Fn6<A, B, C, D, E, F, R> =
        functions.fn6(name, first, second, third, fourth, fifth, sixth) { a, b, c, d, e, f ->
            block(a, b, c, d, e, f)
        }

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, F : ShType, G : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        seventh: Expr<G>,
        name: String? = null,
        block: TessEvalDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>, Expr<G>) -> Expr<R>,
    ): Fn7<A, B, C, D, E, F, G, R> =
        functions.fn7(name, first, second, third, fourth, fifth, sixth, seventh) { a, b, c, d, e, f, g ->
            block(a, b, c, d, e, f, g)
        }

    public fun <
        A : ShType,
        B : ShType,
        C : ShType,
        D : ShType,
        E : ShType,
        F : ShType,
        G : ShType,
        H : ShType,
        R : ShType,
        > fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        seventh: Expr<G>,
        eighth: Expr<H>,
        name: String? = null,
        block: TessEvalDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>, Expr<G>, Expr<H>) -> Expr<R>,
    ): Fn8<A, B, C, D, E, F, G, H, R> =
        functions.fn8(name, first, second, third, fourth, fifth, sixth, seventh, eighth) { a, b, c, d, e, f, g, h ->
            block(a, b, c, d, e, f, g, h)
        }
}

internal const val GLSL_300 = 300
internal const val GLSL_320 = 320
internal const val MAX_GEOMETRY_VERTICES = 256
internal const val MAX_PATCH_VERTICES = 32

private fun glInChecked(index: Int, limit: Int, label: String): HighVec4 {
    require(index in 0 until limit) { "$label has $limit vertices, index was $index" }
    return glInExpr(index.intLit)
}

private fun glInDynamic(index: Expr<IntS>): HighVec4 {
    requireInt(index)
    return glInExpr(index)
}

private fun glInExpr(index: Expr<*>): HighVec4 = Expr(
    Shape.Vector(ScalarKind.Float, Precision.High, 4),
    ExprNode.GlIn(index),
)

private fun requireInt(index: Expr<*>) {
    require(index.shape == Shape.Scalar(ScalarKind.Int, null)) {
        "gl_in index must be an int, was ${index.shape}"
    }
}
