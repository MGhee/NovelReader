package my.novelreader.coreui

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
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
}