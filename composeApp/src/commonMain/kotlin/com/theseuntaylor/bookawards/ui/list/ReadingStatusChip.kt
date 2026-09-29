package com.theseuntaylor.bookawards.ui.list

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.theseuntaylor.bookawards.data.ReadingStatus
import com.theseuntaylor.bookawards.ui.common.LabelChip

@Composable
internal fun ReadingStatusChip(status: ReadingStatus) {
    LabelChip(
        text = status.label,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
}
