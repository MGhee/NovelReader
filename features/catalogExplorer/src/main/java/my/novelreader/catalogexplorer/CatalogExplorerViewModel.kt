package my.novelreader.catalogexplorer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import my.novelreader.algorithms.sortedByFuzzyMatch
import my.novelreader.core.LanguageCode
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.core.utils.toState
import my.novelreader.coreui.BaseViewModel
import my.novelreader.coreui.states.PagedListIteratorState
import my.novelreader.data.CatalogItem
import my.novelreader.data.ScraperRepository
import my.novelreader.scraper.domain.BookResult
import javax.inject.Inject


@HiltViewModel
internal class CatalogExplorerViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val scraperRepository: ScraperRepository,
) : BaseViewModel() {
    val databaseList = scraperRepository.databaseList()
    val sourcesList by scraperRepository.sourcesCatalogListFlow()
        .toState(viewModelScope, listOf())

    val languagesList by scraperRepository.sourcesLanguagesListFlow()
        .toState(viewModelScope, listOf())

    // Search state
    var searchMode by mutableStateOf(false)
    var searchTextInput by mutableStateOf("")
    val searchResults: SnapshotStateList<SourceSearchResult> = mutableStateListOf()

    // Popular books cache (lazy-loaded per source)
    private val _popularBooksCache = mutableMapOf<String, PagedListIteratorState<BookResult>>()
    private val _semaphore = Semaphore(3) // Limit concurrent fetches to 3

    // Search debounce
    private var searchJob: Job? = null

    fun toggleSourceLanguage(languageCode: LanguageCode) {
        val languages = appPreferences.SOURCES_LANGUAGES_ISO639_1.value
        appPreferences.SOURCES_LANGUAGES_ISO639_1.value =
            when (languageCode.iso639_1 in languages) {
                true -> languages - languageCode.iso639_1
                false -> languages + languageCode.iso639_1
            }
    }

    fun onSourceSetPinned(id: String, pinned: Boolean) {
        appPreferences.FINDER_SOURCES_PINNED.value = appPreferences.FINDER_SOURCES_PINNED
            .value.let { if (pinned) it.plus(id) else it.minus(id) }
    }

    fun getOrCreatePopularBooksIterator(source: CatalogItem): PagedListIteratorState<BookResult> {
        return _popularBooksCache.getOrPut(source.catalog.id) {
            PagedListIteratorState(viewModelScope) { index ->
                source.catalog.getCatalogList(index)
            }.also { iterator ->
                // Fetch with semaphore concurrency control
                viewModelScope.launch {
                    _semaphore.acquire()
                    try {
                        iterator.fetchNext()
                    } finally {
                        _semaphore.release()
                    }
                }
            }
        }
    }

    fun onSearchTextChange(text: String) {
        searchTextInput = text
        searchJob?.cancel()
        if (text.isBlank()) {
            searchResults.clear()
            return
        }
        searchJob = viewModelScope.launch {
            delay(400) // 400ms debounce
            performLiveSearch(text)
        }
    }

    private suspend fun performLiveSearch(query: String) {
        val activeSources = sourcesList
        searchResults.clear()

        // Create search iterators for all active sources
        activeSources.forEach { catalogItem ->
            val iterator = PagedListIteratorState<BookResult>(
                viewModelScope as CoroutineScope
            ) { index ->
                catalogItem.catalog.getCatalogSearch(index, query)
            }
            searchResults.add(SourceSearchResult(catalogItem, iterator))
            iterator.fetchNext()
        }
    }
}