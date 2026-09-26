package com.theseuntaylor.bookawards.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NominationFilterTest {

    private val booker2022 = Nomination(Award.BOOKER, 2022, "Booker 22", "A", NominationStatus.WINNER)
    private val booker2023 = Nomination(Award.BOOKER, 2023, "Booker 23", "B", NominationStatus.WINNER)
    private val pulitzer2023 = Nomination(Award.PULITZER, 2023, "Pulitzer 23", "C", NominationStatus.WINNER)
    private val nba2023 = Nomination(Award.NATIONAL_BOOK_AWARD, 2023, "NBA 23", "D", NominationStatus.WINNER)

    private val all = listOf(booker2022, pulitzer2023, nba2023, booker2023)

    @Test
    fun keepsOnlySelectedAwards() {
        val result = all.filterByAwards(setOf(Award.BOOKER))

        assertEquals(listOf(booker2023, booker2022), result)
    }

    @Test
    fun sortsNewestYearFirstThenByAwardName() {
        val result = all.filterByAwards(Award.entries.toSet())

        assertEquals(listOf(booker2023, nba2023, pulitzer2023, booker2022), result)
    }

    @Test
    fun emptySelectionShowsNothing() {
        assertTrue(all.filterByAwards(emptySet()).isEmpty())
    }

    @Test
    fun toggleAddsMissingAward() {
        assertEquals(setOf(Award.BOOKER, Award.PULITZER), setOf(Award.BOOKER).toggle(Award.PULITZER))
    }

    @Test
    fun toggleRemovesSelectedAward() {
        assertEquals(setOf(Award.BOOKER), setOf(Award.BOOKER, Award.PULITZER).toggle(Award.PULITZER))
    }
}
