package com.theseuntaylor.bookawards.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.theseuntaylor.bookawards.data.Nomination
import com.theseuntaylor.bookawards.data.NominationStatus
import com.theseuntaylor.bookawards.data.ReadingStatus
import com.theseuntaylor.bookawards.ui.common.WinnerChip
import com.theseuntaylor.bookawards.ui.list.ReadingStatusChip

private val coverWidth = 104.dp
private val coverHeight = 156.dp

@Composable
internal fun BookCoverCard(
    nomination: Nomination,
    readingStatus: ReadingStatus?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(coverWidth).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(coverWidth, coverHeight)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // The title stands in for a missing cover, and shows while one loads.
            Text(
                text = nomination.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center).padding(8.dp)
            )
            nomination.coverThumbnailUrl?.let { url ->
                // Only describe the cover once it's on screen; until then the title placeholder is what's shown.
                var loaded by remember(url) { mutableStateOf(false) }
                AsyncImage(
                    model = url,
                    contentDescription = if (loaded) "Cover of ${nomination.title}" else null,
                    contentScale = ContentScale.Crop,
                    onSuccess = { loaded = true },
                    modifier = Modifier.matchParentSize()
                )
            }
            readingStatus?.let {
                Box(Modifier.align(Alignment.BottomStart).padding(4.dp)) { ReadingStatusChip(it) }
            }
        }
        Text(
            text = nomination.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (nomination.status == NominationStatus.WINNER) {
            WinnerChip()
        } else {
            Text(
                text = nomination.status.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = nomination.award.shortName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
