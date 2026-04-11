package ru.redbyte.redbytefx

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

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
    private val functionNameAllocator: IdentifierAllocator
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
     *
     * [name] is optional. When omitted, RedByteFX generates a deterministic internal name for the
     * compiled shader. Supplying a name is mainly useful for debugging and for keeping generated
     * AGSL readable.
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
     *
     * [name] is optional. When omitted, RedByteFX generates a deterministic internal name for the
     * compiled shader.
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
     *
     * [name] is optional. When omitted, RedByteFX generates a deterministic internal name for the
     * compiled shader.
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
     *
     * [name] is optional. When omitted, RedByteFX generates a deterministic internal name for the
     * compiled shader.
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
     * Declares a scalar float uniform and derives its debug name from the delegated Kotlin
     * property name.
     *
     * The generated shader identifier is normalized into readable snake_case AGSL style.
     *
     * Example:
     *
     * ```kotlin
     * val amount by autoUniformFloat(0.5f)
     * ```
     */
    public fun autoUniformFloat(
        default: Float = 0f
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, FxParam.Float>> =
        autoNamedUniform { propertyName -> uniformFloat(default, propertyName) }

    /**
     * Declares a time uniform and derives its debug name from the delegated Kotlin property name.
     *
     * The generated shader identifier is normalized into readable snake_case AGSL style.
     */
    public fun autoUniformTime(
        default: Float = 0f
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, FxParam.Float>> =
        autoNamedUniform { propertyName -> uniformTime(default, propertyName) }

    /**
     * Declares a `float2` uniform and derives its debug name from the delegated Kotlin property
     * name.
     *
     * The generated shader identifier is normalized into readable snake_case AGSL style.
     */
    public fun autoUniformFloat2(
        x: Float = 0f,
        y: Float = 0f
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, FxParam.Float2>> =
        autoNamedUniform { propertyName -> uniformFloat2(x, y, propertyName) }

    /**
     * Declares a `float3` uniform and derives its debug name from the delegated Kotlin property
     * name.
     *
     * The generated shader identifier is normalized into readable snake_case AGSL style.
     */
    public fun autoUniformFloat3(
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, FxParam.Float3>> =
        autoNamedUniform { propertyName -> uniformFloat3(x, y, z, propertyName) }

    /**
     * Declares a `float4` uniform and derives its debug name from the delegated Kotlin property
     * name.
     *
     * The generated shader identifier is normalized into readable snake_case AGSL style.
     */
    public fun autoUniformFloat4(
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f,
        w: Float = 0f
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, FxParam.Float4>> =
        autoNamedUniform { propertyName -> uniformFloat4(x, y, z, w, propertyName) }

    private fun <T> autoNamedUniform(
        factory: (String) -> T
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, T>> =
        PropertyDelegateProvider { _, property ->
            val value = factory(property.name)
            object : ReadOnlyProperty<Any?, T> {
                override fun getValue(thisRef: Any?, property: KProperty<*>): T = value
            }
        }

    /**
     * Stores a scalar expression in a generated AGSL local variable.
     *
     * When [name] is provided, it is normalized into a readable AGSL identifier and suffixed if a
     * previous local already used the same generated name.
     */
    public fun let(value: FloatExpr, name: String? = null): FloatExpr =
        localFloatExpr(name, value)

    /**
     * Stores a boolean expression in a generated AGSL local variable.
     *
     * When [name] is provided, it is normalized into a readable AGSL identifier and suffixed if a
     * previous local already used the same generated name.
     */
    public fun let(value: BoolExpr, name: String? = null): BoolExpr =
        localBoolExpr(name, value)

    /**
     * Stores a `float2` expression in a generated AGSL local variable.
     *
     * When [name] is provided, it is normalized into a readable AGSL identifier and suffixed if a
     * previous local already used the same generated name.
     */
    public fun let(value: Float2Expr, name: String? = null): Float2Expr =
        localFloat2Expr(name, value)

    /**
     * Stores a `float3` expression in a generated AGSL local variable.
     *
     * When [name] is provided, it is normalized into a readable AGSL identifier and suffixed if a
     * previous local already used the same generated name.
     */
    public fun let(value: Float3Expr, name: String? = null): Float3Expr =
        localFloat3Expr(name, value)

    /**
     * Stores a `float4` expression in a generated AGSL local variable.
     *
     * When [name] is provided, it is normalized into a readable AGSL identifier and suffixed if a
     * previous local already used the same generated name.
     */
    public fun let(value: Float4Expr, name: String? = null): Float4Expr =
        localFloat4Expr(name, value)

    /**
     * Stores a color expression in a generated AGSL local variable.
     *
     * When [name] is provided, it is normalized into a readable AGSL identifier and suffixed if a
     * previous local already used the same generated name.
     */
    public fun let(value: ColorExpr, name: String? = null): ColorExpr =
        localColorExpr(name, value)

    /**
     * Declares a reusable zero-argument AGSL helper function.
     *
     * This is the DSL equivalent of extracting a named helper from inline shader code when the
     * generated AGSL should still show a readable function boundary. Suggested names are
     * normalized into readable AGSL identifiers and suffixed on collision. Built-in AGSL names
     * such as `mix`, `sin`, and `main` are treated as reserved and are never emitted directly for
     * user helpers.
     */
    public fun <R> fn(
        name: String? = null,
        returns: FxValueType<R>,
        block: FxDsl.() -> R
    ): FxFunction0<R> {
        val functionName = nextFunctionName(name)
        val body = block()
        registerFunction(
            name = functionName,
            returnType = returns,
            parameters = emptyList(),
            body = body as Any
        )
        return FxFunction0(functionName, returns)
    }

    /**
     * Declares a reusable one-argument AGSL helper function.
     *
     * This maps naturally from hand-written AGSL helpers such as `float foo(float x) { ... }`.
     * Suggested names are normalized into readable AGSL identifiers and suffixed on collision.
     * Built-in AGSL names such as `mix`, `sin`, and `main` are treated as reserved and are never
     * emitted directly for user helpers.
     */
    public fun <A1, R> fn(
        name: String? = null,
        arg1: FxValueType<A1>,
        returns: FxValueType<R>,
        block: FxDsl.(A1) -> R
    ): FxFunction1<A1, R> {
        val functionName = nextFunctionName(name)
        val p0Name = parameterName(0)
        val p0 = parameterExpr(arg1, p0Name)
        val body = block(p0)
        registerFunction(
            name = functionName,
            returnType = returns,
            parameters = listOf(UserFunctionParameter(p0Name, arg1)),
            body = body as Any
        )
        return FxFunction1(functionName, returns)
    }

    /**
     * Declares a reusable two-argument AGSL helper function.
     *
     * Prefer this when the original shader already has a small named helper and keeping that
     * function visible is clearer than inlining the math into one large expression tree. Suggested
     * names are normalized into readable AGSL identifiers and suffixed on collision. Built-in
     * AGSL names such as `mix`, `sin`, and `main` are treated as reserved and are never emitted
     * directly for user helpers.
     */
    public fun <A1, A2, R> fn(
        name: String? = null,
        arg1: FxValueType<A1>,
        arg2: FxValueType<A2>,
        returns: FxValueType<R>,
        block: FxDsl.(A1, A2) -> R
    ): FxFunction2<A1, A2, R> {
        val functionName = nextFunctionName(name)
        val p0Name = parameterName(0)
        val p1Name = parameterName(1)
        val p0 = parameterExpr(arg1, p0Name)
        val p1 = parameterExpr(arg2, p1Name)
        val body = block(p0, p1)
        registerFunction(
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
     * Declares a reusable three-argument AGSL helper function.
     *
     * Suggested names are normalized into readable AGSL identifiers and suffixed on collision.
     * Built-in AGSL names such as `mix`, `sin`, and `main` are treated as reserved and are never
     * emitted directly for user helpers.
     */
    public fun <A1, A2, A3, R> fn(
        name: String? = null,
        arg1: FxValueType<A1>,
        arg2: FxValueType<A2>,
        arg3: FxValueType<A3>,
        returns: FxValueType<R>,
        block: FxDsl.(A1, A2, A3) -> R
    ): FxFunction3<A1, A2, A3, R> {
        val functionName = nextFunctionName(name)
        val p0Name = parameterName(0)
        val p1Name = parameterName(1)
        val p2Name = parameterName(2)
        val p0 = parameterExpr(arg1, p0Name)
        val p1 = parameterExpr(arg2, p1Name)
        val p2 = parameterExpr(arg3, p2Name)
        val body = block(p0, p1, p2)
        registerFunction(
            name = functionName,
            returnType = returns,
            parameters = listOf(
                UserFunctionParameter(p0Name, arg1),
                UserFunctionParameter(p1Name, arg2),
                UserFunctionParameter(p2Name, arg3)
            ),
            body = body as Any
        )
        return FxFunction3(functionName, returns)
    }

    /**
     * Declares a reusable four-argument AGSL helper function.
     *
     * Suggested names are normalized into readable AGSL identifiers and suffixed on collision.
     * Built-in AGSL names such as `mix`, `sin`, and `main` are treated as reserved and are never
     * emitted directly for user helpers.
     */
    public fun <A1, A2, A3, A4, R> fn(
        name: String? = null,
        arg1: FxValueType<A1>,
        arg2: FxValueType<A2>,
        arg3: FxValueType<A3>,
        arg4: FxValueType<A4>,
        returns: FxValueType<R>,
        block: FxDsl.(A1, A2, A3, A4) -> R
    ): FxFunction4<A1, A2, A3, A4, R> {
        val functionName = nextFunctionName(name)
        val p0Name = parameterName(0)
        val p1Name = parameterName(1)
        val p2Name = parameterName(2)
        val p3Name = parameterName(3)
        val p0 = parameterExpr(arg1, p0Name)
        val p1 = parameterExpr(arg2, p1Name)
        val p2 = parameterExpr(arg3, p2Name)
        val p3 = parameterExpr(arg4, p3Name)
        val body = block(p0, p1, p2, p3)
        registerFunction(
            name = functionName,
            returnType = returns,
            parameters = listOf(
                UserFunctionParameter(p0Name, arg1),
                UserFunctionParameter(p1Name, arg2),
                UserFunctionParameter(p2Name, arg3),
                UserFunctionParameter(p3Name, arg4)
            ),
            body = body as Any
        )
        return FxFunction4(functionName, returns)
    }

    /**
     * Declares a reusable AGSL helper with **five or more** formal parameters.
     *
     * Prefer the typed [fn] overloads for arity 0–4 when you can; use [fnN] when you need a longer
     * parameter list without adding another fixed-arity overload. The body receives a
     * [FxParameterPack]: call [FxParameterPack.at] with the zero-based index to read each
     * parameter expression (`p0`, `p1`, … in generated AGSL).
     */
    public fun <R> fnN(
        name: String? = null,
        returns: FxValueType<R>,
        vararg parameters: FxValueType<*>,
        block: FxDsl.(FxParameterPack) -> R
    ): FxFunctionN<R> {
        require(parameters.isNotEmpty()) {
            "fnN requires at least one parameter; use fn { } for zero-arg helpers."
        }
        val functionName = nextFunctionName(name)
        val exprs = parameters.mapIndexed { index, type ->
            parameterExpr(type, parameterName(index)) as Any
        }
        val pack = FxParameterPack(exprs)
        val body = block(pack)
        registerFunction(
            name = functionName,
            returnType = returns,
            parameters = parameters.mapIndexed { index, type ->
                UserFunctionParameter(parameterName(index), type)
            },
            body = body as Any
        )
        return FxFunctionN(functionName, returns, parameters.size)
    }

    /**
     * Returns the center of the current resolution in pixels.
     */
    public fun center(): Float2Expr = resolution * 0.5f

    /**
     * Samples the input content at [coord], clamping the coordinate to valid bounds when needed.
     *
     * [coord] is interpreted in **pixel / sample space** (same units as [fragCoord] and
     * [resolution]). For re-sampling from **normalized UV** `[0,1]²`, use **`sampleUv(uv)`** from
     * `:redbytefx-stdlib`, which expands to a sample-space coordinate internally.
     *
     * If UV is only used for masks or gradients while the base pixel read stays at [fragCoord],
     * keep this [sample] for the base layer and use UV expressions only for mask math — you do not
     * always need `sampleUv`. See **`docs/agsl-vs-redbytefx.md`** (`sample` vs `sampleUv` table).
     */
    public fun sample(coord: Float2Expr = fragCoord): ColorExpr =
        colorExpr { ctx -> "rb_sample(${emit(coord, ctx)})" }

    /**
     * Samples the input content at [coord] without any coordinate clamping.
     *
     * This is mainly for deliberate out-of-bounds sampling or edge-behavior experiments. Most
     * shaders should use [sample] or stdlib `sampleUv(...)` instead.
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
            ?.let { sanitizeSuggestedIdentifier(it, leadingDigitPrefix = "fn_") }
            ?: "fn_${functions.size}"

        return functionNameAllocator.reserve(base)
    }

    private fun parameterName(index: Int): String = "p$index"

    private fun registerFunction(
        name: String,
        returnType: FxValueType<*>,
        parameters: List<UserFunctionParameter>,
        body: Any
    ) {
        functions += UserFunctionDefinition(
            name = name,
            returnType = returnType,
            parameters = parameters,
            body = body
        )
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
