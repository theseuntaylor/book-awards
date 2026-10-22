package com.theseuntaylor.bookawards.data

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// @Serializable for the same reason as ReadingStatus: iOS finds no serializer by reflection.
@Serializable
enum class ShowStatus(val label: String) {
    WANT_TO_SEE("Want to see"),
    BOOKED("Booked"),
    SEEN("Seen")
}

/** Your status per show, keyed by [Show.id] (`venue:slug`, stable across pipeline runs). */
class ShowList(private val settings: Settings) {
    private val statuses = MutableStateFlow(load())

    val all: StateFlow<Map<String, ShowStatus>> = statuses.asStateFlow()

    fun set(showId: String, status: ShowStatus?) {
        statuses.update { current -> if (status == null) current - showId else current + (showId to status) }
        settings.putString(KEY, Json.encodeToString(statuses.value))
    }

    private fun load(): Map<String, ShowStatus> =
        settings.getStringOrNull(KEY)?.let { runCatching { Json.decodeFromString<Map<String, ShowStatus>>(it) }.getOrNull() }
            .orEmpty()

    private companion object {
        const val KEY = "showStatuses"
    }
}
