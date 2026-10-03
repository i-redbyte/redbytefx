package ru.redbyte.redbytefx

public class LocalVar<T : ShType> internal constructor(
    public val expr: Expr<T>,
    private val slot: LocalSlot,
    private val sink: StatementSink,
) {
    public fun set(value: Expr<T>) {
        require(value.shape == slot.shape) { "Local expects ${slot.shape}, was ${value.shape}" }
        sink.add(PrimitiveCommand.LocalSet(slot, value))
    }
}

private enum class CommandFrame {
    Repeat,
    When,
}

internal class StatementSink {
    var stage: MutableList<PrimitiveCommand> = mutableListOf()
    private val stack = ArrayDeque<MutableList<PrimitiveCommand>>()
    private val frames = ArrayDeque<CommandFrame>()
    private var redirectDepth = 0

    fun capturing(): Boolean = redirectDepth > 0 || stack.isNotEmpty()

    fun add(command: PrimitiveCommand) {
        (stack.lastOrNull() ?: stage) += command
    }

    fun repeat(count: Int, body: (Expr<IntS>) -> Unit) {
        require(count in 1..MAX_REPEAT_COUNT) {
            "repeat count must be 1..$MAX_REPEAT_COUNT, was $count"
        }
        val indexName = if (stack.isEmpty()) "i" else "i${stack.size}"
        val inner = mutableListOf<PrimitiveCommand>()
        stack.addLast(inner)
        frames.addLast(CommandFrame.Repeat)
        try {
            val index = Expr<IntS>(Shape.Scalar(ScalarKind.Int, null), ExprNode.Param(indexName))
            body(index)
        } finally {
            frames.removeLast()
            stack.removeLast()
        }
        add(PrimitiveCommand.Repeat(count, indexName, inner.toList()))
    }

    fun whenTrue(condition: Expr<*>, body: () -> Unit) {
        require(condition.shape == Shape.Scalar(ScalarKind.Bool, null)) {
            "whenTrue condition must be a bool, was ${condition.shape}"
        }
        val inner = mutableListOf<PrimitiveCommand>()
        stack.addLast(inner)
        frames.addLast(CommandFrame.When)
        try {
            body()
        } finally {
            frames.removeLast()
            stack.removeLast()
        }
        add(PrimitiveCommand.When(condition, inner.toList()))
    }

    fun <T : ShType> declareLocal(initializer: Expr<T>, name: String?): LocalVar<T> {
        if (CommandFrame.Repeat in frames) {
            throw ProgramException(
                ProgramCode.LocalInsideRepeat,
                "local is not allowed inside repeat",
            )
        }
        require(isLocalShape(initializer.shape)) {
            "local requires a float, a float vector, or an int, was ${initializer.shape}"
        }
        val slot = LocalSlot(initializer.shape, name)
        add(PrimitiveCommand.LocalSet(slot, initializer))
        return LocalVar(Expr(initializer.shape, ExprNode.SlotRef(slot)), slot, this)
    }

    fun <T> isolate(body: () -> T): Pair<List<PrimitiveCommand>, T> {
        val savedStage = stage
        val savedStack = ArrayDeque(stack)
        val savedFrames = ArrayDeque(frames)
        val local = mutableListOf<PrimitiveCommand>()
        stage = local
        stack.clear()
        frames.clear()
        redirectDepth += 1
        try {
            val result = body()
            return local.toList() to result
        } finally {
            redirectDepth -= 1
            stage = savedStage
            stack.clear()
            stack.addAll(savedStack)
            frames.clear()
            frames.addAll(savedFrames)
        }
    }
}

private fun isLocalShape(shape: Shape): Boolean = when (shape) {
    is Shape.Scalar -> shape.kind == ScalarKind.Float || shape.kind == ScalarKind.Int
    is Shape.Vector -> shape.kind == ScalarKind.Float
    else -> false
}

internal fun hasEmit(commands: List<PrimitiveCommand>): Boolean = commands.any { command ->
    command == PrimitiveCommand.EmitVertex ||
        (command is PrimitiveCommand.Repeat && hasEmit(command.body)) ||
        (command is PrimitiveCommand.When && hasEmit(command.body))
}

internal fun hasPosition(commands: List<PrimitiveCommand>): Boolean = commands.any { command ->
    command is PrimitiveCommand.Position ||
        (command is PrimitiveCommand.Repeat && hasPosition(command.body)) ||
        (command is PrimitiveCommand.When && hasPosition(command.body))
}

internal fun writesGlPosition(commands: List<PrimitiveCommand>): Boolean = commands.any { command ->
    when (command) {
        is PrimitiveCommand.Position, PrimitiveCommand.PassPosition -> true
        is PrimitiveCommand.Repeat -> writesGlPosition(command.body)
        is PrimitiveCommand.When -> writesGlPosition(command.body)
        else -> false
    }
}

internal fun requirePositionBeforeEmit(commands: List<PrimitiveCommand>) {
    var armed = false
    fun walk(list: List<PrimitiveCommand>) {
        for (command in list) {
            when (command) {
                is PrimitiveCommand.Position -> armed = true
                PrimitiveCommand.EmitVertex -> {
                    if (!armed) {
                        throw ProgramException(
                            ProgramCode.EmitVertexWithoutPosition,
                            "EmitVertex requires a preceding gl_Position",
                        )
                    }
                }
                is PrimitiveCommand.Repeat -> walk(command.body)
                is PrimitiveCommand.When -> walk(command.body)
                else -> Unit
            }
        }
    }
    walk(commands)
}

internal fun commandExprs(commands: List<PrimitiveCommand>): List<Expr<*>> {
    val exprs = mutableListOf<Expr<*>>()
    fun walk(list: List<PrimitiveCommand>) {
        for (command in list) {
            when (command) {
                is PrimitiveCommand.Position -> exprs += command.value
                is PrimitiveCommand.OuterLevel -> exprs += command.value
                is PrimitiveCommand.InnerLevel -> exprs += command.value
                is PrimitiveCommand.VaryingSet -> exprs += command.value
                is PrimitiveCommand.Store -> {
                    exprs += command.target
                    exprs += command.value
                }
                is PrimitiveCommand.Repeat -> walk(command.body)
                is PrimitiveCommand.When -> {
                    exprs += command.condition
                    walk(command.body)
                }
                is PrimitiveCommand.LocalSet -> exprs += command.value
                is PrimitiveCommand.DiscardIf -> exprs += command.condition
                PrimitiveCommand.EmitVertex, PrimitiveCommand.EndPrimitive,
                PrimitiveCommand.PassPosition, PrimitiveCommand.Discard, PrimitiveCommand.Barrier,
                -> Unit
            }
        }
    }
    walk(commands)
    return exprs
}

internal fun writtenVaryings(commands: List<PrimitiveCommand>): Set<Varying<*>> {
    val written = linkedSetOf<Varying<*>>()
    fun walk(list: List<PrimitiveCommand>) {
        for (command in list) {
            when (command) {
                is PrimitiveCommand.VaryingSet -> written += command.varying
                is PrimitiveCommand.Repeat -> walk(command.body)
                is PrimitiveCommand.When -> walk(command.body)
                else -> Unit
            }
        }
    }
    walk(commands)
    return written
}
