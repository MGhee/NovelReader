package my.novelreader.libraryexplorer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import android.util.Log
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

    // Download progress tracking: maps bookUrl to Pair(downloadedCount, totalCount)
    var downloadProgress by mutableStateOf<Map<String, Pair<Int, Int>>>(emptyMap())

    // Pull-to-refresh state
    var isPullRefreshing by mutableStateOf(false)

    // List of books to display (all books from library)
    private val _list = mutableStateOf<List<BookWithContext>>(emptyList())
    val list: List<BookWithContext> get() = _list.value

    var readFilter by appPreferences.LIBRARY_FILTER_READ.state(viewModelScope)
    var readSort by appPreferences.LIBRARY_SORT_LAST_READ.state(viewModelScope)

    // The most recently read book for the continue reading banner
    var continueReadingBook by mutableStateOf<BookWithContext?>(null)
        private set
    var continueReadingChapterTitle by mutableStateOf<String?>(null)
        private set
    var continueReadingChapterPosition by mutableStateOf(0)
        private set

    init {
        viewModelScope.launch {
            appRepository.libraryBooks.getBooksInLibraryWithContextFlow.collect { books ->
                _list.value = books.sortedByDescending { it.book.lastReadEpochTimeMilli }

                // Update continue reading banner
                val lastRead = _list.value.firstOrNull { it.book.lastReadEpochTimeMilli > 0 }
                continueReadingBook = lastRead
                val chapter = lastRead?.book?.lastReadChapter?.let { chapterUrl ->
                    appRepository.bookChapters.get(chapterUrl)
                }
                continueReadingChapterTitle = chapter?.title
                continueReadingChapterPosition = (chapter?.position ?: -1) + 1
            }
        }

        // Observe active downloads from WorkManager (handles app restart)
        viewModelScope.launch {
            workersInteractions.observeActiveDownloads().collect { activeProgress ->
                downloadProgress = activeProgress
            }
        }
    }

    fun readFilterToggle() {
        readFilter = readFilter.next()
    }

    fun readSortToggle() {
        readSort = readSort.next()
    }

    fun onLibraryRefresh() {
        isPullRefreshing = true
        viewModelScope.launch {
            try {
                workersInteractions.checkForLibraryUpdates(my.novelreader.core.domain.LibraryCategory.DEFAULT)
            } finally {
                isPullRefreshing = false
            }
        }
    }

    fun bookCompletedToggle(bookUrl: String) {
        viewModelScope.launch {
            val book = appRepository.libraryBooks.get(bookUrl)
            if (book == null) {
                toasty.show(R.string.failed_to_update_book)
                return@launch
            }
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
        viewModelScope.launch {
            Log.d("LibraryViewModel", "downloadAllBookChapters called for $bookUrl")
            val chapters = appRepository.bookChapters.chapters(bookUrl)
            val downloadedCount = appRepository.chapterBody.getDownloadedCount(bookUrl)

            Log.d("LibraryViewModel", "Found $downloadedCount/${chapters.size} chapters already downloaded")

            if (downloadedCount == chapters.size && chapters.isNotEmpty()) {
                toasty.show(R.string.all_chapters_already_downloaded)
                return@launch
            }

            toasty.show(R.string.downloading_all_chapters_started)

            Log.d("LibraryViewModel", "Enqueueing WorkManager download for $bookUrl")
            // Delegate to WorkManager for background download
            workersInteractions.downloadAllBookChapters(bookUrl)

            Log.d("LibraryViewModel", "Starting progress observation for $bookUrl")
            // Start observing progress for this book
            observeDownloadProgress(bookUrl)
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
                // Silently ignore file deletion errors
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
                // Silently ignore file deletion errors
            }

            toasty.show(R.string.delete_downloaded_chapters)

            // Now download all chapters again using WorkManager
            toasty.show(R.string.downloading_all_chapters_started)
            workersInteractions.downloadAllBookChapters(bookUrl)

            // Start observing progress for this book
            observeDownloadProgress(bookUrl)
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
                // Silently ignore file deletion errors
            }

            appRepository.libraryBooks.remove(bookUrl)
            toasty.show(R.string.removed_from_library)
        }
    }

    private fun observeDownloadProgress(bookUrl: String) {
        viewModelScope.launch {
            workersInteractions.observeDownloadProgress(bookUrl).collect { progress ->
                downloadProgress = if (progress != null) {
                    downloadProgress.toMutableMap().apply { put(bookUrl, progress) }
                } else {
                    downloadProgress.toMutableMap().apply { remove(bookUrl) }
                }
            }
        }
    }

    fun updateLastSeenChaptersCount(bookUrl: String, chaptersCount: Int) {
        viewModelScope.launch {
            appRepository.libraryBooks.updateLastSeenChaptersCount(bookUrl, chaptersCount)
        }
    }

    fun getBook(bookUrl: String) = appRepository.libraryBooks.getFlow(bookUrl).filterNotNull()
}

