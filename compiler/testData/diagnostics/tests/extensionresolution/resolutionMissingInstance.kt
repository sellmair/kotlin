// FILE: Validator.kt

interface Validator<A> {
    fun A.isValid(): Boolean
}

data class User(val id: Int, val name: String)

fun <A> validate(a: A, with validator: Validator<A>): Boolean = a.isValid()

val result = <!UNABLE_TO_RESOLVE_EXTENSION!>validate<!>(User(1, "Alice"))
