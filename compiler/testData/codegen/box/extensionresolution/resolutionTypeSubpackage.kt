// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Semigroup.kt

package com.extensionresolution

import org.data.Money

interface Semigroup<A> {
    fun A.combine(b: A): A
}

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

data class Money(val amount: Double)

// FILE: MoneySemigroup.kt

package org.data.instances

import com.extensionresolution.Semigroup
import org.data.Money

extension object MoneySemigroup : Semigroup<Money> {
    override fun Money.combine(b: Money): Money = Money(this.amount + b.amount)
}
