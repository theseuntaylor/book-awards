package com.theseuntaylor.bookawards.data

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

data class Announcement(
    val id: String,
    val award: Award,
    val date: LocalDate,
    val title: String,
    val body: String
)

data class ScheduledAnnouncement(
    val id: String,
    val title: String,
    val body: String,
    val fireAt: LocalDateTime
)

// Only dates the prizes have published; add next season's once they're announced.
val announcements = listOf(
    Announcement(
        id = "nba-2026-finalists",
        award = Award.NATIONAL_BOOK_AWARD,
        date = LocalDate(2026, 10, 6),
        title = "National Book Award finalists today",
        body = "The five fiction finalists for the 2026 National Book Award are announced today."
    ),
    Announcement(
        id = "booker-2026-winner",
        award = Award.BOOKER,
        date = LocalDate(2026, 11, 9),
        title = "Booker Prize winner tonight",
        body = "The 2026 Booker Prize winner is revealed this evening in London."
    ),
    Announcement(
        id = "nba-2026-winner",
        award = Award.NATIONAL_BOOK_AWARD,
        date = LocalDate(2026, 11, 18),
        title = "National Book Award winners tonight",
        body = "The 2026 National Book Award winners are announced at this evening's ceremony in New York."
    )
)

private val notificationTime = LocalTime(9, 0)

fun List<Announcement>.upcoming(now: Instant, timeZone: TimeZone): List<ScheduledAnnouncement> =
    map { ScheduledAnnouncement(it.id, it.title, it.body, LocalDateTime(it.date, notificationTime)) }
        .filter { it.fireAt.toInstant(timeZone) > now }
