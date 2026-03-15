package my.novelreader.settings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.SnapshotStateList
import my.novelreader.core.domain.RemoteAppVersion
import my.novelreader.text_translator.domain.TranslationModelState
import my.novelreader.coreui.theme.Themes

data class SettingsScreenState(
    val databaseSize: MutableState<String>,
    val imageFolderSize: MutableState<String>,
    val followsSystemTheme: State<Boolean>,
    val currentTheme: State<Themes>,
    val isTranslationSettingsVisible: State<Boolean>,
    val translationModelsStates: SnapshotStateList<TranslationModelState>,
    val updateAppSetting: UpdateApp,
    val libraryAutoUpdate: LibraryAutoUpdate,
    val geminiApiKey: State<String>,
    val geminiModel: State<String>,
    val preferOnlineTranslation: State<Boolean>,
    val syncServerUrl: State<String>,
    val syncApiKey: State<String>,
) {
    data class UpdateApp(
        val currentAppVersion: String,
        val appUpdateCheckerEnabled: MutableState<Boolean>,
        val showNewVersionDialog: MutableState<RemoteAppVersion?>,
        val checkingForNewVersion: MutableState<Boolean>,
    )

    data class LibraryAutoUpdate(
        val autoUpdateEnabled: MutableState<Boolean>,
        val autoUpdateIntervalHours: MutableState<Int>,
    )
}