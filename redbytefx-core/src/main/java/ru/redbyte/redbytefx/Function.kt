package ru.redbyte.redbytefx

/**
 * Callable handles for user functions declared with `fn { … }` inside a shader stage.
 *
 * [FragmentDsl.fn], [ShaderDsl.VertexDsl.fn], and other stage DSLs return [Fn0] … [Fn8]. The number
 * is the **parameter count**, not an arbitrary id. Witness expressions (`fn(witness) { … }`) fix each
 * parameter type at compile time. Call the handle with [invoke] to emit `name(args…)` in AGSL/GLSL.
 *
 * Functions cannot nest, recurse, or call across stages; see the language reference.
 * The body receiver is [FnDsl], not the surrounding stage, so stage builtins are not implicit.
 */
internal class Formal(
    val name: String,
    val shape: Shape,
)

internal class UserFunction(
    val name: String,
    val stage: AuthoringPlace,
    val parameters: List<Formal>,
    body: Expr<*>,
    result: Shape,
) {
    var body: Expr<*> = body
    var result: Shape = result
    var statements: List<PrimitiveCommand> = emptyList()
}

/** User function with no parameters (from `fn(name) { … }`). */
public class Fn0<R : ShType> internal constructor(
    private val function: UserFunction,
) {
    /** Emits a call to this function in generated shader source. */
    public operator fun invoke(): Expr<R> = call(emptyList())

    private fun call(args: List<Expr<*>>): Expr<R> =
        Expr(function.result, ExprNode.UserCall(function, args))
}

/** User function with one parameter (witness type [A]). */
public class Fn1<A : ShType, R : ShType> internal constructor(
    private val function: UserFunction,
) {
    /** Emits a call to this function in generated shader source. */
    public operator fun invoke(arg: Expr<A>): Expr<R> {
        require(arg.shape == function.parameters[0].shape) {
            "Function \"${function.name}\" expects ${function.parameters[0].shape}, was ${arg.shape}"
        }
        return Expr(function.result, ExprNode.UserCall(function, listOf(arg)))
    }
}

/** User function with two parameters. */
public class Fn2<A : ShType, B : ShType, R : ShType> internal constructor(
    private val function: UserFunction,
) {
    /** Emits a call to this function in generated shader source. */
    public operator fun invoke(first: Expr<A>, second: Expr<B>): Expr<R> {
        require(first.shape == function.parameters[0].shape) {
            "Function \"${function.name}\" expects ${function.parameters[0].shape}, was ${first.shape}"
        }
        require(second.shape == function.parameters[1].shape) {
            "Function \"${function.name}\" expects ${function.parameters[1].shape}, was ${second.shape}"
        }
        return Expr(function.result, ExprNode.UserCall(function, listOf(first, second)))
    }
}

/** User function with three parameters. */
public class Fn3<A : ShType, B : ShType, C : ShType, R : ShType> internal constructor(
    private val function: UserFunction,
) {
    /** Emits a call to this function in generated shader source. */
    public operator fun invoke(first: Expr<A>, second: Expr<B>, third: Expr<C>): Expr<R> {
        requireArgument(function, 0, first)
        requireArgument(function, 1, second)
        requireArgument(function, 2, third)
        return Expr(function.result, ExprNode.UserCall(function, listOf(first, second, third)))
    }
}

/** User function with four parameters. */
public class Fn4<A : ShType, B : ShType, C : ShType, D : ShType, R : ShType> internal constructor(
    private val function: UserFunction,
) {
    /** Emits a call to this function in generated shader source. */
    public operator fun invoke(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
    ): Expr<R> {
        requireArgument(function, 0, first)
        requireArgument(function, 1, second)
        requireArgument(function, 2, third)
        requireArgument(function, 3, fourth)
        return Expr(function.result, ExprNode.UserCall(function, listOf(first, second, third, fourth)))
    }
}

/** User function with five parameters. */
public class Fn5<A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, R : ShType> internal constructor(
    private val function: UserFunction,
) {
    /** Emits a call to this function in generated shader source. */
    public operator fun invoke(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
    ): Expr<R> {
        requireArgument(function, 0, first)
        requireArgument(function, 1, second)
        requireArgument(function, 2, third)
        requireArgument(function, 3, fourth)
        requireArgument(function, 4, fifth)
        return Expr(function.result, ExprNode.UserCall(function, listOf(first, second, third, fourth, fifth)))
    }
}

/** User function with six parameters. */
public class Fn6<
    A : ShType,
    B : ShType,
    C : ShType,
    D : ShType,
    E : ShType,
    F : ShType,
    R : ShType,
    > internal constructor(
    private val function: UserFunction,
) {
    /** Emits a call to this function in generated shader source. */
    public operator fun invoke(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
    ): Expr<R> {
        requireArgument(function, 0, first)
        requireArgument(function, 1, second)
        requireArgument(function, 2, third)
        requireArgument(function, 3, fourth)
        requireArgument(function, 4, fifth)
        requireArgument(function, 5, sixth)
        return Expr(
            function.result,
            ExprNode.UserCall(function, listOf(first, second, third, fourth, fifth, sixth)),
        )
    }
}

