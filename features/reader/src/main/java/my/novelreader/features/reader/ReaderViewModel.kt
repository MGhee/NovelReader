package my.novelreader.features.reader

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.novelreader.coreui.BaseViewModel
import my.novelreader.coreui.mappers.toTheme
import my.novelreader.coreui.theme.BookColorExtractor
import my.novelreader.coreui.theme.ThemeProvider
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.core.Toasty
import my.novelreader.core.utils.StateExtra_Boolean
import my.novelreader.core.utils.StateExtra_String
import my.novelreader.core.utils.toState
import my.novelreader.data.AppRepository
import my.novelreader.features.reader.domain.ChapterState
import my.novelreader.features.reader.manager.ReaderManager
import my.novelreader.features.reader.ui.ReaderScreenState
import my.novelreader.features.reader.ui.ReaderViewHandlersActions
import my.novelreader.reader.R
import javax.inject.Inject
import kotlin.properties.Delegates

interface ReaderStateBundle {
    var bookUrl: String
    var chapterUrl: String
    var introScrollToSpeaker: Boolean
}

@HiltViewModel
internal class ReaderViewModel @Inject constructor(
    stateHandler: SavedStateHandle,
    appPreferences: AppPreferences,
    private val appRepository: AppRepository,
    private val readerManager: ReaderManager,
    readerViewHandlersActions: ReaderViewHandlersActions,
    private val toasty: Toasty,
    private val bookColorExtractor: BookColorExtractor,
    private val themeProvider: ThemeProvider,
) : BaseViewModel(), ReaderStateBundle {

    override var bookUrl by StateExtra_String(stateHandler)
    override var chapterUrl by StateExtra_String(stateHandler)
    override var introScrollToSpeaker by StateExtra_Boolean(stateHandler)

    private val readerSession = readerManager.initiateOrGetSession(
        bookUrl = bookUrl,
        chapterUrl = chapterUrl
    )

    private val readingPosStats = readerSession.readingStats
    private val themeId = appPreferences.THEME_ID.state(viewModelScope)
    private val chaptersInBook = appRepository.bookChapters
        .getChaptersWithContextFlow(bookUrl)
        .toState(viewModelScope, listOf())

    val state = ReaderScreenState(
        showReaderInfo = mutableStateOf(false),
        readerInfo = ReaderScreenState.CurrentInfo(
            bookTitle = derivedStateOf {
                readerSession.bookTitle ?: ""
            },
            chapterTitle = derivedStateOf {
                readingPosStats.value?.chapterTitle ?: ""
            },
            chapterCurrentNumber = derivedStateOf {
                readingPosStats.value?.run { chapterIndex + 1 } ?: 0
            },
            chapterPercentageProgress = readerSession.readingChapterProgressPercentage,
            chaptersCount = derivedStateOf { readingPosStats.value?.chapterCount ?: 0 },
            chapterUrl = derivedStateOf { readingPosStats.value?.chapterUrl ?: "" },
            chapters = chaptersInBook,
        ),
        settings = ReaderScreenState.Settings(
            selectedSetting = mutableStateOf(ReaderScreenState.Settings.Type.None),
            isTextSelectable = appPreferences.READER_SELECTABLE_TEXT.state(viewModelScope),
            textToSpeech = readerSession.readerTextToSpeech.state,
            liveTranslation = readerSession.readerLiveTranslation.state,
            fullScreen = appPreferences.READER_FULL_SCREEN.state(viewModelScope),
            style = ReaderScreenState.Settings.StyleSettingsData(
                followSystem = appPreferences.THEME_FOLLOW_SYSTEM.state(viewModelScope),
                currentTheme = derivedStateOf { themeId.value.toTheme },
                readerTheme = derivedStateOf {
                    val readerThemeId = appPreferences.READER_THEME_ID.state(viewModelScope).value
                    if (appPreferences.BOOK_DYNAMIC_THEME_ENABLED.value)
                        readerThemeId.toTheme
                    else themeId.value.toTheme
                },
                isDynamicColorActive = appPreferences.BOOK_DYNAMIC_THEME_ENABLED.state(viewModelScope),
                textFont = appPreferences.READER_FONT_FAMILY.state(viewModelScope),
                textSize = appPreferences.READER_FONT_SIZE.state(viewModelScope),
                textIndent = appPreferences.READER_TEXT_INDENT.state(viewModelScope),
                pageTurnVolumeKeys = appPreferences.READER_PAGE_TURN_VOLUME_KEYS.state(viewModelScope),
                pageTurnTapEdge = appPreferences.READER_PAGE_TURN_TAP_EDGE.state(viewModelScope),
                marginLevel = appPreferences.READER_MARGIN_LEVEL.state(viewModelScope),
                lineSpacingLevel = appPreferences.READER_LINE_SPACING_LEVEL.state(viewModelScope),
                lineBreakHeight = appPreferences.READER_LINE_BREAK_HEIGHT.state(viewModelScope),
                orientation = appPreferences.READER_ORIENTATION.state(viewModelScope),
                keepScreenOn = appPreferences.READER_KEEP_SCREEN_ON.state(viewModelScope),
            )
        ),
        showInvalidChapterDialog = mutableStateOf(false)
    )

    init {
        readerViewHandlersActions.showInvalidChapterDialog = {
            withContext(Dispatchers.Main) {
                state.showInvalidChapterDialog.value = true
            }
        }

        viewModelScope.launch {
            snapshotFlow { readingPosStats.value?.chapterUrl }
                .collectLatest { newChapterUrl ->
                    if (!newChapterUrl.isNullOrEmpty()) {
                        chapterUrl = newChapterUrl
                    }
                }
        }

        // Extract and apply book color if dynamic theme is enabled
        viewModelScope.launch(Dispatchers.IO) {
            if (!appPreferences.BOOK_DYNAMIC_THEME_ENABLED.value) return@launch
            val book = appRepository.libraryBooks.get(bookUrl) ?: return@launch
            val seedColor = book.coverSeedColor ?: run {
                // Extract and cache
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

    val items = readerSession.items
    val chaptersLoader = readerSession.readerChaptersLoader
    val readerSpeaker = readerSession.readerTextToSpeech
    var readingCurrentChapter by Delegates.observable(readerSession.currentChapter) { _, _, new ->
        readerSession.currentChapter = new
    }
    val onTranslatorChanged = readerSession.readerLiveTranslation.onTranslatorChanged
    val ttsScrolledToTheTop = readerSession.readerTextToSpeech.scrolledToTheTop
    val ttsScrolledToTheBottom = readerSession.readerTextToSpeech.scrolledToTheBottom

    fun onCloseManually() {
        readerManager.close()
    }


    fun startSpeaker(itemIndex: Int) =
        readerSession.startSpeaker(itemIndex = itemIndex)

    fun reloadReader() {
        val currentChapter = readingCurrentChapter.copy()
        readerSession.reloadReader()
        chaptersLoader.tryLoadRestartedInitial(currentChapter)
    }

    fun updateInfoViewTo(itemIndex: Int) =
        readerSession.updateInfoViewTo(itemIndex = itemIndex)

    fun markChapterStartAsSeen(chapterUrl: String) =
        readerSession.markChapterStartAsSeen(chapterUrl = chapterUrl)

    fun markChapterEndAsSeen(chapterUrl: String) =
        readerSession.markChapterEndAsSeen(chapterUrl = chapterUrl)

    fun saveCurrentReadingPosition() {
        readerSession.saveCurrentPosition(readingCurrentChapter)
    }

    fun openChapterFromList(chapterUrl: String) {
        val chapterIndex = chaptersLoader.orderedChapters.indexOfFirst { it.url == chapterUrl }
        if (chapterIndex == -1) {
            state.showInvalidChapterDialog.value = true
            return
        }

        saveCurrentReadingPosition()
        readingCurrentChapter = ChapterState(
            chapterUrl = chapterUrl,
            chapterItemPosition = 0,
            offset = 0,
        )
        // Use tryLoadRestartedInitial so the position comes from readingCurrentChapter (0)
        // directly, avoiding a race with the IO save above that could return a stale
        // lastReadChapter from the database and restore an old position.
        chaptersLoader.tryLoadRestartedInitial(chapterLastState = readingCurrentChapter)
    }

    fun downloadChapterFromList(chapterUrl: String) {
        viewModelScope.launch {
            appRepository.chapterBody.fetchBody(chapterUrl)
                .onSuccess { toasty.show(R.string.chapter_downloaded) }
                .onError { toasty.show(R.string.chapter_download_failed) }
        }
    }

    fun navigateToNextChapter() {
        val currentIndex = chaptersLoader.orderedChapters.indexOfFirst {
            it.url == readingCurrentChapter.chapterUrl
        }
        if (currentIndex != -1 && currentIndex < chaptersLoader.orderedChapters.size - 1) {
            val nextChapter = chaptersLoader.orderedChapters[currentIndex + 1]
            openChapterFromList(nextChapter.url)
        }
    }

    fun navigateToPreviousChapter() {
        val currentIndex = chaptersLoader.orderedChapters.indexOfFirst {
            it.url == readingCurrentChapter.chapterUrl
        }
        if (currentIndex > 0) {
            val previousChapter = chaptersLoader.orderedChapters[currentIndex - 1]
            openChapterFromList(previousChapter.url)
        }
    }

    fun downloadAllChapters() {
        viewModelScope.launch {
            var failed = 0
            val chapters = chaptersInBook.value
            chapters.forEach { chapter ->
                appRepository.chapterBody.fetchBody(chapter.chapter.url)
                    .onError { failed++ }
            }
            if (failed > 0) {
                toasty.show("Download failed for $failed chapters")
            } else if (chapters.isNotEmpty()) {
                toasty.show(R.string.chapters_deleted)
            }
        }
    }

    fun deleteAllChapters() {
        viewModelScope.launch {
            val chapterUrls = chaptersInBook.value.map { it.chapter.url }
            if (chapterUrls.isNotEmpty()) {
                appRepository.chapterBody.removeRows(chapterUrls)
                toasty.show(R.string.chapters_deleted)
            }
        }
    }
}
