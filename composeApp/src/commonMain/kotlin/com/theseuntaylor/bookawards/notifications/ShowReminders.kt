package com.theseuntaylor.bookawards.notifications

import com.russhwolf.settings.Settings
import com.theseuntaylor.bookawards.data.ScheduledAnnouncement
import com.theseuntaylor.bookawards.data.Show
import com.theseuntaylor.bookawards.data.ShowStatus
import com.theseuntaylor.bookawards.data.shortLabel
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant

private val reminderTime = LocalTime(9, 0)
private const val LAST_WEEK_DAYS = 7

/**
 * For shows marked Want to see: 9am on opening day, and 9am a week before closing. Booked and Seen shows
 * get nothing. A run shorter than a week only gets the opening reminder.
 */
fun showReminders(
    shows: List<Show>,
    statuses: Map<String, ShowStatus>,
    now: Instant,
    timeZone: TimeZone
): List<ScheduledAnnouncement> = shows
    .filter { statuses[it.id] == ShowStatus.WANT_TO_SEE }
    .flatMap { show ->
        val opening = show.startDate?.let { start ->
            ScheduledAnnouncement(
                id = "show-opening|${show.id}",
                title = "${show.title} opens today",
                body = "At ${show.venue}. You marked it Want to see.",
                fireAt = LocalDateTime(start, reminderTime)
            )
        }
        val closing = show.endDate?.let { end ->
            val lastWeek = end.minus(DatePeriod(days = LAST_WEEK_DAYS))
            if (show.startDate != null && lastWeek <= show.startDate) return@let null
            ScheduledAnnouncement(
                id = "show-closing|${show.id}",
                title = "${show.title} closes on ${end.shortLabel(withYear = false)}",
                body = "Last week to see it at ${show.venue}.",
                fireAt = LocalDateTime(lastWeek, reminderTime)
            )
        }
        listOfNotNull(opening, closing)
    }
    .filter { it.fireAt.toInstant(timeZone) > now }

class ShowReminderPreferences(private val settings: Settings) {
    var enabled: Boolean
        get() = settings.getBoolean(ENABLED, false)
        set(value) = settings.putBoolean(ENABLED, value)

    /** What was scheduled last, so reminders for dropped shows or changed statuses can be cancelled later. */
    var scheduledIds: List<String>
        get() = settings.getStringOrNull(IDS)?.split('\n')?.filter { it.isNotEmpty() }.orEmpty()
        set(value) = settings.putString(IDS, value.joinToString("\n"))

    private companion object {
        const val ENABLED = "showReminders.enabled"
        const val IDS = "showReminders.scheduledIds"
    }
}

/** The Theatre tab's bell, independent of any UI toolkit, like [ReminderScheduler] for announcements. */
class ShowReminderScheduler(
    private val notifier: AnnouncementNotifier,
    private val preferences: ShowReminderPreferences,
    private val clock: () -> Instant = { Clock.System.now() },
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) {
    val enabled: Boolean get() = preferences.enabled

    suspend fun toggle(shows: List<Show>, statuses: Map<String, ShowStatus>): String =
        if (enabled) {
            notifier.cancel(preferences.scheduledIds)
            preferences.scheduledIds = emptyList()
            preferences.enabled = false
            "Show reminders off"
        } else if (notifier.requestPermission()) {
            preferences.enabled = true
            val count = reschedule(shows, statuses)
            "You'll be reminded when shows you want to see open and in their last week" +
                if (count == 0) ". Mark a show Want to see to get one." else ""
        } else {
            "Notifications are turned off for Book Awards in system settings"
        }

    /** Call whenever shows or statuses change. Returns how many reminders are scheduled. */
    fun reschedule(shows: List<Show>, statuses: Map<String, ShowStatus>): Int {
        if (!enabled) return 0
        val wanted = showReminders(shows, statuses, clock(), timeZone)
        val wantedIds = wanted.map { it.id }.toSet()
        val stale = preferences.scheduledIds.filter { it !in wantedIds }
        if (stale.isNotEmpty()) notifier.cancel(stale)
        notifier.schedule(wanted)
        preferences.scheduledIds = wanted.map { it.id }
        return wanted.size
    }
}
