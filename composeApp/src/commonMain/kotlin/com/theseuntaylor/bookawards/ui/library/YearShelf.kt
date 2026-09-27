package com.theseuntaylor.bookawards.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.theseuntaylor.bookawards.data.Nomination
import com.theseuntaylor.bookawards.data.ReadingStatus

/** One year of nominations as a sideways-scrolling row of covers. */
@Composable
internal fun YearShelf(
    year: Int,
    books: List<Nomination>,
    readingStatuses: Map<String, ReadingStatus>,
    onOpenNomination: (Nomination) -> Unit
) {
    Column(Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(year.toString(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(
                text = if (books.size == 1) "1 book" else "${books.size} books",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(books, key = { it.id }) { book ->
                BookCoverCard(
                    nomination = book,
                    readingStatus = readingStatuses[book.bookKey],
                    onClick = { onOpenNomination(book) }
                )
            }
        }
    }
}
