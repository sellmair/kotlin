// FILE: Validator.kt

interface Validator<A> {
    fun A.isValid(): Boolean

    companion object {
        extension class UserValidator1(): Validator<User> {
            override fun User.isValid(): Boolean {
                return id > 0 && name.length > 0
            }
        }
    }
}

data class User(val id: Int, val name: String) {
    companion object {
        extension object UserValidator2: Validator<User> {
            override fun User.isValid(): Boolean {
                return id > 0 && name.length > 0
            }
        }
    }
}

fun <A> validate(a: A, with validator: Validator<A>): Boolean = a.isValid()

val result = <!UNABLE_TO_RESOLVE_EXTENSION!>validate<!>(User(1, "Alice"))
