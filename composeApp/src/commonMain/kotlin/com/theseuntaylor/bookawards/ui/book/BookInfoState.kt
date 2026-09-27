package com.theseuntaylor.bookawards.ui.book

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.theseuntaylor.bookawards.AppDependencies
import com.theseuntaylor.bookawards.data.BookInfo
import com.theseuntaylor.bookawards.data.Nomination
import kotlinx.coroutines.CancellationException

internal sealed interface BookInfoState {
    data object Loading : BookInfoState
    data object Unavailable : BookInfoState
    data class Found(val info: BookInfo) : BookInfoState
}

@Composable
internal fun rememberBookInfo(nomination: Nomination): State<BookInfoState> =
    produceState<BookInfoState>(BookInfoState.Loading, nomination.bookKey) {
        value = try {
            AppDependencies.openLibrary.lookup(nomination.title, nomination.author)
                ?.let(BookInfoState::Found) ?: BookInfoState.Unavailable
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BookInfoState.Unavailable
        }
    }
