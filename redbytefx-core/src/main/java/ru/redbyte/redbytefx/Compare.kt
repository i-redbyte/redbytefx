package ru.redbyte.redbytefx

public infix fun <P : Prec> Expr<Flt<P>>.gt(other: Expr<Flt<P>>): Expr<BoolS> = compare(CompareOp.Gt, this, other)

public infix fun <P : Prec> Expr<Flt<P>>.lt(other: Expr<Flt<P>>): Expr<BoolS> = compare(CompareOp.Lt, this, other)

public infix fun <P : Prec> Expr<Flt<P>>.ge(other: Expr<Flt<P>>): Expr<BoolS> = compare(CompareOp.Ge, this, other)

public infix fun <P : Prec> Expr<Flt<P>>.le(other: Expr<Flt<P>>): Expr<BoolS> = compare(CompareOp.Le, this, other)

public infix fun <P : Prec> Expr<Flt<P>>.eq(other: Expr<Flt<P>>): Expr<BoolS> = compare(CompareOp.Eq, this, other)

public infix fun <P : Prec> Expr<Flt<P>>.ne(other: Expr<Flt<P>>): Expr<BoolS> = compare(CompareOp.Ne, this, other)

public fun <T : ShType> ifElse(condition: Expr<BoolS>, ifTrue: Expr<T>, ifFalse: Expr<T>): Expr<T> {
    require(condition.shape == boolScalar) { "ifElse condition must be a bool, was ${condition.shape}" }
    require(ifTrue.shape == ifFalse.shape) {
        "ifElse branches must share a shape, was ${ifTrue.shape} and ${ifFalse.shape}"
    }
    return Expr(ifTrue.shape, ExprNode.Select(condition, ifTrue, ifFalse))
}

private fun <P : Prec> compare(op: CompareOp, left: Expr<Flt<P>>, right: Expr<Flt<P>>): Expr<BoolS> {
    require(isFloatScalar(left.shape) && left.shape == right.shape) {
        "$op requires float scalars of one precision, was ${left.shape} and ${right.shape}"
    }
    return Expr(boolScalar, ExprNode.Compare(op, left, right))
}

private val boolScalar = Shape.Scalar(ScalarKind.Bool, null)

internal val CompareOp.symbol: String
    get() = when (this) {
        CompareOp.Gt -> ">"
        CompareOp.Lt -> "<"
        CompareOp.Ge -> ">="
        CompareOp.Le -> "<="
        CompareOp.Eq -> "=="
        CompareOp.Ne -> "!="
    }
