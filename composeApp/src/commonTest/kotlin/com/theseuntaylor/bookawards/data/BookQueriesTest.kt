package com.theseuntaylor.bookawards.data

import kotlin.test.Test
import kotlin.test.assertEquals

class BookQueriesTest {

    private val prophetBooker = Nomination(Award.BOOKER, 2023, "Prophet Song", "Paul Lynch", NominationStatus.WINNER, "Q1")
    private val prophetOther = Nomination(Award.PULITZER, 2024, "Prophet Song", "Paul Lynch", NominationStatus.FINALIST, "Q1")
    private val gracelandLonglist = Nomination(Award.BOOKER, 2020, "Grace", "Paul Lynch", NominationStatus.LONGLIST, "Q2")
    private val graceShortlist = Nomination(Award.NATIONAL_BOOK_AWARD, 2020, "Grace", "Paul Lynch", NominationStatus.SHORTLIST, "Q2")
    private val redSky = Nomination(Award.BOOKER, 2013, "Red Sky in Morning", "Paul Lynch", NominationStatus.NOMINEE, "Q3")
    private val unrelated = Nomination(Award.BOOKER, 2023, "The Bee Sting", "Paul Murray", NominationStatus.SHORTLIST, "Q4")

    private val all = listOf(prophetBooker, gracelandLonglist, redSky, unrelated, prophetOther, graceShortlist)

    @Test
    fun nominationsForBookFollowTheBookAcrossAwardsNewestFirst() {
        assertEquals(listOf(prophetOther, prophetBooker), all.nominationsForBook("Q1"))
    }

    @Test
    fun otherBooksByAuthorKeepEachBooksBestResultNewestFirst() {
        assertEquals(listOf(graceShortlist, redSky), all.otherBooksByAuthor(prophetBooker))
    }

    @Test
    fun bookKeyFallsBackToTitleAndAuthorWithoutWikidataId() {
        val handAdded = Nomination(Award.PULITZER, 2025, "James", "Percival Everett", NominationStatus.WINNER)

        assertEquals("James|Percival Everett", handAdded.bookKey)
    }

    @Test
    fun parsesPipelineOutputIncludingNomineesAndMissingIds() {
        val json = """
            {"nominations": [
              {"award": "BOOKER", "year": 1971, "title": "In a Free State", "author": "V. S. Naipaul", "status": "NOMINEE", "wikidataId": "Q5"},
              {"award": "PULITZER", "year": 2025, "title": "James", "author": "Percival Everett", "status": "WINNER", "wikidataId": null}
            ]}
        """.trimIndent()

        assertEquals(
            listOf(
                Nomination(Award.BOOKER, 1971, "In a Free State", "V. S. Naipaul", NominationStatus.NOMINEE, "Q5"),
                Nomination(Award.PULITZER, 2025, "James", "Percival Everett", NominationStatus.WINNER)
            ),
            parseNominations(json)
        )
    }
}
