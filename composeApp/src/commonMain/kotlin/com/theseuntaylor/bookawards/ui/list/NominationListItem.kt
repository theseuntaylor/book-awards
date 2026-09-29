package com.theseuntaylor.bookawards.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.theseuntaylor.bookawards.data.Nomination
import com.theseuntaylor.bookawards.data.NominationStatus
import com.theseuntaylor.bookawards.data.ReadingStatus
import com.theseuntaylor.bookawards.ui.common.WinnerChip

@Composable
internal fun NominationListItem(
    nomination: Nomination,
    readingStatus: ReadingStatus?,
    onClick: () -> Unit
) {
    val isWinner = nomination.status == NominationStatus.WINNER
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        overlineContent = { Text(nomination.award.displayName) },
        headlineContent = { Text(nomination.title) },
        supportingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Long author names shrink so the chip stays visible.
                Text(nomination.author, modifier = Modifier.weight(1f, fill = false))
                readingStatus?.let { ReadingStatusChip(it) }
            }
        },
        trailingContent = {
            if (isWinner) {
                WinnerChip()
            } else {
                Text(
                    text = nomination.status.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
