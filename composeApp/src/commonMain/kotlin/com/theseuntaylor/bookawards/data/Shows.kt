package com.theseuntaylor.bookawards.data

import com.theseuntaylor.bookawards.resources.Res
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true }

/**
 * A production from data/shows.json (built by data/fetch-shows.mjs). A null [startDate] means it's already
 * running ("Until 24 Oct"); a null [endDate] means no end has been announced ("From 5 Nov").
 */
@Serializable
data class Show(
    val id: String,
    val title: String,
    val credits: String? = null,
    val venue: String,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val url: String,
    val imageUrl: String? = null,
    val description: String? = null,
    /** Why it's listed, written when it was curated. */
    val note: String? = null
)

data class ShowsData(val generatedAt: String?, val shows: List<Show>)

/** Skips entries this app version can't read and duplicate IDs, like [parseAwards]. Throws if it isn't a shows file. */
fun parseShows(text: String): ShowsData {
    val root = json.parseToJsonElement(text).jsonObject
    val shows = root.getValue("shows").jsonArray
        .mapNotNull { element -> runCatching { json.decodeFromJsonElement(Show.serializer(), element) }.getOrNull() }
        .distinctBy { it.id }
    return ShowsData(root["generatedAt"]?.jsonPrimitive?.contentOrNull, shows)
}

suspend fun loadBundledShowsText(): String = Res.readBytes("files/shows.json").decodeToString()

fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
