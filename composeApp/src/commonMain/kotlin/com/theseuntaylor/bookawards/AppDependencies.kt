package com.theseuntaylor.bookawards

import com.russhwolf.settings.Settings
import com.theseuntaylor.bookawards.data.NominationCache
import com.theseuntaylor.bookawards.data.NominationStore
import com.theseuntaylor.bookawards.data.OpenLibrary
import com.theseuntaylor.bookawards.data.ReadingList
import com.theseuntaylor.bookawards.data.RemoteNominations
import com.theseuntaylor.bookawards.data.awardsDataUrl
import com.theseuntaylor.bookawards.notifications.NotificationPreferences
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent

object AppDependencies {
    val httpClient by lazy {
        HttpClient {
            // Open Library asks clients to identify themselves.
            install(UserAgent) { agent = "book-awards/0.1 (https://github.com/theseuntaylor/book-awards)" }
            // Per-request timeouts are ignored (and throw) unless the plugin is installed.
            install(HttpTimeout)
        }
    }
    val openLibrary by lazy { OpenLibrary(httpClient) }

    private val settings by lazy { Settings() }
    val readingList by lazy { ReadingList(settings) }
    val notificationPreferences by lazy { NotificationPreferences(settings) }
    val nominationStore by lazy {
        NominationStore(RemoteNominations(httpClient, awardsDataUrl), NominationCache(settings), readingList)
    }
}
