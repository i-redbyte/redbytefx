package ru.redbyte.redbytefx.compose

/** Counts host invalidations the way the redbyteFx modifier receives them. */
internal class InvalidationCounter {
    var count: Int = 0
}

internal fun FxController.invalidationCounter(): InvalidationCounter =
    InvalidationCounter().also { counter -> addRuntimeInvalidationListener { counter.count += 1 } }
