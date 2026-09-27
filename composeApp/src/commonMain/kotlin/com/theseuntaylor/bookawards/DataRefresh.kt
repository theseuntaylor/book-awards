package com.theseuntaylor.bookawards

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.theseuntaylor.bookawards.data.NominationStore
import com.theseuntaylor.bookawards.data.RefreshOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DataRefresh internal constructor(
    private val store: NominationStore,
    private val scope: CoroutineScope,
    private val snackbarHostState: SnackbarHostState
) {
    var refreshing by mutableStateOf(false)
        private set

    /** Pull to refresh: always says what happened, since a silent failure looks like nothing did. */
    fun refreshNow() {
        if (refreshing) return
        scope.launch {
            refreshing = true
            val outcome = store.refresh()
            refreshing = false
            snackbarHostState.showSnackbar(
                when (outcome) {
                    is RefreshOutcome.Updated -> newNominationsMessage(outcome.newNominations)
                    RefreshOutcome.UpToDate -> "You're up to date"
                    RefreshOutcome.Failed -> "Couldn't check for new nominations"
                }
            )
        }
    }

    /** The launch check stays quiet unless it actually brought in something new. */
    internal suspend fun start() {
        val outcome = store.start()
        if (outcome is RefreshOutcome.Updated) snackbarHostState.showSnackbar(newNominationsMessage(outcome.newNominations))
    }

    private fun newNominationsMessage(count: Int) =
        if (count == 1) "1 new nomination" else "$count new nominations"
}

@Composable
fun rememberDataRefresh(store: NominationStore, snackbarHostState: SnackbarHostState): DataRefresh {
    val scope = rememberCoroutineScope()
    val refresh = remember(store, snackbarHostState) { DataRefresh(store, scope, snackbarHostState) }
    LaunchedEffect(refresh) { refresh.start() }
    return refresh
}
