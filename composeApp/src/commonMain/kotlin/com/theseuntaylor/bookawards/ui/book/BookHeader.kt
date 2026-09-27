package com.theseuntaylor.bookawards.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.theseuntaylor.bookawards.data.Nomination

@Composable
internal fun BookHeader(nomination: Nomination, bookInfo: BookInfoState) {
    val info = (bookInfo as? BookInfoState.Found)?.info
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 112.dp, height = 168.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            info?.coverUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Cover of ${nomination.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(nomination.title, style = MaterialTheme.typography.headlineSmall)
            Text(nomination.author, style = MaterialTheme.typography.titleMedium)
            info?.firstPublished?.let {
                Text(
                    "First published $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
