package com.theseuntaylor.bookawards.data

import kotlinx.serialization.Serializable

@Serializable
enum class Award(val displayName: String) {
    BOOKER("Booker Prize"),
    PULITZER("Pulitzer Prize for Fiction"),
    NATIONAL_BOOK_AWARD("National Book Award for Fiction")
}

// Declared best result first, matching statusRank in data/fetch-awards.mjs.
@Serializable
enum class NominationStatus(val label: String) {
    WINNER("Winner"),
    SHORTLIST("Shortlist"),
    FINALIST("Finalist"),
    LONGLIST("Longlist"),
    NOMINEE("Nominee")
}

@Serializable
data class Nomination(
    val award: Award,
    val year: Int,
    val title: String,
    val author: String,
    val status: NominationStatus,
    val wikidataId: String? = null
) {
    // Hand-added entries have no Wikidata ID yet, so fall back to title and author.
    val bookKey: String get() = wikidataId ?: "$title|$author"

    val id: String get() = "${award.name}|$year|$bookKey"
}
