package ru.redbyte.redbytefx

/**
 * Allocates unique AGSL identifiers by reserving `base`, then `base_1`, `base_2`, … on collision.
 *
 * Used for uniforms, `let(...)` locals, and `fn(...)` names. Collisions are resolved silently by
 * appending `_1`, `_2`, … to the base name.
 */
internal class IdentifierAllocator(
    initialOccupied: Set<String> = emptySet()
) {
    private val occupied = initialOccupied.toMutableSet()

    fun reserve(base: String): String {
        var candidate = base
        var suffix = 1
        while (!occupied.add(candidate)) {
            candidate = "${base}_${suffix++}"
        }
        return candidate
    }

    fun snapshot(): Set<String> = occupied.toSet()
}
