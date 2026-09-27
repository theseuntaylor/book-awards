package com.theseuntaylor.bookawards.ui.book

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.theseuntaylor.bookawards.data.ReadingStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadingStatusRow(readingStatus: ReadingStatus?, onChange: (ReadingStatus?) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
        ReadingStatus.entries.forEachIndexed { index, status ->
            SegmentedButton(
                selected = readingStatus == status,
                // Tapping the selected status again takes the book off your list.
                onClick = { onChange(if (readingStatus == status) null else status) },
                shape = SegmentedButtonDefaults.itemShape(index, ReadingStatus.entries.size)
            ) {
                Text(status.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
