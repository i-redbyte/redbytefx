package ru.redbyte.redbytefx

/**
 * Marker interface for scalar float expressions in the RedByteFX DSL.
 */
public interface FloatExpr

/**
 * Marker interface for boolean expressions in the RedByteFX DSL.
 */
public interface BoolExpr

/**
 * Marker interface for `float2` expressions in the RedByteFX DSL.
 */
public interface Float2Expr

/**
 * Marker interface for `float3` expressions in the RedByteFX DSL.
 */
public interface Float3Expr

/**
 * Marker interface for `float4` expressions in the RedByteFX DSL.
 */
public interface Float4Expr

/**
 * Marker interface for color expressions represented as `half4`.
 */
public interface ColorExpr

/**
 * Root receiver for the RedByteFX authoring DSL.
 *
 * Instances of this type are created by [redbytefx]. Use its helpers to declare uniforms, build
 * expressions, sample the input content, and return the final output color.
 */
public class FxDsl internal constructor(
    private val defaults: LinkedHashMap<FxParam, DefaultValue>,
    private val functions: MutableList<UserFunctionDefinition>,
    private val usedFunctionNames: MutableSet<String>
) {
    /**
     * Current fragment coordinate in pixels.
     */
    public val fragCoord: Float2Expr = FRAG_COORD_EXPR

    /**
     * Current content resolution in pixels.
     */
    public val resolution: Float2Expr = RESOLUTION_EXPR

    /**
     * Declares a scalar float uniform.
     */
    public fun uniformFloat(
        default: Float = 0f,
        name: String? = null
    ): FxParam.Float {
        val param = FxParam.Float(name)
        defaults[param] = DefaultValue.F(default)
        return param
    }

    /**
     * Declares a scalar float uniform intended to be driven by elapsed time in seconds.
     */
    public fun uniformTime(
        default: Float = 0f,
        name: String? = null
    ): FxParam.Float = uniformFloat(default, name ?: "time")

    /**
     * Declares a `float2` uniform.
     */
    public fun uniformFloat2(
        x: Float = 0f,
        y: Float = 0f,
        name: String? = null
    ): FxParam.Float2 {
        val param = FxParam.Float2(name)
        defaults[param] = DefaultValue.F2(x, y)
        return param
    }

    /**
     * Declares a `float3` uniform.
     */
    public fun uniformFloat3(
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f,
        name: String? = null
    ): FxParam.Float3 {
        val param = FxParam.Float3(name)
        defaults[param] = DefaultValue.F3(x, y, z)
        return param
    }

    /**
     * Declares a `float4` uniform.
     */
    public fun uniformFloat4(
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f,
        w: Float = 0f,
        name: String? = null
    ): FxParam.Float4 {
        val param = FxParam.Float4(name)
        defaults[param] = DefaultValue.F4(x, y, z, w)
        return param
    }

    /**
     * Stores a scalar expression in a generated AGSL local variable.
     */
    public fun let(value: FloatExpr, name: String? = null): FloatExpr =
        localFloatExpr(name, value)

    /**
     * Stores a boolean expression in a generated AGSL local variable.
     */
    public fun let(value: BoolExpr, name: String? = null): BoolExpr =
        localBoolExpr(name, value)

    /**
     * Stores a `float2` expression in a generated AGSL local variable.
     */
    public fun let(value: Float2Expr, name: String? = null): Float2Expr =
        localFloat2Expr(name, value)

    /**
     * Stores a `float3` expression in a generated AGSL local variable.
     */
    public fun let(value: Float3Expr, name: String? = null): Float3Expr =
        localFloat3Expr(name, value)

    /**
     * Stores a `float4` expression in a generated AGSL local variable.
     */
    public fun let(value: Float4Expr, name: String? = null): Float4Expr =
        localFloat4Expr(name, value)

    /**
     * Stores a color expression in a generated AGSL local variable.
     */
    public fun let(value: ColorExpr, name: String? = null): ColorExpr =
        localColorExpr(name, value)

    /**
     * Declares a reusable zero-argument AGSL helper function.
     */
    public fun <R> fn(
        name: String? = null,
        returns: FxValueType<R>,
        block: FxDsl.() -> R
    ): FxFunction0<R> {
        val functionName = nextFunctionName(name)
        val body = block()
        functions += UserFunctionDefinition(
            name = functionName,
            returnType = returns,
            parameters = emptyList(),
            body = body as Any
        )
        return FxFunction0(functionName, returns)
    }

    /**
     * Declares a reusable one-argument AGSL helper function.
     */
    public fun <A1, R> fn(
        name: String? = null,
        arg1: FxValueType<A1>,
        returns: FxValueType<R>,
        block: FxDsl.(A1) -> R
    ): FxFunction1<A1, R> {
        val functionName = nextFunctionName(name)
        val p0Name = "p0"
        val p0 = parameterExpr(arg1, p0Name)
        val body = block(p0)
        functions += UserFunctionDefinition(
            name = functionName,
            returnType = returns,
            parameters = listOf(UserFunctionParameter(p0Name, arg1)),
            body = body as Any
        )
        return FxFunction1(functionName, returns)
    }

    /**
     * Declares a reusable two-argument AGSL helper function.
     */
    public fun <A1, A2, R> fn(
        name: String? = null,
        arg1: FxValueType<A1>,
        arg2: FxValueType<A2>,
        returns: FxValueType<R>,
        block: FxDsl.(A1, A2) -> R
    ): FxFunction2<A1, A2, R> {
        val functionName = nextFunctionName(name)
        val p0Name = "p0"
        val p1Name = "p1"
        val p0 = parameterExpr(arg1, p0Name)
        val p1 = parameterExpr(arg2, p1Name)
        val body = block(p0, p1)
        functions += UserFunctionDefinition(
            name = functionName,
            returnType = returns,
            parameters = listOf(
                UserFunctionParameter(p0Name, arg1),
                UserFunctionParameter(p1Name, arg2)
            ),
            body = body as Any
        )
        return FxFunction2(functionName, returns)
    }

    /**
     * Returns the center of the current resolution in pixels.
     */
    public fun center(): Float2Expr = resolution * 0.5f

    /**
     * Samples the input content at [coord], clamping the coordinate to valid bounds when needed.
     */
    public fun sample(coord: Float2Expr = fragCoord): ColorExpr =
        colorExpr { ctx -> "rb_sample(${emit(coord, ctx)})" }

    /**
     * Samples the input content at [coord] without any coordinate clamping.
     */
    public fun sampleUnclamped(coord: Float2Expr = fragCoord): ColorExpr =
        colorExpr { ctx -> "$RB_INPUT_UNIFORM.eval(${emit(coord, ctx)})" }

    /**
     * Mirrors the content horizontally around the center line.
     */
    public fun flipX(coord: Float2Expr = fragCoord): Float2Expr = flipX(coord, 1f)

    /**
     * Mirrors the content horizontally around the center line using a scalar literal amount.
     */
    public fun flipX(coord: Float2Expr = fragCoord, amount: Float): Float2Expr = flipX(coord, floatLiteral(amount))

    /**
     * Mirrors the content horizontally around the center line.
     *
     * `amount = 0` keeps the original coordinate, `amount = 1` performs a full flip.
     */
    public fun flipX(coord: Float2Expr = fragCoord, amount: FloatExpr): Float2Expr {
        val t = saturate(amount)
        return float2(mix(coord.x, resolution.x - coord.x, t), coord.y)
    }

    /**
     * Mirrors the content vertically around the center line.
     */
    public fun flipY(coord: Float2Expr = fragCoord): Float2Expr = flipY(coord, 1f)

    /**
     * Mirrors the content vertically around the center line using a scalar literal amount.
     */
    public fun flipY(coord: Float2Expr = fragCoord, amount: Float): Float2Expr = flipY(coord, floatLiteral(amount))

    /**
     * Mirrors the content vertically around the center line.
     *
     * `amount = 0` keeps the original coordinate, `amount = 1` performs a full flip.
     */
    public fun flipY(coord: Float2Expr = fragCoord, amount: FloatExpr): Float2Expr {
        val t = saturate(amount)
        return float2(coord.x, mix(coord.y, resolution.y - coord.y, t))
    }

    /**
     * Reflects one horizontal half of the content into the other.
     */
    public fun mirrorX(
        coord: Float2Expr = fragCoord,
        from: MirrorXFrom = MirrorXFrom.Right
    ): Float2Expr = mirrorX(coord, 1f, from)

    /**
     * Reflects one horizontal half of the content into the other using a literal amount.
     */
    public fun mirrorX(
        coord: Float2Expr = fragCoord,
        amount: Float,
        from: MirrorXFrom = MirrorXFrom.Right
    ): Float2Expr = mirrorX(coord, floatLiteral(amount), floatLiteral(from.shaderValue))

    /**
     * Reflects one horizontal half of the content into the other.
     */
    public fun mirrorX(
        coord: Float2Expr = fragCoord,
        amount: FloatExpr,
        from: MirrorXFrom = MirrorXFrom.Right
    ): Float2Expr = mirrorX(coord, amount, floatLiteral(from.shaderValue))

    /**
     * Reflects one horizontal half of the content into the other.
     *
     * The [from] value is interpreted as `0` for left and `1` for right.
     */
    public fun mirrorX(
        coord: Float2Expr = fragCoord,
        amount: FloatExpr,
        from: FloatExpr
    ): Float2Expr {
        val t = saturate(amount)
        val direction = from * 2f - 1f
        val centerX = resolution.x * 0.5f
        val mirroredX = centerX + direction * abs(coord.x - centerX)
        return float2(mix(coord.x, mirroredX, t), coord.y)
    }

    /**
     * Reflects one vertical half of the content into the other.
     */
    public fun mirrorY(
        coord: Float2Expr = fragCoord,
        from: MirrorYFrom = MirrorYFrom.Bottom
    ): Float2Expr = mirrorY(coord, 1f, from)

    /**
     * Reflects one vertical half of the content into the other using a literal amount.
     */
    public fun mirrorY(
        coord: Float2Expr = fragCoord,
        amount: Float,
        from: MirrorYFrom = MirrorYFrom.Bottom
    ): Float2Expr = mirrorY(coord, floatLiteral(amount), floatLiteral(from.shaderValue))

    /**
     * Reflects one vertical half of the content into the other.
     */
    public fun mirrorY(
        coord: Float2Expr = fragCoord,
        amount: FloatExpr,
        from: MirrorYFrom = MirrorYFrom.Bottom
    ): Float2Expr = mirrorY(coord, amount, floatLiteral(from.shaderValue))

    /**
     * Reflects one vertical half of the content into the other.
     *
     * The [from] value is interpreted as `0` for top and `1` for bottom.
     */
    public fun mirrorY(
        coord: Float2Expr = fragCoord,
        amount: FloatExpr,
        from: FloatExpr
    ): Float2Expr {
        val t = saturate(amount)
        val direction = from * 2f - 1f
        val centerY = resolution.y * 0.5f
        val mirroredY = centerY + direction * abs(coord.y - centerY)
        return float2(coord.x, mix(coord.y, mirroredY, t))
    }

    /**
     * Rotates [coord] around the center of the current resolution.
     */
    public fun rotate(
        coord: Float2Expr = fragCoord,
        degrees: Float
    ): Float2Expr = rotate(coord, floatLiteral(degrees), center())

    /**
     * Rotates [coord] around [pivot].
     */
    public fun rotate(
        coord: Float2Expr = fragCoord,
        degrees: FloatExpr,
        pivot: Float2Expr = center()
    ): Float2Expr {
        val delta = coord - pivot
        val angle = radians(degrees)
        val s = sin(angle)
        val c = cos(angle)
        return pivot + float2(
            c * delta.x - s * delta.y,
            s * delta.x + c * delta.y
        )
    }

    /**
     * Scales [coord] around [pivot] using literal per-axis values.
     */
    public fun scale(
        coord: Float2Expr = fragCoord,
        sx: Float,
        sy: Float,
        pivot: Float2Expr = center()
    ): Float2Expr = scale(coord, float2(sx, sy), pivot)

    /**
     * Scales [coord] around [pivot].
     */
    public fun scale(
        coord: Float2Expr = fragCoord,
        scale: Float2Expr,
        pivot: Float2Expr = center()
    ): Float2Expr {
        val safeScale = float2(
            max(scale.x, 0.0001f),
            max(scale.y, 0.0001f)
        )
        return pivot + (coord - pivot) / safeScale
    }

    /**
     * Translates [coord] by subtracting [delta].
     */
    public fun offset(
        coord: Float2Expr = fragCoord,
        delta: Float2Expr
    ): Float2Expr = coord - delta

    private fun nextFunctionName(suggestedName: String?): String {
        val base = suggestedName
            ?.takeIf { it.isNotBlank() }
            ?.let { raw ->
                val cleaned = buildString {
                    raw.forEach { ch ->
                        when {
                            ch.isLetterOrDigit() -> append(ch.lowercaseChar())
                            ch == '_' -> append(ch)
                            else -> append('_')
                        }
                    }
                }.trim('_').ifBlank { "fn_${functions.size}" }

                if (cleaned.firstOrNull()?.isDigit() == true) "fn_$cleaned" else cleaned
            }
            ?: "fn_${functions.size}"

        var candidate = base
        var suffix = 1
        while (!usedFunctionNames.add(candidate)) {
            candidate = "${base}_${suffix++}"
        }
        return candidate
    }
}

