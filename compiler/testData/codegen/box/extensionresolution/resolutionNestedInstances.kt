// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Validator.kt
package com.validation

interface Validator<A> {
    fun A.isValid(): Boolean

    companion object {
        extension class GroupValidator<A>(with val userValidator: Validator<User>): Validator<Group> {
            override fun Group.isValid(): Boolean {
                for (x in users) {
                    if (!x.isValid()) return false
                }
                return true
            }
        }
    }
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

data class Group(val users: List<User>)

fun <A> validate(a: A, with validator: Validator<A>): Boolean = a.isValid()

fun box(): String {
    return if (validate(Group(listOf(User(25, "Bob"))))) {
        "OK"
    } else {
        "fail 1"
    }
}
