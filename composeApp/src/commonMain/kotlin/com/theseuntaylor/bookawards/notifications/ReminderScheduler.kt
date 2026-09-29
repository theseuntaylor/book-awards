package com.theseuntaylor.bookawards.notifications

import com.theseuntaylor.bookawards.data.announcements
import com.theseuntaylor.bookawards.data.upcoming
import kotlin.time.Clock
import kotlinx.datetime.TimeZone

/** Turning announcement reminders on and off, independent of any UI toolkit. */
class ReminderScheduler(
    private val notifier: AnnouncementNotifier,
    private val preferences: NotificationPreferences
) {
    val enabled: Boolean get() = preferences.announcementsEnabled

    /** Returns what to tell the person, since asking for permission can fail. */
    suspend fun toggle(): String =
        if (enabled) {
            notifier.cancel(announcements.map { it.id })
            preferences.announcementsEnabled = false
            "Announcement reminders off"
        } else if (notifier.requestPermission()) {
            val scheduled = upcomingAnnouncements()
            notifier.schedule(scheduled)
            preferences.announcementsEnabled = true
            if (scheduled.isEmpty()) {
                "Reminders on. No announcement dates are known yet."
            } else {
                "You'll be reminded on ${scheduled.size} announcement days"
            }
        } else {
            "Notifications are turned off for Book Awards in system settings"
        }

    /** Reschedules on launch so reminders pick up dates added in a new app version. */
    fun reschedule() {
        if (enabled) notifier.schedule(upcomingAnnouncements())
    }

    private fun upcomingAnnouncements() =
        announcements.upcoming(Clock.System.now(), TimeZone.currentSystemDefault())
}
