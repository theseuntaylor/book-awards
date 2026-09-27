package com.theseuntaylor.bookawards

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.theseuntaylor.bookawards.data.Award
import com.theseuntaylor.bookawards.data.toggle
import com.theseuntaylor.bookawards.notifications.rememberAnnouncementReminders
import com.theseuntaylor.bookawards.theme.BookAwardsTheme
import com.theseuntaylor.bookawards.ui.book.BookScreen
import com.theseuntaylor.bookawards.ui.list.ListScreen
import kotlinx.serialization.Serializable

@Serializable
private object ListRoute

@Serializable
private data class BookRoute(val nominationId: String)

@OptIn(ExperimentalCoilApi::class)
@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(AppDependencies.httpClient)) }
            .build()
    }

    BookAwardsTheme {
        val navController = rememberNavController()
        val readingList = AppDependencies.readingList
        val store = AppDependencies.nominationStore
        val nominations by store.nominations.collectAsState()
        var selectedAwards by remember { mutableStateOf(Award.entries.toSet()) }
        val readingStatuses by readingList.all.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val dataRefresh = rememberDataRefresh(store, snackbarHostState)
        val reminders = rememberAnnouncementReminders(snackbarHostState)

        NavHost(navController, startDestination = ListRoute) {
            composable<ListRoute> {
                ListScreen(
                    nominations = nominations,
                    selectedAwards = selectedAwards,
                    onToggleAward = { selectedAwards = selectedAwards.toggle(it) },
                    readingStatuses = readingStatuses,
                    onOpenNomination = { navController.navigate(BookRoute(it.id)) },
                    remindersOn = reminders.enabled,
                    onToggleReminders = reminders::toggle,
                    refreshing = dataRefresh.refreshing,
                    onRefresh = dataRefresh::refreshNow,
                    snackbarHostState = snackbarHostState
                )
            }
            composable<BookRoute> { entry ->
                val id = entry.toRoute<BookRoute>().nominationId
                val all = nominations ?: return@composable
                val nomination = all.find { it.id == id } ?: return@composable
                BookScreen(
                    nomination = nomination,
                    allNominations = all,
                    readingStatus = readingStatuses[nomination.bookKey],
                    onReadingStatusChange = { readingList.set(nomination.bookKey, it) },
                    onOpenNomination = { navController.navigate(BookRoute(it.id)) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
