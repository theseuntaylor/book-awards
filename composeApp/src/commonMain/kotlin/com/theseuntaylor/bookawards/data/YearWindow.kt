package com.theseuntaylor.bookawards.data

import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** Home shows the current year and the two before it; the Library shelves everything older. */
const val HOME_YEARS = 3

fun currentYear(): Int = Clock.System.todayIn(TimeZone.currentSystemDefault()).year

fun firstHomeYear(currentYear: Int): Int = currentYear - (HOME_YEARS - 1)

// Later years stay on Home too, so nothing dated ahead of the device clock disappears.
fun List<Nomination>.forHome(currentYear: Int): List<Nomination> = filter { it.year >= firstHomeYear(currentYear) }

fun List<Nomination>.forLibrary(currentYear: Int): List<Nomination> = filter { it.year < firstHomeYear(currentYear) }

/** Library shelves: newest year first; on each shelf, winners first, then by award. */
fun List<Nomination>.shelvesByYear(): List<Pair<Int, List<Nomination>>> =
    groupBy { it.year }
        .entries
        .sortedByDescending { it.key }
        .map { (year, books) -> year to books.sortedWith(compareBy<Nomination> { it.status.ordinal }.thenBy { it.award.displayName }) }
