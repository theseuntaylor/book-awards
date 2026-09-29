package com.theseuntaylor.bookawards.notifications

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.theseuntaylor.bookawards.AppDependencies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Compose state over [ReminderScheduler], reporting results in a snackbar. */
class AnnouncementReminders internal constructor(
    private val scheduler: ReminderScheduler,
    private val scope: CoroutineScope,
    private val snackbarHostState: SnackbarHostState
) {
    var enabled by mutableStateOf(scheduler.enabled)
        private set

    fun toggle() {
        scope.launch {
            val message = scheduler.toggle()
            enabled = scheduler.enabled
            snackbarHostState.showSnackbar(message)
        }
    }
}

@Composable
fun rememberAnnouncementReminders(snackbarHostState: SnackbarHostState): AnnouncementReminders {
    val notifier = rememberAnnouncementNotifier()
    val scope = rememberCoroutineScope()
    val scheduler = remember(notifier) { ReminderScheduler(notifier, AppDependencies.notificationPreferences) }
    val reminders = remember(scheduler, snackbarHostState) { AnnouncementReminders(scheduler, scope, snackbarHostState) }
    LaunchedEffect(scheduler) { scheduler.reschedule() }
    return reminders
}
