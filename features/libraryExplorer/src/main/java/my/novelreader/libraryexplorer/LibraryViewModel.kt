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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log
import my.novelreader.core.Toasty
import my.novelreader.coreui.BaseViewModel
import my.novelreader.data.AppRepository
import my.novelreader.data.LibraryFilters
import my.novelreader.data.LibrarySort
import my.novelreader.data.SortDirection
import my.novelreader.data.LibraryDisplayMode
import my.novelreader.data.CategoriesRepository
import my.novelreader.core.AppFileResolver
import my.novelreader.core.removeLocalUriPrefix
import java.io.File
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.interactor.WorkersInteractions
import my.novelreader.core.utils.asMutableStateOf
import my.novelreader.feature.local_database.BookWithContext
import my.novelreader.feature.local_database.tables.ContentType
import javax.inject.Inject

@HiltViewModel
internal class LibraryViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val appRepository: AppRepository,
    private val appFileResolver: AppFileResolver,
    private val workersInteractions: WorkersInteractions,
    private val toasty: Toasty,
    private val categoriesRepository: CategoriesRepository,
    stateHandle: SavedStateHandle,
) : BaseViewModel() {
    private companion object {
        const val CHAPTER_SOURCE_DELIMITER = " — "
    }

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

    // Library state flows backed by preferences
    var displayMode by appPreferences.LIBRARY_DISPLAY_MODE.state(viewModelScope)
    var searchQuery by appPreferences.LIBRARY_SEARCH_QUERY.state(viewModelScope)
    var sortType by appPreferences.LIBRARY_SORT_TYPE.state(viewModelScope)
    var sortDirection by appPreferences.LIBRARY_SORT_DIRECTION.state(viewModelScope)

    var currentCategoryId: Int? by mutableStateOf(null)
    val categories = categoriesRepository.getAllCategoriesFlow()

    // The most recently read book for the continue reading banner
    var continueReadingBook by mutableStateOf<BookWithContext?>(null)
        private set
    var continueReadingChapterTitle by mutableStateOf<String?>(null)
        private set
    var continueReadingChapterPosition by mutableStateOf(0)
        private set

    init {
        // Restore category ID from preference
        viewModelScope.launch {
            appPreferences.LIBRARY_CATEGORY_ID.flow().collect { categoryIdStr ->
                currentCategoryId = if (categoryIdStr.isEmpty()) null else categoryIdStr.toIntOrNull()
            }
        }

        // Load and sort books based on preferences
        viewModelScope.launch {
            combine(
                appRepository.libraryBooks.getBooksInLibraryWithContextFlow,
                appPreferences.LIBRARY_SORT_TYPE.flow(),
                appPreferences.LIBRARY_SORT_DIRECTION.flow(),
                appPreferences.LIBRARY_SEARCH_QUERY.flow()
            ) { allBooks, sortTypeStr, sortDirStr, query ->
                Quadruple(allBooks, sortTypeStr, sortDirStr, query)
            }.collect { (allBooks, sortTypeStr, sortDirStr, query) ->
                try {
                    val sortType = try {
                        LibrarySort.valueOf(sortTypeStr)
                    } catch (e: Exception) {
                        LibrarySort.LastRead
                    }
                    val sortDir = try {
                        SortDirection.valueOf(sortDirStr)
                    } catch (e: Exception) {
                        SortDirection.Descending
                    }

                    // Filter by search query (category filtering is handled via DB query in future optimization)
                    var filtered = allBooks.filter { book ->
                        query.isEmpty() || book.book.title.contains(query, ignoreCase = true)
                    }

                    // Apply sorting
                    filtered = when (sortType) {
                        LibrarySort.Title -> filtered.sortedBy { it.book.title }
                        LibrarySort.LastRead -> filtered.sortedByDescending { it.book.lastReadEpochTimeMilli }
                        LibrarySort.DateAdded -> filtered.sortedByDescending { it.book.url }
                        LibrarySort.Completed -> filtered.sortedBy { !it.book.completed }
                        LibrarySort.Unread -> filtered.sortedByDescending { it.newChaptersCount }
                    }

                    if (sortDir == SortDirection.Ascending) {
                        filtered = filtered.reversed()
                    }

                    _list.value = filtered

                    // Update continue reading banner
                    val lastRead = allBooks.maxByOrNull { it.book.lastReadEpochTimeMilli }
                    continueReadingBook = lastRead
                    val chapter = lastRead?.book?.lastReadChapter?.let { chapterUrl ->
                        appRepository.bookChapters.get(chapterUrl)
                    }
                    continueReadingChapterTitle = chapter?.title
                    continueReadingChapterPosition = (chapter?.position ?: -1) + 1
                } catch (e: Exception) {
                    Log.e("LibraryViewModel", "Failed to load books", e)
                }
            }
        }

        // Observe active downloads from WorkManager (handles app restart)
        viewModelScope.launch {
            workersInteractions.observeActiveDownloads().collect { activeProgress ->
                downloadProgress = activeProgress
            }
        }
    }

    data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    fun setSort(sort: LibrarySort, direction: SortDirection) {
        sortType = sort.name
        sortDirection = direction.name
    }

    fun setCategory(categoryId: Int?) {
        currentCategoryId = categoryId
        // Preference persistence handled by preference system observers
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
        val preferredSource = getPreferredMangaSource(bookUrl, book.contentType)
        val chapters = appRepository.bookChapters.chapters(bookUrl).sortedBy { it.position }
        val filteredChapters = preferredSource?.let { selectedSource ->
            chapters.filter { extractChapterSource(it.title) == selectedSource }
        } ?: chapters
        val lastReadChapterUrl = book.lastReadChapter

        if (lastReadChapterUrl != null) {
            val hasLastReadChapter = filteredChapters.any { it.url == lastReadChapterUrl }
            if (hasLastReadChapter) {
                return lastReadChapterUrl
            }

            toasty.show(R.string.last_read_chapter_not_found_opening_first)
        }

        val firstChapterUrl = filteredChapters.firstOrNull()?.url
        if (firstChapterUrl == null) {
            toasty.show(R.string.unable_to_open_book_no_chapters_found)
            return null
        }

        return firstChapterUrl
    }

    suspend fun getPreferredMangaSource(bookUrl: String, contentType: ContentType? = null): String? {
        if (contentType != null && contentType != ContentType.MANGA) return null

        val savedSource = appPreferences.getPreferredMangaSource(bookUrl)
        if (!savedSource.isNullOrBlank()) {
            return savedSource
        }

        val chapters = appRepository.bookChapters.chapters(bookUrl)
        val sources = chapters
            .asSequence()
            .mapNotNull { extractChapterSource(it.title) }
            .groupingBy { it }
            .eachCount()

        return sources.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { it.key.lowercase() }
            )
            .firstOrNull()
            ?.key
    }

    private fun extractChapterSource(title: String): String? =
        title.substringAfterLast(CHAPTER_SOURCE_DELIMITER, "")
            .trim()
            .takeIf { it.isNotBlank() }

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
            workersInteractions.cancelDownload(bookUrl)

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

