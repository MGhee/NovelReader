package my.novelreader.features.reader.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import my.novelreader.coreui.theme.Themes
import my.novelreader.core.appPreferences.ReaderMarginLevel
import my.novelreader.core.appPreferences.ReaderLineSpacingLevel
import my.novelreader.core.appPreferences.ReaderOrientation
import my.novelreader.features.reader.features.LiveTranslationSettingData
import my.novelreader.features.reader.features.TextToSpeechSettingData
import my.novelreader.feature.local_database.ChapterWithContext

internal data class ReaderScreenState(
    val showReaderInfo: MutableState<Boolean>,
    val readerInfo: CurrentInfo,
    val settings: Settings,
    val showInvalidChapterDialog: MutableState<Boolean>,
    val showChapterList: MutableState<Boolean> = mutableStateOf(false),
) {
    data class CurrentInfo(
        val bookTitle: State<String>,
        val chapterTitle: State<String>,
        val chapterCurrentNumber: State<Int>,
        val chapterPercentageProgress: State<Float>,
        val chaptersCount: State<Int>,
        val chapterUrl: State<String>,
        val chapters: State<List<ChapterWithContext>>,
    )

    data class Settings(
        val isTextSelectable: State<Boolean>,
        val fullScreen: State<Boolean>,
        val textToSpeech: TextToSpeechSettingData,
        val liveTranslation: LiveTranslationSettingData,
        val style: StyleSettingsData,
        val selectedSetting: MutableState<Type>,
    ) {
        val keepScreenOn: State<Boolean> get() = style.keepScreenOn
        data class StyleSettingsData(
            val followSystem: State<Boolean>,
            val currentTheme: State<Themes>,
            val textFont: State<String>,
            val textSize: State<Float>,
            val textIndent: State<Boolean>,
            val pageTurnVolumeKeys: State<Boolean>,
            val pageTurnTapEdge: State<Boolean>,
            val marginLevel: State<ReaderMarginLevel>,
            val lineSpacingLevel: State<ReaderLineSpacingLevel>,
            val lineBreakHeight: State<Int>,
            val orientation: State<ReaderOrientation>,
            val keepScreenOn: State<Boolean>,
        )

        enum class Type {
            None, LiveTranslation, TextToSpeech, Style, More
        }
    }
}