package com.theseuntaylor.bookawards.data

import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingListTest {

    private val jamesWithId = Nomination(Award.PULITZER, 2025, "James", "Percival Everett", NominationStatus.WINNER, "Q9")

    @Test
    fun movesFallbackKeyToWikidataIdOnceTheBookHasOne() {
        val saved = mapOf("James|Percival Everett" to ReadingStatus.READ)

        assertEquals(mapOf("Q9" to ReadingStatus.READ), saved.withCurrentBookKeys(listOf(jamesWithId)))
    }

    @Test
    fun keepsStatusAlreadySavedUnderTheId() {
        val saved = mapOf("James|Percival Everett" to ReadingStatus.WANT_TO_READ, "Q9" to ReadingStatus.READ)

        assertEquals(mapOf("Q9" to ReadingStatus.READ), saved.withCurrentBookKeys(listOf(jamesWithId)))
    }

    @Test
    fun leavesOtherKeysAlone() {
        val saved = mapOf("Q1" to ReadingStatus.READING, "Untitled|Nobody" to ReadingStatus.READ)

        assertEquals(saved, saved.withCurrentBookKeys(listOf(jamesWithId)))
    }
}
