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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import my.novelreader.coreui.BaseViewModel
import my.novelreader.coreui.theme.BookColorExtractor
import my.novelreader.coreui.theme.ThemeProvider
import my.novelreader.data.AppRepository
import my.novelreader.data.DownloaderRepository
import my.novelreader.data.EpubImporterRepository
import my.novelreader.data.MangaImagePrefetcher
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
import my.novelreader.feature.local_database.tables.ContentType
import my.novelreader.scraper.MangaSourceInterface
import my.novelreader.scraper.Scraper
import my.novelreader.core.Response
import javax.inject.Inject

interface ChapterStateBundle {
    val rawBookUrl: String
    val bookTitle: String
}

@HiltViewModel
internal class ChaptersViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val appScope: AppCoroutineScope,
    private val scraper: Scraper,
    private val toasty: Toasty,
    private val appPreferences: AppPreferences,
    appFileResolver: AppFileResolver,
    private val downloaderRepository: DownloaderRepository,
    private val chaptersRepository: ChaptersRepository,
    private val epubImporterRepository: EpubImporterRepository,
    private val mangaImagePrefetcher: MangaImagePrefetcher,
    private val workersInteractions: WorkersInteractions,
    private val bookColorExtractor: BookColorExtractor,
    private val themeProvider: ThemeProvider,
    stateHandle: SavedStateHandle,
) : BaseViewModel(), ChapterStateBundle {

    private companion object {
        const val CHAPTER_SOURCE_DELIMITER = " — "
        const val MAX_CONCURRENT_MANGA_CHAPTER_DOWNLOADS = 3
    }

    override val rawBookUrl by StateExtra_String(stateHandle)
    override val bookTitle by StateExtra_String(stateHandle)

    private val bookUrl = appFileResolver.getLocalIfContentType(rawBookUrl, bookFolderName = bookTitle)

    @Volatile
    private var loadChaptersJob: Job? = null

    @Volatile
    private var lastSelectedChapterUrl: String? = null
    private var allChapters: List<ChapterWithContext> = emptyList()
    private var hasUserSelectedChapterSource = false
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
        chapterSourceOptions = mutableStateListOf(),
        selectedChaptersUrl = mutableStateMapOf(),
        isRefreshing = mutableStateOf(false),
        sourceCatalogNameStrRes = mutableStateOf(source?.nameStrId),
        settingChapterSort = appPreferences.CHAPTERS_SORT_ASCENDING.state(viewModelScope),
        selectedChapterSource = mutableStateOf(null),
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

            // Always fetch latest chapters from the source
            updateChaptersList()

            // Always detect and update contentType based on source
            val contentType = if (source is MangaSourceInterface) ContentType.MANGA else ContentType.NOVEL
            appRepository.libraryBooks.updateContentType(bookUrl, contentType)

            val existing = appRepository.libraryBooks.get(bookUrl)
            if (existing != null) {
                // Existing library books can sit with an empty coverImageUrl if they were
                // bookmarked before metadata was fetched. Backfill on chapter-list open for
                // any remote source — the lookup is cached so this is near-free after the
                // first successful fetch.
                if (existing.coverImageUrl.isBlank() && !state.isLocalSource.value) {
                    updateCover()
                }
                return@launch
            }

            chaptersRepository.downloadBookMetadata(bookUrl = bookUrl, bookTitle = bookTitle)
        }

        viewModelScope.launch {
            chaptersRepository.getChaptersSortedFlow(bookUrl = bookUrl).collect {
                android.util.Log.d("ChaptersVM", "Flow emitted ${it.size} chapters for $bookUrl")
                allChapters = it
                syncChapterSourceOptions(it)
                applyChapterSourceFilter()
                android.util.Log.d("ChaptersVM", "After filter: ${state.chapters.size} chapters displayed, selectedSource=${state.selectedChapterSource.value}")
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
            if (!isBookmarked) {
                workersInteractions.cancelDownload(this@ChaptersViewModel.bookUrl)
            }
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
            android.util.Log.d("ChaptersVM", "Fetching chapters for: $url")
            downloaderRepository.bookChaptersList(bookUrl = url)
                .onSuccess {
                    android.util.Log.d("ChaptersVM", "Fetched ${it.size} chapters, first=${it.firstOrNull()?.url}")
                    if (it.isEmpty())
                        toasty.show(R.string.no_chapters_found)
                    appRepository.bookChapters.merge(newChapters = it, bookUrl = url)
                    android.util.Log.d("ChaptersVM", "Merge complete for $url")
                    // Auto-download new chapters for library books
                    if (appRepository.libraryBooks.existInLibrary(url)) {
                        workersInteractions.downloadAllBookChapters(url)
                    }
                }.onError {
                    android.util.Log.e("ChaptersVM", "Error fetching chapters: ${it.message}")
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

        val selectedUrls = state.selectedChaptersUrl.keys.toSet()
        val sortedChapters = state.chapters
            .filter { selectedUrls.contains(it.chapter.url) }
            .sortedBy { it.chapter.position }

        appScope.launch(Dispatchers.Default) {
            var failed = 0
            val isManga = scraper.getCompatibleSource(rawBookUrl) is MangaSourceInterface
            if (isManga) {
                val semaphore = Semaphore(MAX_CONCURRENT_MANGA_CHAPTER_DOWNLOADS)
                failed = coroutineScope {
                    sortedChapters.map { chapter ->
                        async {
                            semaphore.withPermit {
                                downloadMangaChapterForOffline(chapter.chapter.url)
                            }
                        }
                    }.awaitAll().count { it is Response.Error }
                }
            } else {
                sortedChapters.forEach { chapter ->
                    appRepository.chapterBody.fetchBody(chapter.chapter.url)
                        .onError { failed++ }
                }
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
            val isManga = scraper.getCompatibleSource(rawBookUrl) is MangaSourceInterface
            if (isManga) {
                downloadMangaChapterForOffline(chapter.chapter.url)
                    .onSuccess { toasty.show(R.string.chapter_downloaded) }
                    .onError { toasty.show(R.string.chapter_download_failed) }
            } else {
                appRepository.chapterBody.fetchBody(chapter.chapter.url)
                    .onSuccess { toasty.show(R.string.chapter_downloaded) }
                    .onError { toasty.show(R.string.chapter_download_failed) }
            }
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

    fun onChapterBookmarkChange(chapter: ChapterWithContext, bookmarked: Boolean) {
        appScope.launch {
            appRepository.bookChapters.setBookmarked(chapter.chapter.url, bookmarked)
        }
    }

    fun onChapterSourceSelected(source: String?) {
        hasUserSelectedChapterSource = true
        updateSelectedChapterSource(source)
    }

    private fun syncChapterSourceOptions(chapters: List<ChapterWithContext>) {
        val sourceCounts = chapters
            .asSequence()
            .mapNotNull { extractChapterSource(it.chapter.title) }
            .groupingBy { it }
            .eachCount()

        val options = sourceCounts
            .keys
            .asSequence()
            .sortedBy { it.lowercase() }
            .toList()

        val defaultSource = sourceCounts
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { it.key.lowercase() }
            )
            .firstOrNull()
            ?.key

        state.chapterSourceOptions.clear()
        state.chapterSourceOptions.addAll(options)

        val selectedSource = state.selectedChapterSource.value
        when {
            selectedSource != null && selectedSource !in options -> {
                hasUserSelectedChapterSource = false
                updateSelectedChapterSource(defaultSource)
            }

            !hasUserSelectedChapterSource -> updateSelectedChapterSource(defaultSource)
        }
    }

    private fun updateSelectedChapterSource(source: String?) {
        appPreferences.setPreferredMangaSource(bookUrl, source)
        if (state.selectedChapterSource.value == source) return
        state.selectedChapterSource.value = source
        state.selectedChaptersUrl.clear()
        lastSelectedChapterUrl = null
        applyChapterSourceFilter()
    }

    private fun applyChapterSourceFilter() {
        val selectedSource = state.selectedChapterSource.value
        val filtered = if (selectedSource == null) {
            allChapters
        } else {
            allChapters.filter { extractChapterSource(it.chapter.title) == selectedSource }
        }

        state.chapters.clear()
        state.chapters.addAll(filtered)
    }

    private fun extractChapterSource(title: String): String? =
        title.substringAfterLast(CHAPTER_SOURCE_DELIMITER, "")
            .trim()
            .takeIf { it.isNotBlank() }

    private suspend fun downloadMangaChapterForOffline(chapterUrl: String): Response<Unit> {
        val source = scraper.getCompatibleSource(rawBookUrl)
        if (source !is MangaSourceInterface) {
            return Response.Error("Source is not manga", Exception())
        }

        return try {
            val response = source.getChapterImages(chapterUrl)
            if (response !is Response.Success) {
                val error = (response as? Response.Error)?.exception ?: Exception("Failed to fetch chapter images")
                return Response.Error(
                    (response as? Response.Error)?.exception?.message ?: "Failed to fetch chapter images",
                    error
                )
            }

            val imageUrls = response.data
            mangaImagePrefetcher.prefetchAndAwait(imageUrls)
            appRepository.chapterBody.saveMangaChapterPages(chapterUrl, imageUrls)
            Response.Success(Unit)
        } catch (e: Exception) {
            Response.Error(e.message ?: "Download failed", e)
        }
    }
}
