// FILE: Semigroup.kt

interface Semigroup<A> {
    fun A.combine(other: A): A
}

fun <A> duplicate(x: A, with semigroup: Semigroup<A>): A {
    return x.combine(x)
}

val result = <!UNABLE_TO_RESOLVE_IMPLICIT_PARAMETER!>duplicate<!>(2)
