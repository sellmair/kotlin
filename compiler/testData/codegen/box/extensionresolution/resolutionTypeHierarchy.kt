// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Semigroup.kt

package com.extensionresolution

interface Printer<A> {
    fun A.print(): String

    companion object {
        extension object NumberPrinter : Printer<Number> {
            override fun Number.print(): String = "Number($this)"
        }

        extension object IntPrinter : Printer<Int> {
            override fun Int.print(): String = "Int($this)"
        }
    }
}

fun <A> debugPrint(a: A, with printer: Printer<A>): String = a.print()

fun <A> prependPrint(prefix: String, a: A, with printer: Printer<A>): String = prefix + debugPrint(a)

fun box(): String {
    if (prependPrint("It's ", 2) != "It's Int(2)") {
        return "fail 1"
    }
    if (prependPrint("It's ", 2L) != "It's Number(2)") {
        return "fail 2"
    }
    return "OK"
}