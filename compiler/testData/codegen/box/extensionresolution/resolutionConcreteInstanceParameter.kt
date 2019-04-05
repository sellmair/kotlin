// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Semigroup.kt

package com.extensionresolution

interface Semigroup<A> {
    fun A.combine(b: A): A
}

extension internal object IntSemigroup : Semigroup<Int> {
    override fun Int.combine(b: Int): Int = this + b
}

internal fun duplicate(a: Int, with semigroup: IntSemigroup): Int = a.combine(a)

fun box(): String {
    return if (duplicate(2) == 4) {
        "OK"
    } else {
        "fail 1"
    }
}