// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Semigroup.kt

package com.extensionresolution

interface Semigroup<A> {
    fun A.combine(b: A): A
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

// FILE: IntSemigroup.kt

package com.extensionresolution.instances

import com.extensionresolution.Semigroup

extension class IntSemigroup : Semigroup<Int> {
    override fun Int.combine(b: Int): Int = this + b
}