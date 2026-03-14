package my.novelreader.sourceexplorer

import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.novelreader.coreui.BaseViewModel
import my.novelreader.coreui.components.ToolbarMode
import my.novelreader.coreui.states.PagedListIteratorState
import my.novelreader.data.AppRepository
import my.novelreader.mappers.mapToBookMetadata
import my.novelreader.core.Toasty
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.core.utils.StateExtra_String
import my.novelreader.core.utils.asMutableStateOf
import my.novelreader.feature.local_database.BookMetadata
import my.novelreader.scraper.Scraper
import javax.inject.Inject

interface SourceCatalogStateBundle {
    var sourceBaseUrl: String
}


@HiltViewModel
internal class SourceCatalogViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val toasty: Toasty,
    stateHandle: SavedStateHandle,
    appPreferences: AppPreferences,
    scraper: Scraper,
) : BaseViewModel(), SourceCatalogStateBundle {

    override var sourceBaseUrl by StateExtra_String(stateHandle)
    private val source = scraper.getCompatibleSourceCatalog(sourceBaseUrl)!!

    val state = SourceCatalogScreenState(
        sourceCatalogNameStrId = mutableIntStateOf(source.nameStrId),
        searchTextInput = stateHandle.asMutableStateOf("searchTextInput") { "" },
        toolbarMode = stateHandle.asMutableStateOf("toolbarMode") { ToolbarMode.MAIN },
        fetchIterator = PagedListIteratorState(viewModelScope) {
            source.getCatalogList(it).mapToBookMetadata()
        },
        listLayoutMode = appPreferences.BOOKS_LIST_LAYOUT_MODE.state(viewModelScope),
    )

    init {
        onSearchCatalog()
    }

    fun onSearchCatalog() {
        state.fetchIterator.setFunction { source.getCatalogList(it).mapToBookMetadata() }
        state.fetchIterator.reset()
        state.fetchIterator.fetchNext()
    }

    fun onSearchText(input: String) {
        state.fetchIterator.setFunction { source.getCatalogSearch(it, input).mapToBookMetadata() }
        state.fetchIterator.reset()
        state.fetchIterator.fetchNext()
    }

    fun addToLibraryToggle(book: BookMetadata) =
        viewModelScope.launch(Dispatchers.IO)
        {
            val isInLibrary =
                appRepository.toggleBookmark(bookUrl = book.url, bookTitle = book.title)
            val res = if (isInLibrary) R.string.added_to_library else R.string.removed_from_library
            toasty.show(res)
        }
}