package com.theseuntaylor.bookawards.data

enum class Award(val displayName: String) {
    BOOKER("Booker Prize"),
    PULITZER("Pulitzer Prize for Fiction"),
    NATIONAL_BOOK_AWARD("National Book Award for Fiction")
}

enum class NominationStatus(val label: String) {
    WINNER("Winner"),
    SHORTLIST("Shortlist"),
    LONGLIST("Longlist"),
    FINALIST("Finalist")
}

data class Nomination(
    val award: Award,
    val year: Int,
    val title: String,
    val author: String,
    val status: NominationStatus
)
