package my.novelreader.features.reader.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import my.novelreader.core.appPreferences.ReaderLineSpacingLevel
import my.novelreader.core.appPreferences.ReaderMarginLevel
import my.novelreader.core.appPreferences.ReaderOrientation
import my.novelreader.coreui.theme.InternalTheme
import my.novelreader.coreui.theme.Themes
import my.novelreader.coreui.theme.colorApp
import my.novelreader.coreui.theme.rememberMutableStateOf
import my.novelreader.features.reader.domain.ReaderItem
import my.novelreader.features.reader.features.LiveTranslationSettingData
import my.novelreader.features.reader.features.TextSynthesis
import my.novelreader.features.reader.features.TextToSpeechSettingData
import my.novelreader.features.reader.ui.ReaderScreenState.Settings.Type
import my.novelreader.reader.R
import my.novelreader.text_to_speech.Utterance
import my.novelreader.text_to_speech.VoiceData
import my.novelreader.text_translator.domain.TranslationModelState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderScreen(
    state: ReaderScreenState,
    onSelectableTextChange: (Boolean) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onFollowSystem: (Boolean) -> Unit,
    onFullScreen: (Boolean) -> Unit,
    onOrientationChange: (ReaderOrientation) -> Unit,
    onThemeSelected: (Themes) -> Unit,
    onReaderThemeSelected: (Themes) -> Unit = {},
    onTextFontChanged: (String) -> Unit,
    onTextSizeChanged: (Float) -> Unit,
    onTextIndentChange: (Boolean) -> Unit,
    onMarginLevelChange: (ReaderMarginLevel) -> Unit,
    onLineSpacingLevelChange: (ReaderLineSpacingLevel) -> Unit,
    onLineBreakHeightChange: (Int) -> Unit,
    onPressBack: () -> Unit,
    onOpenChapterInWeb: () -> Unit,
    onOpenChapter: (chapterUrl: String) -> Unit,
    onDownloadChapter: (chapterUrl: String) -> Unit,
    onNavigateToNextChapter: () -> Unit = {},
    onNavigateToPreviousChapter: () -> Unit = {},
    onOpenChaptersList: () -> Unit = {},
    onDownloadAllChapters: () -> Unit = {},
    onDeleteAllChapters: () -> Unit = {},
    readerContent: @Composable (paddingValues: PaddingValues) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    // Track full screen state for restoration
    val wasFullScreenBeforeChapterList = remember { mutableStateOf(false) }

    // Capture back action when viewing info
    BackHandler(enabled = state.showReaderInfo.value) {
        state.showReaderInfo.value = false
    }

    BackHandler(enabled = state.showChapterList.value) {
        state.showChapterList.value = false
        state.showReaderInfo.value = false
        // Restore full screen if it was enabled before chapter list opened
        if (wasFullScreenBeforeChapterList.value && !state.settings.fullScreen.value) {
            onFullScreen(true)
        }
    }

    // Exit full screen when chapter list opens
    LaunchedEffect(state.showChapterList.value) {
        if (state.showChapterList.value) {
            wasFullScreenBeforeChapterList.value = state.settings.fullScreen.value
            if (state.settings.fullScreen.value) {
                onFullScreen(false)
            }
        }
    }

    Scaffold(
        topBar = {
            val fullScreen by rememberUpdatedState(state.showReaderInfo.value)
            AnimatedVisibility(
                visible = state.showReaderInfo.value && !state.showChapterList.value,
                enter = expandVertically(initialHeight = { 0 }, expandFrom = Alignment.Top)
                        + fadeIn(),
                exit = shrinkVertically(targetHeight = { 0 }, shrinkTowards = Alignment.Top)
                        + fadeOut(),
            ) {
                Surface(
                    color = MaterialTheme.colorApp.tintedSurface,
                    modifier = Modifier.animateContentSize(),
                ) {
                    Column(
                        modifier = when (fullScreen) {
                            true -> Modifier.displayCutoutPadding()
                            false -> Modifier
                        }
                    ) {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorApp.tintedSurface,
                                scrolledContainerColor = MaterialTheme.colorApp.tintedSurface,
                            ),
                            title = {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = state.readerInfo.bookTitle.value,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = state.readerInfo.chapterTitle.value,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.animateContentSize()
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onPressBack) {
                                    Icon(Icons.Filled.Close, null)
                                }
                            },
                            actions = {
                                val showMenu = remember { mutableStateOf(false) }
                                IconButton(onClick = { showMenu.value = true }) {
                                    Icon(Icons.Filled.MoreVert, null)
                                }
                                DropdownMenu(
                                    expanded = showMenu.value,
                                    onDismissRequest = { showMenu.value = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(id = R.string.open_in_browser)) },
                                        onClick = {
                                            onOpenChapterInWeb()
                                            showMenu.value = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(id = R.string.allow_text_selection)) },
                                        onClick = {
                                            onSelectableTextChange(!state.settings.isTextSelectable.value)
                                            showMenu.value = false
                                        },
                                        trailingIcon = {
                                            Icon(
                                                if (state.settings.isTextSelectable.value) Icons.Filled.Check else Icons.Filled.Close,
                                                null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(id = R.string.features_reader_full_screen)) },
                                        onClick = {
                                            onFullScreen(!state.settings.fullScreen.value)
                                            showMenu.value = false
                                        },
                                        trailingIcon = {
                                            Icon(
                                                if (state.settings.fullScreen.value) Icons.Filled.Check else Icons.Filled.Close,
                                                null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        },
        content = { innerPadding ->
            readerContent(innerPadding)
        },
        bottomBar = {

            val toggleOrSet = { type: Type ->
                state.settings.selectedSetting.value = when (state.settings.selectedSetting.value) {
                    type -> Type.None
                    else -> type
                }
            }
            AnimatedVisibility(
                visible = state.showReaderInfo.value && !state.showChapterList.value,
                enter = expandVertically(initialHeight = { 0 }) + fadeIn(),
                exit = shrinkVertically(targetHeight = { 0 }) + fadeOut(),
            ) {
                Column {
                    ReaderScreenBottomBarDialogs(
                        settings = state.settings,
                        onTextFontChanged = onTextFontChanged,
                        onTextSizeChanged = onTextSizeChanged,
                        onSelectableTextChange = onSelectableTextChange,
                        onFollowSystem = onFollowSystem,
                        onThemeSelected = onThemeSelected,
                        onReaderThemeSelected = onReaderThemeSelected,
                        onKeepScreenOn = onKeepScreenOn,
                        onFullScreen = onFullScreen,
                        onOrientationChange = onOrientationChange,
                        onTextIndentChange = onTextIndentChange,
                        onMarginLevelChange = onMarginLevelChange,
                        onLineSpacingLevelChange = onLineSpacingLevelChange,
                        onLineBreakHeightChange = onLineBreakHeightChange,
                        onOpenChaptersList = {
                            state.settings.selectedSetting.value = Type.None
                            state.showChapterList.value = true
                        },
                        onCloseStyleDialog = {
                            state.settings.selectedSetting.value = Type.None
                            state.showReaderInfo.value = false
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    BottomAppBar(
                        modifier = Modifier
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .animateContentSize(),
                        containerColor = MaterialTheme.colorApp.tintedSurface,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        // Previous Chapter
                        IconButton(
                            onClick = onNavigateToPreviousChapter,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.previous_chapter))
                        }

                        // Chapters List
                        IconButton(
                            onClick = onOpenChaptersList,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.CloudDownload, contentDescription = stringResource(R.string.chapter_list))
                        }

                        // Style Settings
                        IconButton(
                            onClick = { toggleOrSet(Type.Style) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.ColorLens, contentDescription = stringResource(R.string.style))
                        }

                        // Text to Speech
                        IconButton(
                            onClick = { toggleOrSet(Type.TextToSpeech) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.RecordVoiceOver, contentDescription = stringResource(R.string.voice_reader))
                        }

                        // Live Translation or More
                        if (state.settings.liveTranslation.isAvailable) {
                            IconButton(
                                onClick = { toggleOrSet(Type.LiveTranslation) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Translate, contentDescription = stringResource(R.string.translator))
                            }
                        } else {
                            IconButton(
                                onClick = { toggleOrSet(Type.More) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.MoreHoriz, contentDescription = stringResource(R.string.more))
                            }
                        }

                        // Next Chapter
                        IconButton(
                            onClick = onNavigateToNextChapter,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.next_chapter), modifier = Modifier.rotate(180f))
                        }
                    }
                }
            }
        }
    )

    // Chapter List Overlay
    if (state.showChapterList.value) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Dimming background - clickable to close
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable {
                        state.showChapterList.value = false
                        state.showReaderInfo.value = false
                        // Restore full screen if it was enabled before
                        if (wasFullScreenBeforeChapterList.value && !state.settings.fullScreen.value) {
                            onFullScreen(true)
                        }
                    }
            )

            // Chapter List Panel - 75% width on the left
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.75f)
                    .align(Alignment.TopStart)
            ) {
                ReaderChapterListScreen(
                    chapters = state.readerInfo.chapters.value,
                    currentChapterUrl = state.readerInfo.chapterUrl.value,
                    onChapterClick = { chapter ->
                        onOpenChapter(chapter.chapter.url)
                        state.showChapterList.value = false
                        state.showReaderInfo.value = false
                        // Restore full screen if it was enabled before
                        if (wasFullScreenBeforeChapterList.value && !state.settings.fullScreen.value) {
                            onFullScreen(true)
                        }
                    },
                    onDownloadAllClick = onDownloadAllChapters,
                    onDeleteAllClick = onDeleteAllChapters,
                    onBackClick = {
                        state.showChapterList.value = false
                        state.showReaderInfo.value = false
                        // Restore full screen if it was enabled before
                        if (wasFullScreenBeforeChapterList.value && !state.settings.fullScreen.value) {
                            onFullScreen(true)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RowScope.SettingIconItem(
    currentType: Type,
    settingType: Type,
    @StringRes textId: Int,
    icon: ImageVector,
    onClick: (type: Type) -> Unit,
) {
    NavigationBarItem(
        selected = currentType == settingType,
        onClick = { onClick(settingType) },
        icon = { Icon(icon, null) },
        label = { Text(text = stringResource(id = textId)) }
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ViewsPreview(
    @PreviewParameter(PreviewDataProvider::class) data: PreviewDataProvider.Data
) {

    val liveTranslationSettingData = LiveTranslationSettingData(
        isAvailable = true,
        enable = remember { mutableStateOf(true) },
        listOfAvailableModels = remember { mutableStateListOf() },
        source = remember {
            mutableStateOf(
                TranslationModelState(
                    language = "fr",
                    available = true,
                    downloading = false,
                    downloadingFailed = false
                )
            )
        },
        target = remember {
            mutableStateOf(
                TranslationModelState(
                    language = "en",
                    available = true,
                    downloading = false,
                    downloadingFailed = false
                )
            )
        },
        onTargetChange = {},
        onEnable = {},
        onSourceChange = {},
        onDownloadTranslationModel = {}
        , onRedoTranslation = {}
    )

    val textToSpeechSettingData = TextToSpeechSettingData(
        isPlaying = rememberMutableStateOf(false),
        isLoadingChapter = rememberMutableStateOf(false),
        voicePitch = rememberMutableStateOf(1f),
        voiceSpeed = rememberMutableStateOf(1f),
        availableVoices = remember { mutableStateListOf() },
        activeVoice = remember {
            mutableStateOf(
                VoiceData(
                    id = "",
                    language = "",
                    quality = 100,
                    needsInternet = true
                )
            )
        },
        currentActiveItemState = remember {
            mutableStateOf(
                TextSynthesis(
                    playState = Utterance.PlayState.PLAYING,
                    itemPos = ReaderItem.Title(
                        chapterUrl = "",
                        chapterIndex = 0,
                        chapterItemPosition = 1,
                        text = ""
                    )
                )
            )
        },
        isThereActiveItem = rememberMutableStateOf(true),
        setPlaying = {},
        playPreviousItem = {},
        playPreviousChapter = {},
        playNextItem = {},
        playNextChapter = {},
        setVoiceId = {},
        playFirstVisibleItem = {},
        scrollToActiveItem = {},
        setVoiceSpeed = {},
        setVoicePitch = {},
        setCustomSavedVoices = {},
        customSavedVoices = rememberMutableStateOf(value = listOf())
    )

    val style = ReaderScreenState.Settings.StyleSettingsData(
        followSystem = remember { mutableStateOf(true) },
        currentTheme = remember { mutableStateOf(Themes.DARK) },
        readerTheme = remember { mutableStateOf(Themes.DARK) },
        isDynamicColorActive = remember { mutableStateOf(false) },
        textFont = remember { mutableStateOf("Arial") },
        textSize = remember { mutableFloatStateOf(20f) },
        textIndent = remember { mutableStateOf(true) },
        pageTurnVolumeKeys = remember { mutableStateOf(false) },
        pageTurnTapEdge = remember { mutableStateOf(true) },
        marginLevel = remember { mutableStateOf(ReaderMarginLevel.Small) },
        lineSpacingLevel = remember { mutableStateOf(ReaderLineSpacingLevel.Small) },
        lineBreakHeight = remember { mutableIntStateOf(20) },
        orientation = remember { mutableStateOf(ReaderOrientation.Vertical) },
        keepScreenOn = remember { mutableStateOf(false) },
    )

    InternalTheme {
        Surface(color = Color.Black) {
            ReaderScreen(
                state = ReaderScreenState(
                    showReaderInfo = remember { mutableStateOf(true) },
                    readerInfo = ReaderScreenState.CurrentInfo(
                        bookTitle = remember { mutableStateOf("Book Title") },
                        chapterTitle = remember { mutableStateOf("Chapter title") },
                        chapterCurrentNumber = remember { mutableIntStateOf(2) },
                        chapterPercentageProgress = remember { mutableFloatStateOf(0.5f) },
                        chaptersCount = remember { mutableIntStateOf(255) },
                        chapterUrl = remember { mutableStateOf("Chapter url") },
                        chapters = remember { mutableStateOf(listOf()) },
                    ),
                    settings = ReaderScreenState.Settings(
                        isTextSelectable = remember { mutableStateOf(false) },
                        textToSpeech = textToSpeechSettingData,
                        liveTranslation = liveTranslationSettingData,
                        style = ReaderScreenState.Settings.StyleSettingsData(
                            followSystem = remember { mutableStateOf(true) },
                            currentTheme = remember { mutableStateOf(Themes.DARK) },
                            readerTheme = remember { mutableStateOf(Themes.DARK) },
                            isDynamicColorActive = remember { mutableStateOf(false) },
                            textFont = remember { mutableStateOf("Arial") },
                            textSize = remember { mutableFloatStateOf(20f) },
                            textIndent = remember { mutableStateOf(true) },
                            pageTurnVolumeKeys = remember { mutableStateOf(false) },
                            pageTurnTapEdge = remember { mutableStateOf(true) },
                            marginLevel = remember { mutableStateOf(ReaderMarginLevel.Small) },
                            lineSpacingLevel = remember { mutableStateOf(ReaderLineSpacingLevel.Small) },
                            lineBreakHeight = remember { mutableIntStateOf(20) },
                            orientation = remember { mutableStateOf(ReaderOrientation.Vertical) },
                            keepScreenOn = remember { mutableStateOf(false) },
                        ),
                        selectedSetting = remember { mutableStateOf(data.selectedSetting) },
                        fullScreen = remember { mutableStateOf(false) },
                    ),
                    showInvalidChapterDialog = remember { mutableStateOf(false) }
                ),
                onTextSizeChanged = {},
                onTextFontChanged = {},
                onSelectableTextChange = {},
                onFollowSystem = {},
                onThemeSelected = {},
                onReaderThemeSelected = {},
                onTextIndentChange = {},
                onMarginLevelChange = {},
                onLineSpacingLevelChange = {},
                onLineBreakHeightChange = {},
                onPressBack = {},
                onOpenChapterInWeb = {},
                onOpenChapter = {},
                onDownloadChapter = {},
                readerContent = {},
                onKeepScreenOn = {},
                onFullScreen = {},
                onOrientationChange = {}
            )
        }
    }
}


private class PreviewDataProvider : PreviewParameterProvider<PreviewDataProvider.Data> {
    data class Data(
        val selectedSetting: Type
    )

    override val values = sequenceOf(
        Data(selectedSetting = Type.None),
        Data(selectedSetting = Type.LiveTranslation),
        Data(selectedSetting = Type.TextToSpeech),
        Data(selectedSetting = Type.Style),
        Data(selectedSetting = Type.More),
    )
}
