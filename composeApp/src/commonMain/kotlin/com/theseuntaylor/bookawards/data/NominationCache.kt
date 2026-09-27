package com.theseuntaylor.bookawards.data

import com.russhwolf.settings.Settings

/** The last good download, valid only for the bundled data it was fetched on top of. */
class NominationCache(private val settings: Settings) {

    data class Entry(val text: String, val etag: String?)

    fun read(bundledHash: Int): Entry? {
        if (settings.getIntOrNull(BUNDLED_HASH) != bundledHash) return null
        val text = settings.getStringOrNull(TEXT) ?: return null
        return Entry(text, settings.getStringOrNull(ETAG))
    }

    fun write(bundledHash: Int, entry: Entry) {
        settings.putString(TEXT, entry.text)
        if (entry.etag != null) settings.putString(ETAG, entry.etag) else settings.remove(ETAG)
        settings.putInt(BUNDLED_HASH, bundledHash)
    }

    private companion object {
        const val TEXT = "nominationCache.text"
        const val ETAG = "nominationCache.etag"
        const val BUNDLED_HASH = "nominationCache.bundledHash"
    }
}
