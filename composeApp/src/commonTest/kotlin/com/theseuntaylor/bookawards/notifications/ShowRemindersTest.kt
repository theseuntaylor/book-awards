package com.theseuntaylor.bookawards.notifications

import com.russhwolf.settings.MapSettings
import com.theseuntaylor.bookawards.data.ScheduledAnnouncement
import com.theseuntaylor.bookawards.data.Show
import com.theseuntaylor.bookawards.data.ShowStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

// How show reminders can go wrong: reminding for shows you've booked or seen; firing at the wrong time or
// for a time already past; an opening reminder for a show already running, or a closing one when no end is
// known; two reminders on top of each other for a short run; old reminders left behind after you change a
// status or a show is dropped; scheduling while switched off; and switching on without permission.
class ShowRemindersTest {

    private val london = TimeZone.of("Europe/London")
    private val now = LocalDateTime(2026, 10, 20, 12, 0).toInstant(london)

    private val afronauts = Show(
        id = "royal-court:afronauts", title = "The Afronauts", venue = "Royal Court Theatre",
        startDate = LocalDate(2026, 11, 14), endDate = LocalDate(2026, 12, 19), url = "u"
    )

    private fun ids(reminders: List<ScheduledAnnouncement>) = reminders.map { it.id }

    @Test
    fun wantToSeeGetsOpeningDayAndLastWeekRemindersAtNine() {
        val reminders = showReminders(listOf(afronauts), mapOf(afronauts.id to ShowStatus.WANT_TO_SEE), now, london)

        assertEquals(
            listOf(LocalDateTime(2026, 11, 14, 9, 0), LocalDateTime(2026, 12, 12, 9, 0)),
            reminders.map { it.fireAt }
        )
        assertEquals("The Afronauts opens today", reminders[0].title)
        assertEquals("The Afronauts closes on 19 Dec", reminders[1].title)
        assertEquals(listOf("show-opening|royal-court:afronauts", "show-closing|royal-court:afronauts"), ids(reminders))
    }

    @Test
    fun noRemindersForBookedSeenOrUnmarkedShows() {
        val statuses = listOf(ShowStatus.BOOKED, ShowStatus.SEEN, null)

        for (status in statuses) {
            val marked = if (status == null) emptyMap() else mapOf(afronauts.id to status)
            assertEquals(emptyList(), showReminders(listOf(afronauts), marked, now, london), "status $status")
        }
    }

    @Test
    fun skipsRemindersWhoseTimeHasPassed() {
        val openingMorning = LocalDateTime(2026, 11, 14, 9, 30).toInstant(london)

        val reminders = showReminders(listOf(afronauts), mapOf(afronauts.id to ShowStatus.WANT_TO_SEE), openingMorning, london)

        assertEquals(listOf("show-closing|royal-court:afronauts"), ids(reminders))
    }

    @Test
    fun aRunningShowGetsOnlyTheClosingReminderAndAnOpenEndedOneOnlyTheOpening() {
        val running = afronauts.copy(id = "running", startDate = null)
        val openEnded = afronauts.copy(id = "open", endDate = null)
        val wanted = mapOf("running" to ShowStatus.WANT_TO_SEE, "open" to ShowStatus.WANT_TO_SEE)

        assertEquals(
            listOf("show-closing|running", "show-opening|open"),
            ids(showReminders(listOf(running, openEnded), wanted, now, london))
        )
    }

    @Test
    fun aShortRunGetsOnlyTheOpeningReminder() {
        val threeNights = afronauts.copy(startDate = LocalDate(2026, 11, 5), endDate = LocalDate(2026, 11, 7))

        val reminders = showReminders(listOf(threeNights), mapOf(afronauts.id to ShowStatus.WANT_TO_SEE), now, london)

        assertEquals(listOf("show-opening|royal-court:afronauts"), ids(reminders))
    }

    private class FakeNotifier(var permission: Boolean = true) : AnnouncementNotifier {
        val scheduled = mutableMapOf<String, ScheduledAnnouncement>()
        override suspend fun requestPermission() = permission
        override fun schedule(announcements: List<ScheduledAnnouncement>) {
            announcements.forEach { scheduled[it.id] = it }
        }
        override fun cancel(ids: List<String>) {
            ids.forEach { scheduled.remove(it) }
        }
    }

    private val wanted = mapOf(afronauts.id to ShowStatus.WANT_TO_SEE)

    @Test
    fun changingAStatusCancelsRemindersScheduledEarlier() = runTest {
        val notifier = FakeNotifier()
        val scheduler = ShowReminderScheduler(notifier, ShowReminderPreferences(MapSettings()), clock = { now }, timeZone = london)
        scheduler.toggle(listOf(afronauts), wanted)
        assertEquals(2, notifier.scheduled.size)

        scheduler.reschedule(listOf(afronauts), mapOf(afronauts.id to ShowStatus.BOOKED))

        assertEquals(emptyMap(), notifier.scheduled)
    }

    @Test
    fun aDroppedShowLosesItsReminders() = runTest {
        val notifier = FakeNotifier()
        val settings = MapSettings()
        ShowReminderScheduler(notifier, ShowReminderPreferences(settings), clock = { now }, timeZone = london)
            .toggle(listOf(afronauts), wanted)

        // A new process: only the saved IDs know what was scheduled.
        ShowReminderScheduler(notifier, ShowReminderPreferences(settings), clock = { now }, timeZone = london)
            .reschedule(emptyList(), wanted)

        assertEquals(emptyMap(), notifier.scheduled)
    }

    @Test
    fun schedulesNothingWhileSwitchedOffAndCancelsAllWhenSwitchedOff() = runTest {
        val notifier = FakeNotifier()
        val scheduler = ShowReminderScheduler(notifier, ShowReminderPreferences(MapSettings()), clock = { now }, timeZone = london)
        scheduler.reschedule(listOf(afronauts), wanted)
        assertEquals(emptyMap(), notifier.scheduled)

        scheduler.toggle(listOf(afronauts), wanted)
        assertTrue(scheduler.enabled)
        scheduler.toggle(listOf(afronauts), wanted)

        assertFalse(scheduler.enabled)
        assertEquals(emptyMap(), notifier.scheduled)
    }

    @Test
    fun staysOffWithoutPermission() = runTest {
        val notifier = FakeNotifier(permission = false)
        val scheduler = ShowReminderScheduler(notifier, ShowReminderPreferences(MapSettings()), clock = { now }, timeZone = london)

        val message = scheduler.toggle(listOf(afronauts), wanted)

        assertFalse(scheduler.enabled)
        assertEquals(emptyMap(), notifier.scheduled)
        assertEquals("Notifications are turned off for Book Awards in system settings", message)
    }
}
