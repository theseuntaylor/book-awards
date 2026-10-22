package com.theseuntaylor.bookawards.data

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

enum class ShowSection(val label: String) {
    ON_NOW("On now"),
    OPENING_SOON("Opening soon"),
    LATER("Later")
}

data class ShowGroup(val section: ShowSection, val shows: List<Show>)

private const val OPENING_SOON_DAYS = 30

/** Drops finished runs. The pipeline does too, but the app may be showing a file from weeks ago. */
fun List<Show>.upcoming(today: LocalDate): List<Show> = filter { it.endDate == null || it.endDate >= today }

fun Show.section(today: LocalDate): ShowSection = when {
    startDate == null || startDate <= today -> ShowSection.ON_NOW
    startDate <= today.plus(DatePeriod(days = OPENING_SOON_DAYS)) -> ShowSection.OPENING_SOON
    else -> ShowSection.LATER
}

/** On now: closing soonest first, since that's what you might miss. The rest: opening soonest first. */
fun List<Show>.sections(today: LocalDate): List<ShowGroup> {
    val grouped = groupBy { it.section(today) }
    return ShowSection.entries.mapNotNull { section ->
        val shows = grouped[section] ?: return@mapNotNull null
        val sorted = if (section == ShowSection.ON_NOW) {
            shows.sortedWith(compareBy(nullsLast()) { it: Show -> it.endDate }.thenBy { it.title })
        } else {
            shows.sortedWith(compareBy<Show> { it.startDate }.thenBy { it.title })
        }
        ShowGroup(section, sorted)
    }
}

/** The theatre without the space: "National Theatre (Olivier Theatre)" is "National Theatre". */
val Show.venueName: String get() = venue.substringBefore(" (").trim()

fun List<Show>.venues(): List<String> = map { it.venueName }.distinct().sorted()

/** Filters by venues hidden rather than shown, so a venue new to the data appears without anyone opting in. */
fun List<Show>.withoutVenues(hidden: Set<String>): List<Show> =
    if (hidden.isEmpty()) this else filter { it.venueName !in hidden }

private val monthAbbreviations = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "24 Oct", or "24 Oct 2027" with the year. */
fun LocalDate.shortLabel(withYear: Boolean): String =
    "$day ${monthAbbreviations[month.ordinal]}" + if (withYear) " $year" else ""

/** "Until 24 Oct", "From 5 Nov", "6 Dec", "14 Nov – 19 Dec", "25 Feb – 24 Apr 2027". Years only when not this year. */
fun Show.dateLabel(today: LocalDate): String {
    val start = startDate
    val end = endDate
    return when {
        start == null && end == null -> ""
        start == null -> "Until ${end!!.shortLabel(end.year != today.year)}"
        end == null -> "From ${start.shortLabel(start.year != today.year)}"
        start == end -> start.shortLabel(start.year != today.year)
        start.year != end.year -> "${start.shortLabel(true)} – ${end.shortLabel(true)}"
        else -> "${start.shortLabel(false)} – ${end.shortLabel(end.year != today.year)}"
    }
}