/** User function with seven parameters. */
public class Fn7<
    A : ShType,
    B : ShType,
    C : ShType,
    D : ShType,
    E : ShType,
    F : ShType,
    G : ShType,
    R : ShType,
    > internal constructor(
    private val function: UserFunction,
) {
    /** Emits a call to this function in generated shader source. */
    public operator fun invoke(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        seventh: Expr<G>,
    ): Expr<R> {
        for (index in 0..6) {
            requireArgument(function, index, listOf(first, second, third, fourth, fifth, sixth, seventh)[index])
        }
        return Expr(
            function.result,
            ExprNode.UserCall(function, listOf(first, second, third, fourth, fifth, sixth, seventh)),
        )
    }
}

/** User function with eight parameters. */
public class Fn8<
    A : ShType,
    B : ShType,
    C : ShType,
    D : ShType,
    E : ShType,
    F : ShType,
    G : ShType,
    H : ShType,
    R : ShType,
    > internal constructor(
    private val function: UserFunction,
) {
    /** Emits a call to this function in generated shader source. */
    public operator fun invoke(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        seventh: Expr<G>,
        eighth: Expr<H>,
    ): Expr<R> {
        val args = listOf(first, second, third, fourth, fifth, sixth, seventh, eighth)
        args.forEachIndexed { index, arg -> requireArgument(function, index, arg) }
        return Expr(function.result, ExprNode.UserCall(function, args))
    }
}

/**
 * Body of `fn { … }`.
 *
 * [let], [local], [whenTrue], [repeat], and [recur] are available here. Stage builtins
 * ([FragmentDsl.sample], [FragmentDsl.texture], [FragmentDsl.fragCoord], [FragmentDsl.resolution],
 * attributes, `gl_Position`) are not on this receiver. Qualify the stage
 * (`this@fragment.sample()`, `this@fragment.texture()`) when the `fn` was declared in that
 * fragment. A vertex or compute `fn` cannot capture them.
 */
@RedByteFxDsl
public class FnDsl internal constructor(
    private val advance: (AuthoringAction) -> Unit,
    private val functions: StageFunctions,
    private val sink: StatementSink,
) {
    public fun <T : ShType> local(initializer: Expr<T>, name: String? = null): LocalVar<T> =
        sink.declareLocal(initializer, name)

    public fun whenTrue(condition: Expr<BoolS>, body: () -> Unit) {
        sink.whenTrue(condition, body)
    }

    public fun repeat(count: Int, body: (Expr<IntS>) -> Unit) {
        advance(AuthoringAction.Repeat)
        sink.repeat(count, body)
    }

    public fun <T : ShType> recur(arg: Expr<T>): Expr<T> = functions.recur(arg)

    @JvmName("letValue")
    public fun <T : ShType> let(value: Expr<T>, name: String? = null): Expr<T> = value.let(name)

    public fun <T : ShType> Expr<T>.let(name: String? = null): Expr<T> {
        advance(AuthoringAction.Let)
        return Expr(shape, ExprNode.Local(name, this))
    }
}

