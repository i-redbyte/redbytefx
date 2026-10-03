package ru.redbyte.redbytefx

internal enum class SwizzleAlphabet {
    Xyzw,
    Rgba,
}

internal sealed interface SwizzleState {
    data object Start : SwizzleState

    data class InSet(val alphabet: SwizzleAlphabet, val length: Int) : SwizzleState {
        init {
            require(length in 1..4) { "Swizzle length must be 1..4" }
        }
    }

    data object Error : SwizzleState
}

internal enum class SwizzleCode {
    Empty,
    TooLong,
    MixedSets,
    UnknownSymbol,
}

internal data class SwizzleMove(
    val state: SwizzleState,
    val component: Int?,
    val code: SwizzleCode?,
)

internal sealed interface SwizzleOutcome {
    data class Accepted(val components: List<Int>, val alphabet: SwizzleAlphabet) : SwizzleOutcome

    data class Rejected(val code: SwizzleCode) : SwizzleOutcome
}

internal fun swizzleMove(state: SwizzleState, symbol: Char): SwizzleMove {
    if (state == SwizzleState.Error) return SwizzleMove(SwizzleState.Error, null, null)
    val alphabet = alphabetOf(symbol) ?: return unknown()
    val component = componentOf(symbol) ?: return unknown()
    return when (state) {
        SwizzleState.Start -> SwizzleMove(SwizzleState.InSet(alphabet, 1), component, null)
        is SwizzleState.InSet -> moveInSet(state, alphabet, component)
        SwizzleState.Error -> error("Error is handled before the symbol is read")
    }
}

internal fun readSwizzle(mask: String): SwizzleOutcome {
    if (mask.isEmpty()) return SwizzleOutcome.Rejected(SwizzleCode.Empty)
    var state: SwizzleState = SwizzleState.Start
    val components = ArrayList<Int>(mask.length)
    for (symbol in mask) {
        val move = swizzleMove(state, symbol)
        val code = move.code
        if (code != null) return SwizzleOutcome.Rejected(code)
        val component = move.component ?: return SwizzleOutcome.Rejected(SwizzleCode.UnknownSymbol)
        components += component
        state = move.state
    }
    val finished = state as? SwizzleState.InSet
        ?: return SwizzleOutcome.Rejected(SwizzleCode.UnknownSymbol)
    return SwizzleOutcome.Accepted(components, finished.alphabet)
}

private fun moveInSet(
    state: SwizzleState.InSet,
    alphabet: SwizzleAlphabet,
    component: Int,
): SwizzleMove {
    if (alphabet != state.alphabet) {
        return SwizzleMove(SwizzleState.Error, null, SwizzleCode.MixedSets)
    }
    if (state.length == 4) {
        return SwizzleMove(SwizzleState.Error, null, SwizzleCode.TooLong)
    }
    return SwizzleMove(SwizzleState.InSet(alphabet, state.length + 1), component, null)
}

private fun alphabetOf(symbol: Char): SwizzleAlphabet? = when (symbol) {
    'x', 'y', 'z', 'w' -> SwizzleAlphabet.Xyzw
    'r', 'g', 'b', 'a' -> SwizzleAlphabet.Rgba
    else -> null
}

private fun componentOf(symbol: Char): Int? = when (symbol) {
    'x', 'r' -> 0
    'y', 'g' -> 1
    'z', 'b' -> 2
    'w', 'a' -> 3
    else -> null
}

private fun unknown(): SwizzleMove =
    SwizzleMove(SwizzleState.Error, null, SwizzleCode.UnknownSymbol)
