package ru.redbyte.redbytefx

/**
 * Public description of a value type used by [FxDsl.fn] and [FxDsl.fnN].
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
 * Typed parameter bundle for [FxDsl.fnN] bodies. Use [at] to read each formal parameter expression.
 */
public class FxParameterPack internal constructor(private val values: List<Any>) {
    /**
     * Returns the parameter expression at [index] (`p0`, `p1`, … in generated AGSL).
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T> at(index: Int): T {
        require(index in values.indices) {
            "Parameter index $index out of bounds (size ${values.size})"
        }
        return values[index] as T
    }
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
 * Handle for a generated three-argument AGSL helper function.
 */
public class FxFunction3<A1, A2, A3, R> internal constructor(
    private val name: String,
    private val returns: FxValueType<R>
) {
    /**
     * Creates a call expression for the underlying AGSL helper.
     */
    public operator fun invoke(arg1: A1, arg2: A2, arg3: A3): R =
        callFunction(returns, name, listOf(arg1 as Any, arg2 as Any, arg3 as Any))
}

/**
 * Handle for a generated four-argument AGSL helper function.
 */
public class FxFunction4<A1, A2, A3, A4, R> internal constructor(
    private val name: String,
    private val returns: FxValueType<R>
) {
    /**
     * Creates a call expression for the underlying AGSL helper.
     */
    public operator fun invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4): R =
        callFunction(
            returns,
            name,
            listOf(arg1 as Any, arg2 as Any, arg3 as Any, arg4 as Any)
        )
}

/**
 * Handle for a generated AGSL helper with arity five or higher (see [FxDsl.fnN]).
 */
public class FxFunctionN<R> internal constructor(
    private val name: String,
    private val returns: FxValueType<R>,
    private val arity: Int
) {
    /**
     * Creates a call expression for the underlying AGSL helper.
     */
    public operator fun invoke(vararg args: Any): R {
        require(args.size == arity) {
            "Expected $arity arguments for $name, got ${args.size}"
        }
        return callFunction(returns, name, args.toList())
    }
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

/**
 * Calls a three-argument helper using a scalar literal for the first argument.
 */
public operator fun <A2, A3, R> FxFunction3<FloatExpr, A2, A3, R>.invoke(
    arg1: Float,
    arg2: A2,
    arg3: A3
): R = this(floatLiteral(arg1), arg2, arg3)

/**
 * Calls a four-argument helper using a scalar literal for the first argument.
 */
public operator fun <A2, A3, A4, R> FxFunction4<FloatExpr, A2, A3, A4, R>.invoke(
    arg1: Float,
    arg2: A2,
    arg3: A3,
    arg4: A4
): R = this(floatLiteral(arg1), arg2, arg3, arg4)

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
