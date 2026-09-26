package com.theseuntaylor.bookawards.data

fun List<Nomination>.filterByAwards(selected: Set<Award>): List<Nomination> =
    filter { it.award in selected }
        .sortedWith(compareByDescending<Nomination> { it.year }.thenBy { it.award.displayName })

fun Set<Award>.toggle(award: Award): Set<Award> =
    if (award in this) this - award else this + award
