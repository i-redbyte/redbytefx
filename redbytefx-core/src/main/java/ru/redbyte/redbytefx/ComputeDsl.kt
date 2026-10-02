package ru.redbyte.redbytefx

public class ComputeDsl internal constructor(
    private val advance: (AuthoringAction) -> Unit,
    private val record: (Expr<*>, Expr<*>) -> Unit,
) {
    public fun <T : ShType> Expr<T>.store(value: Expr<T>) {
        advance(AuthoringAction.StorageWrite)
        record(this, value)
    }
}
