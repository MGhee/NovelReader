package my.novelreader.coreui.mappers

import my.novelreader.coreui.theme.Themes
import my.novelreader.core.appPreferences.PreferenceThemes

val PreferenceThemes.toTheme
    get() = when (this) {
        PreferenceThemes.Light -> Themes.LIGHT
        PreferenceThemes.Dark -> Themes.DARK
        PreferenceThemes.Black -> Themes.BLACK
    }

val Themes.toPreferenceTheme
    get() = when (this) {
        Themes.LIGHT -> PreferenceThemes.Light
        Themes.DARK -> PreferenceThemes.Dark
        Themes.BLACK -> PreferenceThemes.Black
    }