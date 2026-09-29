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
import com.theseuntaylor.bookawards.data.launchMessage
import com.theseuntaylor.bookawards.data.message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DataRefresh internal constructor(
    private val store: NominationStore,
    private val scope: CoroutineScope,
    private val snackbarHostState: SnackbarHostState
) {
    var refreshing by mutableStateOf(false)
        private set

    fun refreshNow() {
        if (refreshing) return
        scope.launch {
            refreshing = true
            val outcome = store.refresh()
            refreshing = false
            snackbarHostState.showSnackbar(outcome.message())
        }
    }

    internal suspend fun start() {
        store.start()?.launchMessage()?.let { snackbarHostState.showSnackbar(it) }
    }
}

@Composable
fun rememberDataRefresh(store: NominationStore, snackbarHostState: SnackbarHostState): DataRefresh {
    val scope = rememberCoroutineScope()
    val refresh = remember(store, snackbarHostState) { DataRefresh(store, scope, snackbarHostState) }
    LaunchedEffect(refresh) { refresh.start() }
    return refresh
}
