package com.theseuntaylor.bookawards.data

/**
 * Seed data only. This is a plain Kotlin list rather than JSON/CSV so it's
 * easy to browse and extend directly in the IDE with type-safe autocomplete.
 * Replace/expand this with a full per-year dataset for each award.
 */
val sampleNominations: List<Nomination> = listOf(
    Nomination(Award.BOOKER, 2023, "Prophet Song", "Paul Lynch", NominationStatus.WINNER),
    Nomination(Award.BOOKER, 2023, "The Bee Sting", "Paul Murray", NominationStatus.SHORTLIST),
    Nomination(Award.BOOKER, 2023, "Western Lane", "Chetna Maroo", NominationStatus.SHORTLIST),
    Nomination(Award.BOOKER, 2022, "The Seven Moons of Maali Almeida", "Shehan Karunatilaka", NominationStatus.WINNER),

    Nomination(Award.PULITZER, 2023, "Demon Copperhead", "Barbara Kingsolver", NominationStatus.WINNER),
    Nomination(Award.PULITZER, 2023, "Trust", "Hernan Diaz", NominationStatus.WINNER),
    Nomination(Award.PULITZER, 2022, "The Netanyahus", "Joshua Cohen", NominationStatus.WINNER),

    Nomination(Award.NATIONAL_BOOK_AWARD, 2023, "Blackouts", "Justin Torres", NominationStatus.WINNER),
    Nomination(Award.NATIONAL_BOOK_AWARD, 2022, "The Rabbit Hutch", "Tess Gunty", NominationStatus.WINNER)
)
