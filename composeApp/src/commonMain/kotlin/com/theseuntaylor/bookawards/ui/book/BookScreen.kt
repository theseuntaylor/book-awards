package com.theseuntaylor.bookawards.ui.book

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import com.theseuntaylor.bookawards.data.Nomination
import com.theseuntaylor.bookawards.data.ReadingStatus
import com.theseuntaylor.bookawards.data.nominationsForBook
import com.theseuntaylor.bookawards.data.otherBooksByAuthor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(
    nomination: Nomination,
    allNominations: List<Nomination>,
    readingStatus: ReadingStatus?,
    onReadingStatusChange: (ReadingStatus?) -> Unit,
    onOpenNomination: (Nomination) -> Unit,
    onBack: () -> Unit
) {
    val bookInfo by rememberBookInfo(nomination)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 16.dp)) {
            item { BookHeader(nomination, bookInfo) }
            item { ReadingStatusRow(readingStatus, onReadingStatusChange) }

            item { SectionTitle("Nominations") }
            items(allNominations.nominationsForBook(nomination.bookKey), key = { it.id }) { entry ->
                BookNominationItem(entry)
            }

            item { AboutSection(bookInfo) }

            val otherBooks = allNominations.otherBooksByAuthor(nomination)
            if (otherBooks.isNotEmpty()) {
                item { SectionTitle("Also by ${nomination.author}") }
                items(otherBooks, key = { it.id }) { other ->
                    OtherBookItem(other, onClick = { onOpenNomination(other) })
                }
            }
        }
    }
}
