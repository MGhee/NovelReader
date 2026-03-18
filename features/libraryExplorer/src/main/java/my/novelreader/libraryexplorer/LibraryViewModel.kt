package my.novelreader.libraryexplorer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import my.novelreader.core.Toasty
import my.novelreader.coreui.BaseViewModel
import my.novelreader.data.AppRepository
import my.novelreader.core.AppFileResolver
import my.novelreader.core.removeLocalUriPrefix
import java.io.File
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.interactor.WorkersInteractions
import my.novelreader.core.utils.asMutableStateOf
import my.novelreader.feature.local_database.BookWithContext
import javax.inject.Inject

@HiltViewModel
internal class LibraryViewModel @Inject constructor(
    appPreferences: AppPreferences,
    private val appRepository: AppRepository,
    private val appFileResolver: AppFileResolver,
    private val workersInteractions: WorkersInteractions,
    private val toasty: Toasty,
    stateHandle: SavedStateHandle,
) : BaseViewModel() {
    var showBottomSheet by stateHandle.asMutableStateOf("showBottomSheet") { false }
    var bookOptionsSheetBook by stateHandle.asMutableStateOf<BookWithContext?>(
        key = "bookOptionsSheetBook",
        default = { null }
    )

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

    suspend fun getBookOpenChapterUrl(bookUrl: String): String? {
        val book = appRepository.libraryBooks.get(bookUrl) ?: return null
        val lastReadChapterUrl = book.lastReadChapter

        if (lastReadChapterUrl != null) {
            val hasLastReadChapter = appRepository.bookChapters.get(lastReadChapterUrl) != null
            if (hasLastReadChapter) {
                return lastReadChapterUrl
            }

            toasty.show(R.string.last_read_chapter_not_found_opening_first)
        }

        val firstChapterUrl = appRepository.bookChapters.getFirstChapter(bookUrl)?.url
        if (firstChapterUrl == null) {
            toasty.show(R.string.unable_to_open_book_no_chapters_found)
            return null
        }

        return firstChapterUrl
    }

    fun downloadAllBookChapters(bookUrl: String) {
        toasty.show(R.string.downloading_all_chapters_started)
        viewModelScope.launch {
            val chapters = appRepository.bookChapters.chapters(bookUrl)
            var downloaded = 0
            chapters.forEach { chapter ->
                appRepository.chapterBody.fetchBody(chapter.url)
                downloaded++
            }
        }
    }

    fun deleteDownloadedChapters(bookUrl: String) {
        viewModelScope.launch {
            val chapters = appRepository.bookChapters.chapters(bookUrl).map { it.url }
            if (chapters.isNotEmpty()) {
                appRepository.chapterBody.removeRows(chapters)
            }

            // delete local chapter files
            try {
                val localFolderName = appFileResolver.getLocalBookFolderName(bookUrl).removeLocalUriPrefix
                val bookFolder = java.io.File(appFileResolver.folderBooks, localFolderName)
                if (bookFolder.exists()) bookFolder.deleteRecursively()
            } catch (_: Exception) {
            }

            toasty.show(R.string.delete_downloaded_chapters)
        }
    }

    fun reDownloadAllBookChapters(bookUrl: String) {
        viewModelScope.launch {
            val chapters = appRepository.bookChapters.chapters(bookUrl).map { it.url }
            if (chapters.isNotEmpty()) {
                appRepository.chapterBody.removeRows(chapters)
            }

            // delete local chapter files
            try {
                val localFolderName = appFileResolver.getLocalBookFolderName(bookUrl).removeLocalUriPrefix
                val bookFolder = java.io.File(appFileResolver.folderBooks, localFolderName)
                if (bookFolder.exists()) bookFolder.deleteRecursively()
            } catch (_: Exception) {
            }

            toasty.show(R.string.delete_downloaded_chapters)

            // Now download all chapters again
            val chaptersToDownload = appRepository.bookChapters.chapters(bookUrl)
            chaptersToDownload.forEach { chapter ->
                appRepository.chapterBody.fetchBody(chapter.url)
            }
            toasty.show(R.string.downloading_all_chapters_started)
        }
    }

    fun removeBook(bookUrl: String) {
        viewModelScope.launch {
            val chapters = appRepository.bookChapters.chapters(bookUrl).map { it.url }
            if (chapters.isNotEmpty()) {
                appRepository.chapterBody.removeRows(chapters)
                appRepository.bookChapters.removeAllFromBook(bookUrl)
            }

            // delete stored files for this book
            try {
                val localFolderName = appFileResolver.getLocalBookFolderName(bookUrl).removeLocalUriPrefix
                val bookFolder = File(appFileResolver.folderBooks, localFolderName)
                if (bookFolder.exists()) bookFolder.deleteRecursively()
            } catch (_: Exception) {
            }

            appRepository.libraryBooks.remove(bookUrl)
            toasty.show(R.string.removed_from_library)
        }
    }

    

    fun getBook(bookUrl: String) = appRepository.libraryBooks.getFlow(bookUrl).filterNotNull()
}

