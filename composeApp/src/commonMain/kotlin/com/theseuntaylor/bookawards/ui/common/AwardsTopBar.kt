package com.theseuntaylor.bookawards.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.theseuntaylor.bookawards.data.Award

/** A top app bar with the award filter chips under it, tinted together when content scrolls beneath. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AwardsTopBar(
    title: String,
    selectedAwards: Set<Award>,
    onToggleAward: (Award) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val barColor by animateColorAsState(
        if (scrollBehavior.state.overlappedFraction > 0.01f) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    )
    Surface(color = barColor) {
        Column {
            TopAppBar(
                title = { Text(title) },
                actions = actions,
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
