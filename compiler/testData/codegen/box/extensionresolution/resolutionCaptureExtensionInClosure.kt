// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Repository.kt

package com.typeclasses

interface Repository<A> {
    fun save(a: A): A
    fun findAll(): List<A>
}

data class User(val name: String) {
    companion object {
        extension object UserRepository : Repository<User> {
            override fun save(a: User): User = a
            override fun findAll(): List<User> = emptyList()
        }
    }
}

fun <A> persistCache(value: A, with R: Repository<A>): List<A> {
    val f: (A) -> A = { a -> save(a) }
    return listOf(f(value))
}

fun box(): String {
    val x = persistCache<User>(User("I"))
    return if (x == listOf(User("I"))) {
        "OK"
    } else {
        "fail 1"
    }
}