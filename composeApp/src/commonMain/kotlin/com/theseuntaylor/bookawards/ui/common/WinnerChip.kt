package com.theseuntaylor.bookawards.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.theseuntaylor.bookawards.data.NominationStatus

/** Primary-tinted so a win stands apart from reading-status chips, which use the secondary colours. */
@Composable
fun WinnerChip() {
    LabelChip(
        text = NominationStatus.WINNER.label,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
}
