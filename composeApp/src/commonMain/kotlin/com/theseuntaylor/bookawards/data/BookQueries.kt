package com.theseuntaylor.bookawards.data

fun List<Nomination>.nominationsForBook(bookKey: String): List<Nomination> =
    filter { it.bookKey == bookKey }
        .sortedWith(compareByDescending<Nomination> { it.year }.thenBy { it.award.displayName })

/** One entry per other book by the same author, keeping that book's best result. */
fun List<Nomination>.otherBooksByAuthor(nomination: Nomination): List<Nomination> =
    filter { it.author == nomination.author && it.bookKey != nomination.bookKey }
        .groupBy { it.bookKey }
        .map { (_, nominations) -> nominations.minWith(compareBy<Nomination> { it.status.ordinal }.thenByDescending { it.year }) }
        .sortedByDescending { it.year }
