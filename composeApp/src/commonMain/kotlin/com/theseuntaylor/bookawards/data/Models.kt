package com.theseuntaylor.bookawards.data

import kotlinx.serialization.Serializable

@Serializable
enum class Award(val displayName: String, val shortName: String) {
    BOOKER("Booker Prize", "Booker"),
    PULITZER("Pulitzer Prize for Fiction", "Pulitzer"),
    NATIONAL_BOOK_AWARD("National Book Award for Fiction", "Nat. Book Award")
}

// Declared best result first, matching statusRank in data/combine.mjs.
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
    val wikidataId: String? = null,
    /** Open Library cover ID, looked up by the pipeline (data/covers.mjs). */
    val coverId: Int? = null
) {
    // Hand-added entries have no Wikidata ID yet, so fall back to title and author.
    val bookKey: String get() = wikidataId ?: "$title|$author"

    val id: String get() = "${award.name}|$year|$bookKey"

    /** About 180px wide: sharp enough for shelf thumbnails without downloading full-size covers. */
    val coverThumbnailUrl: String? get() = coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" }

    val coverLargeUrl: String? get() = coverId?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }
}
