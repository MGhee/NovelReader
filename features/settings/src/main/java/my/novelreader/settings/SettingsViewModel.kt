package my.novelreader.settings

import android.content.Context
import android.text.format.Formatter
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.novelreader.coreui.BaseViewModel
import my.novelreader.coreui.mappers.toPreferenceTheme
import my.novelreader.coreui.mappers.toTheme
import my.novelreader.coreui.theme.Themes
import my.novelreader.data.AppRemoteRepository
import my.novelreader.data.AppRepository
import my.novelreader.data.auth.GoogleAuthManager
import my.novelreader.data.auth.GoogleAuthResult
import my.novelreader.data.auth.SessionManager
import my.novelreader.core.AppCoroutineScope
import my.novelreader.core.AppFileResolver
import my.novelreader.core.Toasty
import my.novelreader.core.Response
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.core.utils.asMutableStateOf
import my.novelreader.text_translator.domain.TranslationManager
import my.novelreader.interactor.WorkersInteractions
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val appScope: AppCoroutineScope,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context,
    private val translationManager: TranslationManager,
    stateHandle: SavedStateHandle,
    private val appFileResolver: AppFileResolver,
    private val appRemoteRepository: AppRemoteRepository,
    private val toasty: Toasty,
    private val workersInteractions: WorkersInteractions,
    private val googleAuthManager: GoogleAuthManager,
    private val sessionManager: SessionManager,
) : BaseViewModel() {

    private val themeId by appPreferences.THEME_ID.state(viewModelScope)

    private val syncSessionTokenState = appPreferences.SYNC_SESSION_TOKEN.state(viewModelScope)

    val state = SettingsScreenState(
        databaseSize = stateHandle.asMutableStateOf("databaseSize") { "" },
        imageFolderSize = stateHandle.asMutableStateOf("imageFolderSize") { "" },
        followsSystemTheme = appPreferences.THEME_FOLLOW_SYSTEM.state(viewModelScope),
        currentTheme = derivedStateOf { themeId.toTheme },
        isTranslationSettingsVisible = mutableStateOf(translationManager.available),
        translationModelsStates = translationManager.models,
        updateAppSetting = SettingsScreenState.UpdateApp(
            currentAppVersion = appRemoteRepository.getCurrentAppVersion().toString(),
            showNewVersionDialog = mutableStateOf(null),
            appUpdateCheckerEnabled = appPreferences.GLOBAL_APP_UPDATER_CHECKER_ENABLED.state(
                viewModelScope
            ),
            checkingForNewVersion = mutableStateOf(false)
        ),
        libraryAutoUpdate = SettingsScreenState.LibraryAutoUpdate(
            autoUpdateEnabled = appPreferences.GLOBAL_APP_AUTOMATIC_LIBRARY_UPDATES_ENABLED.state(
                viewModelScope
            ),
            autoUpdateIntervalHours = appPreferences.GLOBAL_APP_AUTOMATIC_LIBRARY_UPDATES_INTERVAL_HOURS.state(
                viewModelScope
            ),
            autoDownloadNewChapters = appPreferences.GLOBAL_APP_AUTOMATIC_DOWNLOAD_NEW_CHAPTERS.state(
                viewModelScope
            )
        ),
        geminiApiKey = appPreferences.TRANSLATION_GEMINI_API_KEY.state(viewModelScope),
        geminiModel = appPreferences.TRANSLATION_GEMINI_MODEL.state(viewModelScope),
        preferOnlineTranslation = appPreferences.TRANSLATION_PREFER_ONLINE.state(viewModelScope),
        syncServerUrl = appPreferences.SYNC_SERVER_URL.state(viewModelScope),
        isLoggedIn = derivedStateOf { syncSessionTokenState.value.isNotBlank() },
        syncUserEmail = appPreferences.SYNC_USER_EMAIL.state(viewModelScope),
        syncUserDisplayName = appPreferences.SYNC_USER_DISPLAY_NAME.state(viewModelScope),
        isSyncSigningIn = mutableStateOf(false),
    )

    init {
        updateDatabaseSize()
        updateImagesFolderSize()
        viewModelScope.launch {
            appRepository.eventDataRestored.collect {
                updateDatabaseSize()
                updateImagesFolderSize()
            }
        }
    }

    fun downloadTranslationModel(lang: String) {
        translationManager.downloadModel(lang)
    }

    fun removeTranslationModel(lang: String) {
        translationManager.removeModel(lang)
    }

    fun cleanDatabase() = appScope.launch(Dispatchers.IO) {
        appRepository.settings.clearNonLibraryData()
        appRepository.vacuum()
        updateDatabaseSize()
    }

    fun cleanImagesFolder() = appScope.launch(Dispatchers.IO) {
        val libraryFolders = appRepository.libraryBooks.getAllInLibrary()
            .asSequence()
            .map { appFileResolver.getLocalBookFolderName(it.url) }
            .toSet()

        appRepository.settings.folderBooks.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory && it.exists() }
            ?.filter { it.name !in libraryFolders }
            ?.forEach { it.deleteRecursively() }
        updateImagesFolderSize()
        Glide.get(context).clearDiskCache()
    }

    fun syncWithServer() {
        val serverUrl = appPreferences.SYNC_SERVER_URL.value
        val authToken = sessionManager.getEffectiveToken()
        toasty.show("Syncing with $serverUrl...")
        workersInteractions.syncWithServer(serverUrl, authToken)
    }

    fun onSyncServerUrlChange(url: String) {
        appPreferences.SYNC_SERVER_URL.value = url
    }

    fun onFollowSystemChange(follow: Boolean) {
        appPreferences.THEME_FOLLOW_SYSTEM.value = follow
    }

    fun onThemeChange(themes: Themes) {
        appPreferences.THEME_ID.value = themes.toPreferenceTheme
    }

    fun onGeminiApiKeyChange(apiKey: String) {
        appPreferences.TRANSLATION_GEMINI_API_KEY.value = apiKey
    }

    fun onGeminiModelChange(model: String) {
        appPreferences.TRANSLATION_GEMINI_MODEL.value = model
    }

    fun onSignInWithGoogle(activityContext: Context) {
        state.isSyncSigningIn.value = true
        viewModelScope.launch {
            val serverUrl = appPreferences.SYNC_SERVER_URL.value
            if (serverUrl.isBlank()) {
                toasty.show("Sync server URL not configured")
                state.isSyncSigningIn.value = false
                return@launch
            }

            val webClientId = appPreferences.SYNC_GOOGLE_WEB_CLIENT_ID.value
            if (webClientId.isBlank()) {
                toasty.show("Google OAuth not configured")
                state.isSyncSigningIn.value = false
                return@launch
            }

            val signInResult = withContext(Dispatchers.Default) {
                googleAuthManager.signIn(activityContext, webClientId)
            }

            when (signInResult) {
                is GoogleAuthResult.Success -> {
                    val exchangeResult = sessionManager.exchangeGoogleToken(serverUrl, signInResult.idToken)
                    when (exchangeResult) {
                        is Response.Success -> {
                            toasty.show("Signed in as ${signInResult.email}")
                        }
                        is Response.Error -> {
                            toasty.show("Failed to authenticate: ${exchangeResult.message}")
                        }
                    }
                }
                is GoogleAuthResult.Error -> {
                    toasty.show("Sign-in failed: ${signInResult.message}")
                }
            }
            state.isSyncSigningIn.value = false
        }
    }

    fun onSignOut() {
        sessionManager.logout()
        toasty.show("Signed out")
    }

    fun onPreferOnlineTranslationChange(prefer: Boolean) {
        appPreferences.TRANSLATION_PREFER_ONLINE.value = prefer
    }

    private fun updateDatabaseSize() = viewModelScope.launch {
        val size = appRepository.getDatabaseSizeBytes()
        state.databaseSize.value = Formatter.formatFileSize(appPreferences.context, size)
    }

    private fun updateImagesFolderSize() = viewModelScope.launch {
        val size = getFolderSizeBytes(appRepository.settings.folderBooks)
        state.imageFolderSize.value = Formatter.formatFileSize(appPreferences.context, size)
    }

    fun onCheckForUpdatesManual() {
        viewModelScope.launch {
            state.updateAppSetting.checkingForNewVersion.value = true
            val current = appRemoteRepository.getCurrentAppVersion()
            appRemoteRepository.getLastAppVersion()
                .onSuccess { new ->
                    if (new.version > current) {
                        state.updateAppSetting.showNewVersionDialog.value = new
                    } else {
                        toasty.show(R.string.you_already_have_the_last_version)
                    }
                }.onError {
                    toasty.show(R.string.failed_to_check_last_app_version)
                }
            state.updateAppSetting.checkingForNewVersion.value = false
        }
    }
}

private suspend fun getFolderSizeBytes(file: File): Long = withContext(Dispatchers.IO) {
    when {
        !file.exists() -> 0
        file.isFile -> file.length()
        else -> file.walkBottomUp().sumOf { if (it.isDirectory) 0 else it.length() }
    }
}


