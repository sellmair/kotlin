// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Semigroup.kt

package com.extensionresolution

interface Semigroup<A> {
    fun A.combine(b: A): A
}

fun <A> duplicate(a: A, with semigroup: Semigroup<A>): A = a.combine(a)

fun <A> fourTimes(a: A, with semigroup: Semigroup<A>): A = duplicate(duplicate(a))

fun box(): String {
    val x = fourTimes(2, object: Semigroup<Int>{
        override fun Int.combine(b: Int): Int = this + b
    })

    return if (x == 8) {
        "OK"
    } else {
        "fail 1"
    }
}
