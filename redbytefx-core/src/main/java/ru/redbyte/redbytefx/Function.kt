package ru.redbyte.redbytefx

internal class Formal(
    val name: String,
    val shape: Shape,
)

internal class UserFunction(
    val name: String,
    val stage: AuthoringPlace,
    val parameters: List<Formal>,
    val body: Expr<*>,
    val result: Shape,
)

public class Fn0<R : ShType> internal constructor(
    private val function: UserFunction,
) {
    public operator fun invoke(): Expr<R> = call(emptyList())

    private fun call(args: List<Expr<*>>): Expr<R> =
        Expr(function.result, ExprNode.UserCall(function, args))
}

public class Fn1<A : ShType, R : ShType> internal constructor(
    private val function: UserFunction,
) {
    public operator fun invoke(arg: Expr<A>): Expr<R> {
        require(arg.shape == function.parameters[0].shape) {
            "Function \"${function.name}\" expects ${function.parameters[0].shape}, was ${arg.shape}"
        }
        return Expr(function.result, ExprNode.UserCall(function, listOf(arg)))
    }
}

public class Fn2<A : ShType, B : ShType, R : ShType> internal constructor(
    private val function: UserFunction,
) {
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

public class Fn3<A : ShType, B : ShType, C : ShType, R : ShType> internal constructor(
    private val function: UserFunction,
) {
    public operator fun invoke(first: Expr<A>, second: Expr<B>, third: Expr<C>): Expr<R> {
        requireArgument(function, 0, first)
        requireArgument(function, 1, second)
        requireArgument(function, 2, third)
        return Expr(function.result, ExprNode.UserCall(function, listOf(first, second, third)))
    }
}

public class Fn4<A : ShType, B : ShType, C : ShType, D : ShType, R : ShType> internal constructor(
    private val function: UserFunction,
) {
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

internal class StageFunctions(
    private val advance: (AuthoringAction) -> Unit,
    private val parent: () -> AuthoringPlace,
    private val names: IdentifierAllocator,
    private val register: (UserFunction) -> Unit,
) {
    private var index = 0

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

    private fun <R : ShType> define(
        name: String?,
        parameters: List<Formal>,
        body: () -> Expr<R>,
    ): UserFunction {
        advance(AuthoringAction.EnterFunction)
        try {
            val result = body()
            advance(AuthoringAction.Return)
            val function = UserFunction(
                name = names.reserve(functionBase(name, index)),
                stage = parent(),
                parameters = parameters,
                body = result,
                result = result.shape,
            )
            index += 1
            register(function)
            return function
        } finally {
            advance(AuthoringAction.LeaveFunction)
        }
    }
}

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
