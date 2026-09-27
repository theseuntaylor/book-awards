package com.theseuntaylor.bookawards.data

import com.theseuntaylor.bookawards.resources.Res
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

private val json = Json { ignoreUnknownKeys = true }

/**
 * Drops entries this app version can't read (e.g. an award added to the pipeline after this release)
 * and duplicate IDs (the list keys on them) instead of rejecting the whole file.
 * Throws if the text isn't an awards file at all.
 */
fun parseNominations(text: String): List<Nomination> =
    json.parseToJsonElement(text).jsonObject.getValue("nominations").jsonArray
        .mapNotNull { element -> runCatching { json.decodeFromJsonElement(Nomination.serializer(), element) }.getOrNull() }
        .distinctBy { it.id }

/** Guards against replacing good data with a truncated or broken publish. */
fun isPlausibleUpdate(update: List<Nomination>, current: List<Nomination>): Boolean =
    update.isNotEmpty() && update.size * 2 >= current.size

suspend fun loadBundledNominationsText(): String = Res.readBytes("files/awards.json").decodeToString()
