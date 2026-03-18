package my.novelreader.features.reader.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import my.novelreader.coreui.theme.Themes
import my.novelreader.features.reader.ui.settingDialogs.MoreSettingDialog
import my.novelreader.features.reader.ui.settingDialogs.StyleSettingDialog
import my.novelreader.features.reader.ui.settingDialogs.TranslatorSettingDialog
import my.novelreader.features.reader.ui.settingDialogs.VoiceReaderSettingDialog

@Composable
internal fun ReaderScreenBottomBarDialogs(
    settings: ReaderScreenState.Settings,
    onTextFontChanged: (String) -> Unit,
    onTextSizeChanged: (Float) -> Unit,
    onSelectableTextChange: (Boolean) -> Unit,
    onFollowSystem: (Boolean) -> Unit,
    onThemeSelected: (Themes) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onFullScreen: (Boolean) -> Unit,
    onOrientationChange: (my.novelreader.core.appPreferences.ReaderOrientation) -> Unit,
    onTextIndentChange: (Boolean) -> Unit,
    onMarginLevelChange: (my.novelreader.core.appPreferences.ReaderMarginLevel) -> Unit,
    onLineSpacingLevelChange: (my.novelreader.core.appPreferences.ReaderLineSpacingLevel) -> Unit,
    onLineBreakHeightChange: (Int) -> Unit,
    onOpenChaptersList: () -> Unit,
    onCloseStyleDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(Modifier.padding(horizontal = 24.dp)) {
            AnimatedContent(targetState = settings.selectedSetting.value, label = "") { target ->
                when (target) {
                    ReaderScreenState.Settings.Type.LiveTranslation -> TranslatorSettingDialog(
                        state = settings.liveTranslation
                    )
                    ReaderScreenState.Settings.Type.TextToSpeech -> VoiceReaderSettingDialog(
                        state = settings.textToSpeech
                    )
                    ReaderScreenState.Settings.Type.Style -> {
                        StyleSettingDialog(
                            state = settings.style,
                            onFollowSystemChange = onFollowSystem,
                            onThemeChange = onThemeSelected,
                            onTextFontChange = onTextFontChanged,
                            onTextSizeChange = onTextSizeChanged,
                            onTextIndentChange = onTextIndentChange,
                            onMarginLevelChange = onMarginLevelChange,
                            onLineSpacingLevelChange = onLineSpacingLevelChange,
                            onLineBreakHeightChange = onLineBreakHeightChange,
                            onKeepScreenOnChange = onKeepScreenOn,
                            onOrientationChange = onOrientationChange,
                            onClose = onCloseStyleDialog,
                        )
                    }
                    ReaderScreenState.Settings.Type.More -> MoreSettingDialog(
                        allowTextSelection = settings.isTextSelectable.value,
                        onAllowTextSelectionChange = onSelectableTextChange,
                        fullScreen = settings.fullScreen.value,
                        onFullScreen = onFullScreen,
                        orientation = settings.style.orientation.value,
                        onOrientationChange = onOrientationChange,
                        onOpenChaptersList = onOpenChaptersList,
                    )
                    ReaderScreenState.Settings.Type.None -> Unit
                }
            }
        }
    }
}