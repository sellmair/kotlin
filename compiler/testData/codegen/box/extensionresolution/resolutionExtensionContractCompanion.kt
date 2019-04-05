// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Semigroup.kt

package com.extensionresolution

interface Semigroup<A> {
    fun A.combine(b: A): A

    companion object {
        extension object IntSemigroup : Semigroup<Int> {
            override fun Int.combine(b: Int): Int = this + b
        }
    }
}

fun <A> duplicate(a: A, with semigroup: Semigroup<A>) : A = a.combine(a)

fun box(): String {
    val x = duplicate(2)
    return if (x == 4) {
        "OK"
    } else {
        "fail 1"
    }
}
