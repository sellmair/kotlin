// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Semigroup.kt

package com.extensionresolution

interface Semigroup<A> {
    fun A.combine(b: A): A
}

data class Wrapper<A>(val value: A)

extension internal class WrapperSemigroup<A>(with val semigroup: Semigroup<A>) : Semigroup<Wrapper<A>> {
    override fun Wrapper<A>.combine(b: Wrapper<A>): Wrapper<A> = Wrapper(this.value.combine(b.value))
}

extension internal object IntSemigroup : Semigroup<Int> {
    override fun Int.combine(b: Int): Int = this + b
}

fun <A> duplicate(a: A, with semigroup: Semigroup<A>) : A = a.combine(a)

fun box(): String {
    val x = duplicate(Wrapper(2))
    return if (x == Wrapper(4)) {
        "OK"
    } else {
        "fail 1"
    }
}
