package com.theseuntaylor.bookawards.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Ends Home's list, so the older years are one tap away rather than only behind the tab. */
@Composable
internal fun LibraryLink(onOpenLibrary: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        OutlinedButton(onClick = onOpenLibrary) {
            Text("Earlier years are in the Library")
        }
    }
}
