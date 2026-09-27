package com.theseuntaylor.bookawards.notifications

import androidx.compose.runtime.Composable
import com.russhwolf.settings.Settings
import com.theseuntaylor.bookawards.data.ScheduledAnnouncement

interface AnnouncementNotifier {
    suspend fun requestPermission(): Boolean

    /** Idempotent: rescheduling an announcement with the same ID replaces it. */
    fun schedule(announcements: List<ScheduledAnnouncement>)

    fun cancel(ids: List<String>)
}

@Composable
expect fun rememberAnnouncementNotifier(): AnnouncementNotifier

class NotificationPreferences(private val settings: Settings) {
    var announcementsEnabled: Boolean
        get() = settings.getBoolean(KEY, false)
        set(value) = settings.putBoolean(KEY, value)

    private companion object {
        const val KEY = "announcementNotifications"
    }
}
