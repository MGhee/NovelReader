package my.novelreader.libraryexplorer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import my.novelreader.coreui.BaseViewModel
import my.novelreader.coreui.components.BookSettingsDialogState
import my.novelreader.data.AppRepository
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.core.utils.asMutableStateOf
import javax.inject.Inject

@HiltViewModel
internal class LibraryViewModel @Inject constructor(
    appPreferences: AppPreferences,
    private val appRepository: AppRepository,
    stateHandle: SavedStateHandle,
) : BaseViewModel() {
    var bookSettingsDialogState by stateHandle.asMutableStateOf<BookSettingsDialogState>(
        key = "bookSettingsDialogState",
        default = { BookSettingsDialogState.Hide },
    )
    var showBottomSheet by stateHandle.asMutableStateOf("showBottomSheet") { false }

    var readFilter by appPreferences.LIBRARY_FILTER_READ.state(viewModelScope)
    var readSort by appPreferences.LIBRARY_SORT_LAST_READ.state(viewModelScope)

    fun readFilterToggle() {
        readFilter = readFilter.next()
    }

    fun readSortToggle() {
        readSort = readSort.next()
    }

    fun bookCompletedToggle(bookUrl: String) {
        viewModelScope.launch {
            val book = appRepository.libraryBooks.get(bookUrl) ?: return@launch
            appRepository.libraryBooks.update(book.copy(completed = !book.completed))
        }
    }

    fun getBook(bookUrl: String) = appRepository.libraryBooks.getFlow(bookUrl).filterNotNull()
}

