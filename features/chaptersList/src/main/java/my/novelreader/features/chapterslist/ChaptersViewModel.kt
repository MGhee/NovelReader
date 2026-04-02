package my.novelreader.features.chapterslist

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.novelreader.coreui.BaseViewModel
import my.novelreader.coreui.theme.BookColorExtractor
import my.novelreader.coreui.theme.ThemeProvider
import my.novelreader.data.AppRepository
import my.novelreader.data.DownloaderRepository
import my.novelreader.data.EpubImporterRepository
import my.novelreader.interactor.WorkersInteractions
import my.novelreader.chapterslist.R
import my.novelreader.core.AppCoroutineScope
import my.novelreader.core.AppFileResolver
import my.novelreader.core.Toasty
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.core.isContentUri
import my.novelreader.core.isLocalUri
import my.novelreader.core.utils.StateExtra_String
import my.novelreader.core.utils.toState
import my.novelreader.feature.local_database.ChapterWithContext
import my.novelreader.scraper.Scraper
import javax.inject.Inject

interface ChapterStateBundle {
    val rawBookUrl: String
    val bookTitle: String
}

@HiltViewModel
internal class ChaptersViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val appScope: AppCoroutineScope,
    scraper: Scraper,
    private val toasty: Toasty,
    private val appPreferences: AppPreferences,
    appFileResolver: AppFileResolver,
    private val downloaderRepository: DownloaderRepository,
    private val chaptersRepository: ChaptersRepository,
    private val epubImporterRepository: EpubImporterRepository,
    private val workersInteractions: WorkersInteractions,
    private val bookColorExtractor: BookColorExtractor,
    private val themeProvider: ThemeProvider,
    stateHandle: SavedStateHandle,
) : BaseViewModel(), ChapterStateBundle {

    override val rawBookUrl by StateExtra_String(stateHandle)
    override val bookTitle by StateExtra_String(stateHandle)

    private val bookUrl = appFileResolver.getLocalIfContentType(rawBookUrl, bookFolderName = bookTitle)

    @Volatile
    private var loadChaptersJob: Job? = null

    @Volatile
    private var lastSelectedChapterUrl: String? = null
    private val source = scraper.getCompatibleSource(bookUrl)
    private val book = appRepository.libraryBooks.getFlow(bookUrl)
        .filterNotNull()
        .map(ChaptersScreenState::BookState)
        .toState(
            viewModelScope,
            ChaptersScreenState.BookState(title = bookTitle, url = bookUrl, coverImageUrl = null)
        )

    val state = ChaptersScreenState(
        book = book,
        error = mutableStateOf(""),
        chapters = mutableStateListOf(),
        selectedChaptersUrl = mutableStateMapOf(),
        isRefreshing = mutableStateOf(false),
        sourceCatalogNameStrRes = mutableStateOf(source?.nameStrId),
        settingChapterSort = appPreferences.CHAPTERS_SORT_ASCENDING.state(viewModelScope),
        isLocalSource = mutableStateOf(bookUrl.isLocalUri),
        isRefreshable = mutableStateOf(rawBookUrl.isContentUri || !bookUrl.isLocalUri)
    )

    init {
        appScope.launch {
            if (rawBookUrl.isContentUri && appRepository.libraryBooks.get(bookUrl) == null) {
                importUriContent()
            }
        }

        viewModelScope.launch {
            if (state.isLocalSource.value) return@launch

            if (!appRepository.bookChapters.hasChapters(bookUrl))
                updateChaptersList()

            if (appRepository.libraryBooks.get(bookUrl) != null)
                return@launch

            chaptersRepository.downloadBookMetadata(bookUrl = bookUrl, bookTitle = bookTitle)
        }

        viewModelScope.launch {
            chaptersRepository.getChaptersSortedFlow(bookUrl = bookUrl).collect {
                state.chapters.clear()
                state.chapters.addAll(it)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (!appPreferences.BOOK_DYNAMIC_THEME_ENABLED.value) return@launch
            val book = appRepository.libraryBooks.get(bookUrl) ?: return@launch
            val seedColor = book.coverSeedColor ?: run {
                val extracted = bookColorExtractor.extractSeedColor(bookUrl, book.coverImageUrl)
                    ?: return@launch
                appRepository.libraryBooks.updateCoverSeedColor(bookUrl, extracted)
                extracted
            }
            withContext(Dispatchers.Main) {
                themeProvider.setActiveBookSeedColor(seedColor)
            }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val isBookmarked =
                appRepository.toggleBookmark(bookTitle = bookTitle, bookUrl = bookUrl)
            val msg = if (isBookmarked) R.string.added_to_library else R.string.removed_from_library
            toasty.show(msg)
        }
    }

    fun onPullRefresh() {
        if (!state.isRefreshable.value) {
            toasty.show(R.string.local_book_nothing_to_update)
            state.isRefreshing.value = false
            return
        }
        toasty.show(R.string.updating_book_info)
        if (rawBookUrl.isContentUri) {
            importUriContent()
        } else if (!state.isLocalSource.value) {
            updateCover()
            updateDescription()
            updateChaptersList()
        }
    }

    private fun updateCover() = viewModelScope.launch {
        if (state.isLocalSource.value || book.value.coverImageUrl?.isLocalUri == true) return@launch
        downloaderRepository.bookCoverImageUrl(bookUrl = bookUrl).onSuccess {
            if (it == null) return@onSuccess
            appRepository.libraryBooks.updateCover(bookUrl, it)
        }
    }

    private fun updateDescription() = viewModelScope.launch {
        if (state.isLocalSource.value) return@launch
        downloaderRepository.bookDescription(bookUrl = bookUrl).onSuccess {
            if (it == null) return@onSuccess
            appRepository.libraryBooks.updateDescription(bookUrl, it)
        }
    }

    private fun importUriContent() {
        if (loadChaptersJob?.isActive == true) return
        loadChaptersJob = appScope.launch {
            state.error.value = ""
            state.isRefreshing.value = true
            val isInLibrary = appRepository.libraryBooks.existInLibrary(bookUrl)
            epubImporterRepository.importEpubFromContentUri(
                contentUri = rawBookUrl,
                bookTitle = bookTitle,
                addToLibrary = isInLibrary
            ).onError {
                state.error.value = it.message
            }
            state.isRefreshing.value = false
        }
    }

    private fun updateChaptersList() {
        if (loadChaptersJob?.isActive == true) return
        loadChaptersJob = appScope.launch {
            state.error.value = ""
            state.isRefreshing.value = true
            val url = bookUrl
            downloaderRepository.bookChaptersList(bookUrl = url)
                .onSuccess {
                    if (it.isEmpty())
                        toasty.show(R.string.no_chapters_found)
                    appRepository.bookChapters.merge(newChapters = it, bookUrl = url)
                    // Auto-download new chapters for library books
                    if (appRepository.libraryBooks.existInLibrary(url)) {
                        workersInteractions.downloadAllBookChapters(url)
                    }
                }.onError {
                    state.error.value = it.message
                }
            state.isRefreshing.value = false

        }
    }

    suspend fun getLastReadChapter(): String? =
        chaptersRepository.getLastReadChapter(bookUrl = bookUrl)

    fun setAsUnreadSelected() {
        val list = state.selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.Default) {
            appRepository.bookChapters.setAsUnread(list.map { it.first })
        }
    }

    fun setAsReadSelected() {
        val list = state.selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.Default) {
            appRepository.bookChapters.setAsRead(list.map { it.first })
        }
    }

    fun setAsReadUpToSelected() {
        if (state.selectedChaptersUrl.size > 1) return
        val selectedIndex = state.selectedChaptersUrl.keys.firstOrNull()?.let { selectedUrl ->
            state.chapters.indexOfFirst { it.chapter.url == selectedUrl }
        } ?: return

        if (selectedIndex != -1) {
            val chaptersToMarkAsRead = state.chapters.take(selectedIndex + 1).map { it.chapter.url }
            appScope.launch(Dispatchers.Default) {
                appRepository.bookChapters.setAsRead(chaptersToMarkAsRead)
            }
        }
    }

    fun setAsReadUpToUnSelected() {
        if (state.selectedChaptersUrl.size > 1) return
        val selectedIndex = state.selectedChaptersUrl.keys.firstOrNull()?.let { selectedUrl ->
            state.chapters.indexOfFirst { it.chapter.url == selectedUrl }
        } ?: return

        if (selectedIndex != -1) {
            val chaptersToMarkAsUnread = state.chapters.take(selectedIndex + 1).map { it.chapter.url }
            appScope.launch(Dispatchers.Default) {
                appRepository.bookChapters.setAsUnread(chaptersToMarkAsUnread)
            }
        }
    }

    fun downloadSelected() {
        if (state.isLocalSource.value) return

        // Get selected chapter URLs
        val selectedUrls = state.selectedChaptersUrl.keys.toSet()

        // Filter and sort chapters by position to ensure sequential download
        val sortedChapters = state.chapters
            .filter { selectedUrls.contains(it.chapter.url) }
            .sortedBy { it.chapter.position }

        // Download chapters sequentially in order
        appScope.launch(Dispatchers.Default) {
            var failed = 0
            sortedChapters.forEach { chapter ->
                appRepository.chapterBody.fetchBody(chapter.chapter.url)
                    .onError { failed++ }
            }
            if (failed > 0) {
                toasty.show("Download failed for $failed chapters")
            }
        }
    }

    fun deleteDownloadsSelected() {
        if (state.isLocalSource.value) return
        val list = state.selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.Default) {
            appRepository.chapterBody.removeRows(list.map { it.first })
            toasty.show(R.string.chapters_deleted)
        }
    }

    fun onSelectionModeChapterClick(chapter: ChapterWithContext) {
        val url = chapter.chapter.url
        if (state.selectedChaptersUrl.containsKey(url)) {
            state.selectedChaptersUrl.remove(url)
        } else {
            state.selectedChaptersUrl[url] = Unit
        }
        lastSelectedChapterUrl = url
    }

    fun saveImageAsCover(uri: Uri) {
        appRepository.libraryBooks.saveImageAsCover(imageUri = uri, bookUrl = bookUrl)
    }

    fun onSelectionModeChapterLongClick(chapter: ChapterWithContext) {
        val url = chapter.chapter.url
        if (url != lastSelectedChapterUrl) {
            val indexOld = state.chapters.indexOfFirst { it.chapter.url == lastSelectedChapterUrl }
            val indexNew = state.chapters.indexOfFirst { it.chapter.url == url }
            val min = minOf(indexOld, indexNew)
            val max = maxOf(indexOld, indexNew)
            if (min >= 0 && max >= 0) {
                for (index in min..max) {
                    state.selectedChaptersUrl[state.chapters[index].chapter.url] = Unit
                }
                lastSelectedChapterUrl = state.chapters[indexNew].chapter.url
                return
            }
        }

        if (state.selectedChaptersUrl.containsKey(url)) {
            state.selectedChaptersUrl.remove(url)
        } else {
            state.selectedChaptersUrl[url] = Unit
        }
        lastSelectedChapterUrl = url
    }

    fun onChapterLongClick(chapter: ChapterWithContext) {
        val url = chapter.chapter.url
        state.selectedChaptersUrl[url] = Unit
        lastSelectedChapterUrl = url
    }

    fun onChapterDownload(chapter: ChapterWithContext) {
        if (state.isLocalSource.value) return
        appScope.launch {
            appRepository.chapterBody.fetchBody(chapter.chapter.url)
                .onSuccess { toasty.show(R.string.chapter_downloaded) }
                .onError { toasty.show(R.string.chapter_download_failed) }
        }
    }

    fun unselectAll() {
        state.selectedChaptersUrl.clear()
    }

    fun selectAll() {
        state.chapters
            .toList()
            .map { it.chapter.url to Unit }
            .let { state.selectedChaptersUrl.putAll(it) }
    }

    fun invertSelection() {
        val allChaptersUrl = state.chapters.asSequence().map { it.chapter.url }.toSet()
        val selectedUrl = state.selectedChaptersUrl.asSequence().map { it.key }.toSet()
        val inverse = (allChaptersUrl - selectedUrl).asSequence().associateWith { }
        state.selectedChaptersUrl.clear()
        state.selectedChaptersUrl.putAll(inverse)
    }
}
