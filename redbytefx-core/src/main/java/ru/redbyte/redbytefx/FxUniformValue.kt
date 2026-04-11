package ru.redbyte.redbytefx

/**
 * Bitwise float equality for runtime uniform updates.
 *
 * Matches [FxInstance] setters: distinguishes signed zero, and treats each `NaN` payload as its own
 * stable value (so repeated writes of the same non-canonical NaN do not churn the runtime).
 */
public fun sameFloatUniformValue(a: Float, b: Float): Boolean = a.toBits() == b.toBits()
