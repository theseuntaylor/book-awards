package com.theseuntaylor.bookawards.data

import com.theseuntaylor.bookawards.resources.Res
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true }

/** `generatedAt` is the pipeline's ISO-8601 UTC timestamp; files from before it was added have none. */
data class AwardsData(val generatedAt: String?, val nominations: List<Nomination>)

/**
 * Drops entries this app version can't read (e.g. an award added to the pipeline after this release)
 * and duplicate IDs (the list keys on them) instead of rejecting the whole file.
 * Throws if the text isn't an awards file at all.
 */
fun parseAwards(text: String): AwardsData {
    val root = json.parseToJsonElement(text).jsonObject
    val nominations = root.getValue("nominations").jsonArray
        .mapNotNull { element -> runCatching { json.decodeFromJsonElement(Nomination.serializer(), element) }.getOrNull() }
        .distinctBy { it.id }
    return AwardsData(root["generatedAt"]?.jsonPrimitive?.contentOrNull, nominations)
}

fun parseNominations(text: String): List<Nomination> = parseAwards(text).nominations

/**
 * Only strictly newer data may replace what's shown: a CDN can serve a stale copy, and an app update can
 * bundle data newer than what's published. Undated (older-format) files never replace dated ones.
 */
fun isNewer(candidate: String?, current: String?): Boolean =
    candidate != null && (current == null || candidate > current)

/** Guards against replacing good data with a truncated or broken publish. */
fun isPlausibleUpdate(update: List<Nomination>, current: List<Nomination>): Boolean =
    update.isNotEmpty() && update.size * 2 >= current.size

suspend fun loadBundledNominationsText(): String = Res.readBytes("files/awards.json").decodeToString()
