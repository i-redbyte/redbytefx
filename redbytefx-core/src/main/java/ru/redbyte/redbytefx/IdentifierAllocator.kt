package ru.redbyte.redbytefx

/**
 * Allocates unique AGSL identifiers by reserving `base`, then `base_1`, `base_2`, … on collision.
 *
 * Used for uniforms, `let(...)` locals, and `fn(...)` names. Collisions are resolved silently by
 * appending `_1`, `_2`, … to the base name.
 */
internal class IdentifierAllocator(
    initialOccupied: Set<String> = emptySet(),
) {
    private val occupied = initialOccupied.toMutableSet()
    private val nextSuffix = mutableMapOf<String, Int>()

    fun reserve(base: String): String {
        if (occupied.add(base)) return base
        var suffix = nextSuffix[base] ?: 1
        var candidate = "${base}_$suffix"
        while (!occupied.add(candidate)) {
            suffix += 1
            candidate = "${base}_$suffix"
        }
        nextSuffix[base] = suffix + 1
        return candidate
    }

    fun snapshot(): Set<String> = occupied.toSet()
}
