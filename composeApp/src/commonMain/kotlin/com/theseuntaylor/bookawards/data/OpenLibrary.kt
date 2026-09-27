package com.theseuntaylor.bookawards.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class BookInfo(
    val workKey: String,
    val coverId: Int?,
    val firstPublished: Int?,
    val description: String? = null
) {
    val coverUrl: String? get() = coverId?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }
    val openLibraryUrl: String get() = "https://openlibrary.org$workKey"
}

class OpenLibrary(private val client: HttpClient) {

    suspend fun lookup(title: String, author: String): BookInfo? {
        val search = client.get("https://openlibrary.org/search.json") {
            parameter("title", title)
            // Co-authored entries join names with ", "; the first author is enough to match.
            parameter("author", author.substringBefore(", "))
            parameter("fields", "key,cover_i,first_publish_year")
            parameter("limit", 1)
        }.bodyAsText()
        val book = parseSearchResult(search) ?: return null
        val work = client.get("https://openlibrary.org${book.workKey}.json").bodyAsText()
        return book.copy(description = parseDescription(work))
    }
}

fun parseSearchResult(text: String): BookInfo? {
    val doc = Json.parseToJsonElement(text).jsonObject["docs"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
    return BookInfo(
        workKey = doc["key"]?.jsonPrimitive?.content ?: return null,
        coverId = doc["cover_i"]?.jsonPrimitive?.intOrNull,
        firstPublished = doc["first_publish_year"]?.jsonPrimitive?.intOrNull
    )
}

/** Open Library stores a work's description either as a plain string or as a typed text object. */
fun parseDescription(text: String): String? =
    when (val description = Json.parseToJsonElement(text).jsonObject["description"]) {
        is JsonPrimitive -> description.content
        is JsonObject -> description["value"]?.jsonPrimitive?.content
        else -> null
    }?.trim()?.takeIf { it.isNotEmpty() }
