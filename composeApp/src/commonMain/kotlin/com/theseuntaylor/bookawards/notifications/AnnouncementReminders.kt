package com.theseuntaylor.bookawards.notifications

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.theseuntaylor.bookawards.AppDependencies
import com.theseuntaylor.bookawards.data.announcements
import com.theseuntaylor.bookawards.data.upcoming
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

class AnnouncementReminders internal constructor(
    private val notifier: AnnouncementNotifier,
    private val scope: CoroutineScope,
    private val snackbarHostState: SnackbarHostState
) {
    private val preferences = AppDependencies.notificationPreferences
    var enabled by mutableStateOf(preferences.announcementsEnabled)
        private set

    fun toggle() {
        scope.launch {
            val message = if (enabled) {
                notifier.cancel(announcements.map { it.id })
                saveEnabled(false)
                "Announcement reminders off"
            } else if (notifier.requestPermission()) {
                val scheduled = upcomingAnnouncements()
                notifier.schedule(scheduled)
                saveEnabled(true)
                if (scheduled.isEmpty()) {
                    "Reminders on. No announcement dates are known yet."
                } else {
                    "You'll be reminded on ${scheduled.size} announcement days"
                }
            } else {
                "Notifications are turned off for Book Awards in system settings"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    /** Reschedules on launch so reminders pick up dates added in a new app version. */
    internal fun refresh() {
        if (enabled) notifier.schedule(upcomingAnnouncements())
    }

    private fun saveEnabled(value: Boolean) {
        preferences.announcementsEnabled = value
        enabled = value
    }

    private fun upcomingAnnouncements() =
        announcements.upcoming(Clock.System.now(), TimeZone.currentSystemDefault())
}

@Composable
fun rememberAnnouncementReminders(snackbarHostState: SnackbarHostState): AnnouncementReminders {
    val notifier = rememberAnnouncementNotifier()
    val scope = rememberCoroutineScope()
    val reminders = remember(notifier, snackbarHostState) { AnnouncementReminders(notifier, scope, snackbarHostState) }
    LaunchedEffect(reminders) { reminders.refresh() }
    return reminders
}
