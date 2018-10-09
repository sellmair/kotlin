// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Semigroup.kt

package com.typeclasses

interface Semigroup<A> {
    fun A.combine(b: A): A

    companion object {
        extension object IntSemigroup : Semigroup<Int> {
            override fun Int.combine(b: Int): Int = this + b
        }
    }
}

// FILE: Eq.kt

package com.typeclasses

interface Eq<A> {
    fun A.eqv(b: A): Boolean
    fun A.neqv(b: A): Boolean = !this.eqv(b)
}

// FILE: IntEq.kt

package com.typeclasses.instances

import com.typeclasses.Eq

extension object IntEq : Eq<Int> {
    override fun Int.eqv(b: Int): Boolean = this == b
}

// FILE: Wrapper.kt

package org.data

import com.typeclasses.Eq

data class Wrapper<A>(val value: A) {
    companion object {
        extension class WrapperEq<A>(with val eq: Eq<A>) : Eq<Wrapper<A>> {
            override fun Wrapper<A>.eqv(b: Wrapper<A>): Boolean = this.value.eqv(b.value)
        }
    }
}

// FILE: WrapperSemigroup.kt

package org.data.instances

import org.data.Wrapper
import com.typeclasses.Semigroup

extension class WrapperSemigroup<A>(with val semigroup: Semigroup<A>) : Semigroup<Wrapper<A>> {
    override fun Wrapper<A>.combine(b: Wrapper<A>): Wrapper<A> = Wrapper(this.value.combine(b.value))
}

// FILE: Box.kt

package net.consumer

import com.typeclasses.Semigroup
import com.typeclasses.Eq
import org.data.Wrapper

fun <A> sum(a: A, b: A, with semigroup: Semigroup<A>): A = a.combine(b)

fun <A> sumIfDifferent(a: A, b: A, with semigroup: Semigroup<A>, with eq: Eq<A>): A {
    return if (a.neqv(b)) {
        sum(a, b)
    } else {
        a
    }
}

fun box(): String {
    val x = sumIfDifferent(Wrapper(1), Wrapper(2))
    val y = sumIfDifferent(Wrapper(0), Wrapper(0))
    return if (x == Wrapper(3) && y == Wrapper(0)) {
        "OK"
    } else {
        "fail 1"
    }
}
