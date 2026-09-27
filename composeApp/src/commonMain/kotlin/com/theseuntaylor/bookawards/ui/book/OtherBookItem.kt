package com.theseuntaylor.bookawards.ui.book

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.theseuntaylor.bookawards.data.Nomination

@Composable
internal fun OtherBookItem(nomination: Nomination, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(nomination.title) },
        supportingContent = { Text("${nomination.award.displayName} ${nomination.status.label.lowercase()}, ${nomination.year}") }
    )
}
