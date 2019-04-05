// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Semigroup.kt

package com.extensionresolution

interface Semigroup<A> {
    fun A.combine(b: A): A
}

// FILE: IntSemigroup.kt

package net.consumer

import com.extensionresolution.Semigroup

extension internal object IntSemigroup: Semigroup<Int> {
    override fun Int.combine(b: Int): Int = this + b
}

// FILE: Box.kt

package net.consumer

import com.extensionresolution.Semigroup

fun <A> duplicate(a: A, with semigroup: Semigroup<A>) : A = a.combine(a)

fun box(): String {
    val x = duplicate(2)

    return if (x == 4) {
        "OK"
    } else {
        "fail 1"
    }
}
