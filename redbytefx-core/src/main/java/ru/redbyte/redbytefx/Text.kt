package ru.redbyte.redbytefx

internal fun formatFloat(value: Float): String {
    require(value.isFinite()) { "Float literal must be finite, was $value" }
    val text = value.toString()
    return if ('.' in text || 'e' in text || 'E' in text) text else "$text.0"
}

internal fun sanitizeIdentifier(raw: String, prefix: String): String {
    val cleaned = sanitizeSuggestedIdentifier(raw, leadingDigitPrefix = prefix)
    val normalized = if (cleaned.firstOrNull()?.isDigit() == true) {
        "${prefix}${cleaned}"
    } else {
        cleaned
    }
    return if (normalized.startsWith(prefix)) normalized else prefix + normalized
}

internal fun sanitizeSuggestedIdentifier(raw: String, leadingDigitPrefix: String): String {
    val cleaned = buildString {
        raw.forEachIndexed { index, ch ->
            when {
                ch == '_' || !ch.isLetterOrDigit() -> {
                    if (isNotEmpty() && last() != '_') append('_')
                }
                ch.isUpperCase() -> {
                    val previous = raw.getOrNull(index - 1)
                    val next = raw.getOrNull(index + 1)
                    val boundaryFromLowerOrDigit = previous?.let { it.isLowerCase() || it.isDigit() } == true
                    val boundaryBeforeWordTail = previous?.isUpperCase() == true && next?.isLowerCase() == true
                    if (isNotEmpty() && last() != '_' && (boundaryFromLowerOrDigit || boundaryBeforeWordTail)) {
                        append('_')
                    }
                    append(ch.lowercaseChar())
                }
                else -> append(ch.lowercaseChar())
            }
        }
    }.trim('_').ifBlank { "value" }
    return if (cleaned.firstOrNull()?.isDigit() == true) {
        "$leadingDigitPrefix$cleaned"
    } else {
        cleaned
    }
}
