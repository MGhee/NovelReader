package my.novelreader.coreui

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import my.novelreader.coreui.mappers.toTheme
import my.novelreader.coreui.theme.ThemeProvider
import my.novelreader.coreui.theme.Themes
import my.novelreader.core.appPreferences.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppThemeProvider @Inject constructor(
    private val appPreferences: AppPreferences
) : ThemeProvider {

    override fun followSystem(stateCoroutineScope: CoroutineScope): State<Boolean> {
        return appPreferences.THEME_FOLLOW_SYSTEM.state(stateCoroutineScope)
    }

    override fun currentTheme(stateCoroutineScope: CoroutineScope): State<Themes> = derivedStateOf {
        appPreferences.THEME_ID.state(stateCoroutineScope).value.toTheme
    }

    override fun bookSeedColor(stateCoroutineScope: CoroutineScope): State<Int?> = derivedStateOf {
        val enabled = appPreferences.BOOK_DYNAMIC_THEME_ENABLED.state(stateCoroutineScope).value
        if (!enabled) return@derivedStateOf null
        val raw = appPreferences.BOOK_DYNAMIC_THEME_SEED_COLOR.state(stateCoroutineScope).value
        if (raw == 0) null else raw
    }

    override fun setActiveBookSeedColor(seedColor: Int?) {
        appPreferences.BOOK_DYNAMIC_THEME_SEED_COLOR.value = seedColor ?: 0
    }
}