// FILE: Validator.kt

interface Validator<A> {
    fun A.isValid(): Boolean

    companion object {
        extension class GroupValidator<A>(with val userValidator: Validator<User>): Validator<Group> {
            override fun Group.isValid(): Boolean {
                return lead.isValid()
            }
        }
    }
}

data class User(val id: Int, val name: String)
data class Group(val lead: User)

fun <A> validate(a: A, with validator: Validator<A>): Boolean = a.isValid()

val result = <!UNABLE_TO_RESOLVE_EXTENSION!>validate<!>(Group(User(1, "Alice")))