/**
 * Source side for [FxDsl.mirrorX].
 *
 * @property shaderValue Numeric value used when this side is stored in a shader uniform, where
 * `0` maps to the left side and `1` maps to the right side.
 */
public enum class MirrorXFrom(public val shaderValue: Float) {
    Left(0f),
    Right(1f)
}

/**
 * Source side for [FxDsl.mirrorY].
 *
 * @property shaderValue Numeric value used when this side is stored in a shader uniform, where
 * `0` maps to the top side and `1` maps to the bottom side.
 */
public enum class MirrorYFrom(public val shaderValue: Float) {
    Top(0f),
    Bottom(1f)
}

/**
 * Public description of a value type used by [FxDsl.fn].
 */
public sealed interface FxValueType<T> {
    /**
     * AGSL type name emitted by the compiler.
     */
    public val agslType: String
}

/**
 * Public value-type token for [BoolExpr].
 */
public data object BoolType : FxValueType<BoolExpr> {
    /** AGSL type emitted for [BoolExpr]. */
    public override val agslType: String = "bool"
}

/**
 * Public value-type token for [FloatExpr].
 */
public data object FloatType : FxValueType<FloatExpr> {
    /** AGSL type emitted for [FloatExpr]. */
    public override val agslType: String = "float"
}

/**
 * Public value-type token for [Float2Expr].
 */
