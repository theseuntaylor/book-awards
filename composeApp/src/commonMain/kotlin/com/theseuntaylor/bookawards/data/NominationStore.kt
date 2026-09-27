package com.theseuntaylor.bookawards.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface RefreshOutcome {
    data class Updated(val newNominations: Int) : RefreshOutcome
    data object UpToDate : RefreshOutcome
    data object Failed : RefreshOutcome
}

/** Shows the best data on hand immediately, then swaps in the published file when it's newer. */
class NominationStore(
    private val remote: RemoteNominations,
    private val cache: NominationCache,
    private val readingList: ReadingList
) {
    private val state = MutableStateFlow<List<Nomination>?>(null)
    val nominations: StateFlow<List<Nomination>?> = state.asStateFlow()

    private val lock = Mutex()
    private var bundledHash = 0
    private var etag: String? = null
    private var startedRefresh = false

    /** Loads once per process and checks for new data the first time; later calls (e.g. rotation) do nothing. */
    suspend fun start(): RefreshOutcome? {
        lock.withLock {
            if (startedRefresh) return null
            startedRefresh = true
            val bundledText = loadBundledNominationsText()
            bundledHash = bundledText.hashCode()
            val cached = cache.read(bundledHash)?.let { entry ->
                runCatching { parseNominations(entry.text) }.getOrNull()
                    ?.takeIf { it.isNotEmpty() }
                    ?.also { etag = entry.etag }
            }
            publish(cached ?: parseNominations(bundledText))
        }
        return refresh()
    }

    suspend fun refresh(): RefreshOutcome = lock.withLock {
        val current = state.value ?: return RefreshOutcome.Failed
        when (val result = remote.fetchLatest(etag)) {
            RemoteResult.NotModified -> RefreshOutcome.UpToDate
            RemoteResult.Failed -> RefreshOutcome.Failed
            is RemoteResult.Updated -> {
                val update = runCatching { parseNominations(result.text) }.getOrNull()
                if (update == null || !isPlausibleUpdate(update, current)) return RefreshOutcome.Failed
                cache.write(bundledHash, NominationCache.Entry(result.text, result.etag))
                etag = result.etag
                val known = current.mapTo(HashSet()) { it.id }
                publish(update)
                val added = update.count { it.id !in known }
                if (added > 0) RefreshOutcome.Updated(added) else RefreshOutcome.UpToDate
            }
        }
    }

    private fun publish(nominations: List<Nomination>) {
        readingList.adoptCurrentKeys(nominations)
        state.value = nominations
    }
}
