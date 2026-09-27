package com.theseuntaylor.bookawards.ui.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.theseuntaylor.bookawards.data.Nomination
import com.theseuntaylor.bookawards.data.ReadingStatus

@Composable
internal fun NominationList(
    nominations: List<Nomination>,
    readingStatuses: Map<String, ReadingStatus>,
    onOpenNomination: (Nomination) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    footer: (@Composable () -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val atTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 } }
    // The list keeps the row on screen when items are inserted above it, which would hide newly
    // downloaded nominations; staying at the top shows them instead.
    if (atTop) listState.requestScrollToItem(0)

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 16.dp + bottomInset)
    ) {
        nominations.groupBy { it.year }.forEach { (year, nominationsForYear) ->
            stickyHeader(key = year) {
                YearHeader(year)
            }
            items(nominationsForYear, key = { it.id }) { nomination ->
                NominationListItem(
                    nomination = nomination,
                    readingStatus = readingStatuses[nomination.bookKey],
                    onClick = { onOpenNomination(nomination) }
                )
            }
        }
        if (footer != null) item(key = "footer") { footer() }
    }
}
