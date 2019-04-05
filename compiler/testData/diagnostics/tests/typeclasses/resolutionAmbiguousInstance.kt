// FILE: Semigroup.kt

data class Money(val amount: Int) {
    companion object {
        extension object AnotherMoneySemigroup : Semigroup<Money> {
            override fun Money.combine(other: Money): Money = Money(this.amount + other.amount)
        }
    }
}

interface Semigroup<A> {
    fun A.combine(other: A): A

    companion object {
        extension object MoneySemigroup : Semigroup<Money> {
            override fun Money.combine(other: Money): Money = Money(this.amount + other.amount)
        }
    }
}

fun <A> duplicate(x: A, with semigroup: Semigroup<A>): A {
    return x.combine(x)
}

val result = <!UNABLE_TO_RESOLVE_IMPLICIT_PARAMETER!>duplicate<!>(Money(2))
