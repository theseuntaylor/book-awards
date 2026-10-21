package com.theseuntaylor.bookawards.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

// How the Theatre list can go wrong: a finished run still shown (or one on its last night hidden);
// open-ended runs ("Until", "From") dropped or misfiled; the 30-day "Opening soon" edge off by one;
// empty sections shown; a jumbled order; a venue filter that splits one theatre's spaces apart; and date
// labels that drop or repeat the year.
class ShowQueriesTest {

    private val today = LocalDate(2026, 10, 20)

    private fun show(
        id: String,
        start: LocalDate?,
        end: LocalDate?,
        venue: String = "Royal Court Theatre",
        title: String = id
    ) = Show(id = id, title = title, venue = venue, startDate = start, endDate = end, url = "https://example.org/$id")

    @Test
    fun hidesRunsThatEndedButKeepsOneOnItsLastNight() {
        val ended = show("ended", LocalDate(2026, 9, 1), LocalDate(2026, 10, 19))
        val lastNight = show("last-night", LocalDate(2026, 9, 1), today)

        assertEquals(listOf(lastNight), listOf(ended, lastNight).upcoming(today))
    }

    @Test
    fun keepsRunsWithNoEndAnnounced() {
        val noEnd = show("from", LocalDate(2026, 11, 5), null)

        assertEquals(listOf(noEnd), listOf(noEnd).upcoming(today))
    }

    @Test
    fun filesShowsWithNoStartOrStartingTodayUnderOnNow() {
        assertEquals(ShowSection.ON_NOW, show("until", null, LocalDate(2026, 10, 24)).section(today))
        assertEquals(ShowSection.ON_NOW, show("today", today, LocalDate(2026, 11, 1)).section(today))
        assertEquals(ShowSection.ON_NOW, show("started", LocalDate(2026, 10, 1), LocalDate(2026, 11, 1)).section(today))
    }

    @Test
    fun opensWithinThirtyDaysIsOpeningSoonAndLaterIsLater() {
        assertEquals(ShowSection.OPENING_SOON, show("tomorrow", LocalDate(2026, 10, 21), null).section(today))
        assertEquals(ShowSection.OPENING_SOON, show("day-30", LocalDate(2026, 11, 19), null).section(today))
        assertEquals(ShowSection.LATER, show("day-31", LocalDate(2026, 11, 20), null).section(today))
    }

    @Test
    fun groupsIntoSectionsInOrderAndLeavesOutEmptyOnes() {
        val later = show("later", LocalDate(2027, 2, 25), LocalDate(2027, 4, 24))
        val onNow = show("on-now", null, LocalDate(2026, 10, 24))

        val sections = listOf(later, onNow).sections(today)

        assertEquals(listOf(ShowSection.ON_NOW, ShowSection.LATER), sections.map { it.section })
        assertEquals(listOf(listOf(onNow), listOf(later)), sections.map { it.shows })
    }

    @Test
    fun sortsOnNowByClosingSoonestAndTheRestByOpeningThenTitle() {
        val closesLate = show("closes-late", null, LocalDate(2026, 12, 1))
        val closesSoon = show("closes-soon", LocalDate(2026, 10, 1), LocalDate(2026, 10, 24))
        val noEnd = show("no-end", LocalDate(2026, 10, 1), null)
        val soonB = show("soon-b", LocalDate(2026, 11, 1), null, title = "Beta")
        val soonA = show("soon-a", LocalDate(2026, 11, 1), null, title = "Alpha")
        val soonFirst = show("soon-first", LocalDate(2026, 10, 25), null, title = "Zed")

        val sections = listOf(noEnd, soonB, closesLate, soonA, closesSoon, soonFirst).sections(today)

        assertEquals(listOf("closes-soon", "closes-late", "no-end"), sections[0].shows.map { it.id })
        assertEquals(listOf("soon-first", "soon-a", "soon-b"), sections[1].shows.map { it.id })
    }

    @Test
    fun treatsATheatresSpacesAsOneVenue() {
        val olivier = show("a", null, today, venue = "National Theatre (Olivier Theatre)")
        val lyttelton = show("b", null, today, venue = "National Theatre (Lyttelton Theatre)")
        val court = show("c", null, today, venue = "Royal Court Theatre")

        assertEquals("National Theatre", olivier.venueName)
        assertEquals(listOf("National Theatre", "Royal Court Theatre"), listOf(court, olivier, lyttelton).venues())
        assertEquals(listOf(court), listOf(olivier, lyttelton, court).withoutVenues(setOf("National Theatre")))
        assertEquals(listOf(olivier, court), listOf(olivier, court).withoutVenues(emptySet()))
    }

    @Test
    fun labelsDatesTheWayVenuesWriteThem() {
        assertEquals("Until 24 Oct", show("a", null, LocalDate(2026, 10, 24)).dateLabel(today))
        assertEquals("From 5 Nov", show("b", LocalDate(2026, 11, 5), null).dateLabel(today))
        assertEquals("6 Dec", show("c", LocalDate(2026, 12, 6), LocalDate(2026, 12, 6)).dateLabel(today))
        assertEquals("14 Nov – 19 Dec", show("d", LocalDate(2026, 11, 14), LocalDate(2026, 12, 19)).dateLabel(today))
    }

    @Test
    fun addsTheYearOnlyWhenItIsNotThisYear() {
        assertEquals("25 Feb – 24 Apr 2027", show("a", LocalDate(2027, 2, 25), LocalDate(2027, 4, 24)).dateLabel(today))
        assertEquals("21 Nov 2026 – 2 Jan 2027", show("b", LocalDate(2026, 11, 21), LocalDate(2027, 1, 2)).dateLabel(today))
        assertEquals("Until 10 Jan 2027", show("c", null, LocalDate(2027, 1, 10)).dateLabel(today))
    }

    @Test
    fun parsesPublishedShowsAndSkipsEntriesItCannotRead() {
        val text = """
            {"generatedAt": "2026-10-20T08:00:00Z", "shows": [
              {"id": "a", "title": "A", "venue": "V", "startDate": null, "endDate": "2026-10-24", "url": "u"},
              {"id": "b", "title": "B", "venue": "V", "startDate": "not a date", "endDate": "2026-10-24", "url": "u"},
              {"id": "a", "title": "A again", "venue": "V", "endDate": "2026-10-24", "url": "u"},
              {"id": "c", "title": "C", "venue": "V", "startDate": "2026-11-01", "url": "u", "newField": 1}
            ]}
        """.trimIndent()

        val data = parseShows(text)

        assertEquals("2026-10-20T08:00:00Z", data.generatedAt)
        assertEquals(listOf("a", "c"), data.shows.map { it.id })
    }
}
