package com.theseuntaylor.bookawards.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.theseuntaylor.bookawards.data.Award
import com.theseuntaylor.bookawards.data.Nomination
import com.theseuntaylor.bookawards.data.ReadingStatus
import com.theseuntaylor.bookawards.data.filterByAwards
import com.theseuntaylor.bookawards.ui.common.AwardsTopBar

/** Home: the most recent award years, newest first. Older years live in the Library tab. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    nominations: List<Nomination>?,
    selectedAwards: Set<Award>,
    onToggleAward: (Award) -> Unit,
    readingStatuses: Map<String, ReadingStatus>,
    onOpenNomination: (Nomination) -> Unit,
    onOpenLibrary: () -> Unit,
    remindersOn: Boolean,
    onToggleReminders: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    snackbarHostState: SnackbarHostState,
    bottomBar: @Composable () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = bottomBar,
        topBar = {
            AwardsTopBar(
                title = "Book Awards",
                selectedAwards = selectedAwards,
                onToggleAward = onToggleAward,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onToggleReminders) {
                        Icon(
                            imageVector = if (remindersOn) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = if (remindersOn) {
                                "Turn off announcement reminders"
                            } else {
                                "Remind me on announcement days"
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (nominations == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.padding(top = padding.calculateTopPadding())
            ) {
                NominationList(
                    nominations = nominations.filterByAwards(selectedAwards),
                    readingStatuses = readingStatuses,
                    onOpenNomination = onOpenNomination,
                    bottomInset = padding.calculateBottomPadding(),
                    footer = { LibraryLink(onOpenLibrary) }
                )
            }
        }
    }
}
