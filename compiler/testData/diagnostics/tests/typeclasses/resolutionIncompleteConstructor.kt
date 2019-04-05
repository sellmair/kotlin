// FILE: Semigroup.kt

interface Semigroup<A> {
    fun A.combine(other: A): A
}

data class Wrapper<A>(val wrapped: A) {
    companion object {
        extension class WrapperSemigroup<A>(with val SG: Semigroup<A>): Semigroup<Wrapper<A>> {
            override fun Wrapper<A>.combine(other: Wrapper<A>): Wrapper<A> = Wrapper(wrapped.combine(other.wrapped))
        }
    }
}

fun <A> duplicate(x: A, with semigroup: Semigroup<A>): A {
    return x.combine(x)
}

val result = <!UNABLE_TO_RESOLVE_IMPLICIT_PARAMETER!>duplicate<!>(Wrapper(2))
