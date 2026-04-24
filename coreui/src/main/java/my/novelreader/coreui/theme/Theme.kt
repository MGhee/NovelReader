package my.novelreader.coreui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController


@Composable
fun Theme(
    themeProvider: ThemeProvider,
    content: @Composable () -> @Composable Unit,
) {
    val scope = rememberCoroutineScope()
    val followSystemsTheme by themeProvider.followSystem(scope)
    val selectedTheme by themeProvider.currentTheme(scope)
    val bookSeedColor by themeProvider.bookSeedColor(scope)
    val amoledMode by themeProvider.amoledMode(scope)

    val isSystemThemeLight = !isSystemInDarkTheme()
    val theme: Themes = when (followSystemsTheme) {
        true -> when {
            isSystemThemeLight && !selectedTheme.isLight -> Themes.LIGHT
            !isSystemThemeLight && selectedTheme.isLight -> Themes.DARK
            else -> selectedTheme
        }
        false -> selectedTheme
    }
    InternalTheme(
        theme = theme,
        bookSeedColor = bookSeedColor,
        amoledMode = amoledMode,
        content = content,
    )
}

@Composable
fun InternalTheme(
    theme: Themes = if (isSystemInDarkTheme()) Themes.DARK else Themes.LIGHT,
    bookSeedColor: Int? = null,
    amoledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (bookSeedColor != null) {
        remember(bookSeedColor, theme) {
            generateBookColorScheme(bookSeedColor, theme)
        }
    } else {
        when {
            amoledMode && !theme.isLight -> amoledColorScheme()
            else -> theme.colorScheme()
        }
    }

    val appColor = if (bookSeedColor != null) {
        remember(bookSeedColor, theme) {
            generateBookAppColor(bookSeedColor, theme)
        }
    } else {
        theme.appColor()
    }

    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(
        color = colorScheme.primary,
        darkIcons = theme.isLight
    )
    val textSelectionColors = remember(appColor.accent) {
        TextSelectionColors(
            handleColor = appColor.accent,
            backgroundColor = appColor.accent.copy(alpha = 0.3f)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colorScheme.onPrimary,
            LocalAppColor provides appColor,
            LocalTextSelectionColors provides textSelectionColors,
            content = content
        )
    }
}

/**
 * Single source of truth: each [Themes] entry maps to exactly one Material
 * [ColorScheme]. The matching [AppColor] is in [Themes.appColor].
 */
private fun Themes.colorScheme(): ColorScheme = when (this) {
    Themes.LIGHT -> light_colorScheme
    Themes.DARK -> dark_colorScheme
    Themes.BLACK -> black_colorScheme
    Themes.DARK_TEAL -> darkTeal_colorScheme
    Themes.SEPIA -> sepia_colorScheme
    Themes.OCEAN -> ocean_colorScheme
    Themes.FOREST -> forest_colorScheme
    Themes.SUNSET -> sunset_colorScheme
    Themes.PURPLE -> purple_colorScheme
}

private fun Themes.appColor(): AppColor = when (this) {
    Themes.LIGHT -> light_appColor
    Themes.DARK -> dark_appColor
    Themes.BLACK -> black_appColor
    Themes.DARK_TEAL -> darkTeal_appColor
    Themes.SEPIA -> sepia_appColor
    Themes.OCEAN -> ocean_appColor
    Themes.FOREST -> forest_appColor
    Themes.SUNSET -> sunset_appColor
    Themes.PURPLE -> purple_appColor
}

/**
 * AMOLED override: pure black backgrounds, kept on top of the chosen dark theme.
 * The accent comes from the underlying theme so AMOLED is just a "darker"
 * variant rather than its own separate palette.
 */
private fun amoledColorScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFF00FF88),
    onPrimary = Color(0xFF000000),
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF1A1A1A),
    secondary = Color(0xFF00DD77),
    tertiary = Color(0xFF00CC66),
)
