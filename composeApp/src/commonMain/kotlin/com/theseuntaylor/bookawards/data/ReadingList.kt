package com.theseuntaylor.bookawards.data

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

enum class ReadingStatus(val label: String) {
    WANT_TO_READ("Want to read"),
    READING("Reading"),
    READ("Read")
}

/** Reading status per book, keyed by [Nomination.bookKey] so it follows the book across awards and years. */
class ReadingList(private val settings: Settings) {
    private val statuses = MutableStateFlow(load())

    val all: StateFlow<Map<String, ReadingStatus>> = statuses.asStateFlow()

    fun set(bookKey: String, status: ReadingStatus?) {
        statuses.update { current -> if (status == null) current - bookKey else current + (bookKey to status) }
        save()
    }

    /** Call after loading new data, so statuses on hand-added books survive them gaining a Wikidata ID. */
    fun adoptCurrentKeys(nominations: List<Nomination>) {
        val updated = statuses.value.withCurrentBookKeys(nominations)
        if (updated != statuses.value) {
            statuses.value = updated
            save()
        }
    }

    private fun save() = settings.putString(KEY, Json.encodeToString(statuses.value))

    private fun load(): Map<String, ReadingStatus> =
        settings.getStringOrNull(KEY)?.let { Json.decodeFromString<Map<String, ReadingStatus>>(it) }.orEmpty()

    private companion object {
        const val KEY = "readingStatuses"
    }
}

/**
 * Moves statuses saved under a book's title|author fallback key to its Wikidata ID once the data has one.
 * A status already saved under the ID wins.
 */
fun Map<String, ReadingStatus>.withCurrentBookKeys(nominations: List<Nomination>): Map<String, ReadingStatus> {
    val idByFallbackKey = nominations
        .filter { it.wikidataId != null }
        .associate { it.copy(wikidataId = null).bookKey to it.bookKey }
    val moved = filterKeys { it in idByFallbackKey && idByFallbackKey.getValue(it) !in this }
        .mapKeys { (fallbackKey, _) -> idByFallbackKey.getValue(fallbackKey) }
    return filterKeys { it !in idByFallbackKey } + moved
}
