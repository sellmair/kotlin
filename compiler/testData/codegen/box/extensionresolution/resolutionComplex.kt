// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Repository.kt
package com.data

interface Repository<A> {
    fun loadAll(): List<A>
    fun loadById(id: Int): A?
}

// FILE: Validator.kt
package com.validation

import com.domain.Group
import com.domain.User

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

// FILE: User.kt
package com.domain

import com.validation.Validator

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

// FILE: UserRepository.kt

package com.data.instances

import com.data.Repository
import com.domain.User

extension object UserRepository: Repository<User> {
    override fun loadAll(): List<User> {
        return listOf(User(25, "Bob"))
    }

    override fun loadById(id: Int): User? {
        return if (id == 25) {
            User(25, "Bob")
        } else {
            null
        }
    }
}

// FILE: GroupRepository.kt

package com.domain.instances

import com.data.Repository
import com.domain.Group
import com.domain.User

extension class GroupRepository(with val userRepository: Repository<User>): Repository<Group> {
    override fun loadAll(): List<Group> {
        return listOf(Group(userRepository.loadAll()))
    }

    override fun loadById(id: Int): Group? {
        return Group(userRepository.loadAll())
    }
}

// FILE: Box.kt

package net.consumer

import com.data.Repository
import com.validation.Validator
import com.domain.User
import com.domain.Group

fun <A> validate(a: A, with validator: Validator<A>): Boolean = a.isValid()

fun <A> retrieveIfValid(id: Int, with repository: Repository<A>, with validator: Validator<A>): A? {
    val x = loadById(id)
    if (x == null) return null
    return if (validate(x)) x else null
}

fun box(): String {
    val user = retrieveIfValid<User>(25)
    if (user != User(25, "Bob")) {
        return "fail 1"
    }

    val group = retrieveIfValid<Group>(1)
    return if (group == Group(listOf(User(25, "Bob")))) {
        "OK"
    } else {
        "fail 2"
    }
}
