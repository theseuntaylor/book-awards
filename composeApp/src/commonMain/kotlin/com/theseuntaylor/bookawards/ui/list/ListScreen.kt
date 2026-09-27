package com.theseuntaylor.bookawards.ui.list

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.theseuntaylor.bookawards.data.Award
import com.theseuntaylor.bookawards.data.Nomination
import com.theseuntaylor.bookawards.data.ReadingStatus
import com.theseuntaylor.bookawards.data.filterByAwards

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    nominations: List<Nomination>?,
    selectedAwards: Set<Award>,
    onToggleAward: (Award) -> Unit,
    readingStatuses: Map<String, ReadingStatus>,
    onOpenNomination: (Nomination) -> Unit,
    remindersOn: Boolean,
    onToggleReminders: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val barColor by animateColorAsState(
        if (scrollBehavior.state.overlappedFraction > 0.01f) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // The chips live in the bar's surface so both change color together on scroll.
            Surface(color = barColor) {
                Column {
                    TopAppBar(
                        title = { Text("Book Awards") },
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
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        )
                    )
                    AwardFilterRow(selectedAwards = selectedAwards, onToggle = onToggleAward)
                }
            }
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
                    bottomInset = padding.calculateBottomPadding()
                )
            }
        }
    }
}
