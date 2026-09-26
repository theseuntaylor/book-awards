package com.theseuntaylor.bookawards

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.theseuntaylor.bookawards.data.Award
import com.theseuntaylor.bookawards.data.Nomination
import com.theseuntaylor.bookawards.data.NominationStatus
import com.theseuntaylor.bookawards.data.filterByAwards
import com.theseuntaylor.bookawards.data.sampleNominations
import com.theseuntaylor.bookawards.data.toggle
import com.theseuntaylor.bookawards.theme.BookAwardsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    BookAwardsTheme {
        var selectedAwards by remember { mutableStateOf(Award.entries.toSet()) }

        val visibleNominations = sampleNominations.filterByAwards(selectedAwards)
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
            topBar = {
                // The chips live in the bar's surface so both change color together on scroll.
                Surface(color = barColor) {
                    Column {
                        TopAppBar(
                            title = { Text("Book Awards") },
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent
                            )
                        )
                        AwardFilterRow(
                            selectedAwards = selectedAwards,
                            onToggle = { award -> selectedAwards = selectedAwards.toggle(award) }
                        )
                    }
                }
            }
        ) { padding ->
            NominationList(
                nominations = visibleNominations,
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
                bottomInset = padding.calculateBottomPadding()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AwardFilterRow(
    selectedAwards: Set<Award>,
    onToggle: (Award) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Award.entries.forEach { award ->
            val selected = award in selectedAwards
            FilterChip(
                selected = selected,
                onClick = { onToggle(award) },
                label = { Text(award.displayName) },
                leadingIcon = if (selected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun NominationList(
    nominations: List<Nomination>,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 16.dp + bottomInset)
    ) {
        nominations.groupBy { it.year }.forEach { (year, nominationsForYear) ->
            stickyHeader(key = year) {
                YearHeader(year)
            }
            items(nominationsForYear) { nomination ->
                NominationListItem(nomination)
            }
        }
    }
}

@Composable
private fun YearHeader(year: Int) {
    Text(
        text = year.toString(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun NominationListItem(nomination: Nomination) {
    val isWinner = nomination.status == NominationStatus.WINNER
    ListItem(
        overlineContent = { Text(nomination.award.displayName) },
        headlineContent = { Text(nomination.title) },
        supportingContent = { Text(nomination.author) },
        trailingContent = {
            Text(
                text = nomination.status.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}
