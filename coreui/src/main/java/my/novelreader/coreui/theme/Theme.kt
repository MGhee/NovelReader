package my.novelreader.coreui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
        content = content,
    )
}

@Composable
fun InternalTheme(
    theme: Themes = if (isSystemInDarkTheme()) Themes.DARK else Themes.LIGHT,
    bookSeedColor: Int? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = if (bookSeedColor != null) {
        remember(bookSeedColor, theme) {
            generateBookColorScheme(bookSeedColor, theme)
        }
    } else {
        when (theme) {
            Themes.LIGHT -> light_colorScheme
            Themes.DARK -> dark_colorScheme
            Themes.BLACK -> black_colorScheme
            Themes.DARK_TEAL -> darkTeal_colorScheme
            Themes.SEPIA -> sepia_colorScheme
        }
    }

    val appColor = if (bookSeedColor != null) {
        remember(bookSeedColor, theme) {
            generateBookAppColor(bookSeedColor, theme)
        }
    } else {
        when (theme) {
            Themes.LIGHT -> light_appColor
            Themes.DARK -> dark_appColor
            Themes.BLACK -> black_appColor
            Themes.DARK_TEAL -> darkTeal_appColor
            Themes.SEPIA -> sepia_appColor
        }
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