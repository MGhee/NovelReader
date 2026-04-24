package my.novelreader.features.mangareader

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.encodeToString
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.core.utils.StateExtra_String
import my.novelreader.data.AppRepository
import my.novelreader.features.mangareader.loader.ChapterLoader
import my.novelreader.features.mangareader.loader.ChapterInfo
import my.novelreader.data.MangaImagePrefetcher
import my.novelreader.features.mangareader.model.MangaReaderChapter
import my.novelreader.features.mangareader.model.ViewerChapters
import my.novelreader.features.mangareader.setting.ReadingMode
import my.novelreader.features.mangareader.setting.ReaderPreferences
import my.novelreader.features.mangareader.setting.TapZoneStyle
import my.novelreader.features.mangareader.viewer.Viewer
import my.novelreader.features.mangareader.widget.ReaderPageImageView
import my.novelreader.scraper.MangaDirection
import my.novelreader.scraper.MangaReadingMode
import my.novelreader.scraper.MangaSourceInterface
import my.novelreader.scraper.Scraper
import my.novelreader.feature.local_database.tables.Chapter
import okhttp3.OkHttpClient
import javax.inject.Inject

interface MangaReaderStateBundle {
    var bookUrl: String
    var chapterUrl: String
}

@HiltViewModel
class MangaReaderViewModel @Inject constructor(
    stateHandler: SavedStateHandle,
    private val appRepository: AppRepository,
    private val readerPreferences: ReaderPreferences,
    private val scraper: Scraper,
    private val imagePrefetcher: MangaImagePrefetcher,
    private val okHttpClient: OkHttpClient,
    private val appPreferences: AppPreferences,
) : ViewModel(), MangaReaderStateBundle {

    override var bookUrl by StateExtra_String(stateHandler)
    override var chapterUrl by StateExtra_String(stateHandler)

    // UI state
    val showMenus = mutableStateOf(false)
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val showSettingsSheet = mutableStateOf(false)
    val showChapterListSheet = mutableStateOf(false)
    val showPageActionsSheet = mutableStateOf(false)

    // Viewer and chapters
    private val _currentViewer = MutableStateFlow<Viewer?>(null)
    val currentViewer: StateFlow<Viewer?> = _currentViewer

    private val _currentChapter = MutableStateFlow<MangaReaderChapter?>(null)
    val currentChapter: StateFlow<MangaReaderChapter?> = _currentChapter

    private val _viewerChapters = MutableStateFlow<ViewerChapters?>(null)
    val viewerChapters: StateFlow<ViewerChapters?> = _viewerChapters

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex

    var initialPageIndex = 0
        private set

    // Preferences exposed
    val readerPrefs = readerPreferences
    val readingMode = readerPreferences.readingMode
    val tapZoneStyle = readerPreferences.tapZoneStyle
    val brightness = readerPreferences.brightness
    val backgroundColor = readerPreferences.backgroundColor

    private lateinit var chapterLoader: ChapterLoader
    private var allChapters: List<MangaReaderChapter> = emptyList()
    private val pageProgressFlow = MutableSharedFlow<Int>()

    private val _chapterList = MutableStateFlow<List<Chapter>>(emptyList())
    val chapterList: StateFlow<List<Chapter>> = _chapterList

    init {
        // Set shared OkHttpClient for image loading
        ReaderPageImageView.setSharedClient(okHttpClient)

        loadChapterAndSetupViewer()

        // Setup debounced page progress saving (250ms delay)
        viewModelScope.launch {
            pageProgressFlow
                .debounce(250)
                .collect { pageIndex ->
                    currentChapter.value?.url?.let {
                        appRepository.bookChapters.updateLastReadMangaPage(it, pageIndex)
                    }
                }
        }
    }

    private fun loadChapterAndSetupViewer() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                // Get the book metadata
                val book = appRepository.libraryBooks.get(bookUrl) ?: run {
                    isLoading.value = false
                    return@launch
                }

                // Resolve the manga source
                val source = scraper.getCompatibleSource(book.url) as? MangaSourceInterface ?: run {
                    isLoading.value = false
                    return@launch
                }

                // Load or auto-apply reading mode for this source
                val sourceId = source.id
                val readingMode = if (sourceId == "asurascans") {
                    // Asura Scans always defaults to Webtoon, never saved
                    ReadingMode.Webtoon
                } else {
                    getSourceReadingMode(sourceId) ?: run {
                        // First time opening this source - auto-apply source's recommended mode
                        val sourceReadingMode = when (source.readingMode) {
                            MangaReadingMode.WEBTOON -> ReadingMode.Webtoon
                            MangaReadingMode.PAGE_BY_PAGE -> when (source.direction) {
                                MangaDirection.LTR -> ReadingMode.LeftToRight
                                MangaDirection.RTL -> ReadingMode.RightToLeft
                            }
                        }
                        saveSourceReadingMode(sourceId, sourceReadingMode)
                        sourceReadingMode
                    }
                }
                readerPreferences.setReadingMode(readingMode)

                // Get all chapters for this book
                val chapters = appRepository.bookChapters.chapters(bookUrl)
                    .sortedBy { it.position }
                val chapterInfos = chapters.map { ChapterInfo(it.url, it.title, it.position) }
                _chapterList.value = chapters

                if (chapters.isEmpty()) {
                    isLoading.value = false
                    return@launch
                }

                // Create chapter loader with local cache lookup for offline reading
                chapterLoader = ChapterLoader(
                    source = source,
                    chapters = chapterInfos,
                    getCachedImageUrls = { chapterUrl ->
                        appRepository.chapterBody.getMangaChapterPages(chapterUrl)
                    }
                )
                allChapters = emptyList() // Will be built as chapters load

                // Load the requested chapter
                val loadResult = chapterLoader.loadChapter(chapterUrl)
                if (loadResult.isSuccess) {
                    val chapter = loadResult.getOrNull()
                    _currentChapter.value = chapter
                    errorMessage.value = null

                    // Persist as last read chapter
                    appRepository.libraryBooks.updateLastReadChapter(bookUrl, chapterUrl)

                    // Restore last read page
                    val lastReadPage = chapters.find { it.url == chapterUrl }?.lastReadMangaPage ?: 0
                    initialPageIndex = lastReadPage

                    // Prefetch all chapter images - wait until cached before showing viewer
                    chapter?.let {
                        val imageUrls = it.pages.map { page -> page.imageUrl }
                        imagePrefetcher.prefetchAndAwait(imageUrls)
                        // Auto-save page list for offline access on future opens
                        appRepository.chapterBody.saveMangaChapterPages(chapterUrl, imageUrls)
                    }

                    // Build viewer chapters (current for now; prev/next later with prefetch)
                    _viewerChapters.value = ViewerChapters(
                        prevChapter = null,
                        currentChapter = chapter!!,
                        nextChapter = null
                    )
                    setupViewer()
                } else {
                    errorMessage.value = loadResult.exceptionOrNull()?.message ?: "Failed to load chapter"
                }
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun setupViewer() {
        // Viewer creation is deferred to the Compose layer where Context is available
        // This method is a placeholder for future logic
    }

    fun onReadingModeChanged(mode: ReadingMode) {
        readerPreferences.setReadingMode(mode)
        // Save as source-specific preference (but not for Asura Scans)
        val source = scraper.getCompatibleSource(bookUrl)
        if (source is MangaSourceInterface && source.id != "asurascans") {
            saveSourceReadingMode(source.id, mode)
        }
        setupViewer()
    }

    fun onTapZoneStyleChanged(style: TapZoneStyle) {
        readerPreferences.setTapZoneStyle(style)
    }

    fun onPageChanged(index: Int) {
        _currentPageIndex.value = index
        viewModelScope.launch {
            pageProgressFlow.emit(index)
        }
    }

    fun openChapter(chapterUrl: String) {
        this.chapterUrl = chapterUrl
        viewModelScope.launch {
            isLoading.value = true
            try {
                // Mark previous chapter as read
                currentChapter.value?.url?.let {
                    appRepository.bookChapters.setAsRead(it, true)
                }

                // Load new chapter
                val loadResult = chapterLoader.loadChapter(chapterUrl)
                if (loadResult.isSuccess) {
                    val chapter = loadResult.getOrNull()
                    _currentChapter.value = chapter
                    _currentPageIndex.value = 0
                    initialPageIndex = 0

                    // Persist as last read chapter
                    appRepository.libraryBooks.updateLastReadChapter(bookUrl, chapterUrl)

                    // Prefetch all chapter images - wait until cached before showing viewer
                    chapter?.let {
                        val imageUrls = it.pages.map { page -> page.imageUrl }
                        imagePrefetcher.prefetchAndAwait(imageUrls)
                        // Auto-save page list for offline access on future opens
                        appRepository.chapterBody.saveMangaChapterPages(chapterUrl, imageUrls)
                    }

                    // Build viewer chapters
                    _viewerChapters.value = ViewerChapters(
                        prevChapter = null,
                        currentChapter = chapter!!,
                        nextChapter = null
                    )
                }
            } finally {
                isLoading.value = false
            }
        }
    }

    fun loadPrevChapter() {
        val currentUrl = _currentChapter.value?.url ?: return
        val chapters = _chapterList.value
        val currentIndex = chapters.indexOfFirst { it.url == currentUrl }
        if (currentIndex > 0) {
            openChapter(chapters[currentIndex - 1].url)
        }
    }

    fun loadNextChapter() {
        val currentUrl = _currentChapter.value?.url ?: return
        val chapters = _chapterList.value
        val currentIndex = chapters.indexOfFirst { it.url == currentUrl }
        if (currentIndex < chapters.size - 1) {
            openChapter(chapters[currentIndex + 1].url)
        }
    }

    fun onPageLongPressed() {
        showPageActionsSheet.value = true
    }

    fun cleanup() {
        _currentViewer.value?.destroy()
        imagePrefetcher.cancel()
    }

    /**
     * Get saved reading mode for a specific source, or null if not yet set.
     */
    private fun getSourceReadingMode(sourceId: String): ReadingMode? {
        val modesJson = appPreferences.MANGA_SOURCE_READING_MODES.value
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val element = json.parseToJsonElement(modesJson)
            val modeStr = element.jsonObject[sourceId]?.jsonPrimitive?.content
            modeStr?.let { ReadingMode.valueOf(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save reading mode preference for a specific source.
     */
    private fun saveSourceReadingMode(sourceId: String, mode: ReadingMode) {
        try {
            val json = Json { ignoreUnknownKeys = true }
            val modesJson = appPreferences.MANGA_SOURCE_READING_MODES.value
            val modesMap = try {
                json.parseToJsonElement(modesJson).jsonObject.toMutableMap()
            } catch (e: Exception) {
                mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
            }
            modesMap[sourceId] = JsonPrimitive(mode.name)
            appPreferences.MANGA_SOURCE_READING_MODES.value = json.encodeToString(modesMap)
        } catch (e: Exception) {
            // Silently ignore save errors
        }
    }
}
