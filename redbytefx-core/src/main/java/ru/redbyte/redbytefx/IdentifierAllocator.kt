package ru.redbyte.redbytefx

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
