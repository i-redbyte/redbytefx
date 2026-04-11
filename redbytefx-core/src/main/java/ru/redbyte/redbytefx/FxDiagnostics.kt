package ru.redbyte.redbytefx

/**
 * Severity of a [FxDiagnostic]. The compiler currently reports **errors** only.
 */
public enum class FxDiagnosticSeverity {
    ERROR,
}

/**
 * Stable machine-readable code for filtering and tooling. Messages may still change between
 * library versions; codes are intended to stay comparable.
 */
public enum class FxDiagnosticCode {
    FN_RETURN_TYPE_MISMATCH,
    UNSUPPORTED_DSL_IMPLEMENTATION,
    UNSUPPORTED_EMIT_ANY,
    NON_FINITE_FLOAT_LITERAL,
    MISSING_UNIFORM_BINDING,
}

/**
 * One structured shader/authoring diagnostic. Prefer reading [fullText] (or [FxDiagnosticException])
 * for the full human-readable text, including [hint] when present.
 */
public data class FxDiagnostic(
    public val severity: FxDiagnosticSeverity,
    public val code: FxDiagnosticCode,
    public val message: String,
    public val hint: String? = null,
) {
    public fun fullText(): String =
        if (hint.isNullOrBlank()) {
            message
        } else {
            "$message $hint"
        }
}

/**
 * Thrown when RedByteFX cannot compile a shader or cannot apply a uniform binding. Extends
 * [IllegalStateException] so existing `catch (IllegalStateException)` paths keep working.
 *
 * Inspect [diagnostics] for structured [FxDiagnostic] entries (code, message, hint).
 */
public class FxDiagnosticException(
    public val diagnostics: List<FxDiagnostic>,
) : IllegalStateException(formatDiagnostics(diagnostics)) {
    init {
        require(diagnostics.isNotEmpty()) { "diagnostics must not be empty" }
    }

    public constructor(diagnostic: FxDiagnostic) : this(listOf(diagnostic))

    public val primary: FxDiagnostic
        get() = diagnostics.first()
}

private fun formatDiagnostics(diagnostics: List<FxDiagnostic>): String =
    diagnostics.joinToString(separator = "\n") { it.fullText() }

internal fun compileFail(diagnostic: FxDiagnostic): Nothing {
    throw FxDiagnosticException(diagnostic)
}

internal fun unsupportedDslImplementationDiagnostic(
    typeLabel: String,
    expr: Any,
): FxDiagnostic = FxDiagnostic(
    severity = FxDiagnosticSeverity.ERROR,
    code = FxDiagnosticCode.UNSUPPORTED_DSL_IMPLEMENTATION,
    message = unsupportedDslImplementationMessage(typeLabel, expr),
    hint = null,
)

internal fun unsupportedExpressionArgumentDiagnostic(expr: Any): FxDiagnostic = FxDiagnostic(
    severity = FxDiagnosticSeverity.ERROR,
    code = FxDiagnosticCode.UNSUPPORTED_EMIT_ANY,
    message = unsupportedExpressionArgumentBaseMessage(expr),
    hint = unsupportedExpressionArgumentHint(expr),
)

internal fun fnReturnTypeMismatchDiagnostic(
    functionName: String,
    returnType: FxValueType<*>,
    body: Any,
): FxDiagnostic = FxDiagnostic(
    severity = FxDiagnosticSeverity.ERROR,
    code = FxDiagnosticCode.FN_RETURN_TYPE_MISMATCH,
    message = fnBodyReturnTypeMismatchMessage(functionName, returnType, body),
    hint = null,
)

internal fun missingUniformBindingDiagnostic(
    typeLabel: String,
    debugName: String?,
): FxDiagnostic = FxDiagnostic(
    severity = FxDiagnosticSeverity.ERROR,
    code = FxDiagnosticCode.MISSING_UNIFORM_BINDING,
    message = missingUniformBindingMessage(typeLabel, debugName),
    hint = null,
)

internal fun nonFiniteFloatLiteralDiagnostic(value: Float): FxDiagnostic = FxDiagnostic(
    severity = FxDiagnosticSeverity.ERROR,
    code = FxDiagnosticCode.NON_FINITE_FLOAT_LITERAL,
    message = nonFiniteFloatLiteralMessage(value),
    hint = null,
)
