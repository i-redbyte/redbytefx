package ru.redbyte.redbytefx

/**
 * Compute shader stage DSL ([ShaderTarget.Gles31]).
 *
 * [globalId], [localId], and [workGroupId] mirror `gl_GlobalInvocationID` and friends.
 * [store] writes storage buffers; [barrier] and `shared*` arrays coordinate workgroup memory.
 */
public class ComputeDsl internal constructor(
    private val advance: (AuthoringAction) -> Unit,
    private val sink: StatementSink,
    private val functions: StageFunctions,
    private val names: IdentifierAllocator,
    private val shared: MutableList<BlockMember>,
    private val checkStore: (Expr<*>, Expr<*>) -> Unit,
) {
    public val globalId: Expr<Vec3<IntS>> = invocation(InvocationKind.Global)

    public val localId: Expr<Vec3<IntS>> = invocation(InvocationKind.Local)

    public val workGroupId: Expr<Vec3<IntS>> = invocation(InvocationKind.WorkGroup)

    public fun <T : ShType> Expr<T>.store(value: Expr<T>) {
        advance(AuthoringAction.StorageWrite)
        checkStore(this, value)
        sink.add(PrimitiveCommand.Store(this, value))
    }

    public fun sharedFloat(name: String, size: Int): StorageArray<Flt<High>> =
        sharedArray(name, Shape.Scalar(ScalarKind.Float, Precision.High), size)

    public fun sharedVec2(name: String, size: Int): StorageArray<Vec2<Flt<High>>> =
        sharedArray(name, Shape.Vector(ScalarKind.Float, Precision.High, 2), size)

    public fun sharedVec3(name: String, size: Int): StorageArray<Vec3<Flt<High>>> =
        sharedArray(name, Shape.Vector(ScalarKind.Float, Precision.High, 3), size)

    public fun sharedVec4(name: String, size: Int): StorageArray<Vec4<Flt<High>>> =
        sharedArray(name, Shape.Vector(ScalarKind.Float, Precision.High, 4), size)

    public fun barrier() {
        advance(AuthoringAction.Barrier)
        sink.add(PrimitiveCommand.Barrier)
    }

    public fun repeat(count: Int, body: (Expr<IntS>) -> Unit) {
        advance(AuthoringAction.Repeat)
        sink.repeat(count, body)
    }

    public fun <T : ShType> local(initializer: Expr<T>, name: String? = null): LocalVar<T> =
        sink.declareLocal(initializer, name)

    public fun whenTrue(condition: Expr<BoolS>, body: () -> Unit) {
        sink.whenTrue(condition, body)
    }

    public fun <R : ShType> fn(name: String? = null, block: ComputeDsl.() -> Expr<R>): Fn0<R> =
        functions.fn0(name) { block() }

    public fun <A : ShType, R : ShType> fn(
        witness: Expr<A>,
        name: String? = null,
        block: ComputeDsl.(Expr<A>) -> Expr<R>,
    ): Fn1<A, R> = functions.fn1(name, witness) { block(it) }

    public fun <A : ShType, B : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        name: String? = null,
        block: ComputeDsl.(Expr<A>, Expr<B>) -> Expr<R>,
    ): Fn2<A, B, R> = functions.fn2(name, first, second) { left, right -> block(left, right) }

    public fun <A : ShType, B : ShType, C : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        name: String? = null,
        block: ComputeDsl.(Expr<A>, Expr<B>, Expr<C>) -> Expr<R>,
    ): Fn3<A, B, C, R> = functions.fn3(name, first, second, third) { left, mid, right ->
        block(left, mid, right)
    }

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        name: String? = null,
        block: ComputeDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>) -> Expr<R>,
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
        block: ComputeDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>) -> Expr<R>,
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
        block: ComputeDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>) -> Expr<R>,
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
        block: ComputeDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>, Expr<G>) -> Expr<R>,
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
        block: ComputeDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>, Expr<G>, Expr<H>) -> Expr<R>,
    ): Fn8<A, B, C, D, E, F, G, H, R> =
        functions.fn8(name, first, second, third, fourth, fifth, sixth, seventh, eighth) { a, b, c, d, e, f, g, h ->
            block(a, b, c, d, e, f, g, h)
        }

    private fun <T : ShType> sharedArray(name: String, shape: Shape, size: Int): StorageArray<T> {
        advance(AuthoringAction.DeclareShared)
        require(size > 0) { "Shared array size must be positive, was $size" }
        require(name.isNotBlank()) { "Shared array name must not be blank" }
        val memberName = names.reserve(sanitizeSuggestedIdentifier(name, "s"))
        val member = BlockMember("", memberName, shape, size, shared = true)
        shared += member
        return StorageArray(member)
    }
}

private fun invocation(kind: InvocationKind): Expr<Vec3<IntS>> = Expr(
    Shape.Vector(ScalarKind.Int, null, 3),
    ExprNode.Invocation(kind),
)
