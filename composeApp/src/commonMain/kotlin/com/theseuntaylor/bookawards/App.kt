package com.theseuntaylor.bookawards

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.theseuntaylor.bookawards.data.Award
import com.theseuntaylor.bookawards.data.currentYear
import com.theseuntaylor.bookawards.data.forHome
import com.theseuntaylor.bookawards.data.forLibrary
import com.theseuntaylor.bookawards.data.toggle
import com.theseuntaylor.bookawards.notifications.rememberAnnouncementReminders
import com.theseuntaylor.bookawards.theme.BookAwardsTheme
import com.theseuntaylor.bookawards.ui.book.BookScreen
import com.theseuntaylor.bookawards.ui.library.LibraryScreen
import com.theseuntaylor.bookawards.ui.list.ListScreen
import com.theseuntaylor.bookawards.ui.navigation.AppNavigationBar
import com.theseuntaylor.bookawards.ui.navigation.AppTab
import kotlinx.serialization.Serializable

@Serializable
private object HomeRoute

@Serializable
private object LibraryRoute

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
        val year = remember { currentYear() }
        val selectTab = { tab: AppTab -> navController.selectTab(tab) }

        NavHost(navController, startDestination = HomeRoute) {
            composable<HomeRoute> {
                ListScreen(
                    nominations = remember(nominations, year) { nominations?.forHome(year) },
                    selectedAwards = selectedAwards,
                    onToggleAward = { selectedAwards = selectedAwards.toggle(it) },
                    readingStatuses = readingStatuses,
                    onOpenNomination = { navController.navigate(BookRoute(it.id)) },
                    onOpenLibrary = { selectTab(AppTab.LIBRARY) },
                    remindersOn = reminders.enabled,
                    onToggleReminders = reminders::toggle,
                    refreshing = dataRefresh.refreshing,
                    onRefresh = dataRefresh::refreshNow,
                    snackbarHostState = snackbarHostState,
                    bottomBar = { AppNavigationBar(AppTab.HOME, selectTab) }
                )
            }
            composable<LibraryRoute> {
                LibraryScreen(
                    nominations = remember(nominations, year) { nominations?.forLibrary(year) },
                    selectedAwards = selectedAwards,
                    onToggleAward = { selectedAwards = selectedAwards.toggle(it) },
                    readingStatuses = readingStatuses,
                    onOpenNomination = { navController.navigate(BookRoute(it.id)) },
                    snackbarHostState = snackbarHostState,
                    bottomBar = { AppNavigationBar(AppTab.LIBRARY, selectTab) }
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

/** Standard bottom-navigation behaviour: one copy of each tab, each keeping its own scroll position. */
private fun NavController.selectTab(tab: AppTab) {
    val route: Any = when (tab) {
        AppTab.HOME -> HomeRoute
        AppTab.LIBRARY -> LibraryRoute
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
