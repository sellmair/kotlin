// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Validator.kt

package com.extensionresolution

interface Validator<A> {
    fun A.isValid(): Boolean
}

data class User(val id: Int, val name: String)

extension internal object UserValidator: Validator<User> {
    override fun User.isValid(): Boolean {
        return id > 0 && name.length > 0
    }
}

internal fun validate(a: User, with validator: UserValidator): Boolean = a.isValid()

fun box(): String {
    return if (validate(User(1, "Alice"))) {
        "OK"
    } else {
        "fail 1"
    }
}