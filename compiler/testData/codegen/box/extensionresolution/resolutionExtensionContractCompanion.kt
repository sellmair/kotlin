// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Validator.kt

package com.extensionresolution

interface Validator<A> {
    fun A.isValid(): Boolean

    companion object {
        extension object UserValidator: Validator<User> {
            override fun User.isValid(): Boolean {
                return id > 0 && name.length > 0
            }
        }
    }
}

data class User(val id: Int, val name: String)

fun <A> validate(a: A, with validator: Validator<A>): Boolean = a.isValid()

fun box(): String {
    return if (validate(User(1, "Alice"))) {
        "OK"
    } else {
        "fail 1"
    }
}