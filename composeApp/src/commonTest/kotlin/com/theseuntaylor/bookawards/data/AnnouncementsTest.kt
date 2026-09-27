package com.theseuntaylor.bookawards.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class AnnouncementsTest {

    private val london = TimeZone.of("Europe/London")
    private val shortlist = Announcement("shortlist", Award.BOOKER, LocalDate(2026, 9, 22), "Shortlist", "")
    private val winner = Announcement("winner", Award.BOOKER, LocalDate(2026, 11, 9), "Winner", "")

    @Test
    fun schedulesFutureAnnouncementsAtNineInTheMorning() {
        val now = LocalDateTime(2026, 10, 1, 12, 0).toInstant(london)

        assertEquals(
            listOf(ScheduledAnnouncement("winner", "Winner", "", LocalDateTime(2026, 11, 9, 9, 0))),
            listOf(shortlist, winner).upcoming(now, london)
        )
    }

    @Test
    fun skipsAnAnnouncementOnceItsReminderTimeHasPassed() {
        val announcementMorning = LocalDateTime(2026, 11, 9, 9, 30).toInstant(london)

        assertEquals(emptyList(), listOf(winner).upcoming(announcementMorning, london))
    }

    @Test
    fun announcementIdsAreUnique() {
        assertEquals(announcements.size, announcements.map { it.id }.toSet().size)
    }
}
