// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Validator.kt
package com.data

interface Validator<A> {
    fun A.isValid(): Boolean
}

data class User(val id: Int, val name: String) {
    companion object {
        extension class UserValidator(): Validator<User> {
            override fun User.isValid(): Boolean {
                return id > 0 && name.length > 0
            }
        }
    }
}

fun <A> validate(a: A, with validator: Validator<A>): Boolean = a.isValid()

fun <A> bothValid(x: A, y: A, with validator: Validator<A>): Boolean = validate(x) && validate(y)

fun box(): String {
    return if (bothValid(User(1, "Alice"), User(2, "Bob"))) {
        "OK"
    } else {
        "fail 1"
    }
}
