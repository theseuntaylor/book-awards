package com.theseuntaylor.bookawards.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
internal fun YearHeader(year: Int) {
    Text(
        text = year.toString(),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            // A pinned header is drawn over the row scrolling under it; without this, taps fall through to that row.
            .pointerInput(Unit) { detectTapGestures {} }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
