package com.theseuntaylor.bookawards.ui.book

import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.theseuntaylor.bookawards.data.Nomination

@Composable
internal fun BookNominationItem(nomination: Nomination) {
    ListItem(
        overlineContent = { Text(nomination.award.displayName) },
        headlineContent = { Text("${nomination.year} · ${nomination.status.label}") }
    )
}
