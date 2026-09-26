package com.theseuntaylor.bookawards

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
import androidx.compose.material3.Card
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.theseuntaylor.bookawards.data.Award
import com.theseuntaylor.bookawards.data.Nomination
import com.theseuntaylor.bookawards.data.filterByAwards
import com.theseuntaylor.bookawards.data.sampleNominations
import com.theseuntaylor.bookawards.data.toggle
import com.theseuntaylor.bookawards.theme.BookAwardsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(dynamicColorScheme: ColorScheme? = null) {
    BookAwardsTheme(dynamicColorScheme) {
        var selectedAwards by remember { mutableStateOf(Award.entries.toSet()) }

        val visibleNominations = sampleNominations.filterByAwards(selectedAwards)

        Scaffold(
            topBar = { TopAppBar(title = { Text("Book Awards") }) }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                AwardFilterRow(
                    selectedAwards = selectedAwards,
                    onToggle = { award -> selectedAwards = selectedAwards.toggle(award) }
                )
                NominationList(visibleNominations)
            }
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
private fun NominationList(nominations: List<Nomination>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        nominations.groupBy { it.year }.forEach { (year, nominationsForYear) ->
            stickyHeader(key = year) {
                YearHeader(year)
            }
            items(nominationsForYear) { nomination ->
                NominationCard(nomination)
            }
        }
    }
}

@Composable
private fun YearHeader(year: Int) {
    Text(
        text = year.toString(),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp)
    )
}

@Composable
private fun NominationCard(nomination: Nomination) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(nomination.award.displayName, style = MaterialTheme.typography.labelMedium)
            Text(nomination.title, style = MaterialTheme.typography.titleMedium)
            Text(nomination.author, style = MaterialTheme.typography.bodyMedium)
            Text(nomination.status.label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
