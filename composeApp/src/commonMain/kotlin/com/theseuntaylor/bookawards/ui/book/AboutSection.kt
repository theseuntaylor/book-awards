package com.theseuntaylor.bookawards.ui.book

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@Composable
internal fun AboutSection(bookInfo: BookInfoState) {
    Column {
        SectionTitle("About")
        val bodyModifier = Modifier.padding(horizontal = 16.dp)
        when (bookInfo) {
            BookInfoState.Loading -> Text("Looking this book up on Open Library…", modifier = bodyModifier, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BookInfoState.Unavailable -> Text("Open Library doesn't have details for this book.", modifier = bodyModifier, color = MaterialTheme.colorScheme.onSurfaceVariant)
            is BookInfoState.Found -> {
                val uriHandler = LocalUriHandler.current
                bookInfo.info.description?.let { Text(it, modifier = bodyModifier, style = MaterialTheme.typography.bodyMedium) }
                TextButton(onClick = { uriHandler.openUri(bookInfo.info.openLibraryUrl) }, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text("View on Open Library")
                }
            }
        }
    }
}
