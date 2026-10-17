package ru.redbyte.redbytefx

internal fun formatFloat(value: Float): String {
    require(value.isFinite()) { "Float literal must be finite, was $value" }
    val text = value.toString()
    return if ('.' in text || 'e' in text || 'E' in text) text else "$text.0"
}

internal fun spelledUniformName(name: String?, names: IdentifierAllocator): String {
    if (name == RB_RESOLUTION_UNIFORM) return RB_RESOLUTION_UNIFORM
    return names.reserve(sanitizeIdentifier(name ?: "value", "u_"))
}
