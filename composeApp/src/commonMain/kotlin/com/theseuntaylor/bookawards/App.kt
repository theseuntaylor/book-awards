package com.theseuntaylor.bookawards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.theseuntaylor.bookawards.data.sampleNominations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        var selectedAwards by remember { mutableStateOf(Award.entries.toSet()) }

        val visibleNominations = sampleNominations
            .filter { it.award in selectedAwards }
            .sortedWith(compareByDescending<Nomination> { it.year }.thenBy { it.award.displayName })

        Scaffold(
            topBar = { TopAppBar(title = { Text("Book Awards") }) }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                AwardFilterRow(
                    selectedAwards = selectedAwards,
                    onToggle = { award ->
                        selectedAwards = if (award in selectedAwards) {
                            selectedAwards - award
                        } else {
                            selectedAwards + award
                        }
                    }
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
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Award.entries.forEach { award ->
            FilterChip(
                selected = award in selectedAwards,
                onClick = { onToggle(award) },
                label = { Text(award.displayName) }
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
        items(nominations) { nomination ->
            NominationCard(nomination)
        }
    }
}

@Composable
private fun NominationCard(nomination: Nomination) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${nomination.year} · ${nomination.award.displayName}", style = MaterialTheme.typography.labelMedium)
            Text(nomination.title, style = MaterialTheme.typography.titleMedium)
            Text(nomination.author, style = MaterialTheme.typography.bodyMedium)
            Text(nomination.status.label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