public data object Float2Type : FxValueType<Float2Expr> {
    /** AGSL type emitted for [Float2Expr]. */
    public override val agslType: String = "float2"
}

/**
 * Public value-type token for [Float3Expr].
 */
public data object Float3Type : FxValueType<Float3Expr> {
    /** AGSL type emitted for [Float3Expr]. */
    public override val agslType: String = "float3"
}

/**
 * Public value-type token for [Float4Expr].
 */
public data object Float4Type : FxValueType<Float4Expr> {
    /** AGSL type emitted for [Float4Expr]. */
    public override val agslType: String = "float4"
}

/**
 * Public value-type token for [ColorExpr].
 */
public data object ColorType : FxValueType<ColorExpr> {
    /** AGSL type emitted for [ColorExpr]. */
    public override val agslType: String = "half4"
}

/**
 * Handle for a generated zero-argument AGSL helper function.
 */
public class FxFunction0<R> internal constructor(
    private val name: String,
    private val returns: FxValueType<R>
) {
    /**
     * Creates a call expression for the underlying AGSL helper.
     */
    public operator fun invoke(): R = callFunction(returns, name, emptyList())
}

/**
 * Handle for a generated one-argument AGSL helper function.
 */
public class FxFunction1<A1, R> internal constructor(
    private val name: String,
    private val returns: FxValueType<R>
) {
    /**
     * Creates a call expression for the underlying AGSL helper.
     */
    public operator fun invoke(arg1: A1): R = callFunction(returns, name, listOf(arg1 as Any))
}

