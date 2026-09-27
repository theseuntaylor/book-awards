package com.theseuntaylor.bookawards.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.theseuntaylor.bookawards.data.Award
import com.theseuntaylor.bookawards.data.Nomination
import com.theseuntaylor.bookawards.data.ReadingStatus
import com.theseuntaylor.bookawards.data.shelvesByYear
import com.theseuntaylor.bookawards.ui.common.AwardsTopBar

/** Library: every year before Home's, one shelf of covers per year, newest first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    nominations: List<Nomination>?,
    selectedAwards: Set<Award>,
    onToggleAward: (Award) -> Unit,
    readingStatuses: Map<String, ReadingStatus>,
    onOpenNomination: (Nomination) -> Unit,
    snackbarHostState: SnackbarHostState,
    bottomBar: @Composable () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val shelves = remember(nominations, selectedAwards) {
        nominations?.filter { it.award in selectedAwards }?.shelvesByYear()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = bottomBar,
        topBar = {
            AwardsTopBar(
                title = "Library",
                selectedAwards = selectedAwards,
                onToggleAward = onToggleAward,
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        when {
            shelves == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            shelves.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No earlier years for the selected awards", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 16.dp)) {
                items(shelves, key = { (year, _) -> year }) { (year, books) ->
                    YearShelf(year, books, readingStatuses, onOpenNomination)
                }
            }
        }
    }
}
