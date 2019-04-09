// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Validator.kt

package com.extensionresolution

interface Validator<A> {
    fun A.isValid(): Boolean
}

data class User(val id: Int, val name: String)

fun <A> validate(a: A, with validator: Validator<A>): Boolean = a.isValid()

fun box(): String {
    val x = validate(User(1, "Alice"), object: Validator<User> {
        override fun User.isValid(): Boolean = id > 0 && name.length > 0
    })

    return if (x) {
        "OK"
    } else {
        "fail 1"
    }
}
