// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Semigroup.kt

package com.extensionresolution

interface Semigroup<A> {
    fun A.combine(b: A): A
}

// FILE: Box.kt

package com.extensionresolution

import org.data.Money

fun <A> duplicate(a: A, with semigroup: Semigroup<A>) : A = a.combine(a)

fun box(): String {
    val x = duplicate(Money(2.0))
    return if (x == Money(4.0)) {
        "OK"
    } else {
        "fail 1"
    }
}

// FILE: Money.kt

package org.data

import com.extensionresolution.Semigroup

data class Money(val amount: Double) {
    companion object {
        extension object MoneySemigroup : Semigroup<Money> {
            override fun Money.combine(b: Money): Money = Money(this.amount + b.amount)
        }
    }
}
