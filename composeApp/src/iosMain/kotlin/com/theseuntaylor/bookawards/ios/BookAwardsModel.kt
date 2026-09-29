package com.theseuntaylor.bookawards.ios

import com.theseuntaylor.bookawards.AppDependencies
import com.theseuntaylor.bookawards.data.Award
import com.theseuntaylor.bookawards.data.BookInfo
import com.theseuntaylor.bookawards.data.Nomination
import com.theseuntaylor.bookawards.data.ReadingStatus
import com.theseuntaylor.bookawards.data.currentYear
import com.theseuntaylor.bookawards.data.filterByAwards
import com.theseuntaylor.bookawards.data.forHome
import com.theseuntaylor.bookawards.data.forLibrary
import com.theseuntaylor.bookawards.data.launchMessage
import com.theseuntaylor.bookawards.data.message
import com.theseuntaylor.bookawards.data.nominationsForBook
import com.theseuntaylor.bookawards.data.otherBooksByAuthor
import com.theseuntaylor.bookawards.data.shelvesByYear
import com.theseuntaylor.bookawards.notifications.IosAnnouncementNotifier
import com.theseuntaylor.bookawards.notifications.ReminderScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

data class YearGroup(val year: Int, val nominations: List<Nomination>)

class Subscription internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}

/**
 * The shared data layer shaped for SwiftUI: flows become main-thread callbacks, and every suspend function
 * catches its own errors so Swift only sees `async` calls that return a value.
 */
class BookAwardsModel {
    private val scope = MainScope()
    private val store = AppDependencies.nominationStore
    private val readingList = AppDependencies.readingList
    private val reminders = ReminderScheduler(IosAnnouncementNotifier(), AppDependencies.notificationPreferences)
    private val year = currentYear()

    fun observeNominations(onChange: (List<Nomination>?) -> Unit): Subscription =
        Subscription(scope.launch { store.nominations.collect(onChange) })

    fun observeReadingStatuses(onChange: (Map<String, ReadingStatus>) -> Unit): Subscription =
        Subscription(scope.launch { readingList.all.collect(onChange) })

    /** Loads data and checks for new nominations once; returns a message only if something new arrived. */
    suspend fun start(): String? {
        reminders.reschedule()
        return try {
            store.start()?.launchMessage()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    suspend fun refresh(): String = store.refresh().message()

    fun homeYears(all: List<Nomination>, awards: Set<Award>): List<YearGroup> =
        all.forHome(year).filterByAwards(awards).groupBy { it.year }.map { (year, books) -> YearGroup(year, books) }

    fun libraryShelves(all: List<Nomination>, awards: Set<Award>): List<YearGroup> =
        all.forLibrary(year).filter { it.award in awards }.shelvesByYear().map { (year, books) -> YearGroup(year, books) }

    fun nominationsForBook(all: List<Nomination>, nomination: Nomination): List<Nomination> =
        all.nominationsForBook(nomination.bookKey)

    fun otherBooksByAuthor(all: List<Nomination>, nomination: Nomination): List<Nomination> =
        all.otherBooksByAuthor(nomination)

    fun setReadingStatus(nomination: Nomination, status: ReadingStatus?) = readingList.set(nomination.bookKey, status)

    /** Null when Open Library has nothing or can't be reached. */
    suspend fun lookUpBook(nomination: Nomination): BookInfo? = try {
        AppDependencies.openLibrary.lookup(nomination.title, nomination.author)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    val remindersEnabled: Boolean get() = reminders.enabled

    suspend fun toggleReminders(): String = reminders.toggle()
}