internal class StageFunctions(
    private val advance: (AuthoringAction) -> Unit,
    private val parent: () -> AuthoringPlace,
    private val names: IdentifierAllocator,
    private val register: (UserFunction) -> Unit,
    private val sink: StatementSink,
) {
    private var index = 0
    private var defining: UserFunction? = null

    fun <T : ShType> recur(arg: Expr<T>): Expr<T> {
        val function = defining ?: throw IllegalArgumentException("recur is only valid inside a function")
        require(function.parameters.size == 1 && arg.shape == function.parameters[0].shape) {
            "recur expects the function parameter, was ${arg.shape}"
        }
        return Expr(function.result, ExprNode.UserCall(function, listOf(arg)))
    }

    fun <R : ShType> fn0(name: String?, body: () -> Expr<R>): Fn0<R> =
        Fn0(define(name, emptyList()) { body() })

    fun <A : ShType, R : ShType> fn1(
        name: String?,
        witness: Expr<A>,
        body: (Expr<A>) -> Expr<R>,
    ): Fn1<A, R> {
        val formal = Formal("p0", witness.shape)
        return Fn1(define(name, listOf(formal)) { body(Expr(formal.shape, ExprNode.Param(formal.name))) })
    }

    fun <A : ShType, B : ShType, R : ShType> fn2(
        name: String?,
        first: Expr<A>,
        second: Expr<B>,
        body: (Expr<A>, Expr<B>) -> Expr<R>,
    ): Fn2<A, B, R> {
        val formals = listOf(Formal("p0", first.shape), Formal("p1", second.shape))
        return Fn2(
            define(name, formals) {
                body(
                    Expr(formals[0].shape, ExprNode.Param(formals[0].name)),
                    Expr(formals[1].shape, ExprNode.Param(formals[1].name)),
                )
            },
        )
    }

    fun <A : ShType, B : ShType, C : ShType, R : ShType> fn3(
        name: String?,
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        body: (Expr<A>, Expr<B>, Expr<C>) -> Expr<R>,
    ): Fn3<A, B, C, R> {
        val formals = listOf(
            Formal("p0", first.shape),
            Formal("p1", second.shape),
            Formal("p2", third.shape),
        )
        return Fn3(
            define(name, formals) {
                body(param(formals[0]), param(formals[1]), param(formals[2]))
            },
        )
    }

    fun <A : ShType, B : ShType, C : ShType, D : ShType, R : ShType> fn4(
        name: String?,
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        body: (Expr<A>, Expr<B>, Expr<C>, Expr<D>) -> Expr<R>,
    ): Fn4<A, B, C, D, R> {
        val formals = listOf(
            Formal("p0", first.shape),
            Formal("p1", second.shape),
            Formal("p2", third.shape),
            Formal("p3", fourth.shape),
        )
        return Fn4(
            define(name, formals) {
                body(param(formals[0]), param(formals[1]), param(formals[2]), param(formals[3]))
            },
        )
    }

    fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, R : ShType> fn5(
        name: String?,
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        body: (Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>) -> Expr<R>,
    ): Fn5<A, B, C, D, E, R> {
        val formals = formals(first, second, third, fourth, fifth)
        return Fn5(
            define(name, formals) {
                body(param(formals[0]), param(formals[1]), param(formals[2]), param(formals[3]), param(formals[4]))
            },
        )
    }

    fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, F : ShType, R : ShType> fn6(
        name: String?,
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        body: (Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>) -> Expr<R>,
    ): Fn6<A, B, C, D, E, F, R> {
        val formals = formals(first, second, third, fourth, fifth, sixth)
        return Fn6(
            define(name, formals) {
                body(
                    param(formals[0]),
                    param(formals[1]),
                    param(formals[2]),
                    param(formals[3]),
                    param(formals[4]),
                    param(formals[5]),
                )
            },
        )
    }

    fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, F : ShType, G : ShType, R : ShType> fn7(
        name: String?,
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        seventh: Expr<G>,
        body: (Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>, Expr<G>) -> Expr<R>,
    ): Fn7<A, B, C, D, E, F, G, R> {
        val formals = formals(first, second, third, fourth, fifth, sixth, seventh)
        return Fn7(
            define(name, formals) {
                body(
                    param(formals[0]),
                    param(formals[1]),
                    param(formals[2]),
                    param(formals[3]),
                    param(formals[4]),
                    param(formals[5]),
                    param(formals[6]),
                )
            },
        )
    }

    fun <
        A : ShType,
        B : ShType,
        C : ShType,
        D : ShType,
        E : ShType,
        F : ShType,
        G : ShType,
        H : ShType,
        R : ShType,
        > fn8(
        name: String?,
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        seventh: Expr<G>,
        eighth: Expr<H>,
        body: (Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>, Expr<G>, Expr<H>) -> Expr<R>,
    ): Fn8<A, B, C, D, E, F, G, H, R> {
        val formals = formals(first, second, third, fourth, fifth, sixth, seventh, eighth)
        return Fn8(
            define(name, formals) {
                body(
                    param(formals[0]),
                    param(formals[1]),
                    param(formals[2]),
                    param(formals[3]),
                    param(formals[4]),
                    param(formals[5]),
                    param(formals[6]),
                    param(formals[7]),
                )
            },
        )
    }

    private fun <R : ShType> define(
        name: String?,
        parameters: List<Formal>,
        body: () -> Expr<R>,
    ): UserFunction {
        advance(AuthoringAction.EnterFunction)
        try {
            val provisional = parameters.firstOrNull()?.shape
                ?: Shape.Scalar(ScalarKind.Float, Precision.High)
            val function = UserFunction(
                name = names.reserve(functionBase(name, index)),
                stage = parent(),
                parameters = parameters,
                body = Expr<ShType>(provisional, ExprNode.Param(parameters.firstOrNull()?.name ?: "p0")),
                result = provisional,
            )
            index += 1
            val previous = defining
            defining = function
            val isolated = try {
                sink.isolate(body)
            } finally {
                defining = previous
            }
            advance(AuthoringAction.Return)
            function.statements = isolated.first
            function.body = isolated.second
            function.result = isolated.second.shape
            register(function)
            return function
        } finally {
            advance(AuthoringAction.LeaveFunction)
        }
    }
}

private fun formals(vararg witnesses: Expr<*>): List<Formal> =
    witnesses.mapIndexed { index, witness -> Formal("p$index", witness.shape) }

private fun <T : ShType> param(formal: Formal): Expr<T> = Expr(formal.shape, ExprNode.Param(formal.name))

private fun requireArgument(function: UserFunction, index: Int, arg: Expr<*>) {
    val expected = function.parameters[index].shape
    require(arg.shape == expected) {
        "Function \"${function.name}\" expects $expected, was ${arg.shape}"
    }
}

private fun functionBase(name: String?, index: Int): String {
    if (name.isNullOrBlank()) return "fn$index"
    return sanitizeSuggestedIdentifier(name, "fn")
}
