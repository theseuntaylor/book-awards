package com.theseuntaylor.bookawards.data

import com.russhwolf.settings.Settings

/** The last good download. The store only uses it when it's newer than the bundled data. */
class NominationCache(private val settings: Settings) {

    data class Entry(val text: String, val etag: String?)

    fun read(): Entry? {
        val text = settings.getStringOrNull(TEXT) ?: return null
        return Entry(text, settings.getStringOrNull(ETAG))
    }

    fun write(entry: Entry) {
        settings.putString(TEXT, entry.text)
        if (entry.etag != null) settings.putString(ETAG, entry.etag) else settings.remove(ETAG)
    }

    private companion object {
        const val TEXT = "nominationCache.text"
        const val ETAG = "nominationCache.etag"
    }
}
