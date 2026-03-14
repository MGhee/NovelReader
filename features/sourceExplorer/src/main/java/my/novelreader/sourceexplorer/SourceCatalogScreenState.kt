package my.novelreader.sourceexplorer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import my.novelreader.coreui.components.ToolbarMode
import my.novelreader.coreui.states.PagedListIteratorState
import my.novelreader.core.appPreferences.ListLayoutMode
import my.novelreader.feature.local_database.BookMetadata

internal data class SourceCatalogScreenState(
    val sourceCatalogNameStrId: State<Int>,
    val searchTextInput: MutableState<String>,
    val fetchIterator: PagedListIteratorState<BookMetadata>,
    val toolbarMode: MutableState<ToolbarMode>,
    val listLayoutMode: MutableState<ListLayoutMode>,
)