/**
 * Handle for a generated two-argument AGSL helper function.
 */
public class FxFunction2<A1, A2, R> internal constructor(
    private val name: String,
    private val returns: FxValueType<R>
) {
    /**
     * Creates a call expression for the underlying AGSL helper.
     */
    public operator fun invoke(arg1: A1, arg2: A2): R =
        callFunction(returns, name, listOf(arg1 as Any, arg2 as Any))
}

/**
 * Calls a one-argument helper with a scalar literal.
 */
public operator fun <R> FxFunction1<FloatExpr, R>.invoke(arg1: Float): R =
    this(floatLiteral(arg1))

/**
 * Calls a two-argument helper using a scalar literal for the first argument.
 */
public operator fun <A2, R> FxFunction2<FloatExpr, A2, R>.invoke(arg1: Float, arg2: A2): R =
    this(floatLiteral(arg1), arg2)

/**
 * Calls a two-argument helper using a scalar literal for the second argument.
 */
public operator fun <A1, R> FxFunction2<A1, FloatExpr, R>.invoke(arg1: A1, arg2: Float): R =
    this(arg1, floatLiteral(arg2))

/**
 * Calls a two-argument helper using scalar literals for both arguments.
 */
public operator fun <R> FxFunction2<FloatExpr, FloatExpr, R>.invoke(arg1: Float, arg2: Float): R =
    this(floatLiteral(arg1), floatLiteral(arg2))

internal data class UserFunctionParameter(
    val name: String,
    val type: FxValueType<*>
)

internal data class UserFunctionDefinition(
    val name: String,
    val returnType: FxValueType<*>,
    val parameters: List<UserFunctionParameter>,
    val body: Any
)
